package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.RequestStatus;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter: maintenance requests in {@code maintenance_requests}, with their progress notes in
 * {@code maintenance_notes}. Saving replaces the notes in the same transaction as the request.
 */
public class JdbcMaintenanceRequestRepository implements Repository<MaintenanceRequest, String> {

    private record Row(String id, String equipmentId, String description, Urgency urgency, RequestStatus status,
                       String reportedBy, String assignedTo) {
    }

    private static final String COLUMNS = "id, equipment_id, description, urgency, status, reported_by, assigned_to";

    private static final RowMapper<Row> MAPPER = (row, number) -> new Row(
            row.getString("id"),
            row.getString("equipment_id"),
            row.getString("description"),
            Urgency.valueOf(row.getString("urgency")),
            RequestStatus.valueOf(row.getString("status")),
            row.getString("reported_by"),
            row.getString("assigned_to"));

    private final Database db;
    private final Repository<User, String> users;

    public JdbcMaintenanceRequestRepository(Database db, Repository<User, String> users) {
        this.db = db;
        this.users = users;
    }

    @Override
    public void save(MaintenanceRequest request) {
        Objects.requireNonNull(request, "Cannot save null");
        db.tx().executeWithoutResult(status -> {
            String sql = existsById(request.id())
                    ? "UPDATE maintenance_requests SET equipment_id = :equipment, description = :description, "
                            + "urgency = :urgency, status = :status, reported_by = :reporter, assigned_to = :assigned "
                            + "WHERE id = :id"
                    : "INSERT INTO maintenance_requests (" + COLUMNS + ") VALUES (:id, :equipment, :description, "
                            + ":urgency, :status, :reporter, :assigned)";
            db.jdbc().sql(sql)
                    .param("id", request.id())
                    .param("equipment", request.equipmentId())
                    .param("description", request.description())
                    .param("urgency", request.urgency().name())
                    .param("status", request.status().name())
                    .param("reporter", request.reportedBy().id())
                    .param("assigned", request.assignedTo().orElse(null))
                    .update();
            db.jdbc().sql("DELETE FROM maintenance_notes WHERE request_id = :id").param("id", request.id()).update();
            for (String note : request.progressNotes()) {
                db.jdbc().sql("INSERT INTO maintenance_notes (request_id, note) VALUES (:id, :note)")
                        .param("id", request.id()).param("note", note).update();
            }
        });
    }

    @Override
    public Optional<MaintenanceRequest> findById(String id) {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM maintenance_requests WHERE id = :id")
                .param("id", id).query(MAPPER).optional().map(this::toRequest);
    }

    @Override
    public List<MaintenanceRequest> findAll() {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM maintenance_requests ORDER BY seq")
                .query(MAPPER).list().stream().map(this::toRequest).toList();
    }

    @Override
    public boolean existsById(String id) {
        return db.exists("maintenance_requests", "id", id);
    }

    @Override
    public void deleteById(String id) {
        db.jdbc().sql("DELETE FROM maintenance_requests WHERE id = :id").param("id", id).update();
    }

    @Override
    public int count() {
        return db.count("maintenance_requests");
    }

    private MaintenanceRequest toRequest(Row row) {
        User reporter = users.findById(row.reportedBy())
                .orElseThrow(() -> new IllegalStateException("Stored request " + row.id() + " has an unknown reporter"));
        List<String> notes = db.jdbc().sql("SELECT note FROM maintenance_notes WHERE request_id = :id ORDER BY seq")
                .param("id", row.id()).query(String.class).list();
        return MaintenanceRequest.restore(row.id(), row.equipmentId(), row.description(), row.urgency(), reporter,
                row.status(), row.assignedTo(), notes);
    }
}
