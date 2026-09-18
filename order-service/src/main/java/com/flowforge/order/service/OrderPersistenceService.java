package com.flowforge.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.order.api.OrderNotFoundException;
import com.flowforge.order.api.dto.CreateOrderRequest;
import com.flowforge.order.api.dto.OrderItemRequest;
import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.domain.OrderEntity;
import com.flowforge.order.domain.OrderItemEntity;
import com.flowforge.order.domain.OutboxEntity;
import com.flowforge.order.persistence.OrderRepository;
import com.flowforge.order.persistence.OutboxRepository;
import com.flowforge.platform.domain.OrderStatus;
import com.flowforge.platform.event.OrderCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderPersistenceService(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse saveNewOrder(CreateOrderRequest request) {
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();
        BigDecimal total = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setCustomerId(request.customerId());
        order.setFulfillmentType(request.fulfillmentType());
        order.setCurrency(request.currencyOrDefault());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setSku(itemRequest.sku());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice().setScale(2, RoundingMode.HALF_UP));
            order.addItem(item);
        }
        orderRepository.save(order);

        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getFulfillmentType()
        );
        OutboxEntity outbox = new OutboxEntity();
        outbox.setId(UUID.randomUUID());
        outbox.setOrderId(orderId);
        outbox.setEventType(event.eventType());
        outbox.setPayload(writePayload(event));
        outbox.setStatus(OutboxEntity.PublishStatus.PENDING);
        outbox.setCreatedAt(now);
        outbox.setAttempts(0);
        outboxRepository.save(outbox);

        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return orderRepository.findById(id)
                .map(OrderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order %s was not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream().map(OrderMapper::toResponse).toList();
    }

    private String writePayload(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize order event", e);
        }
    }
}
