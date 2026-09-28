package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentStatus;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.repository.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapter: equipment in the {@code equipment} table. Rows are rebuilt with {@link Equipment#restore}. */
public class JdbcEquipmentRepository implements Repository<Equipment, String> {

    private static final String COLUMNS = "id, name, type, location, status, active, total_usage_hours, "
            + "hours_since_maintenance, threshold_hours";

    private static final RowMapper<Equipment> MAPPER = (row, number) -> Equipment.restore(
            row.getString("id"),
            row.getString("name"),
            EquipmentType.valueOf(row.getString("type")),
            new Location(row.getString("location")),
            row.getDouble("threshold_hours"),
            EquipmentStatus.valueOf(row.getString("status")),
            row.getBoolean("active"),
            row.getDouble("total_usage_hours"),
            row.getDouble("hours_since_maintenance"));

    private final Database db;

    public JdbcEquipmentRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(Equipment equipment) {
        Objects.requireNonNull(equipment, "Cannot save null");
        String sql = existsById(equipment.id())
                ? "UPDATE equipment SET name = :name, type = :type, location = :location, status = :status, "
                        + "active = :active, total_usage_hours = :total, hours_since_maintenance = :since, "
                        + "threshold_hours = :threshold WHERE id = :id"
                : "INSERT INTO equipment (" + COLUMNS + ") VALUES (:id, :name, :type, :location, :status, :active, "
                        + ":total, :since, :threshold)";
        db.jdbc().sql(sql)
                .param("id", equipment.id())
                .param("name", equipment.name())
                .param("type", equipment.type().name())
                .param("location", equipment.location().name())
                .param("status", equipment.status().name())
                .param("active", equipment.isActive())
                .param("total", equipment.totalUsageHours())
                .param("since", equipment.hoursSinceMaintenance())
                .param("threshold", equipment.maintenanceThresholdHours())
                .update();
    }

    @Override
    public Optional<Equipment> findById(String id) {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM equipment WHERE id = :id")
                .param("id", id).query(MAPPER).optional();
    }

    @Override
    public List<Equipment> findAll() {
        return db.jdbc().sql("SELECT " + COLUMNS + " FROM equipment ORDER BY seq").query(MAPPER).list();
    }

    @Override
    public boolean existsById(String id) {
        return db.exists("equipment", "id", id);
    }

    @Override
    public void deleteById(String id) {
        db.jdbc().sql("DELETE FROM equipment WHERE id = :id").param("id", id).update();
    }

    @Override
    public int count() {
        return db.count("equipment");
    }
}
