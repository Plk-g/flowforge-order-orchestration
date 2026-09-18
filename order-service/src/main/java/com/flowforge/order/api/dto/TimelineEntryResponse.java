package com.flowforge.order.api.dto;

import com.flowforge.platform.domain.OrderStatus;

import java.time.Instant;

public record TimelineEntryResponse(
        OrderStatus status,
        String detail,
        Instant occurredAt
) {
}
