package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductTestDataBuilder;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ShoppingCartItemTest {

    @Test
    void newItem_totalAmountCalculated() {
        Product prod = ProductTestDataBuilder.aProduct().build();
        ShoppingCartItem item = ShoppingCartItem.brandNew()
                .shoppingCartId(new ShoppingCartId())
                .product(prod)
                .quantity(new Quantity(2))
                .build();

        assertThat(item.totalAmount()).isEqualTo(prod.price().multiply(new Quantity(2)));
    }

    @Test
    void changeQuantity_toZero_throws() {
        ShoppingCartItem item = ShoppingCartItem.brandNew()
                .shoppingCartId(new ShoppingCartId())
                .product(ProductTestDataBuilder.aProduct().build())
                .quantity(new Quantity(1))
                .build();

        ThrowingCallable changeQuantityToZero = () -> item.changeQuantity(new Quantity(0));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(changeQuantityToZero);
    }

    @Test
    void refresh_withIncompatibleProduct_throws() {
        Product p1 = ProductTestDataBuilder.aProduct().build();
        Product p2 = ProductTestDataBuilder.aProductAltRamMemory().build();

        ShoppingCartItem item = ShoppingCartItem.brandNew()
                .shoppingCartId(new ShoppingCartId())
                .product(p1)
                .quantity(new Quantity(1))
                .build();

        ThrowingCallable refreshIncompatibleProduct = () -> item.refresh(p2);
        assertThatExceptionOfType(ShoppingCartItemIncompatibleProductException.class)
                .isThrownBy(refreshIncompatibleProduct);
    }

    @Test
    void equality_onlyBasedOnId() {
        ShoppingCartItemId id = new ShoppingCartItemId();
        ShoppingCartItem item1 = ShoppingCartItem.existing()
                .id(id)
                .shoppingCartId(new ShoppingCartId())
                .productId(ProductTestDataBuilder.aProduct().build().id())
                .productName(ProductTestDataBuilder.aProduct().build().name())
                .price(new Money("10"))
                .quantity(new Quantity(1))
                .totalAmount(new Money("10"))
                .available(true)
                .build();

        ShoppingCartItem item2 = ShoppingCartItem.existing()
                .id(id)
                .shoppingCartId(new ShoppingCartId())
                .productId(ProductTestDataBuilder.aProductAltRamMemory().build().id())
                .productName(ProductTestDataBuilder.aProductAltRamMemory().build().name())
                .price(new Money("20"))
                .quantity(new Quantity(5))
                .totalAmount(new Money("100"))
                .available(false)
                .build();

        assertThat(item1).isEqualTo(item2);
    }
}
