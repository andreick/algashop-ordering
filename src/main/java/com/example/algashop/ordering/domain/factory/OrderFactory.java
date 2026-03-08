package com.example.algashop.ordering.domain.factory;

import com.example.algashop.ordering.domain.entity.Order;
import com.example.algashop.ordering.domain.entity.PaymentMethod;
import com.example.algashop.ordering.domain.valueobject.Billing;
import com.example.algashop.ordering.domain.valueobject.Product;
import com.example.algashop.ordering.domain.valueobject.Quantity;
import com.example.algashop.ordering.domain.valueobject.Shipping;
import com.example.algashop.ordering.domain.valueobject.id.CustomerId;
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
