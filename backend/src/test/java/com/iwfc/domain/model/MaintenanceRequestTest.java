package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidStatusTransitionException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Maintenance workflow: Pending -> Assigned -> Completed, with an event for every change. */
class MaintenanceRequestTest {

    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Administrator admin = new Administrator("A-1", "Prasad");

    private MaintenanceRequest request() {
        return new MaintenanceRequest("MR-1", "SB-04", "Spin bike 04 resistance failure", Urgency.HIGH, instructor);
    }

    // Z
    @Test
    void should_start_as_pending_with_no_technician_when_reported() {
        MaintenanceRequest request = request();

        assertEquals(RequestStatus.PENDING, request.status());
        assertTrue(request.assignedTo().isEmpty());
        assertTrue(request.progressNotes().isEmpty());
    }

    // O
    @Test
    void should_keep_the_reported_details() {
        MaintenanceRequest request = request();

        assertEquals("SB-04", request.equipmentId());
        assertEquals("Spin bike 04 resistance failure", request.description());
        assertEquals(Urgency.HIGH, request.urgency());
        assertEquals(instructor, request.reportedBy());
    }

    @Test
    void should_become_assigned_when_the_administrator_assigns_a_technician() {
        MaintenanceRequest request = request();

        request.assignTo(admin, "Technician Kamal");

        assertEquals(RequestStatus.ASSIGNED, request.status());
        assertEquals("Technician Kamal", request.assignedTo().orElseThrow());
    }

    // M
    @Test
    void should_record_progress_notes_in_order_while_assigned() {
        MaintenanceRequest request = request();
        request.assignTo(admin, "Kamal");

        request.updateProgress(admin, "Ordered replacement part");
        request.updateProgress(admin, "Part fitted, testing");

        assertEquals(List.of("Ordered replacement part", "Part fitted, testing"), request.progressNotes());
    }

    @Test
    void should_produce_one_event_for_each_status_change() {
        MaintenanceRequest request = request();
        request.assignTo(admin, "Kamal");
        request.complete(admin);

        List<MaintenanceStatusChanged> events = request.pullEvents();

        assertEquals(3, events.size());
        assertNull(events.get(0).previous());
        assertEquals(RequestStatus.PENDING, events.get(0).current());
        assertEquals(RequestStatus.ASSIGNED, events.get(1).current());
        assertEquals(RequestStatus.COMPLETED, events.get(2).current());
    }

    // B
    @Test
    void should_clear_the_events_once_they_are_pulled() {
        MaintenanceRequest request = request();
        request.pullEvents();

        assertTrue(request.pullEvents().isEmpty());
    }

    @Test
    void should_carry_reporter_and_equipment_in_the_event() {
        MaintenanceStatusChanged event = request().pullEvents().get(0);

        assertEquals("MR-1", event.requestId());
        assertEquals("SB-04", event.equipmentId());
        assertEquals("I-1", event.reportedById());
    }

    // I
    @Test
    void should_be_equal_when_ids_match() {
        assertEquals(request(), new MaintenanceRequest("MR-1", "TM-01", "Other", Urgency.LOW, instructor));
    }

    // E - transitions and authorisation
    @Test
    void should_reject_completing_a_request_that_is_still_pending() {
        assertThrows(InvalidStatusTransitionException.class, () -> request().complete(admin));
    }

    @Test
    void should_reject_assigning_a_request_twice() {
        MaintenanceRequest request = request();
        request.assignTo(admin, "Kamal");

        assertThrows(InvalidStatusTransitionException.class, () -> request.assignTo(admin, "Nimal"));
    }

    @Test
    void should_reject_any_change_once_completed() {
        MaintenanceRequest request = request();
        request.assignTo(admin, "Kamal");
        request.complete(admin);

        assertThrows(InvalidStatusTransitionException.class, () -> request.complete(admin));
        assertThrows(InvalidStatusTransitionException.class, () -> request.updateProgress(admin, "late note"));
    }

    @Test
    void should_reject_progress_notes_before_a_technician_is_assigned() {
        assertThrows(InvalidStatusTransitionException.class, () -> request().updateProgress(admin, "too early"));
    }

    @Test
    void should_reject_assigning_when_the_technician_name_is_blank() {
        assertThrows(IllegalArgumentException.class, () -> request().assignTo(admin, " "));
    }

    @Test
    void should_reject_a_member_reporting_a_fault() {
        assertThrows(UnauthorizedAccessException.class,
                () -> new MaintenanceRequest("MR-2", "SB-04", "Broken", Urgency.LOW, new Member("M-1", "Supun")));
    }

    @Test
    void should_reject_an_instructor_assigning_or_completing_requests() {
        MaintenanceRequest request = request();

        assertThrows(UnauthorizedAccessException.class, () -> request.assignTo(instructor, "Kamal"));

        request.assignTo(admin, "Kamal");
        assertThrows(UnauthorizedAccessException.class, () -> request.complete(instructor));
    }

    @Test
    void should_reject_a_request_without_description_or_equipment() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceRequest("MR-3", "SB-04", " ", Urgency.LOW, instructor));
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceRequest("MR-3", null, "Broken", Urgency.LOW, instructor));
    }

    // S - full happy path
    @Test
    void should_complete_the_whole_workflow_when_every_step_is_allowed() {
        MaintenanceRequest request = request();

        request.assignTo(admin, "Kamal");
        request.updateProgress(admin, "Fixed");
        request.complete(admin);

        assertEquals(RequestStatus.COMPLETED, request.status());
        assertTrue(request.isClosed());
    }
}
