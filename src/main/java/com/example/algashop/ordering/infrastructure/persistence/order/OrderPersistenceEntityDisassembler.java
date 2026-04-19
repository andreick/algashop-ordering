package com.example.algashop.ordering.infrastructure.persistence.order;

import com.example.algashop.ordering.domain.model.commons.Address;
import com.example.algashop.ordering.domain.model.commons.Document;
import com.example.algashop.ordering.domain.model.commons.Email;
import com.example.algashop.ordering.domain.model.commons.FullName;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Phone;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.commons.ZipCode;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.order.Billing;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.OrderId;
import com.example.algashop.ordering.domain.model.order.OrderItem;
import com.example.algashop.ordering.domain.model.order.OrderItemId;
import com.example.algashop.ordering.domain.model.order.OrderStatus;
import com.example.algashop.ordering.domain.model.order.PaymentMethod;
import com.example.algashop.ordering.domain.model.order.Recipient;
import com.example.algashop.ordering.domain.model.order.Shipping;
import com.example.algashop.ordering.domain.model.product.ProductId;
import com.example.algashop.ordering.domain.model.product.ProductName;
import com.example.algashop.ordering.infrastructure.persistence.commons.AddressEmbeddable;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderPersistenceEntityDisassembler {

    public Order toDomainEntity(OrderPersistenceEntity persistenceEntity) {
        return Order.existing()
                .id(new OrderId(persistenceEntity.getId()))
                .customerId(new CustomerId(persistenceEntity.getCustomerId()))
                .totalAmount(new Money(persistenceEntity.getTotalAmount()))
                .totalItems(new Quantity(persistenceEntity.getTotalItems()))
                .billing(toValueObject(persistenceEntity.getBilling()))
                .shipping(toValueObject(persistenceEntity.getShipping()))
                .status(persistenceEntity.getStatus() != null
                        ? OrderStatus.valueOf(persistenceEntity.getStatus())
                        : null)
                .paymentMethod(persistenceEntity.getPaymentMethod() != null
                        ? PaymentMethod.valueOf(persistenceEntity.getPaymentMethod())
                        : null)
                .placedAt(persistenceEntity.getPlacedAt())
                .paidAt(persistenceEntity.getPaidAt())
                .canceledAt(persistenceEntity.getCanceledAt())
                .readyAt(persistenceEntity.getReadyAt())
                .items(toDomainEntity(persistenceEntity.getItems()))
                .version(persistenceEntity.getVersion())
                .build();
    }

    private Set<OrderItem> toDomainEntity(Set<OrderItemPersistenceEntity> items) {
        return items.stream().map(this::toDomainEntity).collect(Collectors.toSet());
    }

    private OrderItem toDomainEntity(OrderItemPersistenceEntity persistenceEntity) {
        return OrderItem.existing()
                .id(new OrderItemId(persistenceEntity.getId()))
                .orderId(new OrderId(persistenceEntity.getOrderId()))
                .productId(new ProductId(persistenceEntity.getProductId()))
                .productName(new ProductName(persistenceEntity.getProductName()))
                .price(new Money(persistenceEntity.getPrice()))
                .quantity(new Quantity(persistenceEntity.getQuantity()))
                .totalAmount(new Money(persistenceEntity.getTotalAmount()))
                .build();
    }

    private Shipping toValueObject(ShippingEmbeddable shippingEmbeddable) {
        if (shippingEmbeddable == null) {
            return null;
        }
        RecipientEmbeddable recipientEmbeddable = shippingEmbeddable.getRecipient();
        return Shipping.builder()
                .cost(new Money(shippingEmbeddable.getCost()))
                .expectedDate(shippingEmbeddable.getExpectedDate())
                .recipient(Recipient.builder()
                        .fullName(new FullName(recipientEmbeddable.getFirstName(), recipientEmbeddable.getLastName()))
                        .document(new Document(recipientEmbeddable.getDocument()))
                        .phone(new Phone(recipientEmbeddable.getPhone()))
                        .build())
                .address(toValueObject(shippingEmbeddable.getAddress()))
                .build();
    }

    private Billing toValueObject(BillingEmbeddable billingEmbeddable) {
        if (billingEmbeddable == null) {
            return null;
        }
        return Billing.builder()
                .fullName(new FullName(billingEmbeddable.getFirstName(), billingEmbeddable.getLastName()))
                .document(new Document(billingEmbeddable.getDocument()))
                .phone(new Phone(billingEmbeddable.getPhone()))
                .email(new Email(billingEmbeddable.getEmail()))
                .address(toValueObject(billingEmbeddable.getAddress()))
                .build();
    }

    private Address toValueObject(AddressEmbeddable address) {
        return Address.builder()
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(new ZipCode(address.getZipCode()))
                .build();
    }
}
