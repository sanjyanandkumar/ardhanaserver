package com.ardhana.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ardhana.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    List<Profile> findByIdIn(List<UUID> ids);
}
