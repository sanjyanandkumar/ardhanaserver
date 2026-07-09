package com.ardhana.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.dto.CheckoutInitResponse;
import com.ardhana.dto.CheckoutRequest;
import com.ardhana.dto.OrderResponse;
import com.ardhana.dto.PaymentVerificationRequest;
import com.ardhana.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a PENDING order and a matching Razorpay order. The frontend
     * uses the response to open the Razorpay Checkout widget. Nothing is
     * confirmed and no enrollments exist yet at this point.
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutInitResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        return ResponseEntity.ok(orderService.initiateCheckout(request));
    }

    /**
     * Called once Razorpay Checkout's handler callback fires with a
     * payment id + signature. Verifies the signature server-side, then
     * confirms the order and creates enrollments.
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<OrderResponse> verifyPayment(@RequestBody @Valid PaymentVerificationRequest request) {
        return ResponseEntity.ok(orderService.confirmPayment(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Orders API 400: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", String.valueOf(ex.getMessage())));
    }

    // Thrown when a downstream call (currently: Razorpay order creation)
    // fails — see OrderService.initiateCheckout / RazorpayService.createOrder
    // for the actual root-cause log line with the Razorpay error detail.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServiceError(IllegalStateException ex) {
        log.error("Orders API 502 (downstream/service error): {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", String.valueOf(ex.getMessage())));
    }
}