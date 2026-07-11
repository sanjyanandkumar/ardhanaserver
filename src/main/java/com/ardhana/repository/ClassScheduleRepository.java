package com.ardhana.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.ClassSchedule;
import com.ardhana.model.ClassSessionStatus;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, String> {
    List<ClassSchedule> findAllByOrderByClassDateAscStartTimeAsc();
    List<ClassSchedule> findBySchoolIdOrderByClassDateAscStartTimeAsc(String schoolId);
    List<ClassSchedule> findByTeacherIdOrderByClassDateAscStartTimeAsc(String teacherId);
    List<ClassSchedule> findByTeacherIdAndClassDate(String teacherId, LocalDate classDate);

    // Completed sessions not yet linked to a payout — the "pending pay" set for a teacher.
    List<ClassSchedule> findByTeacherIdAndStatusAndTeacherPayoutIdIsNull(String teacherId, ClassSessionStatus status);

    // Every teacher id with at least one completed-unpaid session, for the admin's pending list.
    @org.springframework.data.jpa.repository.Query(
        "select distinct c.teacherId from ClassSchedule c where c.status = com.ardhana.model.ClassSessionStatus.COMPLETED and c.teacherPayoutId is null")
    List<String> findDistinctTeacherIdsWithPendingCompletedSessions();
}
