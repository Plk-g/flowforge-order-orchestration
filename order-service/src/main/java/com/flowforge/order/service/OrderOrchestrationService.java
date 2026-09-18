package com.flowforge.order.service;

import com.flowforge.order.api.OrderValidationException;
import com.flowforge.order.api.dto.CreateOrderRequest;
import com.flowforge.order.api.dto.OrderResponse;
import com.flowforge.order.camel.OrderRouteBuilder;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderOrchestrationService {

    private final ProducerTemplate producerTemplate;

    public OrderOrchestrationService(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        try {
            return producerTemplate.requestBody(OrderRouteBuilder.CREATE_ORDER_ENDPOINT, request, OrderResponse.class);
        } catch (CamelExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof OrderValidationException validationException) {
                throw validationException;
            }
            throw ex;
        }
    }
}
