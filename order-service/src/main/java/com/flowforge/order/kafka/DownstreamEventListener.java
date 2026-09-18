package com.flowforge.order.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.order.service.OrderStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.kafka.consume-enabled", havingValue = "true", matchIfMissing = true)
public class DownstreamEventListener {

    private static final Logger log = LoggerFactory.getLogger(DownstreamEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderStatusService orderStatusService;

    public DownstreamEventListener(ObjectMapper objectMapper, OrderStatusService orderStatusService) {
        this.objectMapper = objectMapper;
        this.orderStatusService = orderStatusService;
    }

    @KafkaListener(topics = "${app.kafka.payment-events-topic}", groupId = "order-service-payments")
    public void onPayment(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        if (!"PAYMENT_COMPLETED".equals(event.path("eventType").asText())) {
            return;
        }
        UUID orderId = UUID.fromString(event.path("orderId").asText());
        log.info("Applying PAYMENT_COMPLETED to {}", orderId);
        orderStatusService.markPaid(orderId, "Payment authorized for " + event.path("authorizedAmount").asText());
    }

    @KafkaListener(topics = "${app.kafka.shipment-events-topic}", groupId = "order-service-shipments")
    public void onShipment(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        if (!"SHIPMENT_COMPLETED".equals(event.path("eventType").asText())) {
            return;
        }
        UUID orderId = UUID.fromString(event.path("orderId").asText());
        String tracking = event.path("trackingNumber").asText("n/a");
        log.info("Applying SHIPMENT_COMPLETED to {}", orderId);
        orderStatusService.markShipped(orderId, "Shipped via " + event.path("carrier").asText() + " " + tracking);
    }
}
