package com.ardhana.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ardhana.model.UserProfile;
import com.ardhana.model.UserRole;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

@Service
public class AdminService {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminService(UserProfileRepository userProfileRepository,
                        UserRoleRepository userRoleRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<Map<String, Object>> listUsersWithRoles() {
        List<UserRole> allRoles = userRoleRepository.findAll();
        List<UserProfile> profiles = userProfileRepository.findAll();

        Map<String, List<UserRole>> rolesByUser = allRoles.stream()
            .collect(Collectors.groupingBy(r -> r.getUserId().toString()));

        return profiles.stream().map(profile -> {
            String userId = profile.getId();

            List<Map<String, Object>> userRoles = rolesByUser
                .getOrDefault(userId, List.of())
                .stream()
                .map(r -> {
                    Map<String, Object> role = new LinkedHashMap<>();
                    role.put("id", r.getId().toString());
                    role.put("userId", r.getUserId().toString());
                    role.put("role", r.getRole().name());
                    role.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                    return role;
                })
                .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("email", profile.getEmail() != null ? profile.getEmail() : "");
            result.put("fullName", profile.getFullName() != null ? profile.getFullName() : "");
            result.put("roles", userRoles);
            return result;
        }).collect(Collectors.toList());
    }
}
