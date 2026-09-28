package com.iwfc.infrastructure.web;

import com.iwfc.application.security.AuthSession;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.User;

import java.util.List;

/** JSON shapes of the REST API. Times travel as ISO strings, so the domain types never leak into the API. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record LoginRequest(String userId, String password) { }

    public record LoginResponse(String token, String expiresAt, UserResponse user) {
        static LoginResponse from(AuthSession session) {
            return new LoginResponse(session.token(), session.expiresAt().toString(), UserResponse.from(session.user()));
        }
    }

    public record CreateUserRequest(String id, String name, String role, String password) { }

    public record UserResponse(String id, String name, String role, boolean active) {
        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.name(), user.roleName(), user.isActive());
        }
    }

    public record EquipmentRequest(String type, String id, String name, String location) { }

    public record EquipmentEditRequest(String name, String location) { }

    public record UsageRequest(double hours) { }

    public record EquipmentResponse(String id, String name, String type, String location, String status,
                                    boolean active, double totalUsageHours, double hoursSinceMaintenance,
                                    double maintenanceThresholdHours, boolean needsMaintenance) {
        static EquipmentResponse from(Equipment equipment) {
            return new EquipmentResponse(equipment.id(), equipment.name(), equipment.type().name(),
                    equipment.location().name(), equipment.status().name(), equipment.isActive(),
                    equipment.totalUsageHours(), equipment.hoursSinceMaintenance(),
                    equipment.maintenanceThresholdHours(), equipment.needsMaintenance());
        }
    }

    public record SessionRequest(String id, String title, String studio, String date, String start, String end,
                                 int capacity, List<String> equipmentIds, Integer weeks) { }

    public record SessionResponse(String id, String title, String instructorId, String instructorName, String studio,
                                  String start, String end, int capacity, int booked, int availableSpots,
                                  List<String> equipmentIds) {
        static SessionResponse from(FitnessSession session) {
            return new SessionResponse(session.id(), session.title(), session.instructor().id(),
                    session.instructor().name(), session.studio().name(), session.slot().start().toString(),
                    session.slot().end().toString(), session.capacity(), session.bookedCount(),
                    session.availableSpots(), session.equipment().stream().map(Equipment::id).toList());
        }
    }

    public record FaultRequest(String equipmentId, String description, String urgency) { }

    public record AssignRequest(String technician) { }

    public record ProgressRequest(String note) { }

    public record MaintenanceResponse(String id, String equipmentId, String description, String urgency,
                                      String status, String reportedBy, String assignedTo,
                                      List<String> progressNotes) {
        static MaintenanceResponse from(MaintenanceRequest request) {
            return new MaintenanceResponse(request.id(), request.equipmentId(), request.description(),
                    request.urgency().name(), request.status().name(), request.reportedBy().name(),
                    request.assignedTo().orElse(null), request.progressNotes());
        }
    }

    /** Turns text into an enum constant, failing with a clear message (mapped to HTTP 400) instead of a stack trace. */
    static <E extends Enum<E>> E enumOf(Class<E> type, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(type.getSimpleName() + " is required");
        }
        try {
            return Enum.valueOf(type, text.trim().toUpperCase());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + text);
        }
    }
}
