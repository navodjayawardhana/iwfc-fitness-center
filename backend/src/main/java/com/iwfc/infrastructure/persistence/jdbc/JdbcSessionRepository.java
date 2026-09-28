package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter: sessions in {@code sessions}, their equipment in {@code session_equipment} and their bookings in
 * {@code bookings}. Saving rewrites the children in the same transaction, so a booking that was cancelled is gone.
 */
public class JdbcSessionRepository implements Repository<FitnessSession, String> {

    private record Row(String id, String title, String instructorId, String studio, LocalDateTime start,
                       LocalDateTime end, int capacity) {
    }

    private static final String COLUMNS = "id, title, instructor_id, studio, start_at, end_at, capacity";

    private static final RowMapper<Row> MAPPER = (row, number) -> new Row(
            row.getString("id"),
            row.getString("title"),
            row.getString("instructor_id"),
            row.getString("studio"),
            row.getObject("start_at", LocalDateTime.class),
            row.getObject("end_at", LocalDateTime.class),
            row.getInt("capacity"));

    private final Database db;
    private final Repository<User, String> users;
    private final Repository<Equipment, String> equipment;

    public JdbcSessionRepository(Database db, Repository<User, String> users, Repository<Equipment, String> equipment) {
        this.db = db;
        this.users = users;
        this.equipment = equipment;
    }

    @Override
    public void save(FitnessSession session) {
        Objects.requireNonNull(session, "Cannot save null");
        db.tx().executeWithoutResult(status -> {
            String sql = existsById(session.id())
                    ? "UPDATE sessions SET title = :title, instructor_id = :instructor, studio = :studio, "
                            + "start_at = :start, end_at = :end, capacity = :capacity WHERE id = :id"
                    : "INSERT INTO sessions (" + COLUMNS + ") VALUES (:id, :title, :instructor, :studio, :start, "
                            + ":end, :capacity)";
            db.jdbc().sql(sql)
                    .param("id", session.id())
                    .param("title", session.title())
                    .param("instructor", session.instructor().id())
                    .param("studio", session.studio().name())
                    .param("start", session.slot().start())
                    .param("end", session.slot().end())
                    .param("capacity", session.capacity())
                    .update();
            deleteChildren(session.id());
            int position = 0;
            for (Equipment item : session.equipment()) {
                db.jdbc().sql("INSERT INTO session_equipment (session_id, equipment_id, sort_order) "
                                + "VALUES (:session, :equipment, :position)")
                        .param("session", session.id()).param("equipment", item.id()).param("position", position++)
                        .update();
            }
            for (User member : session.bookedMembers()) {
                db.jdbc().sql("INSERT INTO bookings (session_id, user_id) VALUES (:session, :user)")
                        .param("session", session.id()).param("user", member.id()).update();
            }
        });
    }

    @Override
    public Optional<FitnessSession> findById(String id) {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM sessions WHERE id = :id")
                .param("id", id).query(MAPPER).optional().map(this::toSession);
    }

    @Override
    public List<FitnessSession> findAll() {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM sessions ORDER BY seq")
                .query(MAPPER).list().stream().map(this::toSession).toList();
    }

    @Override
    public boolean existsById(String id) {
        return db.exists("sessions", "id", id);
    }

    @Override
    public void deleteById(String id) {
        db.tx().executeWithoutResult(status -> {
            deleteChildren(id);
            db.jdbc().sql("DELETE FROM sessions WHERE id = :id").param("id", id).update();
        });
    }

    @Override
    public int count() {
        return db.count("sessions");
    }

    private void deleteChildren(String sessionId) {
        db.jdbc().sql("DELETE FROM bookings WHERE session_id = :id").param("id", sessionId).update();
        db.jdbc().sql("DELETE FROM session_equipment WHERE session_id = :id").param("id", sessionId).update();
    }

    private FitnessSession toSession(Row row) {
        User instructor = users.findById(row.instructorId()).orElseThrow(
                () -> new IllegalStateException("Stored session " + row.id() + " has an unknown instructor"));
        List<Equipment> items = db.jdbc()
                .sql("SELECT equipment_id FROM session_equipment WHERE session_id = :id ORDER BY sort_order")
                .param("id", row.id()).query(String.class).list().stream()
                .map(equipmentId -> equipment.findById(equipmentId).orElseThrow(
                        () -> new IllegalStateException("Stored session " + row.id() + " uses unknown equipment " + equipmentId)))
                .toList();
        List<User> members = db.jdbc().sql("SELECT user_id FROM bookings WHERE session_id = :id ORDER BY seq")
                .param("id", row.id()).query(String.class).list().stream()
                .map(userId -> users.findById(userId).orElseThrow(
                        () -> new IllegalStateException("Stored session " + row.id() + " has an unknown member " + userId)))
                .toList();
        return FitnessSession.restore(row.id(), row.title(), instructor, new Location(row.studio()),
                new TimeSlot(row.start(), row.end()), row.capacity(), items, members);
    }
}
