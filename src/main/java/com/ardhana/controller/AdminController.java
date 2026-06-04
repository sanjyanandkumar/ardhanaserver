package com.ardhana.controller;

import com.ardhana.service.AdminService;
import com.ardhana.service.UserRoleService;
import com.ardhana.model.AppRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserRoleService userRoleService;

    public AdminController(AdminService adminService, UserRoleService userRoleService) {
        this.adminService = adminService;
        this.userRoleService = userRoleService;
    }

    @GetMapping("/users-with-roles")
    public ResponseEntity<?> listUsersWithRoles() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // ← Guard against unauthenticated requests
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Authentication required"));
        }

        String uid = (String) authentication.getPrincipal();
        System.out.println(">>> Admin check for uid: " + uid);

        boolean isAdmin = userRoleService.hasRole(UUID.fromString(uid), AppRole.admin);
        System.out.println(">>> isAdmin result: " + isAdmin);

        if (!isAdmin) {
            return ResponseEntity.status(403)
                .body(Map.of("message", "Admin access required"));
        }

        System.out.println(">>> Admin verified — fetching users with roles");
        return ResponseEntity.ok(adminService.listUsersWithRoles());
    }
}