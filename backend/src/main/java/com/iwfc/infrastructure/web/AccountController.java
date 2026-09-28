package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.web.ApiDtos.LoginRequest;
import com.iwfc.infrastructure.web.ApiDtos.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Thin adapter: HTTP in, {@link IwfcFacade} call, JSON out. The acting user comes from the X-User-Id header. */
@RestController
public class AccountController {

    private final IwfcFacade system;

    public AccountController(IwfcFacade system) {
        this.system = system;
    }

    @PostMapping("/api/login")
    UserResponse login(@RequestBody LoginRequest request) {
        return UserResponse.from(system.findUser(request.userId()));
    }

    @GetMapping("/api/users")
    List<UserResponse> users(@RequestHeader("X-User-Id") String userId) {
        return system.listUsers(system.findUser(userId)).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/api/notifications")
    List<String> notifications(@RequestHeader("X-User-Id") String userId) {
        return system.inbox(system.findUser(userId));
    }
}
