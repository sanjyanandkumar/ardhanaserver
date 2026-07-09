package com.ardhana.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned from POST /api/orders/checkout. The frontend uses this to open the
 * Razorpay Checkout widget; the payment itself hasn't happened yet — the
 * order is created with status PENDING until /api/orders/verify-payment (or
 * the webhook) confirms it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutInitResponse {
    private UUID orderId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;

    /**
     * True when PAYMENTS_MOCK_ENABLED is on server-side — no real Razorpay
     * order was created, and the frontend should skip opening the Razorpay
     * Checkout widget entirely and go straight to verify-payment (which also
     * skips real signature verification in this mode). Testing-only.
     */
    private boolean testMode;
}
