package com.ardhana.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ardhana.model.LessonProgress;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {
    List<LessonProgress> findByStudentId(UUID studentId);
    List<LessonProgress> findByStudentIdAndLessonIdIn(UUID studentId, List<UUID> lessonIds);
    Optional<LessonProgress> findByLessonIdAndStudentId(UUID lessonId, UUID studentId);
}
