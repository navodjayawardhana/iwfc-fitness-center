package com.iwfc.infrastructure.persistence;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.persistence.jdbc.DatabaseSchema;
import com.iwfc.infrastructure.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The whole system on a database: sample data is seeded once, and everything survives an application restart. */
class DatabaseBootstrapTest {

    private static final Pbkdf2PasswordHasher FAST = new Pbkdf2PasswordHasher(1_000);

    private final DataSource database = TestStorages.h2("boot-" + UUID.randomUUID());

    private IwfcFacade start() {
        return IwfcBootstrap.database(database, FAST);
    }

    // Z
    @Test
    void should_create_the_tables_when_the_database_is_empty() {
        DatabaseSchema.create(database);

        assertDoesNotThrow(() -> DatabaseSchema.create(database));
    }

    @Test
    void should_explain_when_the_tables_cannot_be_created() {
        DataSource broken = new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:no-such-driver:nothing");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> DatabaseSchema.create(broken));

        assertTrue(error.getMessage().contains("Could not create the database tables"));
    }

    // O
    @Test
    void should_seed_sample_users_equipment_and_sessions_on_an_empty_database() {
        IwfcFacade system = start();
        User admin = system.findUser("A-1");

        assertEquals(5, system.listUsers(admin).size());
        assertEquals(8, system.listEquipment().size());
        assertEquals(3, system.availableSessions().size());
        assertEquals("Administrator", system.signIn("A-1", IwfcBootstrap.DEMO_PASSWORD).user().roleName());
    }

    // M
    @Test
    void should_not_seed_again_or_duplicate_anything_on_the_next_start() {
        start();

        IwfcFacade restarted = start();

        assertEquals(5, restarted.listUsers(restarted.findUser("A-1")).size());
        assertEquals(8, restarted.listEquipment().size());
        assertEquals(3, restarted.allSessions().size());
    }

    // I - everything written before the restart is there afterwards
    @Test
    void should_keep_accounts_bookings_faults_and_notifications_across_a_restart() {
        IwfcFacade first = start();
        User admin = first.findUser("A-1");
        first.registerUser(admin, Role.MEMBER, "M-9", "Mia Perera", "mia-secret-1");
        first.bookSession("S-1", first.findUser("M-1"));
        MaintenanceRequest fault = first.reportFault(first.findUser("I-1"), "SB-04", "Resistance failure", Urgency.HIGH);
        first.assignMaintenance(admin, fault.id(), "Technician Kamal");
        first.logEquipmentUsage(first.findUser("I-1"), "TM-01", 100);

        IwfcFacade restarted = start();
        User restartedAdmin = restarted.findUser("A-1");

        assertEquals("Mia Perera", restarted.signIn("M-9", "mia-secret-1").user().name());
        assertEquals(1, restarted.findSession("S-1").bookedCount());
        assertEquals(1, restarted.maintenanceRequests(restartedAdmin).size());
        assertEquals("Technician Kamal", restarted.maintenanceRequests(restartedAdmin).get(0).assignedTo().orElseThrow());
        assertEquals("UNDER_MAINTENANCE", restarted.findEquipment("SB-04").status().name());
        assertEquals(2, restarted.inbox(restarted.findUser("I-1")).size());
        assertTrue(restarted.inbox(restartedAdmin).stream().anyMatch(message -> message.contains("TM-01")));
        assertEquals(3, restarted.maintenanceActivityLog(restartedAdmin).size());
        assertEquals(1, restarted.inbox(restarted.findUser("M-1")).size());
    }

    @Test
    void should_keep_a_deactivated_account_deactivated_after_a_restart() {
        IwfcFacade first = start();
        first.deactivateUser(first.findUser("A-1"), "M-2");

        IwfcFacade restarted = start();

        assertThrows(RuntimeException.class, () -> restarted.signIn("M-2", IwfcBootstrap.DEMO_PASSWORD));
        assertFalse(restarted.findUser("M-2").isActive());
    }

    @Test
    void should_continue_request_numbers_after_a_restart() {
        IwfcFacade first = start();
        first.reportFault(first.findUser("I-1"), "TM-01", "Belt slipping", Urgency.LOW);

        IwfcFacade restarted = start();
        MaintenanceRequest next = restarted.reportFault(restarted.findUser("I-1"), "TM-02", "Noisy", Urgency.LOW);

        assertEquals("MR-002", next.id());
    }

    // E - the business rules are the same when a database sits underneath
    @Test
    void should_still_reject_a_double_booked_studio_on_a_database() {
        IwfcFacade system = start();
        TimeSlot yogaTime = system.findSession("S-1").slot();

        assertThrows(InvalidBookingException.class, () -> system.scheduleSession(system.findUser("I-2"), "CLASH",
                "Clashing class", new Location("Studio A"), yogaTime, 10, List.of()));
        assertEquals(3, system.allSessions().size());
    }

    @Test
    void should_still_reject_duplicate_equipment_and_unauthorized_access_on_a_database() {
        IwfcFacade system = start();

        assertThrows(DuplicateEquipmentException.class, () -> system.addEquipment(system.findUser("A-1"),
                EquipmentType.TREADMILL, "TM-01", "Copy", new Location("Cardio Zone")));
        assertThrows(UnauthorizedAccessException.class, () -> system.maintenanceActivityLog(system.findUser("M-1")));
    }

    // S
    @Test
    void should_cancel_a_session_on_the_database_and_tell_its_members() {
        IwfcFacade system = start();
        system.bookSession("S-2", system.findUser("M-2"));

        system.cancelSession(system.findUser("I-2"), "S-2");
        IwfcFacade restarted = start();

        assertEquals(2, restarted.allSessions().size());
        assertTrue(restarted.inbox(restarted.findUser("M-2")).stream().anyMatch(message -> message.contains("cancelled")));
    }
}
