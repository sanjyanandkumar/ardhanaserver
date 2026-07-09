package com.ardhana.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body posted by the frontend after Razorpay Checkout's handler callback
 * fires with a successful payment.
 */
@Data
public class PaymentVerificationRequest {
    @NotNull
    private UUID orderId;

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;
}
