package com.example.algashop.ordering.infrastructure.persistence.assembler;

import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.OrderItem;
import com.example.algashop.ordering.domain.model.valueobject.Address;
import com.example.algashop.ordering.domain.model.valueobject.Billing;
import com.example.algashop.ordering.domain.model.valueobject.Recipient;
import com.example.algashop.ordering.domain.model.valueobject.Shipping;
import com.example.algashop.ordering.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.embeddable.BillingEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.embeddable.RecipientEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.embeddable.ShippingEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderItemPersistenceEntity;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderPersistenceEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderPersistenceEntityAssembler {

    public OrderPersistenceEntity fromDomain(Order order) {
        return merge(new OrderPersistenceEntity(), order);
    }

    public OrderPersistenceEntity merge(OrderPersistenceEntity orderPersistenceEntity, Order order) {
        orderPersistenceEntity.setId(order.id().value().toLong());
        orderPersistenceEntity.setCustomerId(order.customerId().value());
        orderPersistenceEntity.setTotalAmount(order.totalAmount().value());
        orderPersistenceEntity.setTotalItems(order.totalItems().value());
        orderPersistenceEntity.setStatus(order.status().name());
        orderPersistenceEntity.setPaymentMethod(order.paymentMethod().name());
        orderPersistenceEntity.setPlacedAt(order.placedAt());
        orderPersistenceEntity.setPaidAt(order.paidAt());
        orderPersistenceEntity.setCanceledAt(order.canceledAt());
        orderPersistenceEntity.setReadyAt(order.readyAt());
        orderPersistenceEntity.setBilling(toBillingEmbeddable(order.billing()));
        orderPersistenceEntity.setShipping(toShippingEmbeddable(order.shipping()));
        mergeItems(order, orderPersistenceEntity);
        return orderPersistenceEntity;
    }

    private void mergeItems(Order order, OrderPersistenceEntity orderPersistenceEntity) {
        Set<OrderItem> newOrUpdatedItems = order.items();

        if (newOrUpdatedItems == null || newOrUpdatedItems.isEmpty()) {
            orderPersistenceEntity.clearItems();
            return;
        }

        Set<OrderItemPersistenceEntity> existingItems = orderPersistenceEntity.getItems();
        if (existingItems == null || existingItems.isEmpty()) {
            newOrUpdatedItems.stream()
                    .map(this::fromDomain)
                    .forEach(orderPersistenceEntity::addItem);
            return;
        }

        Map<Long, OrderItemPersistenceEntity> existingItemMap = existingItems.stream()
                .collect(Collectors.toMap(OrderItemPersistenceEntity::getId, item -> item));

        orderPersistenceEntity.clearItems();
        newOrUpdatedItems.stream()
                .forEach(orderItem -> {
                    OrderItemPersistenceEntity itemPersistence = existingItemMap
                            .getOrDefault(orderItem.id().value().toLong(), new OrderItemPersistenceEntity());
                    merge(itemPersistence, orderItem);
                    orderPersistenceEntity.addItem(itemPersistence);
                });
    }

    public OrderItemPersistenceEntity fromDomain(OrderItem orderItem) {
        return merge(new OrderItemPersistenceEntity(), orderItem);
    }

    private OrderItemPersistenceEntity merge(OrderItemPersistenceEntity orderItemPersistenceEntity,
            OrderItem orderItem) {
        orderItemPersistenceEntity.setId(orderItem.id().value().toLong());
        orderItemPersistenceEntity.setProductId(orderItem.productId().value());
        orderItemPersistenceEntity.setProductName(orderItem.productName().value());
        orderItemPersistenceEntity.setPrice(orderItem.price().value());
        orderItemPersistenceEntity.setQuantity(orderItem.quantity().value());
        orderItemPersistenceEntity.setTotalAmount(orderItem.totalAmount().value());
        return orderItemPersistenceEntity;
    }

    private BillingEmbeddable toBillingEmbeddable(Billing billing) {
        if (billing == null) {
            return null;
        }
        return BillingEmbeddable.builder()
                .firstName(billing.fullName().firstName())
                .lastName(billing.fullName().lastName())
                .document(billing.document().value())
                .phone(billing.phone().value())
                .email(billing.email().value())
                .address(toAddressEmbeddable(billing.address()))
                .build();
    }

    private AddressEmbeddable toAddressEmbeddable(Address address) {
        if (address == null) {
            return null;
        }
        return AddressEmbeddable.builder()
                .city(address.city())
                .state(address.state())
                .number(address.number())
                .street(address.street())
                .complement(address.complement())
                .neighborhood(address.neighborhood())
                .zipCode(address.zipCode().value())
                .build();
    }

    private ShippingEmbeddable toShippingEmbeddable(Shipping shipping) {
        if (shipping == null) {
            return null;
        }
        Recipient recipient = shipping.recipient();
        return ShippingEmbeddable.builder()
                .expectedDate(shipping.expectedDate())
                .cost(shipping.cost().value())
                .address(toAddressEmbeddable(shipping.address()))
                .recipient(RecipientEmbeddable.builder()
                        .firstName(recipient.fullName().firstName())
                        .lastName(recipient.fullName().lastName())
                        .document(recipient.document().value())
                        .phone(recipient.phone().value())
                        .build())
                .build();
    }
}
