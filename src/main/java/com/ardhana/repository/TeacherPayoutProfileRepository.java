package com.ardhana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.TeacherPayoutProfile;

@Repository
public interface TeacherPayoutProfileRepository extends JpaRepository<TeacherPayoutProfile, String> {
}
