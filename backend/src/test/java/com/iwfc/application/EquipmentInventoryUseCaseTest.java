package com.iwfc.application;

import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.InvalidEquipmentOperationException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Use cases only orchestrate: rules live in the domain, storage is an in-memory repository. */
class EquipmentInventoryUseCaseTest {

    private final Repository<Equipment, String> repository = new InMemoryRepository<>(Equipment::id);
    private final NotificationService notifications = new NotificationService();
    private final AdminMaintenanceLog log = new AdminMaintenanceLog();
    private final EquipmentInventoryUseCase inventory =
            new EquipmentInventoryUseCase(repository, new EquipmentFactory(), notifications);
    private final Administrator admin = new Administrator("A-1", "Prasad");
    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Location cardio = new Location("Cardio Zone");

    private void register(String id) {
        inventory.add(admin, EquipmentType.TREADMILL, id, "Treadmill " + id, cardio);
    }

    // Z
    @Test
    void should_list_no_equipment_when_inventory_is_empty() {
        assertTrue(inventory.listAll().isEmpty());
    }

    // O
    @Test
    void should_register_equipment_when_an_administrator_adds_it() {
        Equipment added = inventory.add(admin, EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", cardio);

        assertEquals("SB-04", added.id());
        assertEquals(1, inventory.listAll().size());
    }

    // M
    @Test
    void should_list_all_registered_equipment() {
        register("TM-01");
        register("TM-02");
        register("TM-03");

        assertEquals(3, inventory.listAll().size());
    }

    // B - the maintenance alert threshold
    @Test
    void should_raise_a_maintenance_alert_when_usage_reaches_the_threshold() {
        notifications.subscribe(log);
        register("TM-01");

        inventory.logUsage(instructor, "TM-01", 99);
        assertTrue(log.entries().isEmpty());

        inventory.logUsage(instructor, "TM-01", 1);

        assertEquals(1, log.entries().size());
        assertTrue(log.entries().get(0).contains("TM-01"));
    }

    // I
    @Test
    void should_edit_the_name_and_location_of_registered_equipment() {
        register("TM-01");

        inventory.edit(admin, "TM-01", "Treadmill Pro", new Location("Studio A"));

        Equipment edited = inventory.find("TM-01");
        assertEquals("Treadmill Pro", edited.name());
        assertEquals(new Location("Studio A"), edited.location());
    }

    @Test
    void should_deactivate_equipment_without_deleting_it() {
        register("TM-01");

        inventory.deactivate(admin, "TM-01");

        assertFalse(inventory.find("TM-01").isActive());
        assertEquals(1, inventory.listAll().size());
    }

    // E - the three mandatory custom exceptions plus not-found
    @Test
    void should_throw_duplicate_equipment_when_the_id_is_already_registered() {
        register("TM-01");

        DuplicateEquipmentException error = assertThrows(DuplicateEquipmentException.class, () -> register("TM-01"));

        assertTrue(error.getMessage().contains("TM-01"));
        assertEquals(1, inventory.listAll().size());
    }

    @Test
    void should_throw_unauthorized_when_a_member_tries_to_add_equipment() {
        Member member = new Member("M-1", "Supun");

        assertThrows(UnauthorizedAccessException.class,
                () -> inventory.add(member, EquipmentType.TREADMILL, "TM-09", "Treadmill", cardio));
        assertTrue(inventory.listAll().isEmpty());
    }

    @Test
    void should_throw_unauthorized_when_an_instructor_deactivates_equipment() {
        register("TM-01");

        assertThrows(UnauthorizedAccessException.class, () -> inventory.deactivate(instructor, "TM-01"));
    }

    @Test
    void should_throw_unauthorized_when_a_member_logs_usage() {
        register("TM-01");

        assertThrows(UnauthorizedAccessException.class,
                () -> inventory.logUsage(new Member("M-1", "Supun"), "TM-01", 1));
    }

    @Test
    void should_throw_not_found_when_the_equipment_does_not_exist() {
        assertThrows(ResourceNotFoundException.class, () -> inventory.find("missing"));
        assertThrows(ResourceNotFoundException.class, () -> inventory.deactivate(admin, "missing"));
    }

    @Test
    void should_throw_when_usage_is_logged_on_deactivated_equipment() {
        register("TM-01");
        inventory.deactivate(admin, "TM-01");

        assertThrows(InvalidEquipmentOperationException.class, () -> inventory.logUsage(instructor, "TM-01", 1));
    }

    // S
    @Test
    void should_track_cumulative_usage_across_many_sessions() {
        register("TM-01");

        inventory.logUsage(instructor, "TM-01", 1.5);
        inventory.logUsage(instructor, "TM-01", 2.5);

        assertEquals(4.0, inventory.find("TM-01").totalUsageHours());
    }
}
