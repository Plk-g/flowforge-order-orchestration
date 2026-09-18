package com.flowforge.order.service;

import com.flowforge.order.api.OrderNotFoundException;
import com.flowforge.order.domain.OrderEntity;
import com.flowforge.order.persistence.OrderRepository;
import com.flowforge.platform.domain.FulfillmentType;
import com.flowforge.platform.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    private final OrderRepository orderRepository;
    private final OrderPersistenceService persistenceService;

    public OrderStatusService(OrderRepository orderRepository, OrderPersistenceService persistenceService) {
        this.orderRepository = orderRepository;
        this.persistenceService = persistenceService;
    }

    @Transactional
    public void markPaid(UUID orderId, String detail) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order %s was not found".formatted(orderId)));
        if (order.getStatus() == OrderStatus.FAILED || alreadyPast(order.getStatus(), OrderStatus.PAID)) {
            return;
        }
        Instant now = Instant.now();
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(now);
        persistenceService.append(order, OrderStatus.PAID, detail);
        if (order.getFulfillmentType() == FulfillmentType.DIGITAL) {
            order.setStatus(OrderStatus.SHIPPED);
            order.setUpdatedAt(Instant.now());
            persistenceService.append(order, OrderStatus.SHIPPED, "Digital fulfillment complete");
        } else {
            order.setStatus(OrderStatus.SHIPMENT_PENDING);
            order.setUpdatedAt(Instant.now());
            persistenceService.append(order, OrderStatus.SHIPMENT_PENDING, "Waiting for warehouse pickup");
        }
        log.info("Order {} advanced after payment to {}", orderId, order.getStatus());
    }

    @Transactional
    public void markShipped(UUID orderId, String detail) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order %s was not found".formatted(orderId)));
        if (order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.SHIPPED) {
            return;
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setUpdatedAt(Instant.now());
        persistenceService.append(order, OrderStatus.SHIPPED, detail);
        log.info("Order {} marked SHIPPED", orderId);
    }

    private boolean alreadyPast(OrderStatus current, OrderStatus target) {
        return current.ordinal() > target.ordinal();
    }
}
