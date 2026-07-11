package com.ardhana.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ardhana.model.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemResponse> items;
    private Instant createdAt;

    // Only populated on the admin "all orders" listing (OrderService#getAllOrders) —
    // null on the buyer-scoped responses (checkout/verify/user history), which
    // already imply who the buyer is via the authenticated caller.
    private UUID buyerId;
    private String buyerName;
    private String buyerEmail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID pillarId;
        private String pillarName;
        private BigDecimal price;
        private String currency;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}