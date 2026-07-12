package com.ardhana.model;

/**
 * Whether the assigned teacher has responded to a scheduled class yet — a
 * separate dimension from {@link ClassSessionStatus}, which records the
 * class's outcome AFTER it happens. This is set BEFORE the class, so a
 * teacher can flag they're unavailable while there's still time to reassign.
 */
public enum TeacherAssignmentStatus {
    PENDING,
    ACCEPTED,
    UNAVAILABLE
}
