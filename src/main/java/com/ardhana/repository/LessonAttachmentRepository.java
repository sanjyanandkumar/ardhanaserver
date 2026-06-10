package com.ardhana.repository;

import com.ardhana.model.LessonAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonAttachmentRepository extends JpaRepository<LessonAttachment, UUID> {
    Optional<LessonAttachment> findByLessonIdAndStudentId(UUID lessonId, UUID studentId);
    void deleteByLessonIdAndStudentId(UUID lessonId, UUID studentId);
}