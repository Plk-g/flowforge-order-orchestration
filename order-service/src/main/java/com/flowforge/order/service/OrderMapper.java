package com.flowforge.order.service;

import com.flowforge.order.api.dto.OrderItemResponse;
import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.api.dto.TimelineEntryResponse;
import com.flowforge.order.domain.OrderEntity;
import com.flowforge.order.domain.OrderTimelineEntity;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(OrderEntity entity, List<OrderTimelineEntity> timeline) {
        List<OrderItemResponse> items = entity.getItems().stream()
                .map(item -> new OrderItemResponse(item.getSku(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        List<TimelineEntryResponse> entries = timeline.stream()
                .map(entry -> new TimelineEntryResponse(entry.getStatus(), entry.getDetail(), entry.getOccurredAt()))
                .toList();
        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getFulfillmentType(),
                entity.getCurrency(),
                entity.getTotalAmount(),
                items,
                entries,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
