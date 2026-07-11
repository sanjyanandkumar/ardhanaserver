package com.ardhana.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.SchoolLoginActionResponse;
import com.ardhana.model.AppRole;
import com.ardhana.service.SchoolAuthService;
import com.ardhana.service.UserRoleService;

import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only: create or reset a school's login account so they can sign in
 * and pay their platform fee. The generated password is never returned here
 * — it's emailed directly to the school (see EmailService).
 */
@Slf4j
@RestController
@RequestMapping("/api/schools")
public class SchoolLoginController {

    private final SchoolAuthService schoolAuthService;
    private final UserRoleService userRoleService;

    public SchoolLoginController(SchoolAuthService schoolAuthService, UserRoleService userRoleService) {
        this.schoolAuthService = schoolAuthService;
        this.userRoleService = userRoleService;
    }

    @PostMapping("/{id}/create-login")
    public ResponseEntity<SchoolLoginActionResponse> createLogin(Principal principal, @PathVariable String id) {
        requireAdmin(principal);
        return ResponseEntity.ok(schoolAuthService.createLogin(id));
    }

    @PostMapping("/{id}/reset-login-password")
    public ResponseEntity<SchoolLoginActionResponse> resetPassword(Principal principal, @PathVariable String id) {
        requireAdmin(principal);
        return ResponseEntity.ok(schoolAuthService.resetPassword(id));
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to manage a school login", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
