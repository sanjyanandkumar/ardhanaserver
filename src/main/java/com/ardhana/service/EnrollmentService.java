package com.ardhana.service;

import java.util.List;
import java.util.Optional;
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

    public List<Enrollment> findByStudent(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> findByCourse(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public Enrollment enroll(String courseId, String studentId) {
        boolean exists = enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId);
        if (exists) {
            return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow();
        }
        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setStudentId(studentId);
        return enrollmentRepository.save(e);
    }

    @Transactional
    public void unenroll(String id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
        enrollmentRepository.deleteById(id);
    }
}