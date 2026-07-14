package com.ardhana.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ardhana.model.AdminPushToken;
import com.ardhana.repository.AdminPushTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PushNotificationService {

    private final AdminPushTokenRepository repository;

    public PushNotificationService(AdminPushTokenRepository repository) {
        this.repository = repository;
    }

    /** Registers (or refreshes) the calling admin's device token — upserts by token. */
    public void registerToken(String adminUserId, String token, String platform) {
        AdminPushToken entity = repository.findById(token).orElse(new AdminPushToken());
        entity.setToken(token);
        entity.setAdminUserId(adminUserId);
        entity.setPlatform(platform);
        repository.save(entity);
        log.info("Push token registered for admin {}", adminUserId);
    }

    /**
     * Sends a push notification to every registered admin device. Silently
     * no-ops (with a debug log) if Firebase wasn't initialized — see
     * FirebaseConfig for why that's expected outside of Cloud Run.
     */
    public void sendToAllAdmins(String title, String body, String relatedClassScheduleId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Skipping push notification (Firebase not initialized): {}", title);
            return;
        }

        List<AdminPushToken> tokens = repository.findAll();
        if (tokens.isEmpty()) {
            return;
        }

        List<String> tokenValues = tokens.stream().map(AdminPushToken::getToken).collect(Collectors.toList());

        // FCM caps a single multicast call at 500 tokens — fine for the number
        // of admin devices expected here; revisit with batching if that grows.
        MulticastMessage.Builder builder = MulticastMessage.builder()
            .addAllTokens(tokenValues)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build());
        if (relatedClassScheduleId != null) {
            builder.putData("relatedClassScheduleId", relatedClassScheduleId);
        }

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            log.info("Push notification sent: {} succeeded, {} failed",
                    response.getSuccessCount(), response.getFailureCount());
            pruneInvalidTokens(tokenValues, response.getResponses());
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
        }
    }

    /** Removes tokens FCM reports as no longer valid (app uninstalled, token rotated, etc.). */
    private void pruneInvalidTokens(List<String> tokenValues, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) continue;
            FirebaseMessagingException ex = r.getException();
            if (ex != null && (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                repository.deleteById(tokenValues.get(i));
                log.info("Removed invalid/expired push token");
            }
        }
    }
}
