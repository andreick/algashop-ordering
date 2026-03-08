package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.valueobject.Product;
import com.example.algashop.ordering.domain.valueobject.Quantity;
import com.example.algashop.ordering.domain.valueobject.id.CustomerId;

public class ShoppingCartTestDataBuilder {

    private final ShoppingCart shoppingCart;

    private ShoppingCartTestDataBuilder() {
        this.shoppingCart = ShoppingCart.startShopping(new CustomerId());
    }

    public static ShoppingCartTestDataBuilder anEmptyCart() {
        return new ShoppingCartTestDataBuilder();
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
