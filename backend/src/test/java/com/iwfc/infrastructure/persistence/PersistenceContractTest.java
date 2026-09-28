package com.iwfc.infrastructure.persistence;

import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentStatus;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.RequestStatus;
import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.domain.model.UserFactory;
import com.iwfc.infrastructure.persistence.TestStorages.Kind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One contract, two implementations: the in-memory storage and the JDBC storage (on H2 in MySQL mode) must behave
 * the same. That is what lets the domain stay unaware of which one it is using.
 */
class PersistenceContractTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 10, 5, 9, 0);

    private final EquipmentFactory factory = new EquipmentFactory();
    private final Administrator admin = new Administrator("A-1", "Prasad");
    private final Instructor instructor = new Instructor("I-1", "Nimali");
    private final Member supun = new Member("M-1", "Supun");
    private final Member navod = new Member("M-2", "Navod");

    private Storage storage(Kind kind) {
        Storage storage = TestStorages.create(kind);
        List.of(admin, instructor, supun, navod).forEach(storage.users()::save);
        return storage;
    }

    private Equipment treadmill(String id) {
        return factory.create(EquipmentType.TREADMILL, id, "Treadmill " + id, new Location("Cardio Zone"));
    }

    // ---- equipment ------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_start_with_no_equipment(Kind kind) {
        Storage storage = storage(kind);

        assertEquals(0, storage.equipment().count());
        assertTrue(storage.equipment().findAll().isEmpty());
        assertTrue(storage.equipment().findById("TM-01").isEmpty());
        assertFalse(storage.equipment().existsById("TM-01"));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_store_every_field_of_equipment_and_read_it_back(Kind kind) {
        Storage storage = storage(kind);
        Equipment original = factory.create(EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Spin Studio"));
        original.logUsage(12.5);
        original.markFaulty();

        storage.equipment().save(original);
        Equipment loaded = storage.equipment().findById("SB-04").orElseThrow();

        assertEquals("Spin Bike 04", loaded.name());
        assertEquals(EquipmentType.SPIN_BIKE, loaded.type());
        assertEquals(new Location("Spin Studio"), loaded.location());
        assertEquals(EquipmentStatus.FAULTY, loaded.status());
        assertTrue(loaded.isActive());
        assertEquals(12.5, loaded.totalUsageHours());
        assertEquals(12.5, loaded.hoursSinceMaintenance());
        assertEquals(120, loaded.maintenanceThresholdHours());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_keep_equipment_in_the_order_it_was_saved(Kind kind) {
        Storage storage = storage(kind);
        storage.equipment().save(treadmill("TM-03"));
        storage.equipment().save(treadmill("TM-01"));
        storage.equipment().save(treadmill("TM-02"));

        List<String> ids = storage.equipment().findAll().stream().map(Equipment::id).toList();

        assertEquals(List.of("TM-03", "TM-01", "TM-02"), ids);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_replace_equipment_saved_again_with_the_same_id(Kind kind) {
        Storage storage = storage(kind);
        Equipment equipment = treadmill("TM-01");
        storage.equipment().save(equipment);

        equipment.rename("Treadmill Pro");
        equipment.relocate(new Location("Studio A"));
        equipment.deactivate();
        storage.equipment().save(equipment);

        assertEquals(1, storage.equipment().count());
        Equipment loaded = storage.equipment().findById("TM-01").orElseThrow();
        assertEquals("Treadmill Pro", loaded.name());
        assertEquals(new Location("Studio A"), loaded.location());
        assertFalse(loaded.isActive());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_delete_equipment_by_id_and_ignore_an_unknown_id(Kind kind) {
        Storage storage = storage(kind);
        storage.equipment().save(treadmill("TM-01"));

        storage.equipment().deleteById("TM-01");
        storage.equipment().deleteById("missing");

        assertEquals(0, storage.equipment().count());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_refuse_to_save_null(Kind kind) {
        assertThrows(NullPointerException.class, () -> storage(kind).equipment().save(null));
    }

    // ---- users and credentials ------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_store_the_role_and_the_deactivated_state_of_a_user(Kind kind) {
        Storage storage = storage(kind);
        User retired = UserFactory.create(Role.INSTRUCTOR, "I-9", "Retired");
        retired.deactivate();

        storage.users().save(retired);
        User loaded = storage.users().findById("I-9").orElseThrow();

        assertEquals("Instructor", loaded.roleName());
        assertFalse(loaded.isActive());
        assertEquals(5, storage.users().count());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_update_a_user_when_saved_again(Kind kind) {
        Storage storage = storage(kind);
        User member = storage.users().findById("M-1").orElseThrow();
        member.deactivate();

        storage.users().save(member);

        assertFalse(storage.users().findById("M-1").orElseThrow().isActive());
        assertEquals(4, storage.users().count());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_store_and_replace_a_credential_without_touching_other_users(Kind kind) {
        Storage storage = storage(kind);
        storage.credentials().save(new Credential("M-1", "pbkdf2$1000$c2FsdA==$aGFzaA=="));
        storage.credentials().save(new Credential("M-2", "pbkdf2$1000$c2FsdDI=$aGFzaDI="));

        storage.credentials().save(new Credential("M-1", "pbkdf2$1000$bmV3$bmV3aGFzaA=="));

        assertEquals("pbkdf2$1000$bmV3$bmV3aGFzaA==", storage.credentials().findById("M-1").orElseThrow().passwordHash());
        assertEquals("pbkdf2$1000$c2FsdDI=$aGFzaDI=", storage.credentials().findById("M-2").orElseThrow().passwordHash());
        assertEquals(2, storage.credentials().count());
    }

    // ---- maintenance requests -------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_store_a_maintenance_request_and_its_workflow_state(Kind kind) {
        Storage storage = storage(kind);
        storage.equipment().save(treadmill("TM-01"));
        MaintenanceRequest request = new MaintenanceRequest("MR-001", "TM-01", "Belt slipping", Urgency.HIGH, instructor);
        storage.requests().save(request);

        request.assignTo(admin, "Technician Kamal");
        request.updateProgress(admin, "Part ordered");
        request.updateProgress(admin, "Part fitted");
        storage.requests().save(request);
        MaintenanceRequest loaded = storage.requests().findById("MR-001").orElseThrow();

        assertEquals(1, storage.requests().count());
        assertEquals("TM-01", loaded.equipmentId());
        assertEquals("Belt slipping", loaded.description());
        assertEquals(Urgency.HIGH, loaded.urgency());
        assertEquals(RequestStatus.ASSIGNED, loaded.status());
        assertEquals("Technician Kamal", loaded.assignedTo().orElseThrow());
        assertEquals(List.of("Part ordered", "Part fitted"), loaded.progressNotes());
        assertEquals(instructor, loaded.reportedBy());
        assertTrue(loaded.pullEvents().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_let_a_loaded_request_carry_on_through_the_workflow(Kind kind) {
        Storage storage = storage(kind);
        storage.equipment().save(treadmill("TM-01"));
        MaintenanceRequest request = new MaintenanceRequest("MR-001", "TM-01", "Belt", Urgency.LOW, instructor);
        request.assignTo(admin, "Kamal");
        storage.requests().save(request);

        MaintenanceRequest loaded = storage.requests().findById("MR-001").orElseThrow();
        loaded.complete(admin);
        storage.requests().save(loaded);

        assertEquals(RequestStatus.COMPLETED, storage.requests().findById("MR-001").orElseThrow().status());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_list_requests_in_the_order_they_were_reported(Kind kind) {
        Storage storage = storage(kind);
        storage.equipment().save(treadmill("TM-01"));
        storage.requests().save(new MaintenanceRequest("MR-002", "TM-01", "Second", Urgency.LOW, instructor));
        storage.requests().save(new MaintenanceRequest("MR-001", "TM-01", "First", Urgency.LOW, instructor));

        assertEquals(List.of("MR-002", "MR-001"),
                storage.requests().findAll().stream().map(MaintenanceRequest::id).toList());
    }

    // ---- sessions -------------------------------------------------------------------------------

    private FitnessSession spin(Storage storage) {
        storage.equipment().save(factory.create(EquipmentType.SPIN_BIKE, "SB-01", "Spin Bike 01", new Location("Spin Studio")));
        storage.equipment().save(factory.create(EquipmentType.SPIN_BIKE, "SB-02", "Spin Bike 02", new Location("Spin Studio")));
        List<Equipment> bikes = List.of(storage.equipment().findById("SB-01").orElseThrow(),
                storage.equipment().findById("SB-02").orElseThrow());
        return new FitnessSession("S-1", "Spin Class", instructor, new Location("Spin Studio"),
                new TimeSlot(NINE, NINE.plusHours(1)), 8, bikes);
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_store_a_session_with_its_equipment_and_bookings_in_booking_order(Kind kind) {
        Storage storage = storage(kind);
        FitnessSession session = spin(storage);
        session.book(navod);
        session.book(supun);

        storage.sessions().save(session);
        FitnessSession loaded = storage.sessions().findById("S-1").orElseThrow();

        assertEquals("Spin Class", loaded.title());
        assertEquals(instructor, loaded.instructor());
        assertEquals(new Location("Spin Studio"), loaded.studio());
        assertEquals(new TimeSlot(NINE, NINE.plusHours(1)), loaded.slot());
        assertEquals(8, loaded.capacity());
        assertEquals(List.of("SB-01", "SB-02"), loaded.equipment().stream().map(Equipment::id).toList());
        assertEquals(List.of(navod, supun), loaded.bookedMembers());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_forget_a_booking_that_was_cancelled_before_saving(Kind kind) {
        Storage storage = storage(kind);
        FitnessSession session = spin(storage);
        session.book(supun);
        session.book(navod);
        storage.sessions().save(session);

        FitnessSession loaded = storage.sessions().findById("S-1").orElseThrow();
        loaded.cancel(supun);
        storage.sessions().save(loaded);

        assertEquals(List.of(navod), storage.sessions().findById("S-1").orElseThrow().bookedMembers());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_delete_a_session_together_with_its_bookings(Kind kind) {
        Storage storage = storage(kind);
        FitnessSession session = spin(storage);
        session.book(supun);
        storage.sessions().save(session);

        storage.sessions().deleteById("S-1");

        assertFalse(storage.sessions().existsById("S-1"));
        assertEquals(0, storage.sessions().count());
        assertEquals(2, storage.equipment().count());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_still_load_a_session_whose_equipment_has_since_become_faulty(Kind kind) {
        Storage storage = storage(kind);
        storage.sessions().save(spin(storage));
        Equipment bike = storage.equipment().findById("SB-01").orElseThrow();
        bike.markFaulty();
        storage.equipment().save(bike);

        FitnessSession loaded = storage.sessions().findById("S-1").orElseThrow();

        assertEquals(2, loaded.equipment().size());
        assertEquals(EquipmentStatus.FAULTY, loaded.equipment().get(0).status());
    }

    // ---- notifications and the activity log -----------------------------------------------------

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_keep_each_users_notifications_in_order(Kind kind) {
        Storage storage = storage(kind);

        storage.notifications().add("M-1", "first");
        storage.notifications().add("M-2", "other person");
        storage.notifications().add("M-1", "second");

        assertEquals(List.of("first", "second"), storage.notifications().messagesFor("M-1"));
        assertEquals(List.of("other person"), storage.notifications().messagesFor("M-2"));
        assertTrue(storage.notifications().messagesFor("nobody").isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void should_keep_activity_log_entries_in_order(Kind kind) {
        Storage storage = storage(kind);

        storage.activityLog().append("Request MR-001 reported for TM-01");
        storage.activityLog().append("ALERT: TM-01 needs preventative maintenance");

        assertEquals(List.of("Request MR-001 reported for TM-01", "ALERT: TM-01 needs preventative maintenance"),
                storage.activityLog().entries());
    }
}
