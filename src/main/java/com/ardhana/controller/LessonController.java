package com.ardhana.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.model.Lesson;
import com.ardhana.service.LessonService;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public List<Lesson> listByCourse(@RequestParam("courseId") UUID courseId) {
        return lessonService.findByCourse(courseId);
    }

    @GetMapping("/{id}")
    public Lesson get(@PathVariable UUID id) {
        return lessonService.findById(id);
    }

    @PostMapping
    public Lesson create(@RequestBody Lesson lesson) {
        return lessonService.create(lesson);
    }

    @PutMapping("/{id}")
    public Lesson update(@PathVariable UUID id, @RequestBody Lesson lesson) {
        return lessonService.update(id, lesson);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
