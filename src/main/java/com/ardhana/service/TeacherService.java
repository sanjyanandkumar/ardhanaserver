package com.ardhana.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ardhana.dto.TeacherProfileResponse;
import com.ardhana.model.AppRole;
import com.ardhana.model.UserProfile;
import com.ardhana.model.UserRole;
import com.ardhana.repository.ProfilePhotoRepository;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

import lombok.extern.slf4j.Slf4j;

/** Powers the public "Meet Our Teachers" section on the landing page. */
@Slf4j
@Service
public class TeacherService {

    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfilePhotoRepository profilePhotoRepository;

    public TeacherService(UserRoleRepository userRoleRepository,
                           UserProfileRepository userProfileRepository,
                           ProfilePhotoRepository profilePhotoRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userProfileRepository = userProfileRepository;
        this.profilePhotoRepository = profilePhotoRepository;
    }

    /**
     * Returns up to {@code limit} teachers who have filled in a bio — a
     * teacher with no bio yet isn't shown, so the section never displays a
     * blank card. Most-recently-updated profiles come first.
     */
    public List<TeacherProfileResponse> getFeatured(int limit) {
        List<UserRole> teacherRoles = userRoleRepository.findAllByRole(AppRole.teacher);

        List<TeacherProfileResponse> featured = teacherRoles.stream()
            .map(ur -> userProfileRepository.findById(ur.getUserId().toString()).orElse(null))
            .filter(profile -> profile != null && profile.getBio() != null && !profile.getBio().isBlank())
            .sorted(Comparator.comparing(UserProfile::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .map(this::toTeacherProfileResponse)
            .toList();

        log.info("Featured teachers requested: {} of {} teacher(s) have a bio set",
                featured.size(), teacherRoles.size());
        return featured;
    }

    private TeacherProfileResponse toTeacherProfileResponse(UserProfile profile) {
        String avatarUrl = profilePhotoRepository.existsById(profile.getId())
            ? "/auth/profile/photo/" + profile.getId()
            : null;
        return new TeacherProfileResponse(profile.getId(), profile.getFullName(), avatarUrl, profile.getBio());
    }
}
