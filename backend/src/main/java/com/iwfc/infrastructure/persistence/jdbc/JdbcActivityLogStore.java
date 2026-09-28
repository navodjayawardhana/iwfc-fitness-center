package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.application.notification.ActivityLogStore;

import java.util.List;
import java.util.Objects;

/** Adapter: the maintenance activity log in the {@code activity_log} table, oldest first. */
public class JdbcActivityLogStore implements ActivityLogStore {

    private final Database db;

    public JdbcActivityLogStore(Database db) {
        this.db = db;
    }

    @Override
    public void append(String entry) {
        db.jdbc().sql("INSERT INTO activity_log (entry) VALUES (:entry)")
                .param("entry", Objects.requireNonNull(entry)).update();
    }

    @Override
    public List<String> entries() {
        return db.jdbc().sql("SELECT entry FROM activity_log ORDER BY id").query(String.class).list();
    }
}
