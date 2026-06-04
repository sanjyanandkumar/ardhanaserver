package com.ardhana.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ardhana.model.CartItem;
import com.ardhana.repository.CartItemRepository;

@Service
public class CartService {

    private final CartItemRepository repository;

    public CartService(CartItemRepository repository) {
        this.repository = repository;
    }

    public List<CartItem> findByUser(UUID userId) {
        return repository.findByUserIdOrderByAddedAtDesc(userId);
    }

    @Transactional
    public CartItem addOrIncrement(UUID userId, String pillarKey, String pillarName, BigDecimal price) {
        if (userId == null || pillarKey == null || pillarKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and pillarKey are required");
        }
        return repository.findByUserIdAndPillarKey(userId, pillarKey)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + 1);
                    if (pillarName != null) existing.setPillarName(pillarName);
                    if (price != null) existing.setPrice(price);
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setUserId(userId);
                    item.setPillarKey(pillarKey);
                    item.setPillarName(pillarName);
                    item.setPrice(price != null ? price : BigDecimal.ZERO);
                    item.setQuantity(1);
                    return repository.save(item);
                });
    }

    @Transactional
    public CartItem updateQuantity(UUID id, int quantity) {
        CartItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (quantity <= 0) {
            repository.delete(item);
            return null;
        }
        item.setQuantity(quantity);
        return repository.save(item);
    }

    @Transactional
    public void remove(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        repository.deleteById(id);
    }

    @Transactional
    public void clear(UUID userId) {
        repository.deleteByUserId(userId);
    }
}
