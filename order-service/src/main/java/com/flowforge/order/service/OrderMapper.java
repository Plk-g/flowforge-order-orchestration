package com.flowforge.order.service;

import com.flowforge.order.api.dto.OrderItemResponse;
import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.domain.OrderEntity;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(OrderEntity entity) {
        List<OrderItemResponse> items = entity.getItems().stream()
                .map(item -> new OrderItemResponse(item.getSku(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getFulfillmentType(),
                entity.getCurrency(),
                entity.getTotalAmount(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
