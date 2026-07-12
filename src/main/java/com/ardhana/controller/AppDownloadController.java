package com.ardhana.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.AppDownloadFileInfoResponse;
import com.ardhana.dto.AppDownloadLeadRequest;
import com.ardhana.dto.AppDownloadLeadResponse;
import com.ardhana.model.AppRole;
import com.ardhana.service.AppDownloadService;
import com.ardhana.service.UserRoleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Public: check whether a build is available, download it, and submit a
 * name+mobile lead. Admin-only: upload/replace the build and view leads —
 * admin checks are done here at the controller level, matching the rest of
 * the app's admin-only endpoints (SecurityConfig only enforces
 * "authenticated").
 */
@Slf4j
@RestController
@RequestMapping("/api/app-downloads")
public class AppDownloadController {

    private static final long MAX_APK_BYTES = 100L * 1024 * 1024; // 100MB — see application.properties note

    private final AppDownloadService appDownloadService;
    private final UserRoleService userRoleService;

    public AppDownloadController(AppDownloadService appDownloadService, UserRoleService userRoleService) {
        this.appDownloadService = appDownloadService;
        this.userRoleService = userRoleService;
    }

    /** Public: lets the frontend show "no build uploaded yet" instead of a dead button. */
    @GetMapping("/file-info")
    public ResponseEntity<AppDownloadFileInfoResponse> fileInfo() {
        return ResponseEntity.ok(appDownloadService.getFileInfo());
    }

    /** Public: streams the current APK. */
    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadFile() {
        return appDownloadService.getFile()
            .map(file -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getFileData()))
            .orElse(ResponseEntity.notFound().build());
    }

    /** Public: captures a name + mobile before the download starts. */
    @PostMapping("/leads")
    public ResponseEntity<AppDownloadLeadResponse> submitLead(@Valid @RequestBody AppDownloadLeadRequest request) {
        return ResponseEntity.ok(appDownloadService.submitLead(request));
    }

    /** Admin: everyone who has submitted a lead, most recent first. */
    @GetMapping("/leads")
    public ResponseEntity<List<AppDownloadLeadResponse>> leads(Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(appDownloadService.getAllLeads());
    }

    /** Admin: uploads (or replaces) the current Android build. */
    @PostMapping("/upload")
    public ResponseEntity<AppDownloadFileInfoResponse> upload(Principal principal,
                                                                @RequestParam("file") MultipartFile file) throws IOException {
        requireAdmin(principal);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".apk")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .apk files are allowed");
        }
        if (file.getSize() > MAX_APK_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large (max 100MB)");
        }

        appDownloadService.saveFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        log.info("App build uploaded by admin {} ({} bytes)", principal.getName(), file.getSize());
        return ResponseEntity.ok(appDownloadService.getFileInfo());
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        boolean isAdmin = userRoleService.hasRole(UUID.fromString(principal.getName()), AppRole.admin);
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted to access app-download admin endpoints", principal.getName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
