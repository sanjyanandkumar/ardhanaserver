package com.ardhana.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.ardhana.model.Course;
import com.ardhana.repository.CourseRepository;
import com.ardhana.repository.EnrollmentRepository;
import com.ardhana.repository.LessonRepository;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(CourseRepository courseRepository,
                         LessonRepository lessonRepository,
                         EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Course> findAll() {
        return courseRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Course> findAllByTitle() {
        return courseRepository.findAllByOrderByTitleAsc();
    }

    public Course findById(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    @Transactional
    public Course create(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    public Course update(UUID id, Course updates) {
        Course existing = findById(id);
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getCoverUrl() != null) existing.setCoverUrl(updates.getCoverUrl());
        if (updates.getPrice() != null) existing.setPrice(updates.getPrice());
        return courseRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        Course existing = findById(id);
        // cascade-style cleanup: remove lessons & enrollments tied to the course
        lessonRepository.deleteByCourseId(existing.getId());
        enrollmentRepository.findByCourseId(existing.getId())
                .forEach(e -> enrollmentRepository.deleteById(e.getId()));
        courseRepository.delete(existing);
    }
}
