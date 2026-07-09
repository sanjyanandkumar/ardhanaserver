package com.ardhana.service;

import com.ardhana.dto.MyCourseResponse;
import com.ardhana.model.Course;
import com.ardhana.model.Enrollment;
import com.ardhana.model.Lesson;
import com.ardhana.model.LessonProgress;
import com.ardhana.repository.CourseRepository;
import com.ardhana.repository.EnrollmentRepository;
import com.ardhana.repository.LessonProgressRepository;
import com.ardhana.repository.LessonRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StudentCourseService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public StudentCourseService(EnrollmentRepository enrollmentRepository,
                                 CourseRepository courseRepository,
                                 LessonRepository lessonRepository,
                                 LessonProgressRepository lessonProgressRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
    }

    public List<MyCourseResponse> getMyCourses(String studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<MyCourseResponse> result = new ArrayList<>();
        UUID studentUuid = UUID.fromString(studentId);

        for (Enrollment enrollment : enrollments) {
            UUID courseUuid;
            try {
                courseUuid = UUID.fromString(enrollment.getCourseId());
            } catch (IllegalArgumentException e) {
                continue;
            }

            courseRepository.findById(enrollment.getCourseId()).ifPresent(course -> {
                List<Lesson> lessons = lessonRepository
                    .findByCourseIdOrderByPosition(courseUuid); // ← pass UUID not String

                List<UUID> lessonUuids = new ArrayList<>();
                for (Lesson l : lessons) {
                    lessonUuids.add(l.getId());
                }

                List<LessonProgress> progressList = lessonUuids.isEmpty()
                    ? new ArrayList<>()
                    : lessonProgressRepository.findByStudentIdAndLessonIdIn(studentUuid, lessonUuids);

                Map<UUID, LessonProgress> progressMap = new java.util.HashMap<>();
                for (LessonProgress p : progressList) {
                    progressMap.put(p.getLessonId(), p);
                }

                List<MyCourseResponse.LessonProgressDetail> lessonDetails = new ArrayList<>();
                for (Lesson l : lessons) {
                    LessonProgress p = progressMap.get(l.getId());
                    MyCourseResponse.LessonProgressDetail detail =
                        new MyCourseResponse.LessonProgressDetail();
                    detail.setLessonId(l.getId().toString());
                    detail.setTitle(l.getTitle());
                    detail.setPosition(l.getPosition());
                    detail.setCompleted(p != null && p.isCompleted());
                    detail.setNote(p != null ? p.getNote() : null);
                    lessonDetails.add(detail);
                }

                long completed = 0;
                for (MyCourseResponse.LessonProgressDetail d : lessonDetails) {
                    if (d.isCompleted()) completed++;
                }

                MyCourseResponse response = new MyCourseResponse();
                response.setCourseId(course.getId());
                response.setTitle(course.getTitle());
                response.setName(course.getName());
                response.setDescription(course.getDescription());
                response.setLevel(course.getLevel());
                response.setSessions(course.getSessions());
                response.setCompletedLessons((int) completed);
                response.setTotalLessons(lessons.size());
                response.setLessons(lessonDetails);
                result.add(response);
            });
        }
        return result;
    }
}