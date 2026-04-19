package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.DomainException;
import com.example.algashop.ordering.domain.model.ErrorMessages;

public class OrderStatusCannotBeChangedException extends DomainException {

    public OrderStatusCannotBeChangedException(OrderId id, OrderStatus status, OrderStatus newStatus) {
        super(String.format(ErrorMessages.ERROR_ORDER_STATUS_CANNOT_BE_CHANGED, id, status, newStatus));
    }
}
