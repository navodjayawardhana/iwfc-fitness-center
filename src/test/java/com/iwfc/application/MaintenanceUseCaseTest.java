package com.iwfc.application;

import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.notification.ReporterNotifier;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.application.usecase.MaintenanceUseCase;
import com.iwfc.domain.exception.InvalidStatusTransitionException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentStatus;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.RequestStatus;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceUseCaseTest {

    private final Repository<Equipment, String> equipment = new InMemoryRepository<>(Equipment::id);
    private final Repository<MaintenanceRequest, String> requests = new InMemoryRepository<>(MaintenanceRequest::id);
    private final NotificationService notifications = new NotificationService();
    private final AdminMaintenanceLog log = new AdminMaintenanceLog();
    private final EquipmentInventoryUseCase inventory =
            new EquipmentInventoryUseCase(equipment, new EquipmentFactory(), notifications);
    private final MaintenanceUseCase maintenance = new MaintenanceUseCase(requests, equipment, notifications);
    private final Administrator admin = new Administrator("A-1", "Prasad");
    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Member member = new Member("M-1", "Supun");

    @BeforeEach
    void setUp() {
        inventory.add(admin, EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Cardio Zone"));
        notifications.subscribe(new ReporterNotifier(notifications));
        notifications.subscribe(log);
    }

    private MaintenanceRequest report() {
        return maintenance.report(instructor, "SB-04", "Resistance failure", Urgency.HIGH);
    }

    // Z
    @Test
    void should_show_an_empty_log_when_nothing_has_been_reported() {
        assertTrue(maintenance.allRequests(admin).isEmpty());
    }

    // O
    @Test
    void should_create_a_pending_request_and_mark_the_equipment_faulty_when_a_fault_is_reported() {
        MaintenanceRequest request = report();

        assertEquals(RequestStatus.PENDING, request.status());
        assertEquals(EquipmentStatus.FAULTY, inventory.find("SB-04").status());
    }

    @Test
    void should_notify_the_reporting_instructor_when_the_fault_is_reported() {
        report();

        assertEquals(1, notifications.inboxOf("I-1").size());
    }

    // M
    @Test
    void should_give_each_request_a_new_id() {
        MaintenanceRequest first = report();
        inventory.add(admin, EquipmentType.TREADMILL, "TM-01", "Treadmill", new Location("Cardio Zone"));
        MaintenanceRequest second = maintenance.report(instructor, "TM-01", "Belt slipping", Urgency.LOW);

        assertNotEquals(first.id(), second.id());
        assertEquals(2, maintenance.allRequests(admin).size());
    }

    // B - the full workflow and the notification at every change
    @Test
    void should_notify_the_instructor_at_every_step_of_the_workflow() {
        MaintenanceRequest request = report();

        maintenance.assign(admin, request.id(), "Technician Kamal");
        maintenance.updateProgress(admin, request.id(), "Part ordered");
        maintenance.complete(admin, request.id());

        List<String> inbox = notifications.inboxOf("I-1");
        assertEquals(3, inbox.size());
        assertTrue(inbox.get(1).contains("ASSIGNED"));
        assertTrue(inbox.get(2).contains("COMPLETED"));
    }

    @Test
    void should_put_equipment_under_maintenance_when_assigned_and_operational_when_completed() {
        MaintenanceRequest request = report();

        maintenance.assign(admin, request.id(), "Kamal");
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, inventory.find("SB-04").status());

        maintenance.complete(admin, request.id());
        assertEquals(EquipmentStatus.OPERATIONAL, inventory.find("SB-04").status());
    }

    // I
    @Test
    void should_record_every_change_in_the_administrators_log() {
        MaintenanceRequest request = report();
        maintenance.assign(admin, request.id(), "Kamal");

        assertEquals(2, log.entries().size());
    }

    @Test
    void should_show_an_instructor_only_their_own_requests() {
        report();
        Instructor other = new Instructor("I-2", "Other");

        assertEquals(1, maintenance.requestsReportedBy(instructor).size());
        assertTrue(maintenance.requestsReportedBy(other).isEmpty());
    }

    // E
    @Test
    void should_throw_unauthorized_when_a_member_opens_the_maintenance_log() {
        report();

        assertThrows(UnauthorizedAccessException.class, () -> maintenance.allRequests(member));
    }

    @Test
    void should_throw_unauthorized_when_an_instructor_opens_the_maintenance_log() {
        assertThrows(UnauthorizedAccessException.class, () -> maintenance.allRequests(instructor));
    }

    @Test
    void should_throw_unauthorized_when_a_member_reports_a_fault() {
        assertThrows(UnauthorizedAccessException.class,
                () -> maintenance.report(member, "SB-04", "Squeaky", Urgency.LOW));
    }

    @Test
    void should_throw_invalid_transition_when_completing_a_request_that_was_never_assigned() {
        MaintenanceRequest request = report();

        assertThrows(InvalidStatusTransitionException.class, () -> maintenance.complete(admin, request.id()));
        assertEquals(EquipmentStatus.FAULTY, inventory.find("SB-04").status());
    }

    @Test
    void should_throw_not_found_for_unknown_equipment_or_request() {
        assertThrows(ResourceNotFoundException.class,
                () -> maintenance.report(instructor, "NOPE", "Broken", Urgency.LOW));
        assertThrows(ResourceNotFoundException.class, () -> maintenance.assign(admin, "MR-999", "Kamal"));
    }

    // S
    @Test
    void should_keep_the_technician_and_progress_notes_on_the_request() {
        MaintenanceRequest request = report();

        maintenance.assign(admin, request.id(), "Technician Kamal");
        maintenance.updateProgress(admin, request.id(), "Part fitted");

        MaintenanceRequest stored = maintenance.find(admin, request.id());
        assertEquals("Technician Kamal", stored.assignedTo().orElseThrow());
        assertEquals(List.of("Part fitted"), stored.progressNotes());
    }
}
