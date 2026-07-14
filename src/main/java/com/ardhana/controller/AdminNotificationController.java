package com.ardhana.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.AdminNotificationResponse;
import com.ardhana.model.AppRole;
import com.ardhana.service.AdminNotificationService;
import com.ardhana.service.UserRoleService;

import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only: every endpoint here requires the admin role, checked at the
 * controller level (SecurityConfig only enforces "authenticated"), matching
 * the rest of the app's admin-only endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-notifications")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final UserRoleService userRoleService;

    public AdminNotificationController(AdminNotificationService adminNotificationService,
                                        UserRoleService userRoleService) {
        this.adminNotificationService = adminNotificationService;
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> list(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(adminNotificationService.listAll());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(Map.of("count", adminNotificationService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Principal principal, @PathVariable String id) {
        requireAdmin(principal);
        adminNotificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        requireAdmin(principal);
        adminNotificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to access admin notifications", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
