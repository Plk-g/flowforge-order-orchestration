package com.flowforge.order.api.dto;

import com.flowforge.platform.domain.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotNull FulfillmentType fulfillmentType,
        String currency,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "USD" : currency;
    }
}
