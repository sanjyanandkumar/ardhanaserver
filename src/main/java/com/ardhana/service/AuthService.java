package com.ardhana.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ardhana.dto.AuthResponse;
import com.ardhana.dto.LoginRequest;
import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.dto.UpdateProfileRequest;
import com.ardhana.model.AppRole;
import com.ardhana.model.PhoneVerification;
import com.ardhana.model.UserProfile;
import com.ardhana.model.UserRole;
import com.ardhana.repository.PhoneVerificationRepository;
import com.ardhana.repository.ProfilePhotoRepository;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;
import com.ardhana.util.PhoneNumbers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {

    // How long a verified-OTP result stays usable for completing signup.
    // MSG91's own OTP expiry (msg91.otp.expiry-minutes) governs how long the
    // OTP CODE itself is valid; this separately governs how long the
    // ALREADY-VERIFIED result may sit before the user finishes the rest of
    // the signup form.
    private static final Duration PHONE_VERIFICATION_VALID_FOR = Duration.ofMinutes(30);

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final ProfilePhotoRepository profilePhotoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserProfileRepository userProfileRepository,
                       UserRoleRepository userRoleRepository,
                       PhoneVerificationRepository phoneVerificationRepository,
                       ProfilePhotoRepository profilePhotoRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.profilePhotoRepository = profilePhotoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Public self-signup — requires the mobile to have been OTP-verified first (see /api/auth/verify-otp). */
    public AuthResponse register(RegisterRequest request) {
        return register(request, true);
    }

    /**
     * @param requirePhoneVerification false only for the internal admin
     *     bootstrap endpoint (/api/auth/admin/seed-profile), which has no
     *     OTP step of its own — never expose that skip to public signup.
     */
    public AuthResponse register(RegisterRequest request, boolean requirePhoneVerification) {
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (requirePhoneVerification) {
            String mobile = PhoneNumbers.normalize(request.getMobile());
            PhoneVerification verification = phoneVerificationRepository
                    .findFirstByMobileAndUsedFalseOrderByVerifiedAtDesc(mobile)
                    .filter(v -> v.getVerifiedAt().isAfter(Instant.now().minus(PHONE_VERIFICATION_VALID_FOR)))
                    .orElseThrow(() -> {
                        log.warn("Registration blocked: mobile not verified (endsWith={})",
                                mobile != null && mobile.length() >= 4 ? mobile.substring(mobile.length() - 4) : "****");
                        return new IllegalArgumentException(
                                "Please verify your mobile number with the OTP before creating your account.");
                    });
            verification.setUsed(true);
            phoneVerificationRepository.save(verification);
        }

        String id = UUID.randomUUID().toString();

        UserProfile profile = new UserProfile();
        profile.setId(id);
        // Kept in sync with id — some existing lookups (SchoolService, AdminService)
        // still key off this column, which used to hold the Supabase user id.
        profile.setSupabaseUid(id);
        profile.setFullName(request.getFullName());
        profile.setEmail(request.getEmail());
        profile.setMobile(request.getMobile());
        profile.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        profile.setCountry(request.getCountry() != null ? request.getCountry().toUpperCase() : null);

        userProfileRepository.save(profile);

        // Self-registration is only ever used by students — give them the
        // "student" role automatically so they can add courses to cart etc.
        // without an admin having to manually assign a role first.
        UserRole studentRole = new UserRole();
        studentRole.setUserId(UUID.fromString(id));
        studentRole.setRole(AppRole.student);
        userRoleRepository.save(studentRole);

        String token = jwtService.generateToken(id, profile.getEmail());
        return new AuthResponse(token, toProfileResponse(profile, AppRole.student.name().toLowerCase()));
    }

    public AuthResponse login(LoginRequest request) {
        UserProfile profile = userProfileRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (profile.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), profile.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String role = roleFor(profile.getId());
        String token = jwtService.generateToken(profile.getId(), profile.getEmail());
        return new AuthResponse(token, toProfileResponse(profile, role));
    }

    public Optional<ProfileResponse> getProfile(String userId) {
        return userProfileRepository.findById(userId)
            .map(profile -> toProfileResponse(profile, roleFor(userId)));
    }

    /** Updates the editable subset of a signed-in user's own profile. */
    public ProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        profile.setFullName(request.getFullName());
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            profile.setCountry(request.getCountry().toUpperCase());
        }
        // Omitted (null) means "leave unchanged" — the frontend only sends
        // this for teachers, so it never accidentally wipes a non-teacher's
        // (nonexistent) bio.
        if (request.getBio() != null) {
            String trimmed = request.getBio().trim();
            profile.setBio(trimmed.isEmpty() ? null : trimmed);
        }

        userProfileRepository.save(profile);
        log.info("Profile updated for user {}", userId);

        return toProfileResponse(profile, roleFor(userId));
    }

    private String roleFor(String userId) {
        return userRoleRepository.findFirstByUserId(UUID.fromString(userId))
            .map(ur -> ur.getRole().name().toLowerCase()) // "student", "admin", "teacher"
            .orElse(null);
    }

    private ProfileResponse toProfileResponse(UserProfile profile, String role) {
        String avatarUrl = profilePhotoRepository.existsById(profile.getId())
            ? "/auth/profile/photo/" + profile.getId()
            : null;
        return new ProfileResponse(
            profile.getSupabaseUid(),
            profile.getFullName(),
            profile.getEmail(),
            profile.getMobile(),
            role,
            profile.getCountry(),
            avatarUrl,
            profile.getBio()
        );
    }
}
