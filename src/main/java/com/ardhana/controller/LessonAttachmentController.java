package com.ardhana.controller;

import com.ardhana.model.LessonAttachment;
import com.ardhana.repository.LessonAttachmentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lesson-attachments")
public class LessonAttachmentController {

    private final LessonAttachmentRepository attachmentRepository;

    public LessonAttachmentController(LessonAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lessonId") UUID lessonId,
            @RequestParam("studentId") UUID studentId) throws IOException {

        if (file.isEmpty()) return ResponseEntity.badRequest()
            .body(Map.of("message", "File is empty"));
        if (!"application/pdf".equals(file.getContentType())) return ResponseEntity.badRequest()
            .body(Map.of("message", "Only PDF files allowed"));
        if (file.getSize() > 10 * 1024 * 1024) return ResponseEntity.badRequest()
            .body(Map.of("message", "File too large (max 10MB)"));

        // Upsert — replace if exists
        LessonAttachment attachment = attachmentRepository
            .findByLessonIdAndStudentId(lessonId, studentId)
            .orElse(new LessonAttachment());

        attachment.setLessonId(lessonId);
        attachment.setStudentId(studentId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileData(file.getBytes());

        LessonAttachment saved = attachmentRepository.save(attachment);

        return ResponseEntity.ok(Map.of(
            "id", saved.getId().toString(),
            "fileName", saved.getFileName()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        return attachmentRepository.findById(id)
            .map(a -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + a.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(a.getFileData()))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-lesson")
    public ResponseEntity<?> getByLesson(
            @RequestParam UUID lessonId,
            @RequestParam UUID studentId) {
        return attachmentRepository.findByLessonIdAndStudentId(lessonId, studentId)
            .map(a -> ResponseEntity.ok(Map.of(
                "id", a.getId().toString(),
                "fileName", a.getFileName()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/by-lesson")
    @Transactional
    public ResponseEntity<Void> deleteByLesson(
            @RequestParam UUID lessonId,
            @RequestParam UUID studentId) {
        attachmentRepository.deleteByLessonIdAndStudentId(lessonId, studentId);
        return ResponseEntity.noContent().build();
    }
}