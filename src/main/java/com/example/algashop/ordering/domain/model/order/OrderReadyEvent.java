package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.customer.CustomerId;

import java.time.OffsetDateTime;

public record OrderReadyEvent(OrderId orderId, CustomerId customerId, OffsetDateTime readyAt) {
}
