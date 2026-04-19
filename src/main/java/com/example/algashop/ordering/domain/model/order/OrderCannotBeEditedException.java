package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.DomainException;
import com.example.algashop.ordering.domain.model.ErrorMessages;

public class OrderCannotBeEditedException extends DomainException {

    public OrderCannotBeEditedException(OrderId orderId, OrderStatus orderStatus) {
        super(String.format(ErrorMessages.ERROR_ORDER_CANNOT_BE_EDITED, orderId, orderStatus));
    }
}
