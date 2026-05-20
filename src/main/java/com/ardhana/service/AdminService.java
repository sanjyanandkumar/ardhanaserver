package com.ardhana.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${supabase.project.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseAnonKey;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", supabaseAnonKey);
        h.set("Authorization", "Bearer " + supabaseAnonKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listUsersWithRoles() {
        // 1. Fetch all roles
        String rolesUrl = supabaseUrl + "/rest/v1/user_roles?select=*";
        ResponseEntity<List> rolesResp = restTemplate.exchange(
            rolesUrl, HttpMethod.GET, new HttpEntity<>(headers()), List.class
        );
        List<Map<String, Object>> allRoles = rolesResp.getBody() != null
            ? rolesResp.getBody() : List.of();

        System.out.println(">>> All roles count: " + allRoles.size());

        // 2. Fetch all profiles
        String profilesUrl = supabaseUrl + "/rest/v1/profiles?select=id,email,full_name";
        ResponseEntity<List> profilesResp = restTemplate.exchange(
            profilesUrl, HttpMethod.GET, new HttpEntity<>(headers()), List.class
        );
        List<Map<String, Object>> profiles = profilesResp.getBody() != null
            ? profilesResp.getBody() : List.of();

        System.out.println(">>> All profiles count: " + profiles.size());

        // 3. Group roles by userId
        Map<String, List<Map<String, Object>>> rolesByUser = allRoles.stream()
            .collect(Collectors.groupingBy(r -> r.get("user_id").toString()));

        // 4. Merge
        return profiles.stream().map(profile -> {
            String userId = profile.get("id").toString();

            List<Map<String, Object>> userRoles = rolesByUser
                .getOrDefault(userId, List.of())
                .stream()
                .map(r -> {
                    Map<String, Object> role = new LinkedHashMap<>();
                    role.put("id", r.get("id").toString());
                    role.put("userId", r.get("user_id").toString());
                    role.put("role", r.get("role").toString());
                    role.put("createdAt", r.get("created_at") != null
                        ? r.get("created_at").toString() : null);
                    return role;
                })
                .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("email", profile.getOrDefault("email", ""));
            result.put("fullName", profile.getOrDefault("full_name", ""));
            result.put("roles", userRoles);
            return result;
        }).collect(Collectors.toList());
    }
}