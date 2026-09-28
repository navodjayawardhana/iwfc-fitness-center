package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.domain.model.Credential;
import com.iwfc.domain.repository.Repository;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapter: password hashes in the {@code credentials} table, keyed by user id. */
public class JdbcCredentialRepository implements Repository<Credential, String> {

    private static final RowMapper<Credential> MAPPER =
            (row, number) -> new Credential(row.getString("user_id"), row.getString("password_hash"));

    private final Database db;

    public JdbcCredentialRepository(Database db) {
        this.db = db;
    }

    @Override
    public void save(Credential credential) {
        Objects.requireNonNull(credential, "Cannot save null");
        String sql = existsById(credential.userId())
                ? "UPDATE credentials SET password_hash = :hash WHERE user_id = :id"
                : "INSERT INTO credentials (user_id, password_hash) VALUES (:id, :hash)";
        db.jdbc().sql(sql).param("id", credential.userId()).param("hash", credential.passwordHash()).update();
    }

    @Override
    public Optional<Credential> findById(String userId) {
        return db.jdbc().sql("SELECT user_id, password_hash FROM credentials WHERE user_id = :id")
                .param("id", userId).query(MAPPER).optional();
    }

    @Override
    public List<Credential> findAll() {
        return db.jdbc().sql("SELECT user_id, password_hash FROM credentials ORDER BY user_id").query(MAPPER).list();
    }

    @Override
    public boolean existsById(String userId) {
        return db.exists("credentials", "user_id", userId);
    }

    @Override
    public void deleteById(String userId) {
        db.jdbc().sql("DELETE FROM credentials WHERE user_id = :id").param("id", userId).update();
    }

    @Override
    public int count() {
        return db.count("credentials");
    }
}
