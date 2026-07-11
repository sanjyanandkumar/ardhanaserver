package com.ardhana.dto;

import jakarta.validation.constraints.NotNull;

import com.ardhana.model.ClassSessionStatus;

public class ClassSessionStatusRequest {

    @NotNull(message = "Status is required")
    private ClassSessionStatus status;

    // Required when status is NOT_COMPLETED — validated in the service since
    // it's conditional on the status value.
    private String reason;

    public ClassSessionStatus getStatus() { return status; }
    public void setStatus(ClassSessionStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
