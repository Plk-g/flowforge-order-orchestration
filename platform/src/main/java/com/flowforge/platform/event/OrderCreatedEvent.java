package com.flowforge.platform.event;

import com.flowforge.platform.domain.FulfillmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        OrderEventType eventType,
        UUID orderId,
        String customerId,
        BigDecimal totalAmount,
        String currency,
        FulfillmentType fulfillmentType,
        Instant occurredAt
) {
    public static OrderCreatedEvent of(
            UUID orderId,
            String customerId,
            BigDecimal totalAmount,
            String currency,
            FulfillmentType fulfillmentType
    ) {
        return new OrderCreatedEvent(
                OrderEventType.ORDER_CREATED,
                orderId,
                customerId,
                totalAmount,
                currency,
                fulfillmentType,
                Instant.now()
        );
    }
}
