package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidStatusTransitionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity: a fault report for one piece of equipment. It owns its workflow
 * (Pending -> Assigned -> Completed) and records a {@link MaintenanceStatusChanged} event for every
 * change, which the application layer turns into notifications.
 *
 * <p>CMP 7001 mapping: Functional Requirement 3 — Maintenance Reporting: equipment id, description,
 * urgency (Low/Medium/High) and status workflow (Pending/Assigned/Completed).
 * Pattern-influenced design — State (behavioural): behaviour depends on {@link RequestStatus}, and an
 * illegal transition raises {@link InvalidStatusTransitionException} (LO3, LO4 discussion).</p>
 */
public class MaintenanceRequest {

    private final String id;
    private final String equipmentId;
    private final String description;
    private final Urgency urgency;
    private final User reportedBy;
    private RequestStatus status = RequestStatus.PENDING;
    private String assignedTo;
    private final List<String> progressNotes = new ArrayList<>();
    private final List<MaintenanceStatusChanged> events = new ArrayList<>();

    public MaintenanceRequest(String id, String equipmentId, String description, Urgency urgency, User reportedBy) {
        this(id, equipmentId, description, urgency, reportedBy, true);
        record(null);
    }

    private MaintenanceRequest(String id, String equipmentId, String description, Urgency urgency, User reportedBy,
                               boolean checkReporter) {
        this.reportedBy = Objects.requireNonNull(reportedBy, "A request needs a reporter");
        if (checkReporter) {
            reportedBy.ensureCanReportFaults();
        }
        this.id = requireText(id, "Request id");
        this.equipmentId = requireText(equipmentId, "Equipment id");
        this.description = requireText(description, "Description");
        this.urgency = Objects.requireNonNull(urgency, "Urgency is required");
    }

    /**
     * Rebuilds a request from stored data (used by the database adapter). No event is recorded, because
     * nothing has changed: the report was announced when it was first made.
     */
    public static MaintenanceRequest restore(String id, String equipmentId, String description, Urgency urgency,
                                             User reportedBy, RequestStatus status, String assignedTo,
                                             List<String> progressNotes) {
        Objects.requireNonNull(status, "Status is required");
        boolean hasTechnician = assignedTo != null && !assignedTo.isBlank();
        if ((status == RequestStatus.PENDING) == hasTechnician) {
            throw new IllegalArgumentException("A request has a technician exactly when it is no longer pending");
        }
        MaintenanceRequest restored = new MaintenanceRequest(id, equipmentId, description, urgency, reportedBy, false);
        restored.status = status;
        restored.assignedTo = hasTechnician ? assignedTo.trim() : null;
        restored.progressNotes.addAll(progressNotes);
        return restored;
    }

    public void assignTo(User administrator, String technician) {
        administrator.ensureCanManageMaintenance();
        requireStatus(RequestStatus.PENDING, "assigned");
        this.assignedTo = requireText(technician, "Technician name");
        RequestStatus before = status;
        status = RequestStatus.ASSIGNED;
        record(before);
    }

    public void updateProgress(User administrator, String note) {
        administrator.ensureCanManageMaintenance();
        requireStatus(RequestStatus.ASSIGNED, "given a progress update");
        progressNotes.add(requireText(note, "Progress note"));
    }

    public void complete(User administrator) {
        administrator.ensureCanManageMaintenance();
        requireStatus(RequestStatus.ASSIGNED, "completed");
        RequestStatus before = status;
        status = RequestStatus.COMPLETED;
        record(before);
    }

    /** Hands over the events recorded since the last call, so each one is published only once. */
    public List<MaintenanceStatusChanged> pullEvents() {
        List<MaintenanceStatusChanged> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    public boolean isClosed() {
        return status == RequestStatus.COMPLETED;
    }

    public String id() { return id; }
    public String equipmentId() { return equipmentId; }
    public String description() { return description; }
    public Urgency urgency() { return urgency; }
    public User reportedBy() { return reportedBy; }
    public RequestStatus status() { return status; }
    public Optional<String> assignedTo() { return Optional.ofNullable(assignedTo); }
    public List<String> progressNotes() { return Collections.unmodifiableList(progressNotes); }

    private void requireStatus(RequestStatus expected, String action) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(
                    "Request " + id + " is " + status + " and cannot be " + action + " (it must be " + expected + ")");
        }
    }

    private void record(RequestStatus previous) {
        events.add(new MaintenanceStatusChanged(id, equipmentId, previous, status, reportedBy.id()));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MaintenanceRequest that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + equipmentId + " [" + urgency + ", " + status + "] " + description;
    }
}
