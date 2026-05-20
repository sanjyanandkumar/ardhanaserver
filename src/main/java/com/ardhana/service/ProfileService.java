package com.ardhana.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.model.Profile;
import com.ardhana.repository.ProfileRepository;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile findById(UUID id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    public List<Profile> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return profileRepository.findByIdIn(ids);
    }

    @Transactional
    public Profile upsert(Profile incoming) {
        return profileRepository.findById(incoming.getId())
                .map(existing -> {
                    if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
                    if (incoming.getFullName() != null) existing.setFullName(incoming.getFullName());
                    if (incoming.getAvatarUrl() != null) existing.setAvatarUrl(incoming.getAvatarUrl());
                    return profileRepository.save(existing);
                })
                .orElseGet(() -> profileRepository.save(incoming));
    }
}
