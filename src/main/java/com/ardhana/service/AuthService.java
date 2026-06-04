package com.ardhana.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.model.UserProfile;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

@Service
public class AuthService {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;

    public AuthService(UserProfileRepository userProfileRepository,
                       UserRoleRepository userRoleRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public ProfileResponse register(RegisterRequest request) {
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserProfile profile = new UserProfile();
        profile.setId(request.getSupabaseUid()); // id = supabase uid
        profile.setSupabaseUid(request.getSupabaseUid());
        profile.setFullName(request.getFullName());
        profile.setEmail(request.getEmail());
        profile.setMobile(request.getMobile());

        userProfileRepository.save(profile);

        return new ProfileResponse(
            profile.getSupabaseUid(),
            profile.getFullName(),
            profile.getEmail(),
            profile.getMobile(),
            null
        );
    }
    
    public Optional<ProfileResponse> getProfile(String supabaseUid) {
        return userProfileRepository.findBySupabaseUid(supabaseUid)
            .map(profile -> {
                String role = userRoleRepository
                    .findFirstByUserId(UUID.fromString(supabaseUid))
                    .map(ur -> ur.getRole().name().toLowerCase()) // "student", "admin", "teacher"
                    .orElse(null);

                return new ProfileResponse(
                    profile.getSupabaseUid(),
                    profile.getFullName(),
                    profile.getEmail(),
                    profile.getMobile(),
                    role
                );
            });
    }
}