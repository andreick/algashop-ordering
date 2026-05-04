package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.DomainService;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.product.Product;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class BuyNowService {

    private final CustomerHaveFreeShippingSpecification customerHaveFreeShippingSpecification;

    public Order buyNow(Product product,
            Customer customer,
            Billing billing,
            Shipping shipping,
            Quantity quantity,
            PaymentMethod paymentMethod) {

        product.checkOutOfStock();

        Order order = Order.draft(customer.id());
        order.changeBilling(billing);
        order.changePaymentMethod(paymentMethod);
        order.addItem(product, quantity);

        if (haveFreeShipping(customer)) {
            Shipping freeShipping = shipping.toBuilder().cost(Money.ZERO).build();
            order.changeShipping(freeShipping);
        } else {
            order.changeShipping(shipping);
        }

        order.place();

        return order;
    }

    private boolean haveFreeShipping(Customer customer) {
        return customerHaveFreeShippingSpecification.isSatisfiedBy(customer);
    }
}
