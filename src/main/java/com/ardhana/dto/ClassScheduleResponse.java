package com.ardhana.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.ardhana.model.ClassSessionStatus;
import com.ardhana.model.TeacherAssignmentStatus;

/** Enriched with names so the frontend doesn't need to join school/course/teacher itself. */
public class ClassScheduleResponse {
    private String id;
    private String schoolId;
    private String schoolName;
    private String courseId;
    private String courseTitle;
    private String teacherId;
    private String teacherName;
    private LocalDate classDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
    private ClassSessionStatus status;
    private String statusReason;
    private Instant statusUpdatedAt;
    private TeacherAssignmentStatus assignmentStatus;

    public ClassScheduleResponse(String id, String schoolId, String schoolName,
                                  String courseId, String courseTitle,
                                  String teacherId, String teacherName,
                                  LocalDate classDate, LocalTime startTime, LocalTime endTime,
                                  String notes, ClassSessionStatus status,
                                  String statusReason, Instant statusUpdatedAt,
                                  TeacherAssignmentStatus assignmentStatus) {
        this.id = id;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.classDate = classDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
        this.status = status;
        this.statusReason = statusReason;
        this.statusUpdatedAt = statusUpdatedAt;
        this.assignmentStatus = assignmentStatus;
    }

    public String getId() { return id; }
    public String getSchoolId() { return schoolId; }
    public String getSchoolName() { return schoolName; }
    public String getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public LocalDate getClassDate() { return classDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getNotes() { return notes; }
    public ClassSessionStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public Instant getStatusUpdatedAt() { return statusUpdatedAt; }
    public TeacherAssignmentStatus getAssignmentStatus() { return assignmentStatus; }
}
