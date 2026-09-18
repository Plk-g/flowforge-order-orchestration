package com.flowforge.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.platform.domain.FulfillmentType;
import com.flowforge.platform.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String paymentTopic;
    private final long processingDelayMs;

    public OrderCreatedListener(
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.payment-events-topic}") String paymentTopic,
            @Value("${app.processing-delay-ms:1500}") long processingDelayMs
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentTopic = paymentTopic;
        this.processingDelayMs = processingDelayMs;
    }

    @KafkaListener(topics = "${app.kafka.order-events-topic}", groupId = "payment-service")
    public void onOrderEvent(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        if (!"ORDER_CREATED".equals(event.path("eventType").asText())) {
            return;
        }
        UUID orderId = UUID.fromString(event.path("orderId").asText());
        BigDecimal amount = new BigDecimal(event.path("totalAmount").asText());
        String currency = event.path("currency").asText("USD");
        FulfillmentType fulfillmentType = FulfillmentType.valueOf(event.path("fulfillmentType").asText());
        log.info("Authorizing payment for order {} amount {} {}", orderId, amount, currency);
        Thread.sleep(processingDelayMs);
        PaymentCompletedEvent completed = PaymentCompletedEvent.of(orderId, amount, currency, fulfillmentType);
        kafkaTemplate.send(paymentTopic, orderId.toString(), objectMapper.writeValueAsString(completed)).get();
        log.info("Published PAYMENT_COMPLETED for {}", orderId);
    }
}
