package com.ardhana.controller;

import com.ardhana.dto.MyCourseResponse;
import com.ardhana.service.StudentCourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/my-courses")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    public StudentCourseController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<List<MyCourseResponse>> getMyCourses(@PathVariable String studentId) {
        return ResponseEntity.ok(studentCourseService.getMyCourses(studentId));
    }
}