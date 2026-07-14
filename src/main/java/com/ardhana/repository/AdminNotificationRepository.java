package com.ardhana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ardhana.model.AdminNotification;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, String> {
    List<AdminNotification> findAllByOrderByCreatedAtDesc();
    long countByReadFalse();

    @Modifying
    @Query("update AdminNotification n set n.read = true where n.read = false")
    void markAllRead();
}
