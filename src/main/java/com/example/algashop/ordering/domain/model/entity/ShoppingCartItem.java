package com.example.algashop.ordering.domain.model.entity;

import com.example.algashop.ordering.domain.model.exception.ShoppingCartItemIncompatibleProductException;
import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.ProductName;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.ProductId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartItemId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter(value = lombok.AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShoppingCartItem {

    @EqualsAndHashCode.Include
    @NonNull
    private ShoppingCartItemId id;

    @NonNull
    private ShoppingCartId shoppingCartId;

    @NonNull
    private ProductId productId;

    @NonNull
    private ProductName productName;

    @NonNull
    private Money price;

    @NonNull
    private Quantity quantity;

    @NonNull
    private Money totalAmount;

    @NonNull
    private Boolean available;

    @Builder(builderClassName = "BrandNewShoppingCartItemBuilder", builderMethodName = "brandNew")
    private static ShoppingCartItem createBrandNew(ShoppingCartId shoppingCartId, Product product,
            Quantity quantity) {
        ShoppingCartItem shoppingCartItem = new ShoppingCartItem(
                new ShoppingCartItemId(),
                shoppingCartId,
                product.id(),
                product.name(),
                product.price(),
                quantity,
                Money.ZERO,
                product.inStock());
        shoppingCartItem.recalculateTotals();
        return shoppingCartItem;
    }

    @Builder(builderClassName = "ExistingShoppingCartItemBuilder", builderMethodName = "existing")
    public ShoppingCartItem(ShoppingCartItemId id, ShoppingCartId shoppingCartId, ProductId productId,
            ProductName productName, Money price, Quantity quantity, Money totalAmount, Boolean available) {
        this.id(id);
        this.shoppingCartId(shoppingCartId);
        this.productId(productId);
        this.productName(productName);
        this.price(price);
        this.quantity(quantity);
        this.totalAmount(totalAmount);
        this.available(available);
    }

    public ShoppingCartItem quantity(@NonNull Quantity quantity) {
        if (quantity.value() < 1) {
            throw new IllegalArgumentException();
        }
        this.quantity = quantity;
        return this;
    }

    public void refresh(@NonNull Product product) {
        if (!this.productId().equals(product.id())) {
            throw new ShoppingCartItemIncompatibleProductException(this.id(), product.id());
        }

        this.productName(product.name());
        this.price(product.price());
        this.available(product.inStock());
        recalculateTotals();
    }

    public void changeQuantity(@NonNull Quantity newQuantity) {
        this.quantity(newQuantity);
        recalculateTotals();
    }

    private void recalculateTotals() {
        this.totalAmount(this.price.multiply(this.quantity));
    }
}
