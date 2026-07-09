package com.ardhana.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.dto.AuthResponse;
import com.ardhana.dto.LoginRequest;
import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.dto.SendOtpRequest;
import com.ardhana.dto.VerifyOtpRequest;
import com.ardhana.model.PhoneVerification;
import com.ardhana.repository.PhoneVerificationRepository;
import com.ardhana.service.AuthService;
import com.ardhana.service.SmsService;
import com.ardhana.util.PhoneNumbers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SmsService smsService;
    private final PhoneVerificationRepository phoneVerificationRepository;

    public AuthController(AuthService authService, SmsService smsService,
                           PhoneVerificationRepository phoneVerificationRepository) {
        this.authService = authService;
        this.smsService = smsService;
        this.phoneVerificationRepository = phoneVerificationRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /** Kicks off phone verification for signup: sends an OTP via MSG91. */
    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        smsService.sendOtp(PhoneNumbers.normalize(request.getMobile()));
        return ResponseEntity.ok().build();
    }

    /** Resends the OTP to a mobile that already had one sent. */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        smsService.resendOtp(PhoneNumbers.normalize(request.getMobile()));
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the OTP against MSG91, and if correct, records the
     * verification so AuthService.register() can confirm this mobile was
     * actually verified before creating the account.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String mobile = PhoneNumbers.normalize(request.getMobile());
        boolean valid = smsService.verifyOtp(mobile, request.getOtp());
        if (!valid) {
            log.warn("OTP verification rejected for mobile ending in {}", lastDigits(mobile));
            throw new IllegalArgumentException("Incorrect or expired OTP. Please try again.");
        }
        PhoneVerification verification = new PhoneVerification();
        verification.setMobile(mobile);
        phoneVerificationRepository.save(verification);
        return ResponseEntity.ok().build();
    }

    private static String lastDigits(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(mobile.length() - 4);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        return authService.getProfile(principal.getName())
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(404).body(Map.of("message", "Profile not found")));
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signOut() {
        // JWTs are stateless — nothing to invalidate server-side; the client
        // just discards the token.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/seed-profile")
    public ResponseEntity<?> seedProfile(@RequestBody RegisterRequest request) {
        try {
            // No OTP step in this bootstrap flow, so phone verification is
            // intentionally skipped here — never do this for public signup.
            AuthResponse response = authService.register(request, false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", e.getMessage()));
        }
    }
}
