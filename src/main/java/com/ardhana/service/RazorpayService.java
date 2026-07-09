package com.ardhana.service;

import java.math.BigDecimal;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around the Razorpay Java SDK: creating orders and verifying
 * payment / webhook signatures. Keeping this isolated makes it easy to mock
 * in tests and keeps the Razorpay-specific API surface in one place.
 */
@Slf4j
@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    /**
     * Logs at startup (not on every request) whether Razorpay credentials
     * look configured, so a "checkout is broken" report can be diagnosed by
     * scanning startup logs first instead of guessing.
     */
    @PostConstruct
    void logConfigStatus() {
        boolean looksUnconfigured = keyId == null || keyId.isBlank() || keyId.contains("REPLACE_ME")
                || keySecret == null || keySecret.isBlank() || keySecret.contains("REPLACE_ME");
        if (looksUnconfigured) {
            log.warn("Razorpay is NOT configured with real credentials (keyId='{}'). "
                    + "Checkout will fail until RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are set.", keyId);
        } else {
            log.info("Razorpay configured: keyId={}, defaultCurrency={}", keyId, currency);
        }
        if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.contains("REPLACE_ME")) {
            log.warn("Razorpay webhook secret is NOT configured — incoming webhooks will be rejected.");
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public String getCurrency() {
        return currency;
    }

    /**
     * Creates a Razorpay order for the given amount (in the currency's major
     * unit, e.g. rupees/dollars) and returns the Razorpay order id.
     *
     * Note: non-INR currencies require International/multi-currency payments
     * to be enabled on the Razorpay account, or this call fails with a
     * Razorpay API error.
     */
    public String createOrder(BigDecimal amount, String orderCurrency, String receipt) throws RazorpayException {
        String effectiveCurrency = (orderCurrency == null || orderCurrency.isBlank()) ? currency : orderCurrency;

        // Razorpay expects the amount in the smallest currency unit (e.g. paise for INR, cents for USD).
        long amountInSubunits = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInSubunits);
        orderRequest.put("currency", effectiveCurrency);
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1);

        log.debug("Calling Razorpay orders.create: amountInSubunits={}, currency={}, receipt={}, keyId={}",
                amountInSubunits, effectiveCurrency, receipt, keyId);

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            Order order = client.orders.create(orderRequest);
            String id = order.get("id");
            log.debug("Razorpay orders.create succeeded: id={}", id);
            return id;
        } catch (RazorpayException e) {
            // The SDK's message usually contains Razorpay's actual error
            // description (bad key, currency not enabled, invalid amount,
            // etc) — surface it clearly since this is the #1 source of
            // otherwise-silent checkout failures.
            log.error("Razorpay orders.create FAILED: amountInSubunits={}, currency={}, receipt={}, reason={}",
                    amountInSubunits, effectiveCurrency, receipt, e.getMessage());
            throw e;
        }
    }

    /**
     * Verifies the signature Razorpay's Checkout returns to the browser after
     * a successful payment. Returns false (rather than throwing) whenever the
     * signature simply doesn't match.
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    /**
     * Verifies the X-Razorpay-Signature header on an incoming webhook against
     * the raw request body.
     */
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        try {
            return Utils.verifyWebhookSignature(rawPayload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            return false;
        }
    }
}
