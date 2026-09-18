package com.flowforge.shipment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.platform.event.ShipmentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedListener.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String shipmentTopic;
    private final long processingDelayMs;

    public PaymentCompletedListener(
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.shipment-events-topic}") String shipmentTopic,
            @Value("${app.processing-delay-ms:1500}") long processingDelayMs
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.shipmentTopic = shipmentTopic;
        this.processingDelayMs = processingDelayMs;
    }

    @KafkaListener(topics = "${app.kafka.payment-events-topic}", groupId = "shipment-service")
    public void onPayment(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        if (!"PAYMENT_COMPLETED".equals(event.path("eventType").asText())) {
            return;
        }
        if ("DIGITAL".equals(event.path("fulfillmentType").asText())) {
            log.info("Skipping physical shipment for digital order {}", event.path("orderId").asText());
            return;
        }
        UUID orderId = UUID.fromString(event.path("orderId").asText());
        log.info("Scheduling warehouse pickup for {}", orderId);
        Thread.sleep(processingDelayMs);
        String tracking = "FF-" + orderId.toString().substring(0, 8).toUpperCase();
        ShipmentCompletedEvent completed = ShipmentCompletedEvent.of(orderId, "FlowForge Logistics", tracking);
        kafkaTemplate.send(shipmentTopic, orderId.toString(), objectMapper.writeValueAsString(completed)).get();
        log.info("Published SHIPMENT_COMPLETED for {} tracking {}", orderId, tracking);
    }
}
