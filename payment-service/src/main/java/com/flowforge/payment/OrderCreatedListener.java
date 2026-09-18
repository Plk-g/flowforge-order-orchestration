package com.flowforge.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final ObjectMapper objectMapper;

    public OrderCreatedListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.order-events-topic}", groupId = "payment-service")
    public void onOrderEvent(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        String eventType = event.path("eventType").asText();
        if (!"ORDER_CREATED".equals(eventType)) {
            return;
        }
        String orderId = event.path("orderId").asText();
        String amount = event.path("totalAmount").asText();
        log.info("Simulated payment authorization for order {} amount {}", orderId, amount);
    }
}
