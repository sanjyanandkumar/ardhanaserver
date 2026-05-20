package com.ardhana.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.model.Profile;
import com.ardhana.service.ProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{id}")
    public Profile get(@PathVariable UUID id) {
        return profileService.findById(id);
    }

    @GetMapping
    public List<Profile> getByIds(@RequestParam("ids") List<UUID> ids) {
        return profileService.findByIds(ids);
    }

    @PostMapping("/upsert")
    public Profile upsert(@RequestBody Profile profile) {
        return profileService.upsert(profile);
    }
}
