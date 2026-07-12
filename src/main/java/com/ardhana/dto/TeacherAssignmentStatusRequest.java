package com.ardhana.dto;

import jakarta.validation.constraints.NotNull;

import com.ardhana.model.TeacherAssignmentStatus;

public class TeacherAssignmentStatusRequest {

    @NotNull(message = "Status is required")
    private TeacherAssignmentStatus status;

    public TeacherAssignmentStatus getStatus() { return status; }
    public void setStatus(TeacherAssignmentStatus status) { this.status = status; }
}
