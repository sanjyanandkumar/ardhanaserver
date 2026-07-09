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

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    public CartItem addOrIncrement(UUID userId, String pillarKey, String pillarName, BigDecimal price, String currency) {
        if (userId == null || pillarKey == null || pillarKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and pillarKey are required");
        }
        String effectiveCurrency = (currency == null || currency.isBlank()) ? "INR" : currency.toUpperCase();

        // Razorpay orders are single-currency, so a cart can't mix currencies —
        // reject adding an item that doesn't match what's already in the cart.
        List<CartItem> existingItems = repository.findByUserIdOrderByAddedAtDesc(userId);
        boolean mismatchedCurrency = existingItems.stream()
                .anyMatch(i -> !i.getPillarKey().equals(pillarKey) && !i.getCurrency().equalsIgnoreCase(effectiveCurrency));
        if (mismatchedCurrency) {
            String cartCurrency = existingItems.get(0).getCurrency();
            log.warn("Cart add rejected for userId={}: cart currency={}, attempted currency={} (pillarKey={})",
                    userId, cartCurrency, effectiveCurrency, pillarKey);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your cart already has items priced in " + cartCurrency
                    + ". Clear your cart before adding a course priced in " + effectiveCurrency + ".");
        }

        return repository.findByUserIdAndPillarKey(userId, pillarKey)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + 1);
                    if (pillarName != null) existing.setPillarName(pillarName);
                    if (price != null) existing.setPrice(price);
                    existing.setCurrency(effectiveCurrency);
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setUserId(userId);
                    item.setPillarKey(pillarKey);
                    item.setPillarName(pillarName);
                    item.setPrice(price != null ? price : BigDecimal.ZERO);
                    item.setCurrency(effectiveCurrency);
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
