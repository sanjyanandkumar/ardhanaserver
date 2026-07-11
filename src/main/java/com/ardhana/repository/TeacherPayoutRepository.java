package com.ardhana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.TeacherPayout;

@Repository
public interface TeacherPayoutRepository extends JpaRepository<TeacherPayout, String> {
    List<TeacherPayout> findAllByOrderByCreatedAtDesc();
    List<TeacherPayout> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
}
