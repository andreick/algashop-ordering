package com.example.algashop.ordering.domain.exception;

import com.example.algashop.ordering.domain.valueobject.id.OrderId;

public class OrderInvalidShippingDeliveryDateException extends DomainException {

    public OrderInvalidShippingDeliveryDateException(OrderId id) {
        super(String.format(ErrorMessages.ERROR_ORDER_DELIVERY_DATE_CANNOT_BE_IN_THE_PAST, id));
    }
}
