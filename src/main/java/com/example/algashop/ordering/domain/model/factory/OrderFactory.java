package com.example.algashop.ordering.domain.model.factory;

import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.PaymentMethod;
import com.example.algashop.ordering.domain.model.valueobject.Billing;
import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.Shipping;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import lombok.NonNull;

public class OrderFactory {

    private OrderFactory() {
    }

    public static Order filled(
            @NonNull CustomerId customerId,
            @NonNull Shipping shipping,
            @NonNull Billing billing,
            @NonNull PaymentMethod paymentMethod,
            @NonNull Product product,
            @NonNull Quantity productQuantity) {
        Order order = Order.draft(customerId);

        order.changeBilling(billing);
        order.changeShipping(shipping);
        order.changePaymentMethod(paymentMethod);
        order.addItem(product, productQuantity);

        return order;
    }
}
