package com.ardhana.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public List<Lesson> listByCourse(@RequestParam("courseId") String courseId) {
        return lessonService.findByCourse(UUID.fromString(courseId));
    }

    @GetMapping("/{id}")
    public Lesson get(@PathVariable String id) {
        return lessonService.findById(UUID.fromString(id));
    }

    @PostMapping
    public Lesson create(@RequestBody LessonRequest req) {
        Lesson lesson = new Lesson();
        lesson.setCourseId(UUID.fromString(req.getCourseId()));
        lesson.setTitle(req.getTitle());
        lesson.setContent(req.getContent());
        lesson.setPosition(req.getPosition() != null ? req.getPosition() : 0);
        return lessonService.create(lesson);
    }

    @PutMapping("/{id}")
    public Lesson update(@PathVariable String id, @RequestBody LessonRequest req) {
        Lesson lesson = new Lesson();
        lesson.setTitle(req.getTitle());
        lesson.setContent(req.getContent());
        if (req.getPosition() != null) lesson.setPosition(req.getPosition());
        return lessonService.update(UUID.fromString(id), lesson);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lessonService.delete(UUID.fromString(id));
        return ResponseEntity.noContent().build();
    }

    // Inner DTO to accept String courseId from frontend
    public static class LessonRequest {
        private String courseId;
        private String title;
        private String content;
        private Integer position;

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
    }
}