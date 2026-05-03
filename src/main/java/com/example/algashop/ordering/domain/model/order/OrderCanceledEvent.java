package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.customer.CustomerId;

import java.time.OffsetDateTime;

public record OrderCanceledEvent(OrderId orderId, CustomerId customerId, OffsetDateTime canceledAt) {
}
