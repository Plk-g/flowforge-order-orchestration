package com.flowforge.platform.event;

import java.time.Instant;
import java.util.UUID;

public record ShipmentCompletedEvent(
        OrderEventType eventType,
        UUID orderId,
        String carrier,
        String trackingNumber,
        Instant occurredAt
) {
    public static ShipmentCompletedEvent of(UUID orderId, String carrier, String trackingNumber) {
        return new ShipmentCompletedEvent(
                OrderEventType.SHIPMENT_COMPLETED,
                orderId,
                carrier,
                trackingNumber,
                Instant.now()
        );
    }
}
