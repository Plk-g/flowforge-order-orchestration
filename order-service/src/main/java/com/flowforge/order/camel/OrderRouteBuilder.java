package com.flowforge.order.camel;

import com.flowforge.order.api.dto.CreateOrderRequest;
import com.flowforge.order.service.OrderPersistenceService;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderRouteBuilder extends RouteBuilder {

    public static final String CREATE_ORDER_ENDPOINT = "direct:createOrder";

    private final OrderValidator orderValidator;
    private final OrderPersistenceService orderPersistenceService;

    public OrderRouteBuilder(OrderValidator orderValidator, OrderPersistenceService orderPersistenceService) {
        this.orderValidator = orderValidator;
        this.orderPersistenceService = orderPersistenceService;
    }

    @Override
    public void configure() {
        from(CREATE_ORDER_ENDPOINT)
                .routeId("create-order")
                .log("Validating incoming order")
                .process(exchange -> {
                    CreateOrderRequest request = exchange.getIn().getBody(CreateOrderRequest.class);
                    exchange.getIn().setBody(orderValidator.validate(request));
                })
                .bean(orderPersistenceService, "saveNewOrder")
                .log("Persisted order and queued ORDER_CREATED");
    }
}
