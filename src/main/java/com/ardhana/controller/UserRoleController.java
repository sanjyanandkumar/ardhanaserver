package com.ardhana.controller;

import com.ardhana.dto.UserRoleDto;
import com.ardhana.model.AppRole;
import com.ardhana.model.UserRoleProjection;
import com.ardhana.service.UserRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        List<UserRoleDto> roles = userRoleService.findByUserId(UUID.fromString(userId))
            .stream()
            .map(r -> new UserRoleDto(
                r.getId(),
                r.getUserId(),
                r.getRole(),
                r.getCreatedAt()
            ))
            .collect(Collectors.toList());

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
    public ResponseEntity<UserRoleDto> assign(@RequestBody UserRoleDto request) {
        var saved = userRoleService.assign(
            request.getUserId(),
            AppRole.valueOf(request.getRole())
        );
        return ResponseEntity.ok(new UserRoleDto(
            saved.getId(),
            saved.getUserId(),
            saved.getRole().name(),
            saved.getCreatedAt()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        userRoleService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}