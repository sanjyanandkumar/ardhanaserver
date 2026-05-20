package com.ardhana.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.ardhana.dto.UserRoleDto;
import com.ardhana.model.AppRole;
import com.ardhana.model.UserRole;
import com.ardhana.repository.UserRoleRepository;

@Service
public class UserRoleService {

    private final UserRoleRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${supabase.project.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseAnonKey;

    public UserRoleService(UserRoleRepository repository) {
        this.repository = repository;
    }

    private HttpHeaders supabaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseAnonKey);
        headers.set("Authorization", "Bearer " + supabaseAnonKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public List<UserRoleDto> findByUserId(UUID userId) {
        System.out.println(">>> Fetching roles from Supabase REST for: " + userId);

        String url = supabaseUrl + "/rest/v1/user_roles?user_id=eq." + userId + "&select=*";
        System.out.println(">>> URL: " + url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(supabaseHeaders());
            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class
            );

            List<Map<String, Object>> rows = response.getBody();
            System.out.println(">>> Rows from Supabase: " + (rows != null ? rows.size() : 0));

            if (rows == null || rows.isEmpty()) return List.of();

            return rows.stream().map(row -> {
                System.out.println(">>> Row: " + row);
                return new UserRoleDto(
                    UUID.fromString(row.get("id").toString()),
                    UUID.fromString(row.get("user_id").toString()),
                    row.get("role").toString(),
                    row.get("created_at") != null
                        ? Instant.parse(row.get("created_at").toString()
                            .replace(" ", "T")
                            .replaceAll("\\+00$", "Z")
                            .replaceAll("\\+00:00$", "Z"))
                        : null
                );
            }).collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println(">>> Supabase REST error: " + e.getMessage());
            return List.of();
        }
    }

    public boolean hasRole(UUID userId, AppRole role) {
        return findByUserId(userId).stream()
            .anyMatch(r -> r.getRole().equalsIgnoreCase(role.name()));
    }

    @Transactional
    public UserRole assign(UUID userId, AppRole role) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRole(role);
        return repository.save(ur);
    }

    @Transactional
    public void revoke(UUID id) {
        repository.deleteById(id);
    }
}