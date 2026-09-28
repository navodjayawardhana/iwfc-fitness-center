package com.iwfc.infrastructure.persistence.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

/** Reads the MySQL connection settings from environment variables, so no credentials live in code or in git. */
public final class DataSources {

    static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/fitpulse?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
                    + "&useSSL=false&allowPublicKeyRetrieval=true";
    static final String DEFAULT_USER = "fitpulse";

    private DataSources() {
    }

    /** Connection settings. The password is left out of the text form. */
    public record DbSettings(String url, String user, String password) {
        @Override
        public String toString() {
            return "DbSettings[url=" + url + ", user=" + user + "]";
        }
    }

    /**
     * FITPULSE_DB_URL and FITPULSE_DB_USER have local defaults; FITPULSE_DB_PASSWORD has none and must be set.
     */
    public static DbSettings settings(Map<String, String> environment) {
        String password = environment.get("FITPULSE_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "FITPULSE_STORAGE=mysql needs the database password in the FITPULSE_DB_PASSWORD environment variable");
        }
        return new DbSettings(
                valueOrDefault(environment.get("FITPULSE_DB_URL"), DEFAULT_URL),
                valueOrDefault(environment.get("FITPULSE_DB_USER"), DEFAULT_USER),
                password);
    }

    public static DataSource mysql(DbSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.url());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(5);
        config.setPoolName("fitpulse");
        return new HikariDataSource(config);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
