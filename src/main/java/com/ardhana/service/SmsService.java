package com.ardhana.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around MSG91's OTP API (send / verify / retry).
 *
 * MSG91 was chosen specifically because it can send OTP SMS in India using
 * its OWN pre-approved DLT entity/template — no DLT registration needed on
 * our side. Tradeoff: without our own registered sender ID, the SMS shows
 * MSG91's generic sender, not "ARDHNA". Register your own DLT entity +
 * sender ID with MSG91 later if branded SMS matters.
 *
 * NOTE ON API SHAPE: this follows MSG91's long-standing v5 OTP API
 * (send/verify/retry as simple query-param POSTs). MSG91's dashboard shows
 * the exact current request format for your account under API docs —
 * double check the field names there once you have real credentials, since
 * this was written without being able to load their live docs page.
 */
@Slf4j
@Service
public class SmsService {

    private static final String OTP_URL = "https://control.msg91.com/api/v5/otp";
    private static final String VERIFY_URL = "https://control.msg91.com/api/v5/otp/verify";
    private static final String RETRY_URL = "https://control.msg91.com/api/v5/otp/retry";

    @Value("${msg91.auth.key}")
    private String authKey;

    @Value("${msg91.otp.template-id:}")
    private String templateId;

    @Value("${msg91.otp.expiry-minutes:15}")
    private int otpExpiryMinutes;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    void logConfigStatus() {
        boolean looksUnconfigured = authKey == null || authKey.isBlank() || authKey.contains("REPLACE_ME");
        if (looksUnconfigured) {
            log.warn("MSG91 is NOT configured with a real auth key (authKey='{}'). "
                    + "OTP send/verify will fail until MSG91_AUTH_KEY is set.", authKey);
        } else {
            log.info("MSG91 configured: authKey=...{}, templateId={}",
                    authKey.length() > 4 ? authKey.substring(authKey.length() - 4) : "****",
                    (templateId == null || templateId.isBlank()) ? "(default)" : templateId);
        }
    }

    /** Sends a new OTP to the given mobile number, e.g. "919876543210" (country code, no +/leading zeros). */
    public void sendOtp(String mobile) {
        StringBuilder url = new StringBuilder(OTP_URL)
                .append("?mobile=").append(encode(mobile))
                .append("&authkey=").append(encode(authKey))
                .append("&otp_expiry=").append(otpExpiryMinutes);
        if (templateId != null && !templateId.isBlank()) {
            url.append("&template_id=").append(encode(templateId));
        }

        JSONObject body = post(url.toString());
        if (!"success".equalsIgnoreCase(body.optString("type"))) {
            log.error("MSG91 sendOtp FAILED for mobile ending in {}: {}",
                    lastDigits(mobile), body.optString("message"));
            throw new IllegalStateException("Unable to send OTP: " + body.optString("message", "unknown error"));
        }
        log.info("OTP sent via MSG91 to mobile ending in {}", lastDigits(mobile));
    }

    /** Resends the OTP (a fresh SMS) to a mobile that already had one sent. */
    public void resendOtp(String mobile) {
        String url = RETRY_URL
                + "?mobile=" + encode(mobile)
                + "&authkey=" + encode(authKey)
                + "&retrytype=text";
        JSONObject body = post(url);
        if (!"success".equalsIgnoreCase(body.optString("type"))) {
            log.error("MSG91 resendOtp FAILED for mobile ending in {}: {}",
                    lastDigits(mobile), body.optString("message"));
            throw new IllegalStateException("Unable to resend OTP: " + body.optString("message", "unknown error"));
        }
        log.info("OTP resent via MSG91 to mobile ending in {}", lastDigits(mobile));
    }

    /** Verifies a user-submitted OTP against MSG91. Returns true only on an exact, unexpired match. */
    public boolean verifyOtp(String mobile, String otp) {
        String url = VERIFY_URL
                + "?mobile=" + encode(mobile)
                + "&otp=" + encode(otp)
                + "&authkey=" + encode(authKey);
        JSONObject body = post(url);
        boolean valid = "success".equalsIgnoreCase(body.optString("type"));
        if (!valid) {
            log.warn("MSG91 verifyOtp failed for mobile ending in {}: {}",
                    lastDigits(mobile), body.optString("message"));
        } else {
            log.info("OTP verified via MSG91 for mobile ending in {}", lastDigits(mobile));
        }
        return valid;
    }

    private JSONObject post(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("MSG91 response [{}]: {}", response.statusCode(), response.body());
            return new JSONObject(response.body());
        } catch (Exception e) {
            log.error("MSG91 API call failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to reach SMS provider: " + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String lastDigits(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(mobile.length() - 4);
    }
}
