package com.example.algashop.ordering.application.checkout;

import com.example.algashop.ordering.domain.model.commons.ZipCode;
import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.customer.CustomerNotFoundException;
import com.example.algashop.ordering.domain.model.customer.Customers;
import com.example.algashop.ordering.domain.model.order.CheckoutService;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.Orders;
import com.example.algashop.ordering.domain.model.order.PaymentMethod;
import com.example.algashop.ordering.domain.model.order.shipping.OriginAddressService;
import com.example.algashop.ordering.domain.model.order.shipping.ShippingCostService;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCart;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartId;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartNotFoundException;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCarts;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutApplicationService {

    private final Orders orders;
    private final ShoppingCarts shoppingCarts;
    private final Customers customers;

    private final CheckoutService checkoutService;

    private final BillingInputDisassembler billingInputDisassembler;
    private final ShippingInputDisassembler shippingInputDisassembler;

    private final ShippingCostService shippingCostService;
    private final OriginAddressService originAddressService;

    @Transactional
    public String checkout(@NonNull CheckoutInput input) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(input.getPaymentMethod());

        ShoppingCartId shoppingCartId = new ShoppingCartId(input.getShoppingCartId());
        ShoppingCart shoppingCart = shoppingCarts.ofId(shoppingCartId)
                .orElseThrow(ShoppingCartNotFoundException::new);

        Customer customer = customers.ofId(shoppingCart.customerId()).orElseThrow(CustomerNotFoundException::new);

        var shippingCalculationResult = calculateShippingCost(input.getShipping());

        Order order = checkoutService.checkout(customer, shoppingCart,
                billingInputDisassembler.toDomainModel(input.getBilling()),
                shippingInputDisassembler.toDomainModel(input.getShipping(), shippingCalculationResult),
                paymentMethod);

        orders.add(order);
        shoppingCarts.add(shoppingCart);

        return order.id().toString();
    }

    private ShippingCostService.CalculationResult calculateShippingCost(ShippingInput shipping) {
        ZipCode origin = originAddressService.originAddress().zipCode();
        ZipCode destination = new ZipCode(shipping.getAddress().getZipCode());
        return shippingCostService.calculate(new ShippingCostService.CalculationRequest(origin, destination));
    }
}
