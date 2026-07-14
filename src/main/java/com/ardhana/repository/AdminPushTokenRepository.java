package com.ardhana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.AdminPushToken;

@Repository
public interface AdminPushTokenRepository extends JpaRepository<AdminPushToken, String> {
}
