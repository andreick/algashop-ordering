package com.example.algashop.ordering.domain.exception;

import com.example.algashop.ordering.domain.entity.OrderStatus;
import com.example.algashop.ordering.domain.valueobject.id.OrderId;

public class OrderStatusCannotBeChangedException extends DomainException {

    public OrderStatusCannotBeChangedException(OrderId id, OrderStatus status, OrderStatus newStatus) {
        super(String.format(ErrorMessages.ERROR_ORDER_STATUS_CANNOT_BE_CHANGED, id, status, newStatus));
    }
}
