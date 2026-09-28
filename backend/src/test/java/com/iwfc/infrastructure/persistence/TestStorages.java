package com.iwfc.infrastructure.persistence;

import com.iwfc.infrastructure.persistence.jdbc.JdbcStorage;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

/** Builds fresh, empty storage for tests: plain memory, or an H2 database running in MySQL mode. */
final class TestStorages {

    enum Kind { IN_MEMORY, H2 }

    private TestStorages() {
    }

    static Storage create(Kind kind) {
        return kind == Kind.IN_MEMORY ? Storage.inMemory() : JdbcStorage.create(freshH2());
    }

    /** A new database that no other test can see. */
    static DataSource freshH2() {
        return h2("test-" + UUID.randomUUID());
    }

    /** The same name always gives the same database, which is how a "restart" is simulated. */
    static DataSource h2(String name) {
        return new DriverManagerDataSource("jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
