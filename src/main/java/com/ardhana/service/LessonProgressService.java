package com.ardhana.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.model.LessonProgress;
import com.ardhana.repository.LessonProgressRepository;

@Service
public class LessonProgressService {

    private final LessonProgressRepository repository;

    public LessonProgressService(LessonProgressRepository repository) {
        this.repository = repository;
    }

    public List<LessonProgress> findByStudent(UUID studentId) {
        return repository.findByStudentId(studentId);
    }

    public List<LessonProgress> findByStudentAndLessons(UUID studentId, List<UUID> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return List.of();
        return repository.findByStudentIdAndLessonIdIn(studentId, lessonIds);
    }

    @Transactional
    public LessonProgress upsert(LessonProgress incoming) {
        return repository.findByLessonIdAndStudentId(incoming.getLessonId(), incoming.getStudentId())
                .map(existing -> {
                    existing.setCompleted(incoming.isCompleted());
                    existing.setNote(incoming.getNote());
                    existing.setUpdatedBy(incoming.getUpdatedBy());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(incoming));
    }
}
