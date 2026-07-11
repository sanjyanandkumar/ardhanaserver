package com.ardhana.service;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.dto.SchoolLoginActionResponse;
import com.ardhana.model.AppRole;
import com.ardhana.model.School;
import com.ardhana.model.UserProfile;
import com.ardhana.model.UserRole;
import com.ardhana.repository.SchoolRepository;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates and resets a school's own login account (role=school), so they can
 * sign in and pay their platform fee. The generated password is only ever
 * visible in the confirmation email — it is never returned by the API, never
 * logged, and never stored (only its bcrypt hash is persisted).
 */
@Slf4j
@Service
public class SchoolAuthService {

    private static final String PASSWORD_CHARS =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int PASSWORD_LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SchoolRepository schoolRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public SchoolAuthService(SchoolRepository schoolRepository,
                              UserProfileRepository userProfileRepository,
                              UserRoleRepository userRoleRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService) {
        this.schoolRepository = schoolRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Creates a brand-new login for a school that doesn't have one yet. */
    @Transactional
    public SchoolLoginActionResponse createLogin(String schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));

        if (school.getEmail() == null || school.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This school has no email address on file — add one first.");
        }
        if (school.getLoginUserId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This school already has a login. Use reset password instead.");
        }
        if (userProfileRepository.existsByEmail(school.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A user account already exists with this email address.");
        }

        String password = generatePassword();
        String id = UUID.randomUUID().toString();

        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setSupabaseUid(id);
        profile.setFullName(school.getName());
        profile.setEmail(school.getEmail());
        profile.setMobile(school.getPhone());
        profile.setPasswordHash(passwordEncoder.encode(password));
        profile.setSchoolId(school.getId());
        userProfileRepository.save(profile);

        UserRole role = new UserRole();
        role.setUserId(UUID.fromString(id));
        role.setRole(AppRole.school);
        userRoleRepository.save(role);

        school.setLoginUserId(id);
        schoolRepository.save(school);

        emailService.sendSchoolLoginCredentials(school.getEmail(), school.getName(), password);
        log.info("School login created and credentials emailed: schoolId={}, userId={}", schoolId, id);

        return new SchoolLoginActionResponse(school.getEmail());
    }

    /** Generates a fresh password for an existing school login and re-emails it. */
    @Transactional
    public SchoolLoginActionResponse resetPassword(String schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));

        if (school.getLoginUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "This school doesn't have a login yet — create one first.");
        }
        UserProfile profile = userProfileRepository.findById(school.getLoginUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Login account not found"));

        String password = generatePassword();
        profile.setPasswordHash(passwordEncoder.encode(password));
        userProfileRepository.save(profile);

        emailService.sendSchoolLoginCredentials(profile.getEmail(), school.getName(), password);
        log.info("School login password reset and re-emailed: schoolId={}, userId={}", schoolId, profile.getId());

        return new SchoolLoginActionResponse(profile.getEmail());
    }

    private static String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
