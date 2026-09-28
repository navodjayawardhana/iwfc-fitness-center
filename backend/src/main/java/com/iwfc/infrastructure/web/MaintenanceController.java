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
    List<MaintenanceResponse> list(@RequestHeader("Authorization") String authorization,
                                   @RequestParam(defaultValue = "false") boolean mine) {
        User user = ApiAuth.user(system, authorization);
        List<MaintenanceRequest> requests = mine ? system.myMaintenanceRequests(user) : system.maintenanceRequests(user);
        return requests.stream().map(MaintenanceResponse::from).toList();
    }

    @GetMapping("/api/maintenance/activity-log")
    List<String> activityLog(@RequestHeader("Authorization") String authorization) {
        return system.maintenanceActivityLog(ApiAuth.user(system, authorization));
    }

    @PostMapping("/api/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    MaintenanceResponse report(@RequestHeader("Authorization") String authorization, @RequestBody FaultRequest request) {
        return MaintenanceResponse.from(system.reportFault(ApiAuth.user(system, authorization), request.equipmentId(),
                request.description(), ApiDtos.enumOf(Urgency.class, request.urgency())));
    }

    @PostMapping("/api/maintenance/{id}/assign")
    MaintenanceResponse assign(@RequestHeader("Authorization") String authorization, @PathVariable String id,
                               @RequestBody AssignRequest request) {
        User admin = ApiAuth.user(system, authorization);
        system.assignMaintenance(admin, id, request.technician());
        return find(admin, id);
    }

    @PostMapping("/api/maintenance/{id}/progress")
    MaintenanceResponse progress(@RequestHeader("Authorization") String authorization, @PathVariable String id,
                                 @RequestBody ProgressRequest request) {
        User admin = ApiAuth.user(system, authorization);
        system.updateMaintenanceProgress(admin, id, request.note());
        return find(admin, id);
    }

    @PostMapping("/api/maintenance/{id}/complete")
    MaintenanceResponse complete(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        User admin = ApiAuth.user(system, authorization);
        system.completeMaintenance(admin, id);
        return find(admin, id);
    }

    private MaintenanceResponse find(User admin, String id) {
        return MaintenanceResponse.from(system.findMaintenanceRequest(admin, id));
    }
}
