package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.User;
import com.iwfc.domain.model.UserFactory;
import com.iwfc.domain.repository.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapter: users in the {@code users} table. */
public class JdbcUserRepository implements Repository<User, String> {

    private static final RowMapper<User> MAPPER = (row, number) ->
            UserFactory.restore(Role.valueOf(row.getString("role")), row.getString("id"), row.getString("name"),
                    row.getBoolean("active"));

    private final Database db;

    public JdbcUserRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "Cannot save null");
        String sql = existsById(user.id())
                ? "UPDATE users SET name = :name, role = :role, active = :active WHERE id = :id"
                : "INSERT INTO users (id, name, role, active) VALUES (:id, :name, :role, :active)";
        db.jdbc().sql(sql)
                .param("id", user.id())
                .param("name", user.name())
                .param("role", user.role().name())
                .param("active", user.isActive())
                .update();
    }

    @Override
    public Optional<User> findById(String id) {
        return db.jdbc().sql("SELECT id, name, role, active FROM users WHERE id = :id")
                .param("id", id).query(MAPPER).optional();
    }

    @Override
    public List<User> findAll() {
        return db.jdbc().sql("SELECT id, name, role, active FROM users ORDER BY seq").query(MAPPER).list();
    }

    @Override
    public boolean existsById(String id) {
        return db.exists("users", "id", id);
    }

    @Override
    public void deleteById(String id) {
        db.jdbc().sql("DELETE FROM users WHERE id = :id").param("id", id).update();
    }

    @Override
    public int count() {
        return db.count("users");
    }
}
