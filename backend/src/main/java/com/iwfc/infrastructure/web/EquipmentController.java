package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.web.ApiDtos.EquipmentEditRequest;
import com.iwfc.infrastructure.web.ApiDtos.EquipmentRequest;
import com.iwfc.infrastructure.web.ApiDtos.EquipmentResponse;
import com.iwfc.infrastructure.web.ApiDtos.UsageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EquipmentController {

    private final IwfcFacade system;

    public EquipmentController(IwfcFacade system) {
        this.system = system;
    }

    @GetMapping("/api/equipment")
    List<EquipmentResponse> list(@RequestHeader("X-User-Id") String userId) {
        system.login(userId);
        return system.listEquipment().stream().map(EquipmentResponse::from).toList();
    }

    @PostMapping("/api/equipment")
    @ResponseStatus(HttpStatus.CREATED)
    EquipmentResponse add(@RequestHeader("X-User-Id") String userId, @RequestBody EquipmentRequest request) {
        User actor = system.login(userId);
        return EquipmentResponse.from(system.addEquipment(actor, ApiDtos.enumOf(EquipmentType.class, request.type()),
                request.id(), request.name(), new Location(request.location())));
    }

    @PutMapping("/api/equipment/{id}")
    EquipmentResponse edit(@RequestHeader("X-User-Id") String userId, @PathVariable String id,
                           @RequestBody EquipmentEditRequest request) {
        system.editEquipment(system.login(userId), id, request.name(), new Location(request.location()));
        return EquipmentResponse.from(system.findEquipment(id));
    }

    @PostMapping("/api/equipment/{id}/deactivate")
    EquipmentResponse deactivate(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        system.deactivateEquipment(system.login(userId), id);
        return EquipmentResponse.from(system.findEquipment(id));
    }

    @PostMapping("/api/equipment/{id}/usage")
    EquipmentResponse logUsage(@RequestHeader("X-User-Id") String userId, @PathVariable String id,
                               @RequestBody UsageRequest request) {
        system.logEquipmentUsage(system.login(userId), id, request.hours());
        return EquipmentResponse.from(system.findEquipment(id));
    }
}
