package com.ardhana.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ardhana.model.TeacherPayoutStatus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around the RazorpayX Composite Payout API — a SEPARATE
 * product/API from RazorpayService (which only handles taking payments via
 * Checkout). Implemented with raw HttpClient (like SmsService) rather than
 * the razorpay-java SDK because that SDK's payouts/contacts/fund-account
 * client surface could not be confirmed to exist in the version pinned in
 * pom.xml (1.4.9).
 *
 * Supports two fund account types, both confirmed against official docs:
 *  - bank_account: https://razorpay.com/docs/api/x/payout-composite/create/bank-account/
 *  - vpa (UPI):     https://razorpay.com/docs/api/x/payout-composite/create/vpa/
 */
@Slf4j
@Service
public class RazorpayPayoutService {

    private static final String PAYOUTS_URL = "https://api.razorpay.com/v1/payouts";

    @Value("${razorpayx.key.id}")
    private String keyId;

    @Value("${razorpayx.key.secret}")
    private String keySecret;

    @Value("${razorpayx.account.number}")
    private String accountNumber;

    // Bank-account payouts only — UPI payouts always use mode "UPI" (fixed by Razorpay).
    @Value("${razorpayx.payout.mode:IMPS}")
    private String bankPayoutMode;

    @Value("${razorpayx.payout.purpose:payout}")
    private String payoutPurpose;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    void logConfigStatus() {
        boolean looksUnconfigured = keyId == null || keyId.isBlank() || keyId.contains("REPLACE_ME")
                || keySecret == null || keySecret.isBlank() || keySecret.contains("REPLACE_ME")
                || accountNumber == null || accountNumber.isBlank() || accountNumber.contains("REPLACE_ME");
        if (looksUnconfigured) {
            log.warn("RazorpayX is NOT configured with real credentials (keyId='{}', accountNumber='{}'). "
                    + "Teacher payouts will fail until RAZORPAYX_KEY_ID / RAZORPAYX_KEY_SECRET / "
                    + "RAZORPAYX_ACCOUNT_NUMBER are set.", keyId, accountNumber);
        } else {
            log.info("RazorpayX configured: keyId={}, accountNumber=...{}, bankMode={}, purpose={}",
                    keyId,
                    accountNumber.length() > 4 ? accountNumber.substring(accountNumber.length() - 4) : "****",
                    bankPayoutMode, payoutPurpose);
        }
    }

    /** Result of a payout attempt — always returned rather than thrown, so the caller can persist it either way. */
    public static class PayoutResult {
        public String razorpayPayoutId;
        public String razorpayFundAccountId;
        public String razorpayContactId;
        public TeacherPayoutStatus status;
        public String utr;
        public String failureReason;
        public boolean success;
    }

    /**
     * Executes one RazorpayX payout to a teacher's bank account.
     *
     * @param idempotencyKey a stable key (e.g. the local TeacherPayout.id) so that if our own call
     *                       is retried after a network blip, RazorpayX won't create a duplicate payout.
     * @param amount         rupees (major unit) — converted to paise internally.
     * @param referenceId    our own TeacherPayout.id, max 40 chars, surfaced back in the Razorpay dashboard.
     */
    public PayoutResult payToBankAccount(
            String idempotencyKey,
            String teacherName,
            String teacherEmail,
            String teacherMobile,
            String accountHolderName,
            String teacherAccountNumber,
            String ifsc,
            BigDecimal amount,
            String referenceId,
            String narration) {

        JSONObject bankAccount = new JSONObject();
        bankAccount.put("name", accountHolderName);
        bankAccount.put("ifsc", ifsc);
        bankAccount.put("account_number", teacherAccountNumber);

        JSONObject fundAccount = new JSONObject();
        fundAccount.put("account_type", "bank_account");
        fundAccount.put("bank_account", bankAccount);
        fundAccount.put("contact", buildContact(teacherName, teacherEmail, teacherMobile, referenceId));

        return executePayout(idempotencyKey, fundAccount, bankPayoutMode, amount, referenceId, narration, teacherName);
    }

