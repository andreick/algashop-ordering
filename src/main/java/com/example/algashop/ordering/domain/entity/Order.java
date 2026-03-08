package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.exception.OrderCannotBePlacedException;
import com.example.algashop.ordering.domain.exception.OrderDoesNotContainOrderItemException;
import com.example.algashop.ordering.domain.exception.OrderInvalidShippingDeliveryDateException;
import com.example.algashop.ordering.domain.exception.OrderStatusCannotBeChangedException;
import com.example.algashop.ordering.domain.valueobject.Billing;
import com.example.algashop.ordering.domain.valueobject.Money;
import com.example.algashop.ordering.domain.valueobject.Product;
import com.example.algashop.ordering.domain.valueobject.Quantity;
import com.example.algashop.ordering.domain.valueobject.Shipping;
import com.example.algashop.ordering.domain.valueobject.id.CustomerId;
import com.example.algashop.ordering.domain.valueobject.id.OrderId;
import com.example.algashop.ordering.domain.valueobject.id.OrderItemId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Accessors(fluent = true)
@Getter
public class Order {

    private OrderId id;
    private CustomerId customerId;

    private Money totalAmount;
    private Quantity totalItems;

    private OffsetDateTime placedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime canceledAt;
    private OffsetDateTime readyAt;

    private Billing billing;
    private Shipping shipping;

    private OrderStatus status;
    private PaymentMethod paymentMethod;

    private Set<OrderItem> items;

    @Builder(builderClassName = "ExistingOrderBuilder", builderMethodName = "existing")
    public Order(OrderId id, CustomerId customerId,
            Money totalAmount, Quantity totalItems,
            OffsetDateTime placedAt, OffsetDateTime paidAt,
            OffsetDateTime canceledAt, OffsetDateTime readyAt,
            Billing billing, Shipping shipping,
            OrderStatus status, PaymentMethod paymentMethod,
            Set<OrderItem> items) {
        this.setId(id);
        this.setCustomerId(customerId);
        this.setTotalAmount(totalAmount);
        this.setTotalItems(totalItems);
        this.setPlacedAt(placedAt);
        this.setPaidAt(paidAt);
        this.setCanceledAt(canceledAt);
        this.setReadyAt(readyAt);
        this.setBilling(billing);
        this.setShipping(shipping);
        this.setStatus(status);
        this.setPaymentMethod(paymentMethod);
        this.setItems(items);
    }

    public static Order draft(CustomerId customerId) {
        return new Order(
                new OrderId(),
                customerId,
                Money.ZERO,
                Quantity.ZERO,
                null,
                null,
                null,
                null,
                null,
                null,
                OrderStatus.DRAFT,
                null,
                new HashSet<>());
    }

    public void addItem(@NonNull Product product, @NonNull Quantity quantity) {
        product.checkOutOfStock();

        OrderItem orderItem = OrderItem.brandNew()
                .orderId(this.id())
                .quantity(quantity)
                .product(product)
                .build();

        if (this.items == null) {
            this.items = new HashSet<>();
        }

        this.items.add(orderItem);

        this.recalculateTotals();
    }

    public void place() {
        this.verifyIfCanChangeToPlaced();
        this.changeStatus(OrderStatus.PLACED);
        this.setPlacedAt(OffsetDateTime.now());
    }

    public void markAsPaid() {
        this.changeStatus(OrderStatus.PAID);
        this.setPaidAt(OffsetDateTime.now());
    }

    public void changePaymentMethod(@NonNull PaymentMethod paymentMethod) {
        this.setPaymentMethod(paymentMethod);
    }

    public void changeBilling(@NonNull Billing billing) {
        this.setBilling(billing);
    }

    public void changeShipping(@NonNull Shipping newShipping) {
        if (newShipping.expectedDate().isBefore(LocalDate.now())) {
            throw new OrderInvalidShippingDeliveryDateException(this.id());
        }

        this.setShipping(newShipping);
    }

    public void changeItemQuantity(@NonNull OrderItemId orderItemId, @NonNull Quantity quantity) {
        OrderItem orderItem = this.findOrderItem(orderItemId);
        orderItem.changeQuantity(quantity);

        this.recalculateTotals();
    }

    public boolean isDraft() {
        return OrderStatus.DRAFT.equals(this.status());
    }

    public boolean isPlaced() {
        return OrderStatus.PLACED.equals(this.status());
    }

    public boolean isPaid() {
        return OrderStatus.PAID.equals(this.status());
    }

    public Set<OrderItem> items() {
        return Collections.unmodifiableSet(this.items);
    }

    private void recalculateTotals() {
        BigDecimal totalItemsAmount = this.items().stream().map(i -> i.totalAmount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalItemsQuantity = this.items().stream().map(i -> i.quantity().value())
                .reduce(0, Integer::sum);

        BigDecimal shippingCost;
        if (this.shipping() == null) {
            shippingCost = BigDecimal.ZERO;
        } else {
            shippingCost = this.shipping().cost().value();
        }

        BigDecimal totalAmount = totalItemsAmount.add(shippingCost);

        this.setTotalAmount(new Money(totalAmount));
        this.setTotalItems(new Quantity(totalItemsQuantity));
    }

    private void changeStatus(@NonNull OrderStatus newStatus) {
        if (this.status().canNotChangeTo(newStatus)) {
            throw new OrderStatusCannotBeChangedException(this.id(), this.status(), newStatus);
        }
        this.setStatus(newStatus);
    }

    private void verifyIfCanChangeToPlaced() {
        if (this.shipping() == null) {
            throw OrderCannotBePlacedException.noShippingInfo(this.id());
        }
        if (this.billing() == null) {
            throw OrderCannotBePlacedException.noBillingInfo(this.id());
        }
        if (this.paymentMethod() == null) {
            throw OrderCannotBePlacedException.noPaymentMethod(this.id());
        }
        if (this.items() == null || this.items().isEmpty()) {
            throw OrderCannotBePlacedException.noItems(this.id());
        }
    }

    private OrderItem findOrderItem(@NonNull OrderItemId orderItemId) {
        return this.items().stream()
                .filter(i -> i.id().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new OrderDoesNotContainOrderItemException(this.id(), orderItemId));
    }

    private void setId(OrderId id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setCustomerId(CustomerId customerId) {
        Objects.requireNonNull(customerId);
        this.customerId = customerId;
    }

    private void setTotalAmount(Money totalAmount) {
        Objects.requireNonNull(totalAmount);
        this.totalAmount = totalAmount;
    }

    private void setTotalItems(Quantity totalItems) {
        Objects.requireNonNull(totalItems);
        this.totalItems = totalItems;
    }

    private void setPlacedAt(OffsetDateTime placedAt) {
        this.placedAt = placedAt;
    }

    private void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    private void setCanceledAt(OffsetDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    private void setReadyAt(OffsetDateTime readyAt) {
        this.readyAt = readyAt;
    }

    private void setBilling(Billing billing) {
        this.billing = billing;
    }

    private void setShipping(Shipping shipping) {
        this.shipping = shipping;
    }

    private void setStatus(OrderStatus status) {
        Objects.requireNonNull(status);
        this.status = status;
    }

    private void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private void setItems(Set<OrderItem> items) {
        Objects.requireNonNull(items);
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
