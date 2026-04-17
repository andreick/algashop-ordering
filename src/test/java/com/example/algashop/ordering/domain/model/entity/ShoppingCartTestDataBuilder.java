package com.example.algashop.ordering.domain.model.entity;

import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartId;

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
