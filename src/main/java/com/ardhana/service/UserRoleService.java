package com.ardhana.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.dto.UserRoleDto;
import com.ardhana.model.AppRole;
import com.ardhana.model.UserRole;
import com.ardhana.repository.UserRoleRepository;

@Service
public class UserRoleService {

    private final UserRoleRepository repository;

    public UserRoleService(UserRoleRepository repository) {
        this.repository = repository;
    }

    public List<UserRoleDto> findByUserId(UUID userId) {
        return repository.findAllByUserId(userId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public boolean hasRole(UUID userId, AppRole role) {
        return repository.findAllByUserId(userId).stream()
            .anyMatch(r -> r.getRole() == role);
    }

    /** Used to bootstrap the very first admin — no admin can exist to grant it otherwise. */
    public boolean hasAnyAdmin() {
        return repository.existsByRole(AppRole.admin);
    }

    @Transactional
    public UserRoleDto assign(UUID userId, AppRole role) {
        List<UserRole> existingRoles = repository.findAllByUserId(userId);

        UserRole alreadyHasRole = existingRoles.stream()
            .filter(r -> r.getRole() == role)
            .findFirst()
            .orElse(null);
        if (alreadyHasRole != null) {
            return toDto(alreadyHasRole);
        }

        // One role per user — revoke all existing roles first
        if (!existingRoles.isEmpty()) {
            repository.deleteAll(existingRoles);
        }

        UserRole newRole = new UserRole();
        newRole.setUserId(userId);
        newRole.setRole(role);
        UserRole saved = repository.save(newRole);
        return toDto(saved);
    }

    @Transactional
    public void revoke(UUID id) {
        repository.deleteById(id);
    }

    private UserRoleDto toDto(UserRole r) {
        return new UserRoleDto(r.getId(), r.getUserId(), r.getRole().name(), r.getCreatedAt());
    }
}
