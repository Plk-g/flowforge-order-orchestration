package com.flowforge.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.order.persistence.OutboxRepository;
import com.flowforge.order.service.OrderStatusService;
import com.flowforge.platform.event.OrderEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:flowforge;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("app.outbox.publish-enabled", () -> "false");
        registry.add("app.kafka.consume-enabled", () -> "false");
        registry.add("management.tracing.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OrderStatusService orderStatusService;

    @Test
    void createOrderThenQueryViaGraphql() throws Exception {
        String payload = """
                {
                  "customerId": "cust-42",
                  "fulfillmentType": "PHYSICAL",
                  "currency": "USD",
                  "items": [
                    { "sku": "SKU-100", "quantity": 2, "unitPrice": 19.99 }
                  ]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> created = restTemplate.postForEntity(
                url("/api/orders"),
                new HttpEntity<>(payload, headers),
                String.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdBody = objectMapper.readTree(created.getBody());
        String orderId = createdBody.get("id").asText();
        assertThat(createdBody.get("status").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(createdBody.get("totalAmount").decimalValue().toPlainString()).isEqualTo("39.98");
        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll().getFirst().getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);

        String graphql = """
                {
                  "query": "query($id: ID!) { order(id: $id) { id status customerId totalAmount items { sku quantity } timeline { status detail } } }",
                  "variables": { "id": "%s" }
                }
                """.formatted(orderId);

        ResponseEntity<String> queried = restTemplate.postForEntity(
                url("/graphql"),
                new HttpEntity<>(graphql, headers),
                String.class
        );

        assertThat(queried.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode graphqlBody = objectMapper.readTree(queried.getBody());
        assertThat(graphqlBody.path("errors").isMissingNode() || graphqlBody.path("errors").isEmpty()).isTrue();
        JsonNode order = graphqlBody.path("data").path("order");
        assertThat(order.path("id").asText()).isEqualTo(orderId);
        assertThat(order.path("status").asText()).isEqualTo("PAYMENT_PENDING");
        assertThat(order.path("customerId").asText()).isEqualTo("cust-42");
        assertThat(order.path("items")).hasSize(1);
        assertThat(order.path("timeline")).hasSize(3);

        orderStatusService.markPaid(java.util.UUID.fromString(orderId), "Payment authorized in test");
        ResponseEntity<String> afterPay = restTemplate.getForEntity(url("/api/orders/" + orderId), String.class);
        JsonNode paid = objectMapper.readTree(afterPay.getBody());
        assertThat(paid.get("status").asText()).isEqualTo("SHIPMENT_PENDING");
        assertThat(paid.get("timeline").size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void rejectInvalidOrder() {
        String payload = """
                {
                  "customerId": "cust-42",
                  "fulfillmentType": "DIGITAL",
                  "items": []
                }
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/orders"),
                new HttpEntity<>(payload, headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
