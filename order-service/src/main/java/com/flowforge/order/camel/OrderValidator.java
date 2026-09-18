package com.flowforge.order.camel;

import com.flowforge.order.api.OrderValidationException;
import com.flowforge.order.api.dto.CreateOrderRequest;
import com.flowforge.order.api.dto.OrderItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class OrderValidator {

    public CreateOrderRequest validate(CreateOrderRequest request) {
        if (request == null) {
            throw new OrderValidationException("Order payload is required");
        }
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new OrderValidationException("customerId is required");
        }
        if (request.fulfillmentType() == null) {
            throw new OrderValidationException("fulfillmentType is required");
        }
        String currency = request.currencyOrDefault();
        if (currency.length() != 3) {
            throw new OrderValidationException("currency must be a 3-letter ISO code");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new OrderValidationException("At least one order item is required");
        }
        for (OrderItemRequest item : request.items()) {
            if (item.sku() == null || item.sku().isBlank()) {
                throw new OrderValidationException("Item sku is required");
            }
            if (item.quantity() < 1) {
                throw new OrderValidationException("Item quantity must be at least 1");
            }
            if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new OrderValidationException("Item unitPrice must be zero or greater");
            }
        }
        request.currencyOrDefault().toUpperCase(Locale.ROOT);
        return request;
    }
}
