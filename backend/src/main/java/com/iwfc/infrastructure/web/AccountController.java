package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.web.ApiDtos.CreateUserRequest;
import com.iwfc.infrastructure.web.ApiDtos.LoginRequest;
import com.iwfc.infrastructure.web.ApiDtos.LoginResponse;
import com.iwfc.infrastructure.web.ApiDtos.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Thin adapter: sign in, sign out, who am I, notifications and (for administrators) user accounts. */
@RestController
public class AccountController {

    private final IwfcFacade system;

    public AccountController(IwfcFacade system) {
        this.system = system;
    }

    @PostMapping("/api/login")
    LoginResponse login(@RequestBody LoginRequest request) {
        return LoginResponse.from(system.signIn(request.userId(), request.password()));
    }

    @PostMapping("/api/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestHeader("Authorization") String authorization) {
        system.signOut(ApiAuth.token(authorization));
    }

    @GetMapping("/api/me")
    UserResponse me(@RequestHeader("Authorization") String authorization) {
        return UserResponse.from(ApiAuth.user(system, authorization));
    }

    @GetMapping("/api/users")
    List<UserResponse> users(@RequestHeader("Authorization") String authorization) {
        return system.listUsers(ApiAuth.user(system, authorization)).stream().map(UserResponse::from).toList();
    }

    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@RequestHeader("Authorization") String authorization, @RequestBody CreateUserRequest request) {
        User admin = ApiAuth.user(system, authorization);
        return UserResponse.from(system.registerUser(admin, Role.parse(request.role()), request.id(),
                request.name(), request.password()));
    }

    @PostMapping("/api/users/{id}/deactivate")
    UserResponse deactivate(@RequestHeader("Authorization") String authorization, @PathVariable String id) {
        User admin = ApiAuth.user(system, authorization);
        system.deactivateUser(admin, id);
        return UserResponse.from(system.findUser(id));
    }

    @GetMapping("/api/notifications")
    List<String> notifications(@RequestHeader("Authorization") String authorization) {
        return system.inbox(ApiAuth.user(system, authorization));
    }
}
