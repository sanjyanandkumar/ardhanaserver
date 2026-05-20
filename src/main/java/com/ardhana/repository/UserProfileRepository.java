package com.ardhana.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findBySupabaseUid(String supabaseUid);
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
}