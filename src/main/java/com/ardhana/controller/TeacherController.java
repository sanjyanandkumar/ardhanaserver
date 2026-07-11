package com.ardhana.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.dto.TeacherProfileResponse;
import com.ardhana.service.TeacherService;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private static final int FEATURED_LIMIT = 4;

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** Public — powers the landing page's "Meet Our Teachers" section. */
    @GetMapping("/featured")
    public List<TeacherProfileResponse> featured() {
        return teacherService.getFeatured(FEATURED_LIMIT);
    }
}
