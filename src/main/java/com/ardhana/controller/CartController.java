package com.ardhana.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.model.CartItem;
import com.ardhana.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItem> list(@RequestParam("userId") UUID userId) {
        return cartService.findByUser(userId);
    }

    public record AddRequest(UUID userId, String pillarKey, String pillarName, BigDecimal price) {}

    @PostMapping
    public CartItem add(@RequestBody AddRequest req) {
        return cartService.addOrIncrement(req.userId(), req.pillarKey(), req.pillarName(), req.price());
    }

    public record QuantityRequest(int quantity) {}

    @PatchMapping("/{id}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable UUID id, @RequestBody QuantityRequest req) {
        CartItem updated = cartService.updateQuantity(id, req.quantity());
        return updated == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        cartService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@RequestParam("userId") UUID userId) {
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
