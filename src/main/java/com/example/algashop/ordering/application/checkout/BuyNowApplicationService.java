package com.example.algashop.ordering.application.checkout;

import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.commons.ZipCode;
import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.customer.CustomerNotFoundException;
import com.example.algashop.ordering.domain.model.customer.Customers;
import com.example.algashop.ordering.domain.model.order.Billing;
import com.example.algashop.ordering.domain.model.order.BuyNowService;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.Orders;
import com.example.algashop.ordering.domain.model.order.PaymentMethod;
import com.example.algashop.ordering.domain.model.order.Shipping;
import com.example.algashop.ordering.domain.model.order.shipping.OriginAddressService;
import com.example.algashop.ordering.domain.model.order.shipping.ShippingCostService;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductCatalogService;
import com.example.algashop.ordering.domain.model.product.ProductId;
import com.example.algashop.ordering.domain.model.product.ProductNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyNowApplicationService {

    private final BuyNowService buyNowService;
    private final ProductCatalogService productCatalogService;

    private final ShippingCostService shippingCostService;
    private final OriginAddressService originAddressService;

    private final Orders orders;
    private final Customers customers;

    private final ShippingInputDisassembler shippingInputDisassembler;
    private final BillingInputDisassembler billingInputDisassembler;

    @Transactional
    public String buyNow(@NonNull BuyNowInput input) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(input.getPaymentMethod());
        CustomerId customerId = new CustomerId(input.getCustomerId());
        Quantity quantity = new Quantity(input.getQuantity());

        Customer customer = customers.ofId(customerId).orElseThrow(CustomerNotFoundException::new);

        Product product = findProduct(new ProductId(input.getProductId()));

        var shippingCalculationResult = calculateShippingCost(input.getShipping());

        Shipping shipping = shippingInputDisassembler.toDomainModel(input.getShipping(),
                shippingCalculationResult);

        Billing billing = billingInputDisassembler.toDomainModel(input.getBilling());

        Order order = buyNowService.buyNow(product, customer, billing, shipping, quantity, paymentMethod);

        orders.add(order);

        return order.id().toString();
    }

    private ShippingCostService.CalculationResult calculateShippingCost(ShippingInput shipping) {
        ZipCode origin = originAddressService.originAddress().zipCode();
        ZipCode destination = new ZipCode(shipping.getAddress().getZipCode());
        return shippingCostService.calculate(new ShippingCostService.CalculationRequest(origin, destination));
    }

    private Product findProduct(ProductId productId) {
        return productCatalogService.ofId(productId)
                .orElseThrow(ProductNotFoundException::new);
    }
}
