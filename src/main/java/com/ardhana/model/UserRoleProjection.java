package com.ardhana.model;

import java.time.Instant;
import java.util.UUID;

public interface UserRoleProjection {
    UUID getId();
    UUID getUserId();
    String getRole();        // ← plain String, no enum mapping issues
    Instant getCreatedAt();
}