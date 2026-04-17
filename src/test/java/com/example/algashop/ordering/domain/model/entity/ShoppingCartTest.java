package com.example.algashop.ordering.domain.model.entity;

import com.example.algashop.ordering.domain.model.exception.ProductOutOfStockException;
import com.example.algashop.ordering.domain.model.exception.ShoppingCartDoesNotContainItemException;
import com.example.algashop.ordering.domain.model.exception.ShoppingCartDoesNotContainProductException;
import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartItemId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ShoppingCartTest {

    @Test
    void given_newCart_whenStarted_thenTotalsZeroAndEmpty() {
        ShoppingCart cart = ShoppingCart.startShopping(new CustomerId());

        assertThat(cart.totalAmount()).isEqualTo(Money.ZERO);
        assertThat(cart.totalItems()).isEqualTo(Quantity.ZERO);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void given_outOfStockProduct_whenAddItem_thenThrows() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.anEmptyCart().build();
        Product prod = ProductTestDataBuilder.aProductUnavailable().build();

        ThrowingCallable addOutOfStockProduct = () -> cart.addItem(prod, new Quantity(1));
        assertThatExceptionOfType(ProductOutOfStockException.class).isThrownBy(addOutOfStockProduct);
    }

    @Test
    void whenAddSameProductTwice_quantitiesMergeAndTotalsRecalculated() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.anEmptyCart().build();
        Product prod = ProductTestDataBuilder.aProduct().build();

        cart.addItem(prod, new Quantity(1));
        cart.addItem(prod, new Quantity(2));

        assertThat(cart.items()).hasSize(1);
        ShoppingCartItem item = cart.items().iterator().next();
        assertThat(item.quantity()).isEqualTo(new Quantity(3));
        assertThat(item.price()).isEqualTo(prod.price());
        assertThat(item.isAvailable()).isEqualTo(prod.inStock());
        assertThat(cart.totalItems()).isEqualTo(new Quantity(3));
        assertThat(cart.totalAmount()).isEqualTo(prod.price().multiply(new Quantity(3)));
    }

    @Test
    void whenAddDifferentProducts_twoDistinctItemsAndTotals() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.anEmptyCart().build();
        Product p1 = ProductTestDataBuilder.aProduct().build();
        Product p2 = ProductTestDataBuilder.aProductAltRamMemory().build();

        cart.addItem(p1, new Quantity(2));
        cart.addItem(p2, new Quantity(1));

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.totalItems()).isEqualTo(new Quantity(3));
        Money expected = p1.price().multiply(new Quantity(2)).add(p2.price());
        assertThat(cart.totalAmount()).isEqualTo(expected);
    }

    @Test
    void removeNonexistentItem_shouldThrow() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.aShoppingCart().build();

        ThrowingCallable removeNonexistentItem = () -> cart.removeItem(new ShoppingCartItemId());
        assertThatExceptionOfType(ShoppingCartDoesNotContainItemException.class).isThrownBy(removeNonexistentItem);
    }

    @Test
    void empty_shouldClearAll() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.aShoppingCart()
                .withItem(ProductTestDataBuilder.aProduct().build(), new Quantity(1))
                .withItem(ProductTestDataBuilder.aProductAltRamMemory().build(), new Quantity(2))
                .build();

        cart.empty();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.totalItems()).isEqualTo(Quantity.ZERO);
        assertThat(cart.totalAmount()).isEqualTo(Money.ZERO);
    }

    @Test
    void refreshItem_withUnknownProduct_shouldThrow() {
        ShoppingCart cart = ShoppingCartTestDataBuilder.aShoppingCart()
                .withItem(ProductTestDataBuilder.aProduct().build(), new Quantity(1))
                .build();

        ThrowingCallable refreshUnknownProduct = () -> cart
                .refreshItem(ProductTestDataBuilder.aProductAltRamMemory().build());
        assertThatExceptionOfType(ShoppingCartDoesNotContainProductException.class).isThrownBy(refreshUnknownProduct);
    }

    @Test
    void equality_onlyBasedOnId() {
        ShoppingCartId id = new ShoppingCartId();
        ShoppingCart cart1 = ShoppingCart.existing()
                .id(id)
                .customerId(new CustomerId())
                .totalAmount(Money.ZERO)
                .totalItems(Quantity.ZERO)
                .createdAt(OffsetDateTime.now())
                .items(new HashSet<>())
                .build();

        ShoppingCart cart2 = ShoppingCart.existing()
                .id(id)
                .customerId(new CustomerId())
                .totalAmount(new Money("100"))
                .totalItems(new Quantity(5))
                .createdAt(OffsetDateTime.now())
                .items(new HashSet<>())
                .build();

        assertThat(cart1).isEqualTo(cart2);
    }
}
