package com.ardhana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ardhana.model.AppDownloadFile;

@Repository
public interface AppDownloadFileRepository extends JpaRepository<AppDownloadFile, String> {
}
