package com.ardhana.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.model.LessonProgress;
import com.ardhana.service.LessonProgressService;

@RestController
@RequestMapping("/api/lesson-progress")
public class LessonProgressController {

    private final LessonProgressService service;

    public LessonProgressController(LessonProgressService service) {
        this.service = service;
    }

    @GetMapping
    public List<LessonProgress> list(@RequestParam("studentId") UUID studentId,
                                     @RequestParam(value = "lessonIds", required = false) List<UUID> lessonIds) {
        if (lessonIds != null && !lessonIds.isEmpty()) {
            return service.findByStudentAndLessons(studentId, lessonIds);
        }
        return service.findByStudent(studentId);
    }

    @PostMapping("/upsert")
    public LessonProgress upsert(@RequestBody LessonProgress incoming) {
        return service.upsert(incoming);
    }
}
