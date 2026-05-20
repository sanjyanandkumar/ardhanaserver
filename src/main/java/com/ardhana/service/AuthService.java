package com.ardhana.service;

import org.springframework.stereotype.Service;

import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.model.UserProfile;
import com.ardhana.repository.UserProfileRepository;

@Service
public class AuthService {

    private final UserProfileRepository userProfileRepository;

    public AuthService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public ProfileResponse register(RegisterRequest request) {
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userProfileRepository.existsByMobile(request.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered");
        }

        UserProfile profile = new UserProfile();
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
            profile.getRole()
        );
    }

    public ProfileResponse getProfile(String supabaseUid) {
        UserProfile profile = userProfileRepository.findBySupabaseUid(supabaseUid)
            .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        return new ProfileResponse(
            profile.getSupabaseUid(),
            profile.getFullName(),
            profile.getEmail(),
            profile.getMobile(),
            profile.getRole()
        );
    }
}