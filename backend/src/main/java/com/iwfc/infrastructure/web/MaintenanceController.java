package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.web.ApiDtos.AssignRequest;
import com.iwfc.infrastructure.web.ApiDtos.FaultRequest;
import com.iwfc.infrastructure.web.ApiDtos.MaintenanceResponse;
import com.iwfc.infrastructure.web.ApiDtos.ProgressRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MaintenanceController {

    private final IwfcFacade system;

    public MaintenanceController(IwfcFacade system) {
        this.system = system;
    }

    /** All requests for administrators; {@code ?mine=true} gives a reporter only their own. */
    @GetMapping("/api/maintenance")
    List<MaintenanceResponse> list(@RequestHeader("X-User-Id") String userId,
                                   @RequestParam(defaultValue = "false") boolean mine) {
        User user = system.login(userId);
        List<MaintenanceRequest> requests = mine ? system.myMaintenanceRequests(user) : system.maintenanceRequests(user);
        return requests.stream().map(MaintenanceResponse::from).toList();
    }

    @GetMapping("/api/maintenance/activity-log")
    List<String> activityLog(@RequestHeader("X-User-Id") String userId) {
        return system.maintenanceActivityLog(system.login(userId));
    }

    @PostMapping("/api/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    MaintenanceResponse report(@RequestHeader("X-User-Id") String userId, @RequestBody FaultRequest request) {
        return MaintenanceResponse.from(system.reportFault(system.login(userId), request.equipmentId(),
                request.description(), ApiDtos.enumOf(Urgency.class, request.urgency())));
    }

    @PostMapping("/api/maintenance/{id}/assign")
    MaintenanceResponse assign(@RequestHeader("X-User-Id") String userId, @PathVariable String id,
                               @RequestBody AssignRequest request) {
        User admin = system.login(userId);
        system.assignMaintenance(admin, id, request.technician());
        return find(admin, id);
    }

    @PostMapping("/api/maintenance/{id}/progress")
    MaintenanceResponse progress(@RequestHeader("X-User-Id") String userId, @PathVariable String id,
                                 @RequestBody ProgressRequest request) {
        User admin = system.login(userId);
        system.updateMaintenanceProgress(admin, id, request.note());
        return find(admin, id);
    }

    @PostMapping("/api/maintenance/{id}/complete")
    MaintenanceResponse complete(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        User admin = system.login(userId);
        system.completeMaintenance(admin, id);
        return find(admin, id);
    }

    private MaintenanceResponse find(User admin, String id) {
        return system.maintenanceRequests(admin).stream()
                .filter(request -> request.id().equals(id))
                .findFirst()
                .map(MaintenanceResponse::from)
                .orElseThrow();
    }
}
