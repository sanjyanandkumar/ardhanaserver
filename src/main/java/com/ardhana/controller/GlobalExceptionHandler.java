package com.ardhana.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

/**
 * Safety-net logging for any exception that isn't already handled by a
 * controller-local @ExceptionHandler (see e.g. OrderController, which has
 * its own for IllegalArgumentException / IllegalStateException — those take
 * precedence over this class for that controller).
 *
 * Before this existed, an unexpected exception anywhere in the app produced
 * a bare 500/502 with NOTHING in the logs, making failures like a broken
 * Razorpay checkout impossible to diagnose. Every branch here logs first,
 * responds second.
 *
 * IMPORTANT: every response body here is JSON {"message": "..."} — every
 * frontend fetch call site (authService.ts, ardhana-api/client.ts, etc.)
 * does `const body = await res.json(); body.message` to surface the real
 * error to the user. Returning a plain string body (as this used to) makes
 * res.json() throw, silently swallowing the actual error message.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status != null && status.is5xxServerError()) {
            log.error("Unhandled ResponseStatusException: {} {}", ex.getStatusCode(), ex.getReason(), ex);
        } else {
            log.warn("ResponseStatusException: {} {}", ex.getStatusCode(), ex.getReason());
        }
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", String.valueOf(ex.getReason())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Request validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", String.valueOf(ex.getMessage())));
    }

    // Final catch-all — anything reaching here is an unanticipated bug.
    // Always log the full stack trace: this is the difference between a
    // 2-minute fix and an unreproducible mystery.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal error: " + ex.getMessage()));
    }
}
