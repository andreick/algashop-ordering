package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.product.Product;
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
