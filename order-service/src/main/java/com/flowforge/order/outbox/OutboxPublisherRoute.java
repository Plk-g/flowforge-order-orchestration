package com.flowforge.order.outbox;

import com.flowforge.order.domain.OutboxEntity;
import com.flowforge.order.persistence.OutboxRepository;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherRoute.class);

    private final OutboxPublisher outboxPublisher;
    private final boolean publishEnabled;

    public OutboxPublisherRoute(
            OutboxPublisher outboxPublisher,
            @Value("${app.outbox.publish-enabled:true}") boolean publishEnabled
    ) {
        this.outboxPublisher = outboxPublisher;
        this.publishEnabled = publishEnabled;
    }

    @Override
    public void configure() {
        from("timer:order-outbox?period={{app.outbox.poll-interval-ms:1000}}")
                .routeId("publish-order-outbox")
                .autoStartup(publishEnabled)
                .bean(outboxPublisher, "publishPending");
    }

    @Component
    @ConditionalOnProperty(name = "app.outbox.publish-enabled", havingValue = "true", matchIfMissing = true)
    public static class OutboxPublisher {
        private final OutboxRepository outboxRepository;
        private final KafkaTemplate<String, String> kafkaTemplate;
        private final String topic;

        public OutboxPublisher(
                OutboxRepository outboxRepository,
                KafkaTemplate<String, String> kafkaTemplate,
                @Value("${app.kafka.order-events-topic}") String topic
        ) {
            this.outboxRepository = outboxRepository;
            this.kafkaTemplate = kafkaTemplate;
            this.topic = topic;
        }

        @Transactional
        public void publishPending() {
            List<OutboxEntity> pending = outboxRepository.findTop20ByStatusOrderByCreatedAtAsc(
                    OutboxEntity.PublishStatus.PENDING
            );
            for (OutboxEntity event : pending) {
                event.setAttempts(event.getAttempts() + 1);
                try {
                    kafkaTemplate.send(topic, event.getOrderId().toString(), event.getPayload()).get();
                    event.setStatus(OutboxEntity.PublishStatus.PUBLISHED);
                    event.setPublishedAt(Instant.now());
                    log.info("Published {} for order {}", event.getEventType(), event.getOrderId());
                } catch (Exception ex) {
                    log.warn("Failed to publish {} for order {}: {}", event.getEventType(), event.getOrderId(), ex.getMessage());
                    if (event.getAttempts() >= 10) {
                        event.setStatus(OutboxEntity.PublishStatus.FAILED);
                    }
                }
            }
        }
    }
}
