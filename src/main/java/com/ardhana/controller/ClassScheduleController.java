package com.ardhana.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.ClassScheduleRequest;
import com.ardhana.dto.ClassScheduleResponse;
import com.ardhana.dto.ClassSessionStatusRequest;
import com.ardhana.model.AppRole;
import com.ardhana.service.ClassScheduleService;
import com.ardhana.service.UserRoleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin manages the schedule (list/create/update/delete, optionally scoped
 * to a school); teachers see only their own via GET /my. Admin checks are
 * done here at the controller level (matching UserRoleController), since
 * SecurityConfig only enforces "authenticated", not role.
 */
@Slf4j
@RestController
@RequestMapping("/api/class-schedules")
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;
    private final UserRoleService userRoleService;

    public ClassScheduleController(ClassScheduleService classScheduleService,
                                    UserRoleService userRoleService) {
        this.classScheduleService = classScheduleService;
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public ResponseEntity<List<ClassScheduleResponse>> list(Principal principal,
                                                              @RequestParam(required = false) String schoolId) {
        requireAdmin(principal);
        List<ClassScheduleResponse> result = schoolId != null
            ? classScheduleService.listBySchool(schoolId)
            : classScheduleService.listAll();
        return ResponseEntity.ok(result);
    }

    /** The signed-in teacher's own schedule — no admin check, just their own id. */
    @GetMapping("/my")
    public ResponseEntity<List<ClassScheduleResponse>> my(Principal principal) {
        return ResponseEntity.ok(classScheduleService.listForTeacher(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<ClassScheduleResponse> create(Principal principal,
                                                          @Valid @RequestBody ClassScheduleRequest request) {
        requireAdmin(principal);
        return ResponseEntity.ok(classScheduleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassScheduleResponse> update(Principal principal,
                                                          @PathVariable String id,
                                                          @Valid @RequestBody ClassScheduleRequest request) {
        requireAdmin(principal);
        return ResponseEntity.ok(classScheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable String id) {
        requireAdmin(principal);
        classScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The assigned teacher marks their own class completed/not-completed
     * once it's finished; an admin may also do this on a teacher's behalf.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ClassScheduleResponse> updateStatus(Principal principal,
                                                                @PathVariable String id,
                                                                @Valid @RequestBody ClassSessionStatusRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        return ResponseEntity.ok(
                classScheduleService.updateStatus(id, principal.getName(), isAdmin, request));
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to manage class schedules", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
