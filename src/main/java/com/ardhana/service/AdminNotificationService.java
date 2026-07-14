package com.ardhana.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ardhana.dto.AdminNotificationResponse;
import com.ardhana.dto.ClassScheduleResponse;
import com.ardhana.model.AdminNotification;
import com.ardhana.model.AdminNotificationType;
import com.ardhana.repository.AdminNotificationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminNotificationService {

    private final AdminNotificationRepository repository;
    private final PushNotificationService pushNotificationService;

    public AdminNotificationService(AdminNotificationRepository repository,
                                     PushNotificationService pushNotificationService) {
        this.repository = repository;
        this.pushNotificationService = pushNotificationService;
    }

    public List<AdminNotificationResponse> listAll() {
        return repository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long unreadCount() {
        return repository.countByReadFalse();
    }

    @Transactional
    public void markRead(String id) {
        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead();
    }

    /** Called by ClassScheduleService when a teacher flags themselves unavailable for a class. */
    @Transactional
    public void notifyTeacherUnavailable(ClassScheduleResponse schedule) {
        String message = String.format(
            "%s marked themselves unavailable for %s at %s on %s (%s–%s). This class needs reassignment.",
            schedule.getTeacherName(), schedule.getCourseTitle(), schedule.getSchoolName(),
            schedule.getClassDate(), schedule.getStartTime(), schedule.getEndTime()
        );

        AdminNotification notification = new AdminNotification();
        notification.setType(AdminNotificationType.TEACHER_UNAVAILABLE);
        notification.setRelatedClassScheduleId(schedule.getId());
        notification.setMessage(message);
        repository.save(notification);
        log.info("Admin notification created: teacher {} unavailable for class {}",
                schedule.getTeacherId(), schedule.getId());

        // Best-effort — a failed push shouldn't roll back the in-app notification
        // that was just saved above (it already logs its own errors internally).
        pushNotificationService.sendToAllAdmins("Teacher unavailable", message, schedule.getId());
    }

    private AdminNotificationResponse toResponse(AdminNotification n) {
        return new AdminNotificationResponse(
            n.getId(), n.getType(), n.getMessage(), n.getRelatedClassScheduleId(), n.isRead(), n.getCreatedAt());
    }
}