    /**
     * Executes one RazorpayX payout to a teacher's UPI VPA (e.g. "name@bank").
     * Mode is always "UPI" — Razorpay requires this exact value for vpa fund accounts.
     */
    public PayoutResult payToUpi(
            String idempotencyKey,
            String teacherName,
            String teacherEmail,
            String teacherMobile,
            String vpa,
            BigDecimal amount,
            String referenceId,
            String narration) {

        JSONObject vpaObject = new JSONObject();
        vpaObject.put("address", vpa);

        JSONObject fundAccount = new JSONObject();
        fundAccount.put("account_type", "vpa");
        fundAccount.put("vpa", vpaObject);
        fundAccount.put("contact", buildContact(teacherName, teacherEmail, teacherMobile, referenceId));

        return executePayout(idempotencyKey, fundAccount, "UPI", amount, referenceId, narration, teacherName);
    }

    private JSONObject buildContact(String teacherName, String teacherEmail, String teacherMobile, String referenceId) {
        JSONObject contact = new JSONObject();
        contact.put("name", teacherName);
        if (teacherEmail != null && !teacherEmail.isBlank()) {
            contact.put("email", teacherEmail);
        }
        if (teacherMobile != null && !teacherMobile.isBlank()) {
            contact.put("contact", teacherMobile);
        }
        contact.put("type", "employee");
        contact.put("reference_id", referenceId);
        return contact;
    }

    private PayoutResult executePayout(String idempotencyKey, JSONObject fundAccount, String mode,
                                        BigDecimal amount, String referenceId, String narration, String teacherName) {
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject payload = new JSONObject();
        payload.put("account_number", accountNumber);
        payload.put("fund_account", fundAccount);
        payload.put("amount", amountInPaise);
        payload.put("currency", "INR");
        payload.put("mode", mode);
        payload.put("purpose", payoutPurpose);
        payload.put("queue_if_low_balance", true);
        if (referenceId != null && !referenceId.isBlank()) {
            payload.put("reference_id", referenceId.length() > 40 ? referenceId.substring(0, 40) : referenceId);
        }
        if (narration != null && !narration.isBlank()) {
            payload.put("narration", narration.length() > 30 ? narration.substring(0, 30) : narration);
        }

        log.info("RazorpayX payout: teacher={}, mode={}, amountInPaise={}, referenceId={}, idempotencyKey={}",
                teacherName, mode, amountInPaise, referenceId, idempotencyKey);

        PayoutResult result = new PayoutResult();
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAYOUTS_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .header("X-Payout-Idempotency", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("RazorpayX payout response [{}]: {}", response.statusCode(), response.body());

            JSONObject body = new JSONObject(response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300 && !body.has("error")) {
                result.success = true;
                result.razorpayPayoutId = body.optString("id", null);
                result.razorpayFundAccountId = body.optString("fund_account_id", null);
                JSONObject fundAccountObj = body.optJSONObject("fund_account");
                if (fundAccountObj != null) {
                    result.razorpayContactId = fundAccountObj.optString("contact_id", null);
                }
                result.utr = body.optString("utr", null);
                result.status = mapStatus(body.optString("status", "processing"));
                log.info("RazorpayX payout SUCCEEDED: id={}, status={}, referenceId={}",
                        result.razorpayPayoutId, result.status, referenceId);
            } else {
                result.success = false;
                result.status = TeacherPayoutStatus.FAILED;
                JSONObject error = body.optJSONObject("error");
                result.failureReason = error != null
                        ? error.optString("description", "RazorpayX payout failed")
                        : "RazorpayX payout failed with HTTP " + response.statusCode();
                log.error("RazorpayX payout FAILED: referenceId={}, httpStatus={}, reason={}",
                        referenceId, response.statusCode(), result.failureReason);
            }
        } catch (Exception e) {
            result.success = false;
            result.status = TeacherPayoutStatus.FAILED;
            result.failureReason = "Unable to reach RazorpayX: " + e.getMessage();
            log.error("RazorpayX payout call FAILED (exception): referenceId={}, error={}",
                    referenceId, e.getMessage(), e);
        }
        return result;
    }

    private static TeacherPayoutStatus mapStatus(String razorpayStatus) {
        if (razorpayStatus == null) return TeacherPayoutStatus.PROCESSING;
        try {
            return TeacherPayoutStatus.valueOf(razorpayStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TeacherPayoutStatus.PROCESSING;
        }
    }
}
