package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.AbstractEventSourceEntity;
import com.example.algashop.ordering.domain.model.AggregateRoot;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductId;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ShoppingCart extends AbstractEventSourceEntity implements AggregateRoot<ShoppingCartId> {

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
        ShoppingCart shoppingCart = new ShoppingCart(new ShoppingCartId(), customerId, Money.ZERO,
                Quantity.ZERO, OffsetDateTime.now(), new HashSet<>());
        shoppingCart.publishDomainEvent(new ShoppingCartCreatedEvent(
                shoppingCart.id(),
                shoppingCart.customerId(),
                shoppingCart.createdAt()));
        return shoppingCart;
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
        this.publishDomainEvent(new ShoppingCartEmptiedEvent(
                this.id(),
                this.customerId(),
                OffsetDateTime.now()));
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

        this.publishDomainEvent(new ShoppingCartItemAddedEvent(
                this.id(),
                this.customerId(),
                product.id(),
                OffsetDateTime.now()));
    }

    public void removeItem(@NonNull ShoppingCartItemId shoppingCartItemId) {
        ShoppingCartItem shoppingCartItem = findItem(shoppingCartItemId);
        this.items.remove(shoppingCartItem);
        recalculateTotals();
        this.publishDomainEvent(new ShoppingCartItemRemovedEvent(
                this.id(),
                this.customerId(),
                shoppingCartItem.productId(),
                OffsetDateTime.now()));
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
