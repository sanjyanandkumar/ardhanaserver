package com.ardhana.controller;

import com.ardhana.dto.CourseRequest;
import com.ardhana.model.Course;
import com.ardhana.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // Public — students can view
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourse(@PathVariable String id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    // Admin only
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CourseRequest req) {
        return ResponseEntity.ok(courseService.createCourse(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id,
                                                @RequestBody CourseRequest req) {
        return ResponseEntity.ok(courseService.updateCourse(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}