package com.ardhana.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.dto.ProfileResponse;
import com.ardhana.dto.RegisterRequest;
import com.ardhana.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        ProfileResponse response = authService.register(request);
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
        // Supabase handles token invalidation — Java is stateless, nothing to clear
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/seed-profile")
    public ResponseEntity<?> seedProfile(@RequestBody RegisterRequest request) {
        try {
            ProfileResponse profile = authService.register(request);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", e.getMessage()));
        }
    }
}