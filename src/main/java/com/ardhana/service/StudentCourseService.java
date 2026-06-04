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
            courseRepository.findById(enrollment.getCourseId()).ifPresent(course -> {
                List<Lesson> lessons = lessonRepository
                    .findByCourseIdOrderByPosition(course.getId());

                List<UUID> lessonUuids = new ArrayList<>();
                for (Lesson l : lessons) {
                    lessonUuids.add(l.getId());
                }
                
                List<LessonProgress> progressList = lessonProgressRepository
                    .findByStudentIdAndLessonIdIn(studentUuid, lessonUuids);

                Map<String, LessonProgress> progressMap = progressList.stream()
                    .collect(Collectors.toMap(
                        p -> p.getLessonId().toString(),
                        p -> p
                    ));

                List<MyCourseResponse.LessonProgressDetail> lessonDetails = lessons.stream()
                    .map(l -> {
                        LessonProgress p = progressMap.get(l.getId());
                        MyCourseResponse.LessonProgressDetail detail =
                            new MyCourseResponse.LessonProgressDetail();
                        detail.setLessonId(l.getId());
                        detail.setTitle(l.getTitle());
                        detail.setPosition(l.getPosition());
                        detail.setCompleted(p != null && p.isCompleted());
                        detail.setNote(p != null ? p.getNote() : null);
                        return detail;
                    })
                    .collect(Collectors.toList());

                long completed = lessonDetails.stream()
                    .filter(MyCourseResponse.LessonProgressDetail::isCompleted)
                    .count();

                MyCourseResponse response = new MyCourseResponse();
                response.setCourseId(course.getId());
                response.setTitle(course.getTitle());
                response.setDescription(course.getDescription());
                response.setLevel(course.getLevel());
                response.setDurationHours(course.getDurationHours());
                response.setCompletedLessons((int) completed);
                response.setTotalLessons(lessons.size());
                response.setLessons(lessonDetails);
                result.add(response);
            });
        }
        return result;
    }
}