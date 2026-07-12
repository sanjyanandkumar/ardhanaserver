package com.ardhana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.AppDownloadLead;

@Repository
public interface AppDownloadLeadRepository extends JpaRepository<AppDownloadLead, String> {
    List<AppDownloadLead> findAllByOrderByCreatedAtDesc();
}
