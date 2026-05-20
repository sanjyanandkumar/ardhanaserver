package com.ardhana.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.model.Lesson;
import com.ardhana.repository.LessonRepository;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public List<Lesson> findByCourse(UUID courseId) {
        return lessonRepository.findByCourseIdOrderByPositionAsc(courseId);
    }

    public Lesson findById(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    @Transactional
    public Lesson create(Lesson lesson) {
        if (lesson.getPosition() <= 0) {
            int next = (int) lessonRepository.countByCourseId(lesson.getCourseId()) + 1;
            lesson.setPosition(next);
        }
        return lessonRepository.save(lesson);
    }

    @Transactional
    public Lesson update(UUID id, Lesson updates) {
        Lesson existing = findById(id);
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getContent() != null) existing.setContent(updates.getContent());
        if (updates.getPosition() > 0) existing.setPosition(updates.getPosition());
        return lessonRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Lesson existing = findById(id);
        lessonRepository.delete(existing);
    }
}
