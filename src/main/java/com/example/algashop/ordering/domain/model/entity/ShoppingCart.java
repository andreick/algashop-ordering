package com.example.algashop.ordering.domain.model.entity;

import com.example.algashop.ordering.domain.model.exception.ShoppingCartDoesNotContainItemException;
import com.example.algashop.ordering.domain.model.exception.ShoppingCartDoesNotContainProductException;
import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.domain.model.valueobject.id.ProductId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartId;
import com.example.algashop.ordering.domain.model.valueobject.id.ShoppingCartItemId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
@Getter
@Setter(value = lombok.AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShoppingCart implements AggregateRoot<ShoppingCartId> {

    @EqualsAndHashCode.Include
    @NonNull
    private ShoppingCartId id;

    @NonNull
    private CustomerId customerId;

    @NonNull
    private Money totalAmount;

    @NonNull
    private Quantity totalItems;

    @NonNull
    private OffsetDateTime createdAt;

    @NonNull
    private Set<ShoppingCartItem> items;

    private Long version;

    public static ShoppingCart startShopping(CustomerId customerId) {
        return new ShoppingCart(
                new ShoppingCartId(), customerId, Money.ZERO, Quantity.ZERO, OffsetDateTime.now(), new HashSet<>());
    }

    @Builder(builderClassName = "ShoppingCartBuilder", builderMethodName = "existing")
    public ShoppingCart(ShoppingCartId id, CustomerId customerId, Money totalAmount, Quantity totalItems,
            OffsetDateTime createdAt, Set<ShoppingCartItem> items) {
        this.id(id);
        this.customerId(customerId);
        this.totalAmount(totalAmount);
        this.totalItems(totalItems);
        this.createdAt(createdAt);
        this.items(items);
    }

    public Set<ShoppingCartItem> items() {
        return Collections.unmodifiableSet(this.items);
    }

    public void empty() {
        this.items(new HashSet<>());
        this.totalAmount(Money.ZERO);
        this.totalItems(Quantity.ZERO);
    }

    public void addItem(@NonNull Product product, @NonNull Quantity quantity) {
        product.checkOutOfStock();

        try {
            ShoppingCartItem shoppingCartItem = findItem(product.id());
            shoppingCartItem.refresh(product);
            shoppingCartItem.changeQuantity(shoppingCartItem.quantity().add(quantity));
        } catch (ShoppingCartDoesNotContainProductException e) {
            ShoppingCartItem shoppingCartItem = ShoppingCartItem.brandNew()
                    .shoppingCartId(this.id)
                    .product(product)
                    .quantity(quantity)
                    .build();
            this.items.add(shoppingCartItem);
        }
        recalculateTotals();
    }

    public void removeItem(@NonNull ShoppingCartItemId shoppingCartItemId) {
        ShoppingCartItem shoppingCartItem = findItem(shoppingCartItemId);
        this.items.remove(shoppingCartItem);
        recalculateTotals();
    }

    public void refreshItem(@NonNull Product product) {
        ShoppingCartItem shoppingCartItem = findItem(product.id());
        shoppingCartItem.refresh(product);
        recalculateTotals();
    }

    public void changeItemQuantity(@NonNull ShoppingCartItemId shoppingCartItemId, @NonNull Quantity newQuantity) {
        ShoppingCartItem shoppingCartItem = findItem(shoppingCartItemId);
        shoppingCartItem.changeQuantity(newQuantity);
        recalculateTotals();
    }

    public boolean containsUnavailableItems() {
        return this.items.stream().anyMatch(item -> !item.isAvailable());
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public ShoppingCartItem findItem(ShoppingCartItemId shoppingCartItemId) {
        return this.items.stream()
                .filter(item -> shoppingCartItemId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new ShoppingCartDoesNotContainItemException(id, shoppingCartItemId));
    }

    public ShoppingCartItem findItem(ProductId productId) {
        return this.items.stream()
                .filter(item -> productId.equals(item.productId()))
                .findFirst()
                .orElseThrow(() -> new ShoppingCartDoesNotContainProductException(id, productId));
    }

    private void recalculateTotals() {
        this.totalAmount(this.items.stream().map(ShoppingCartItem::totalAmount).reduce(Money.ZERO, Money::add));
        this.totalItems(this.items.stream().map(ShoppingCartItem::quantity).reduce(Quantity.ZERO, Quantity::add));
    }
}
