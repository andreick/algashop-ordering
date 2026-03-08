package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.valueobject.Money;
import com.example.algashop.ordering.domain.valueobject.Product;
import com.example.algashop.ordering.domain.valueobject.ProductName;
import com.example.algashop.ordering.domain.valueobject.Quantity;
import com.example.algashop.ordering.domain.valueobject.id.OrderId;
import com.example.algashop.ordering.domain.valueobject.id.OrderItemId;
import com.example.algashop.ordering.domain.valueobject.id.ProductId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Objects;

@Accessors(fluent = true)
@Getter
public class OrderItem {

    private OrderItemId id;
    private OrderId orderId;

    private ProductId productId;
    private ProductName productName;

    private Money price;
    private Quantity quantity;

    private Money totalAmount;

    @Builder(builderClassName = "ExistingOrderItemBuilder", builderMethodName = "existing")
    public OrderItem(OrderItemId id, OrderId orderId,
            ProductId productId, ProductName productName,
            Money price, Quantity quantity,
            Money totalAmount) {
        this.setId(id);
        this.setOrderId(orderId);
        this.setProductId(productId);
        this.setProductName(productName);
        this.setPrice(price);
        this.setQuantity(quantity);
        this.setTotalAmount(totalAmount);
    }

    @Builder(builderClassName = "BrandNewOrderItemBuilder", builderMethodName = "brandNew")
    private static OrderItem createBrandNew(
            @NonNull OrderId orderId,
            @NonNull Product product,
            @NonNull Quantity quantity) {
        OrderItem orderItem = new OrderItem(
                new OrderItemId(),
                orderId,
                product.id(),
                product.name(),
                product.price(),
                quantity,
                Money.ZERO);

        orderItem.recalculateTotals();

        return orderItem;
    }

    void changeQuantity(@NonNull Quantity quantity) {
        this.setQuantity(quantity);
        this.recalculateTotals();
    }

    private void recalculateTotals() {
        this.setTotalAmount(this.price().multiply(this.quantity()));
    }

    private void setId(OrderItemId id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setOrderId(OrderId orderId) {
        Objects.requireNonNull(orderId);
        this.orderId = orderId;
    }

    private void setProductId(ProductId productId) {
        Objects.requireNonNull(productId);
        this.productId = productId;
    }

    private void setProductName(ProductName productName) {
        Objects.requireNonNull(productName);
        this.productName = productName;
    }

    private void setPrice(Money price) {
        Objects.requireNonNull(price);
        this.price = price;
    }

    private void setQuantity(Quantity quantity) {
        Objects.requireNonNull(quantity);
        this.quantity = quantity;
    }

    private void setTotalAmount(Money totalAmount) {
        Objects.requireNonNull(totalAmount);
        this.totalAmount = totalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
