package com.ardhana.repository;

import com.ardhana.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    // H2 only used for assign/revoke — reads come from Supabase REST
}