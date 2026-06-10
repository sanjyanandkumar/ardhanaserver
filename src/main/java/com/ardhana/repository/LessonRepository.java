package com.ardhana.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ardhana.model.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByCourseIdOrderByPositionAsc(UUID courseId);
    long countByCourseId(UUID courseId);
    void deleteByCourseId(UUID courseId);
    List<Lesson> findByCourseIdOrderByPosition(UUID courseId);
}
