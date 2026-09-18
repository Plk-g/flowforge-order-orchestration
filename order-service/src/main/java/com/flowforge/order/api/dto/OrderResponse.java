package com.flowforge.order.api.dto;

import com.flowforge.platform.domain.FulfillmentType;
import com.flowforge.platform.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        FulfillmentType fulfillmentType,
        String currency,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        List<TimelineEntryResponse> timeline,
        Instant createdAt,
        Instant updatedAt
) {
}
