package com.flowforge.order.graphql;

import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.service.OrderPersistenceService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class OrderGraphqlController {

    private final OrderPersistenceService persistenceService;

    public OrderGraphqlController(OrderPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @QueryMapping
    public OrderResponse order(@Argument UUID id) {
        return persistenceService.getOrder(id);
    }

    @QueryMapping
    public List<OrderResponse> orders() {
        return persistenceService.listOrders();
    }
}
