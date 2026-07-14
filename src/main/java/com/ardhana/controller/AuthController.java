package com.ardhana.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ardhana.dto.AuthResponse;
import com.ardhana.dto.ForgotPasswordRequest;
import com.ardhana.dto.LoginRequest;
import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.dto.SendOtpRequest;
import com.ardhana.dto.UpdateProfileRequest;
import com.ardhana.dto.VerifyOtpRequest;
import com.ardhana.model.PhoneVerification;
import com.ardhana.model.ProfilePhoto;
import com.ardhana.repository.PhoneVerificationRepository;
import com.ardhana.repository.ProfilePhotoRepository;
import com.ardhana.service.AuthService;
import com.ardhana.service.SmsService;
import com.ardhana.util.PhoneNumbers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // image/jpeg, image/png, image/webp — anything else is rejected below.
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024; // 5MB

    private final AuthService authService;
    private final SmsService smsService;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final ProfilePhotoRepository profilePhotoRepository;

    public AuthController(AuthService authService, SmsService smsService,
                           PhoneVerificationRepository phoneVerificationRepository,
                           ProfilePhotoRepository profilePhotoRepository) {
        this.authService = authService;
        this.smsService = smsService;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.profilePhotoRepository = profilePhotoRepository;
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

    /**
     * TEMPORARY/INTERIM: resets a user's password given just their email —
     * no verification step yet (see ForgotPasswordRequest). Planned to be
     * replaced with an email-link or OTP verification step.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        return authService.getProfile(principal.getName())
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(404).body(Map.of("message", "Profile not found")));
    }

    /** Updates the signed-in user's own name/country. */
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(Principal principal,
                                                           @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    /** Uploads (or replaces) the signed-in user's profile photo. */
    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadProfilePhoto(Principal principal,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }
        if (!ALLOWED_PHOTO_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only JPEG, PNG, or WEBP images are allowed"));
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image too large (max 5MB)"));
        }

        String userId = principal.getName();
        ProfilePhoto photo = profilePhotoRepository.findById(userId).orElse(new ProfilePhoto());
        photo.setUserId(userId);
        photo.setFileName(file.getOriginalFilename());
        photo.setContentType(file.getContentType());
        photo.setFileData(file.getBytes());
        profilePhotoRepository.save(photo);

        log.info("Profile photo uploaded for user {} ({} bytes)", userId, file.getSize());
        return ResponseEntity.ok(Map.of("avatarUrl", "/auth/profile/photo/" + userId));
    }

    /** Publicly servable so it can be used directly as an <img src>. */
    @GetMapping("/profile/photo/{userId}")
    public ResponseEntity<byte[]> getProfilePhoto(@PathVariable String userId) {
        return profilePhotoRepository.findById(userId)
            .map(photo -> ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(photo.getFileData()))
            .orElse(ResponseEntity.notFound().build());
    }

    /** Removes the signed-in user's profile photo. */
    @DeleteMapping("/profile/photo")
    public ResponseEntity<Void> deleteProfilePhoto(Principal principal) {
        String userId = principal.getName();
        if (profilePhotoRepository.existsById(userId)) {
            profilePhotoRepository.deleteById(userId);
            log.info("Profile photo removed for user {}", userId);
        }
        return ResponseEntity.noContent().build();
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
