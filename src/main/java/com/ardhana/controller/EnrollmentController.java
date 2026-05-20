package com.ardhana.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.model.Enrollment;
import com.ardhana.service.EnrollmentService;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<Enrollment> list(@RequestParam(value = "studentId", required = false) UUID studentId,
                                 @RequestParam(value = "courseId", required = false) UUID courseId) {
        if (studentId != null) return enrollmentService.findByStudent(studentId);
        if (courseId != null) return enrollmentService.findByCourse(courseId);
        return List.of();
    }

    public record EnrollRequest(UUID courseId, UUID studentId) {}

    @PostMapping
    public Enrollment enroll(@RequestBody EnrollRequest req) {
        return enrollmentService.enroll(req.courseId(), req.studentId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unenroll(@PathVariable UUID id) {
        enrollmentService.unenroll(id);
        return ResponseEntity.noContent().build();
    }
}
