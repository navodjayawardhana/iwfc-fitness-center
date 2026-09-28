package com.iwfc.infrastructure.persistence.jdbc;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Creates the tables from {@code db/schema.sql}. Every statement is "IF NOT EXISTS", so it is safe on every start. */
public final class DatabaseSchema {

    private static final String SCRIPT = "db/schema.sql";

    private DatabaseSchema() {
    }

    public static void create(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SCRIPT));
        } catch (SQLException error) {
            throw new IllegalStateException("Could not create the database tables: " + error.getMessage(), error);
        }
    }
}
