package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidEquipmentOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** ZOMBIES order: Zero, One, Many, Boundaries, Interface, Exceptions, Simple. */
class EquipmentTest {

    private static Equipment treadmill() {
        return new Equipment("TM-01", "Treadmill 01", EquipmentType.TREADMILL, new Location("Cardio Zone"), 100);
    }

    // Z - Zero
    @Test
    void should_start_with_zero_usage_hours_when_equipment_is_new() {
        Equipment equipment = treadmill();

        assertEquals(0.0, equipment.totalUsageHours());
        assertEquals(0.0, equipment.hoursSinceMaintenance());
    }

    @Test
    void should_not_need_maintenance_when_never_used() {
        assertFalse(treadmill().needsMaintenance());
    }

    // O - One
    @Test
    void should_accumulate_usage_when_one_session_is_logged() {
        Equipment equipment = treadmill();

        equipment.logUsage(2.5);

        assertEquals(2.5, equipment.totalUsageHours());
        assertEquals(2.5, equipment.hoursSinceMaintenance());
    }

    // M - Many
    @Test
    void should_add_up_usage_when_many_sessions_are_logged() {
        Equipment equipment = treadmill();

        equipment.logUsage(1.5);
        equipment.logUsage(2.0);
        equipment.logUsage(0.5);

        assertEquals(4.0, equipment.totalUsageHours());
    }

    // B - Boundaries
    @Test
    void should_need_maintenance_when_usage_reaches_the_threshold_exactly() {
        Equipment equipment = treadmill();

        equipment.logUsage(99.9);
        assertFalse(equipment.needsMaintenance());

        equipment.logUsage(0.1);
        assertTrue(equipment.needsMaintenance());
    }

    @Test
    void should_reset_hours_since_maintenance_but_keep_total_when_maintenance_is_completed() {
        Equipment equipment = treadmill();
        equipment.logUsage(120);
        equipment.startMaintenance();

        equipment.completeMaintenance();

        assertEquals(120, equipment.totalUsageHours());
        assertEquals(0, equipment.hoursSinceMaintenance());
        assertFalse(equipment.needsMaintenance());
        assertEquals(EquipmentStatus.OPERATIONAL, equipment.status());
    }

    // I - Interface (identity by id, edit and deactivate contract)
    @Test
    void should_be_the_same_equipment_when_ids_match() {
        Equipment a = treadmill();
        Equipment b = new Equipment("TM-01", "Renamed", EquipmentType.TREADMILL, new Location("Studio A"), 50);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_allow_rename_and_relocation_when_edited() {
        Equipment equipment = treadmill();

        equipment.rename("Treadmill Pro");
        equipment.relocate(new Location("Studio A"));

        assertEquals("Treadmill Pro", equipment.name());
        assertEquals(new Location("Studio A"), equipment.location());
    }

    @Test
    void should_not_be_available_when_deactivated() {
        Equipment equipment = treadmill();

        equipment.deactivate();

        assertFalse(equipment.isActive());
        assertFalse(equipment.isAvailableForSessions());
    }

    @Test
    void should_not_be_available_when_faulty_or_under_maintenance() {
        Equipment equipment = treadmill();

        equipment.markFaulty();
        assertFalse(equipment.isAvailableForSessions());

        equipment.startMaintenance();
        assertFalse(equipment.isAvailableForSessions());
    }

    // E - Exceptions
    @Test
    void should_reject_usage_when_hours_are_zero_or_negative() {
        Equipment equipment = treadmill();

        assertThrows(InvalidEquipmentOperationException.class, () -> equipment.logUsage(0));
        assertThrows(InvalidEquipmentOperationException.class, () -> equipment.logUsage(-1));
    }

    @Test
    void should_reject_usage_when_equipment_is_deactivated() {
        Equipment equipment = treadmill();
        equipment.deactivate();

        assertThrows(InvalidEquipmentOperationException.class, () -> equipment.logUsage(1));
    }

    @Test
    void should_reject_completing_maintenance_when_equipment_is_not_under_maintenance() {
        assertThrows(InvalidEquipmentOperationException.class, () -> treadmill().completeMaintenance());
    }

    @Test
    void should_reject_blank_id_or_name() {
        Location zone = new Location("Cardio Zone");

        assertThrows(InvalidEquipmentOperationException.class,
                () -> new Equipment(" ", "Name", EquipmentType.TREADMILL, zone, 100));
        assertThrows(InvalidEquipmentOperationException.class,
                () -> new Equipment("TM-02", "", EquipmentType.TREADMILL, zone, 100));
    }

    @Test
    void should_reject_blank_location() {
        assertThrows(InvalidEquipmentOperationException.class, () -> new Location(" "));
    }

    // E - Exceptions: status transitions must follow Operational -> Faulty -> Under Maintenance -> Operational
    @Test
    void should_reject_starting_maintenance_when_equipment_is_operational_and_not_due() {
        Equipment equipment = treadmill();

        assertThrows(InvalidEquipmentOperationException.class, equipment::startMaintenance);
        assertEquals(EquipmentStatus.OPERATIONAL, equipment.status());
    }

    @Test
    void should_reject_starting_maintenance_when_it_is_already_under_maintenance() {
        Equipment equipment = treadmill();
        equipment.markFaulty();
        equipment.startMaintenance();

        assertThrows(InvalidEquipmentOperationException.class, equipment::startMaintenance);
    }

    // B - the preventative path is the one allowed shortcut: Operational -> Under Maintenance when due
    @Test
    void should_allow_preventative_maintenance_when_operational_but_due() {
        Equipment equipment = treadmill();
        equipment.logUsage(100);

        equipment.startMaintenance();

        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipment.status());
    }

    @Test
    void should_keep_the_status_when_a_fault_is_reported_on_equipment_that_is_already_faulty() {
        Equipment equipment = treadmill();
        equipment.markFaulty();

        equipment.markFaulty();

        assertEquals(EquipmentStatus.FAULTY, equipment.status());
    }

    @Test
    void should_keep_the_status_when_a_fault_is_reported_during_maintenance() {
        Equipment equipment = treadmill();
        equipment.markFaulty();
        equipment.startMaintenance();

        equipment.markFaulty();

        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipment.status());
    }

    // S - the full status cycle
    @Test
    void should_move_through_faulty_under_maintenance_and_back_to_operational() {
        Equipment equipment = treadmill();
        assertEquals(EquipmentStatus.OPERATIONAL, equipment.status());

        equipment.markFaulty();
        assertEquals(EquipmentStatus.FAULTY, equipment.status());

        equipment.startMaintenance();
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, equipment.status());

        equipment.completeMaintenance();
        assertEquals(EquipmentStatus.OPERATIONAL, equipment.status());
    }

    // S - Simple / happy path
    @Test
    void should_be_available_when_active_and_operational() {
        Equipment equipment = treadmill();

        assertTrue(equipment.isAvailableForSessions());
        assertEquals(EquipmentStatus.OPERATIONAL, equipment.status());
    }
}
