package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidEquipmentOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Factory pattern (creational): the only way the rest of the system creates Equipment. */
class EquipmentFactoryTest {

    private final EquipmentFactory factory = new EquipmentFactory();
    private final Location cardio = new Location("Cardio Zone");

    // Z / O
    @Test
    void should_create_operational_active_equipment_when_type_is_given() {
        Equipment bike = factory.create(EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", cardio);

        assertEquals("SB-04", bike.id());
        assertEquals(EquipmentStatus.OPERATIONAL, bike.status());
        assertTrue(bike.isActive());
        assertEquals(0.0, bike.totalUsageHours());
    }

    // M
    @Test
    void should_apply_a_different_maintenance_threshold_when_type_differs() {
        Equipment treadmill = factory.create(EquipmentType.TREADMILL, "TM-01", "Treadmill", cardio);
        Equipment rower = factory.create(EquipmentType.ROWING_MACHINE, "RM-01", "Rower", cardio);

        assertEquals(100, treadmill.maintenanceThresholdHours());
        assertEquals(150, rower.maintenanceThresholdHours());
    }

    // B
    @Test
    void should_use_a_custom_threshold_when_one_is_supplied() {
        Equipment treadmill = factory.create(EquipmentType.TREADMILL, "TM-02", "Treadmill", cardio, 40);

        assertEquals(40, treadmill.maintenanceThresholdHours());
    }

    // E
    @Test
    void should_reject_creation_when_type_is_missing() {
        assertThrows(InvalidEquipmentOperationException.class,
                () -> factory.create(null, "X-01", "Unknown", cardio));
    }

    @Test
    void should_reject_creation_when_id_is_blank() {
        assertThrows(InvalidEquipmentOperationException.class,
                () -> factory.create(EquipmentType.TREADMILL, " ", "Treadmill", cardio));
    }

    // S
    @Test
    void should_create_equipment_in_the_requested_location() {
        Equipment monitor = factory.create(EquipmentType.HEART_RATE_MONITOR, "HR-09", "HR Monitor", new Location("Studio A"));

        assertEquals(new Location("Studio A"), monitor.location());
    }
}
