package com.ardhana.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findBySupabaseUid(String supabaseUid);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    List<UserProfile> findBySchoolId(String schoolId);
}