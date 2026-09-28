package com.iwfc.infrastructure.persistence.jdbc;

import com.iwfc.infrastructure.persistence.Storage;

import javax.sql.DataSource;

/** Builds the database version of {@link Storage}, creating the tables first if they are not there yet. */
public final class JdbcStorage {

    private JdbcStorage() {
    }

    public static Storage create(DataSource dataSource) {
        DatabaseSchema.create(dataSource);
        Database db = Database.of(dataSource);
        JdbcUserRepository users = new JdbcUserRepository(db);
        JdbcEquipmentRepository equipment = new JdbcEquipmentRepository(db);
        return new Storage(
                users,
                new JdbcCredentialRepository(db),
                equipment,
                new JdbcMaintenanceRequestRepository(db, users),
                new JdbcSessionRepository(db, users, equipment),
                new JdbcNotificationStore(db),
                new JdbcActivityLogStore(db));
    }
}
