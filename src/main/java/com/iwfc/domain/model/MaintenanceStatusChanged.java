package com.iwfc.domain.model;

/**
 * Domain event: a maintenance request has just changed status.
 * {@code previous} is null when the request was first reported.
 */
public record MaintenanceStatusChanged(String requestId, String equipmentId, RequestStatus previous,
                                       RequestStatus current, String reportedById) {
}
