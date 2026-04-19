package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.customer.CustomerTestDataBuilder;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductTestDataBuilder;

public class ShoppingCartTestDataBuilder {

    public CustomerId customerId = CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID;
    public static final ShoppingCartId DEFAULT_SHOPPING_CART_ID = new ShoppingCartId();

    private final ShoppingCart shoppingCart;

    private ShoppingCartTestDataBuilder() {
        this.shoppingCart = ShoppingCart.startShopping(customerId);
    }

    public static ShoppingCartTestDataBuilder anEmptyCart() {
        return new ShoppingCartTestDataBuilder();
    }

    public static ShoppingCartTestDataBuilder aShoppingCart() {
        return anEmptyCart().withDefaultItems();
    }

    private ShoppingCartTestDataBuilder withDefaultItems() {
        this.shoppingCart.addItem(ProductTestDataBuilder.aProduct().build(), new Quantity(2));
        this.shoppingCart.addItem(ProductTestDataBuilder.aProductAltRamMemory().build(), new Quantity(1));
        return this;
    }

    public ShoppingCartTestDataBuilder customerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }

    public ShoppingCartTestDataBuilder withItem(Product product, Quantity quantity) {
        this.shoppingCart.addItem(product, quantity);
        return this;
    }

    public ShoppingCartTestDataBuilder withUnavailableItem() {
        this.shoppingCart.addItem(ProductTestDataBuilder.aProductUnavailable().build(), new Quantity(1));
        return this;
    }

    public ShoppingCart build() {
        return this.shoppingCart;
    }
}
