package com.ardhana.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.dto.ClassScheduleRequest;
import com.ardhana.dto.ClassScheduleResponse;
import com.ardhana.dto.ClassSessionStatusRequest;
import com.ardhana.dto.TeacherAssignmentStatusRequest;
import com.ardhana.model.AppRole;
import com.ardhana.model.ClassSchedule;
import com.ardhana.model.ClassSessionStatus;
import com.ardhana.model.Course;
import com.ardhana.model.School;
import com.ardhana.model.TeacherAssignmentStatus;
import com.ardhana.model.UserProfile;
import com.ardhana.repository.ClassScheduleRepository;
import com.ardhana.repository.CourseRepository;
import com.ardhana.repository.SchoolRepository;
import com.ardhana.repository.UserProfileRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClassScheduleService {

    // Class dates/times are entered as plain local values (no zone info),
    // meant in India time — used to decide whether a class's scheduled end
    // has actually passed yet, regardless of the server's own system zone
    // (Cloud Run defaults to UTC).
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final ClassScheduleRepository classScheduleRepository;
    private final SchoolRepository schoolRepository;
    private final CourseRepository courseRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleService userRoleService;

    public ClassScheduleService(ClassScheduleRepository classScheduleRepository,
                                 SchoolRepository schoolRepository,
                                 CourseRepository courseRepository,
                                 UserProfileRepository userProfileRepository,
                                 UserRoleService userRoleService) {
        this.classScheduleRepository = classScheduleRepository;
        this.schoolRepository = schoolRepository;
        this.courseRepository = courseRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleService = userRoleService;
    }

    public List<ClassScheduleResponse> listAll() {
        return classScheduleRepository.findAllByOrderByClassDateAscStartTimeAsc()
            .stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> listBySchool(String schoolId) {
        return classScheduleRepository.findBySchoolIdOrderByClassDateAscStartTimeAsc(schoolId)
            .stream().map(this::toResponse).toList();
    }

    public List<ClassScheduleResponse> listForTeacher(String teacherId) {
        // A class the teacher marked UNAVAILABLE for drops out of their own
        // view — it still exists for the admin to see/reassign via listAll.
        return classScheduleRepository.findByTeacherIdOrderByClassDateAscStartTimeAsc(teacherId)
            .stream()
            .filter(s -> s.getAssignmentStatus() != TeacherAssignmentStatus.UNAVAILABLE)
            .map(this::toResponse).toList();
    }

    @Transactional
    public ClassScheduleResponse create(ClassScheduleRequest request) {
        ClassSchedule schedule = new ClassSchedule();
        applyRequest(schedule, request, null);
        ClassSchedule saved = classScheduleRepository.save(schedule);
        log.info("Class scheduled: school={} course={} teacher={} date={} {}-{}",
                saved.getSchoolId(), saved.getCourseId(), saved.getTeacherId(),
                saved.getClassDate(), saved.getStartTime(), saved.getEndTime());
        return toResponse(saved);
    }

    @Transactional
    public ClassScheduleResponse update(String id, ClassScheduleRequest request) {
        ClassSchedule schedule = classScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scheduled class not found"));
        applyRequest(schedule, request, id);
        ClassSchedule saved = classScheduleRepository.save(schedule);
        log.info("Class schedule {} updated", id);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        classScheduleRepository.deleteById(id);
        log.info("Class schedule {} deleted", id);
    }

    /**
     * The assigned teacher (or an admin, on their behalf) marks a class
     * COMPLETED or NOT_COMPLETED once its scheduled end time has passed.
     * A reason is required when marking NOT_COMPLETED.
     */
    @Transactional
    public ClassScheduleResponse updateStatus(String id, String callerId, boolean callerIsAdmin,
                                               ClassSessionStatusRequest request) {
        ClassSchedule schedule = classScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scheduled class not found"));

        if (!callerIsAdmin && !schedule.getTeacherId().equals(callerId)) {
            log.warn("User {} attempted to update status of class {} assigned to a different teacher",
                    callerId, id);
            throw new IllegalArgumentException("Only the assigned teacher or an admin can update this class");
        }

        if (request.getStatus() == ClassSessionStatus.SCHEDULED) {
            throw new IllegalArgumentException("Status must be either completed or not-completed");
        }

        LocalDateTime scheduledEnd = LocalDateTime.of(schedule.getClassDate(), schedule.getEndTime());
        if (LocalDateTime.now(APP_ZONE).isBefore(scheduledEnd)) {
            throw new IllegalArgumentException(
                    "This class hasn't finished yet — you can mark it once its scheduled end time has passed");
        }

        if (request.getStatus() == ClassSessionStatus.NOT_COMPLETED
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new IllegalArgumentException("Please provide a reason for not taking this class");
        }

        schedule.setStatus(request.getStatus());
        schedule.setStatusReason(
                request.getStatus() == ClassSessionStatus.NOT_COMPLETED ? request.getReason().trim() : null);
        schedule.setStatusUpdatedAt(Instant.now());

        ClassSchedule saved = classScheduleRepository.save(schedule);
        log.info("Class schedule {} marked {} by {}", id, request.getStatus(), callerId);
        return toResponse(saved);
    }

    /**
     * The assigned teacher (or an admin, on their behalf) accepts a class or
     * flags themselves unavailable for it, any time before it starts. Marking
     * UNAVAILABLE doesn't delete the row — it just drops out of the
     * teacher's own /my list so the admin can see it needs reassignment.
     */
    @Transactional
    public ClassScheduleResponse updateAssignmentStatus(String id, String callerId, boolean callerIsAdmin,
                                                          TeacherAssignmentStatusRequest request) {
        ClassSchedule schedule = classScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scheduled class not found"));

        if (!callerIsAdmin && !schedule.getTeacherId().equals(callerId)) {
            log.warn("User {} attempted to update assignment status of class {} assigned to a different teacher",
                    callerId, id);
            throw new IllegalArgumentException("Only the assigned teacher or an admin can update this class");
        }

        if (request.getStatus() == TeacherAssignmentStatus.PENDING) {
            throw new IllegalArgumentException("Status must be either accepted or unavailable");
        }

        LocalDateTime scheduledStart = LocalDateTime.of(schedule.getClassDate(), schedule.getStartTime());
        if (!LocalDateTime.now(APP_ZONE).isBefore(scheduledStart)) {
            throw new IllegalArgumentException(
                    "This class has already started — you can no longer change your availability");
        }

        schedule.setAssignmentStatus(request.getStatus());
        ClassSchedule saved = classScheduleRepository.save(schedule);
        log.info("Class schedule {} assignment status set to {} by {}", id, request.getStatus(), callerId);
        return toResponse(saved);
    }

    /** Validates references and rejects overlapping bookings before applying request fields onto the entity. */
    private void applyRequest(ClassSchedule schedule, ClassScheduleRequest request, String excludingId) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        String previousTeacherId = schedule.getTeacherId();

        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new IllegalArgumentException("School not found"));
        Course course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        UserProfile teacher = userProfileRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (!userRoleService.hasRole(UUID.fromString(teacher.getId()), AppRole.teacher)) {
            throw new IllegalArgumentException(teacher.getFullName() + " does not have the teacher role");
        }

        boolean overlaps = classScheduleRepository
            .findByTeacherIdAndClassDate(request.getTeacherId(), request.getClassDate())
            .stream()
            .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
            .anyMatch(existing ->
                request.getStartTime().isBefore(existing.getEndTime())
                    && existing.getStartTime().isBefore(request.getEndTime()));
        if (overlaps) {
            log.warn("Rejected overlapping class schedule for teacher {} on {}",
                    request.getTeacherId(), request.getClassDate());
            throw new IllegalArgumentException(
                    teacher.getFullName() + " already has a class scheduled that overlaps this time");
        }

        schedule.setSchoolId(school.getId());
        schedule.setCourseId(course.getId());
        schedule.setTeacherId(teacher.getId());
        schedule.setClassDate(request.getClassDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setNotes(request.getNotes());

        // Reassigning to a different teacher resets their response — the new
        // teacher hasn't seen this class yet, and a stale UNAVAILABLE carried
        // over from the previous teacher would otherwise silently hide it
        // from the new teacher's own schedule too.
        if (excludingId != null && previousTeacherId != null && !previousTeacherId.equals(teacher.getId())) {
            schedule.setAssignmentStatus(TeacherAssignmentStatus.PENDING);
        }
    }

    private ClassScheduleResponse toResponse(ClassSchedule schedule) {
        String schoolName = schoolRepository.findById(schedule.getSchoolId())
            .map(School::getName).orElse("Unknown school");
        Course course = courseRepository.findById(schedule.getCourseId()).orElse(null);
        String courseTitle = course != null
            ? (course.getName() != null ? course.getName() : course.getTitle())
            : "Unknown course";
        String teacherName = userProfileRepository.findById(schedule.getTeacherId())
            .map(UserProfile::getFullName).orElse("Unknown teacher");

        return new ClassScheduleResponse(
            schedule.getId(),
            schedule.getSchoolId(), schoolName,
            schedule.getCourseId(), courseTitle,
            schedule.getTeacherId(), teacherName,
            schedule.getClassDate(), schedule.getStartTime(), schedule.getEndTime(),
            schedule.getNotes(), schedule.getStatus(),
            schedule.getStatusReason(), schedule.getStatusUpdatedAt(),
            schedule.getAssignmentStatus()
        );
    }
}
