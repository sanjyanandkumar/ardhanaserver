package com.ardhana.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.model.Enrollment;
import com.ardhana.repository.EnrollmentRepository;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Enrollment> findByStudent(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> findByCourse(UUID courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public Enrollment enroll(UUID courseId, UUID studentId) {
        return enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseGet(() -> {
                    Enrollment e = new Enrollment();
                    e.setCourseId(courseId);
                    e.setStudentId(studentId);
                    return enrollmentRepository.save(e);
                });
    }

    @Transactional
    public void unenroll(UUID id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
        enrollmentRepository.deleteById(id);
    }
}
