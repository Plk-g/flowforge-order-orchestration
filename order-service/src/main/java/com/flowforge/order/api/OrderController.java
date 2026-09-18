package com.flowforge.order.api;

import com.flowforge.order.api.dto.CreateOrderRequest;
import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.service.OrderOrchestrationService;
import com.flowforge.order.service.OrderPersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Create and retrieve orders")
public class OrderController {

    private final OrderOrchestrationService orchestrationService;
    private final OrderPersistenceService persistenceService;

    public OrderController(
            OrderOrchestrationService orchestrationService,
            OrderPersistenceService persistenceService
    ) {
        this.orchestrationService = orchestrationService;
        this.persistenceService = persistenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an order", description = "Validates the order via Apache Camel, persists it, and queues ORDER_CREATED")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orchestrationService.createOrder(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id")
    public OrderResponse get(@PathVariable UUID id) {
        return persistenceService.getOrder(id);
    }

    @GetMapping
    @Operation(summary = "List orders")
    public List<OrderResponse> list() {
        return persistenceService.listOrders();
    }
}
