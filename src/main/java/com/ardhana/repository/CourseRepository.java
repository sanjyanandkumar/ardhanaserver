package com.ardhana.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ardhana.model.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findAllByOrderByCreatedAtDesc();
    List<Course> findAllByOrderByTitleAsc();
}
