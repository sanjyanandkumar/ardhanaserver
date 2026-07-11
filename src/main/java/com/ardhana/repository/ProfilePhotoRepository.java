package com.ardhana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.ProfilePhoto;

@Repository
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, String> {
}
