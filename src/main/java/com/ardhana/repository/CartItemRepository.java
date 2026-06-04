package com.ardhana.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ardhana.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUserIdOrderByAddedAtDesc(UUID userId);
    Optional<CartItem> findByUserIdAndPillarKey(UUID userId, String pillarKey);
    void deleteByUserId(UUID userId);
}
