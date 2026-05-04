package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.DomainService;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCart;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartCantProceedToCheckoutException;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartItem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@DomainService
@RequiredArgsConstructor
public class CheckoutService {

    private final CustomerHaveFreeShippingSpecification haveFreeShippingSpecification;

    public Order checkout(@NonNull Customer customer,
            @NonNull ShoppingCart shoppingCart,
            @NonNull Billing billing,
            @NonNull Shipping shipping,
            @NonNull PaymentMethod paymentMethod) {

        if (shoppingCart.isEmpty()) {
            throw new ShoppingCartCantProceedToCheckoutException();
        }

        if (shoppingCart.containsUnavailableItems()) {
            throw new ShoppingCartCantProceedToCheckoutException();
        }

        Set<ShoppingCartItem> items = shoppingCart.items();

        Order order = Order.draft(shoppingCart.customerId());
        order.changeBilling(billing);

        if (haveFreeShipping(customer)) {
            Shipping freeShipping = shipping.toBuilder().cost(Money.ZERO).build();
            order.changeShipping(freeShipping);
        } else {
            order.changeShipping(shipping);
        }

        order.changePaymentMethod(paymentMethod);

        for (ShoppingCartItem item : items) {
            order.addItem(new Product(item.productId(), item.name(),
                    item.price(), item.isAvailable()), item.quantity());
        }

        order.place();
        shoppingCart.empty();

        return order;
    }

    private boolean haveFreeShipping(Customer customer) {
        return haveFreeShippingSpecification.isSatisfiedBy(customer);
    }
}
