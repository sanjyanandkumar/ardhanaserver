package com.ardhana.dto;

import java.time.Instant;
import java.util.UUID;

public class UserRoleDto {
    private UUID id;
    private UUID userId;
    private String role;       // ← String so JSON gives "admin" not the enum object
    private Instant createdAt;

    public UserRoleDto(UUID id, UUID userId, String role, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}