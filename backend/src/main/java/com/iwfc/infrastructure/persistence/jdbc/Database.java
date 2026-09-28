package com.iwfc.infrastructure.persistence.jdbc;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/** The two things every JDBC repository needs: a client for SQL and a way to run several statements as one unit. */
public record Database(JdbcClient jdbc, TransactionTemplate tx) {

    public static Database of(DataSource dataSource) {
        return new Database(JdbcClient.create(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    /** True when the table has a row with this id. Table names come from constants in the repositories. */
    boolean exists(String table, String idColumn, String id) {
        Integer found = jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return found != null && found > 0;
    }

    int count(String table) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM " + table).query(Integer.class).single();
        return total == null ? 0 : total;
    }
}
