package com.flowforge.platform.domain;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    PAYMENT_PENDING,
    PAID,
    SHIPMENT_PENDING,
    SHIPPED,
    FAILED
}
