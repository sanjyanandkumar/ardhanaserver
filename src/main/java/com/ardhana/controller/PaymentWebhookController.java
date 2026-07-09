package com.ardhana.controller;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.service.OrderService;
import com.ardhana.service.RazorpayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Server-to-server webhook Razorpay calls directly (no browser involved, no
 * Supabase JWT). This is the durable fallback that confirms an order even if
 * the customer closes the tab right after paying, before the client-side
 * verify-payment call goes through. Configure the webhook URL + secret in the
 * Razorpay Dashboard under Settings > Webhooks, pointing at
 * POST /api/payments/webhook, and set RAZORPAY_WEBHOOK_SECRET to match.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";

    private final RazorpayService razorpayService;
    private final OrderService orderService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature == null || !razorpayService.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("Rejected Razorpay webhook with invalid or missing signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        JSONObject payload = new JSONObject(rawPayload);
        String event = payload.optString("event", "");

        if (EVENT_PAYMENT_CAPTURED.equals(event)) {
            JSONObject paymentEntity = payload
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayOrderId = paymentEntity.optString("order_id", null);
            String razorpayPaymentId = paymentEntity.optString("id", null);

            if (razorpayOrderId != null && razorpayPaymentId != null) {
                orderService.confirmPaymentFromWebhook(razorpayOrderId, razorpayPaymentId);
            }
        }

        // Always 200 on a signature-verified webhook so Razorpay doesn't retry
        // events we've already looked at (even ones we intentionally ignore).
        return ResponseEntity.ok("ok");
    }
}
