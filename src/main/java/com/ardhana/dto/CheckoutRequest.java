package com.ardhana.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull
    private UUID userId;

    @NotEmpty
    private List<CheckoutItemRequest> items;

    @NotNull
    private BigDecimal totalAmount;

    @Data
    public static class CheckoutItemRequest {
        private UUID pillarId;
        private String pillarName;
        private BigDecimal price;
        private String currency;
        private Integer quantity;
    }
}