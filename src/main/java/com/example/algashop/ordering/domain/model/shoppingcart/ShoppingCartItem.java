package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductId;
import com.example.algashop.ordering.domain.model.product.ProductName;
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
    private ProductName name;

    @NonNull
    private Money price;

    @NonNull
    private Quantity quantity;

    @NonNull
    private Money totalAmount;

    @NonNull
    private Boolean isAvailable;

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
        this.name(productName);
        this.price(price);
        this.quantity(quantity);
        this.totalAmount(totalAmount);
        this.isAvailable(available);
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

        this.name(product.name());
        this.price(product.price());
        this.isAvailable(product.inStock());
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
