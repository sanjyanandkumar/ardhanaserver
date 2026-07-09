package com.ardhana.controller;

import com.ardhana.dto.UserRoleDto;
import com.ardhana.model.AppRole;
import com.ardhana.service.UserRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-roles")
@CrossOrigin(origins = {
    "http://localhost:8080",
    "http://localhost:5173",
    "http://localhost:3000"
})
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public ResponseEntity<List<UserRoleDto>> listByUser(@RequestParam String userId) {
        System.out.println(">>> Controller listByUser: " + userId);
        List<UserRoleDto> roles = userRoleService.findByUserId(UUID.fromString(userId));
        System.out.println(">>> Controller returning roles: " + roles.size());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/has-role")
    public ResponseEntity<Boolean> hasRole(
            @RequestParam String userId,
            @RequestParam String role) {
        boolean result = userRoleService.hasRole(
            UUID.fromString(userId),
            AppRole.valueOf(role)
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> assign(@RequestBody UserRoleDto request) {
        String callerId = requireAuthenticated();
        if (callerId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }

        // Bootstrap exception: if no admin exists yet, anyone can grant the
        // first one (otherwise there'd be no way to ever create an admin).
        // Once an admin exists, only an existing admin may assign roles.
        boolean callerIsAdmin = userRoleService.hasRole(UUID.fromString(callerId), AppRole.admin);
        if (!callerIsAdmin && userRoleService.hasAnyAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        }

        UserRoleDto saved = userRoleService.assign(
            request.getUserId(),
            AppRole.valueOf(request.getRole())
        );
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revoke(@PathVariable UUID id) {
        String callerId = requireAuthenticated();
        if (callerId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }

        boolean callerIsAdmin = userRoleService.hasRole(UUID.fromString(callerId), AppRole.admin);
        if (!callerIsAdmin) {
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        }

        userRoleService.revoke(id);
        return ResponseEntity.noContent().build();
    }

    /** Returns the authenticated caller's user id, or null if unauthenticated. */
    private String requireAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return (String) authentication.getPrincipal();
    }
}