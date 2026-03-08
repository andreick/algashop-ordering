package com.example.algashop.ordering.domain.exception;

import com.example.algashop.ordering.domain.entity.OrderStatus;
import com.example.algashop.ordering.domain.valueobject.id.OrderId;

public class OrderCannotBeEditedException extends DomainException {

    public OrderCannotBeEditedException(OrderId orderId, OrderStatus orderStatus) {
        super(String.format(ErrorMessages.ERROR_ORDER_CANNOT_BE_EDITED, orderId, orderStatus));
    }
}
