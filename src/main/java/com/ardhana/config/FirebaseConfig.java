package com.ardhana.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes the Firebase Admin SDK so PushNotificationService can send FCM
 * push notifications to the Android app. Uses Application Default
 * Credentials — on Cloud Run this works automatically via the service's
 * attached service account (no key file needed), as long as that service
 * account has permission to send via Firebase Cloud Messaging for this
 * project. Locally, this silently fails unless GOOGLE_APPLICATION_CREDENTIALS
 * points at a downloaded service account key — everything else in the app
 * runs fine either way; push notifications just won't send.
 *
 * The project ID is set explicitly rather than relying on FirebaseOptions'
 * auto-detection: on Cloud Run, firebase-admin's automatic project ID lookup
 * can fail to resolve from the metadata server/ADC in some environments,
 * which surfaces as "Project ID is required to access messaging service"
 * at send time even though initialization itself appeared to succeed.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String PROJECT_ID = "project-fc7db70e-7e95-4c96-ab5";

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(PROJECT_ID)
                .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized — push notifications enabled");
        } catch (IOException e) {
            log.warn("Firebase Admin SDK not initialized — push notifications disabled: {}", e.getMessage());
        }
    }
}
