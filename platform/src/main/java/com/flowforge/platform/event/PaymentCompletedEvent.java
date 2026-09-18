package com.flowforge.platform.event;

import com.flowforge.platform.domain.FulfillmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        OrderEventType eventType,
        UUID orderId,
        BigDecimal authorizedAmount,
        String currency,
        FulfillmentType fulfillmentType,
        Instant occurredAt
) {
    public static PaymentCompletedEvent of(
            UUID orderId,
            BigDecimal authorizedAmount,
            String currency,
            FulfillmentType fulfillmentType
    ) {
        return new PaymentCompletedEvent(
                OrderEventType.PAYMENT_COMPLETED,
                orderId,
                authorizedAmount,
                currency,
                fulfillmentType,
                Instant.now()
        );
    }
}
