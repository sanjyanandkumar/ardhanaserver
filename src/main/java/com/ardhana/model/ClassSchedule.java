package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A single, one-off scheduled class: a course taught at a school by a teacher on a specific date/time. */
@Entity
@Table(name = "class_schedules")
public class ClassSchedule {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    // References UserProfile.id — must have the "teacher" role (enforced in
    // ClassScheduleService, not at the DB level).
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Set by the assigned teacher (or an admin) after the class's scheduled
    // end time has passed — see ClassScheduleService.updateStatus().
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClassSessionStatus status = ClassSessionStatus.SCHEDULED;

    // Required when status is NOT_COMPLETED; null otherwise.
    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    // Whether the assigned teacher has accepted this class or flagged
    // themselves unavailable — set BEFORE the class happens, distinct from
    // `status` above which records the outcome afterward. See
    // ClassScheduleService.updateAssignmentStatus().
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 20)
    private TeacherAssignmentStatus assignmentStatus = TeacherAssignmentStatus.PENDING;

    // Null means "not yet paid". Set to a TeacherPayout.id once that payout
    // (covering this session, among possibly others) succeeds — see
    // TeacherPayoutService.pay().
    @Column(name = "teacher_payout_id")
    private String teacherPayoutId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ClassSessionStatus.SCHEDULED;
        if (assignmentStatus == null) assignmentStatus = TeacherAssignmentStatus.PENDING;
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSchoolId() { return schoolId; }
    public void setSchoolId(String schoolId) { this.schoolId = schoolId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public LocalDate getClassDate() { return classDate; }
    public void setClassDate(LocalDate classDate) { this.classDate = classDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ClassSessionStatus getStatus() { return status; }
    public void setStatus(ClassSessionStatus status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public Instant getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(Instant statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
    public TeacherAssignmentStatus getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(TeacherAssignmentStatus assignmentStatus) { this.assignmentStatus = assignmentStatus; }
    public String getTeacherPayoutId() { return teacherPayoutId; }
    public void setTeacherPayoutId(String teacherPayoutId) { this.teacherPayoutId = teacherPayoutId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
