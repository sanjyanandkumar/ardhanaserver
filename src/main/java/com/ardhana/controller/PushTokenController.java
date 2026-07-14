package com.ardhana.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.RegisterPushTokenRequest;
import com.ardhana.model.AppRole;
import com.ardhana.service.PushNotificationService;
import com.ardhana.service.UserRoleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only: the Android app registers its FCM device token here once an
 * admin grants notification permission, so PushNotificationService knows
 * where to send alerts (e.g. a teacher marking a class unavailable).
 */
@Slf4j
@RestController
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final PushNotificationService pushNotificationService;
    private final UserRoleService userRoleService;

    public PushTokenController(PushNotificationService pushNotificationService, UserRoleService userRoleService) {
        this.pushNotificationService = pushNotificationService;
        this.userRoleService = userRoleService;
    }

    @PostMapping
    public ResponseEntity<Void> register(Principal principal, @Valid @RequestBody RegisterPushTokenRequest request) {
        requireAdmin(principal);
        pushNotificationService.registerToken(principal.getName(), request.getToken(), request.getPlatform());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to register a push token", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
