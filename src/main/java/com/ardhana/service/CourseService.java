package com.ardhana.service;

import com.ardhana.dto.CourseRequest;
import com.ardhana.model.Course;
import com.ardhana.repository.CourseRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAllByOrderByCreatedAtDesc();
    }

    public Course getCourse(String id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }

    public Course createCourse(CourseRequest req) {
        Course course = new Course();
        applyRequest(course, req);
        return courseRepository.save(course);
    }

    public Course updateCourse(String id, CourseRequest req) {
        Course course = getCourse(id);
        applyRequest(course, req);
        return courseRepository.save(course);
    }

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }

    private void applyRequest(Course course, CourseRequest req) {
        if (req.getTitle() != null) course.setTitle(req.getTitle());
        if (req.getName() != null) course.setName(req.getName());
        if (req.getDescription() != null) course.setDescription(req.getDescription());
        if (req.getPriceInr() != null) course.setPriceInr(req.getPriceInr());
        if (req.getPriceUsd() != null) course.setPriceUsd(req.getPriceUsd());
        if (req.getPriceEur() != null) course.setPriceEur(req.getPriceEur());
        if (req.getPriceGbp() != null) course.setPriceGbp(req.getPriceGbp());
        if (req.getSessions() != null) course.setSessions(req.getSessions());
        if (req.getLevel() != null) course.setLevel(req.getLevel());
        if (req.getThumbnailUrl() != null) course.setThumbnailUrl(req.getThumbnailUrl());
        if (req.getFocuses() != null) course.setFocuses(req.getFocuses());
        if (req.getOutcome() != null) course.setOutcome(req.getOutcome());
        if (req.getActive() != null) course.setActive(req.getActive());
    }
}