package com.ardhana.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ardhana.dto.AppDownloadFileInfoResponse;
import com.ardhana.dto.AppDownloadLeadRequest;
import com.ardhana.dto.AppDownloadLeadResponse;
import com.ardhana.model.AppDownloadFile;
import com.ardhana.model.AppDownloadLead;
import com.ardhana.repository.AppDownloadFileRepository;
import com.ardhana.repository.AppDownloadLeadRepository;
import com.ardhana.util.PhoneNumbers;

@Service
public class AppDownloadService {

    // Single current build for now — see AppDownloadFile for why this is a
    // fixed key rather than a per-upload row.
    private static final String PLATFORM = "android";

    private final AppDownloadFileRepository fileRepository;
    private final AppDownloadLeadRepository leadRepository;

    public AppDownloadService(AppDownloadFileRepository fileRepository, AppDownloadLeadRepository leadRepository) {
        this.fileRepository = fileRepository;
        this.leadRepository = leadRepository;
    }

    public AppDownloadFileInfoResponse getFileInfo() {
        return fileRepository.findById(PLATFORM)
            .map(f -> new AppDownloadFileInfoResponse(true, f.getFileName(), f.getFileSize(), f.getUploadedAt()))
            .orElse(new AppDownloadFileInfoResponse(false, null, 0, null));
    }

    public Optional<AppDownloadFile> getFile() {
        return fileRepository.findById(PLATFORM);
    }

    public void saveFile(String fileName, String contentType, byte[] data) {
        AppDownloadFile file = fileRepository.findById(PLATFORM).orElse(new AppDownloadFile());
        file.setPlatform(PLATFORM);
        file.setFileName(fileName);
        file.setContentType(contentType);
        file.setFileSize(data.length);
        file.setFileData(data);
        fileRepository.save(file);
    }

    public AppDownloadLeadResponse submitLead(AppDownloadLeadRequest request) {
        AppDownloadLead lead = new AppDownloadLead();
        lead.setFullName(request.getFullName().trim());
        lead.setMobile(PhoneNumbers.normalize(request.getMobile()));
        lead.setPlatform(PLATFORM);
        leadRepository.save(lead);
        return toResponse(lead);
    }

    public List<AppDownloadLeadResponse> getAllLeads() {
        return leadRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AppDownloadLeadResponse toResponse(AppDownloadLead lead) {
        return new AppDownloadLeadResponse(
            lead.getId(), lead.getFullName(), lead.getMobile(), lead.getPlatform(), lead.getCreatedAt());
    }
}
