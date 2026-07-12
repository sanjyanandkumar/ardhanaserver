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

    // Landing page now shows these in a horizontally-scrolling carousel
    // (right/left arrows) rather than a fixed 4-up grid, so it's worth
    // sending more than 4 — the frontend only reveals the arrows once
    // there's actually more to scroll to.
    private static final int FEATURED_LIMIT = 12;

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
