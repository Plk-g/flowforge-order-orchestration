package com.flowforge.order.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String sku,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {
}
