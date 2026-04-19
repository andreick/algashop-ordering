package com.example.algashop.ordering.infrastructure.persistence.order;

import com.example.algashop.ordering.domain.model.IdGenerator;
import com.example.algashop.ordering.infrastructure.persistence.commons.AddressEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.customer.CustomerPersistenceEntityTestDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

public class OrderPersistenceEntityTestDataBuilder {

    private OrderPersistenceEntityTestDataBuilder() {
    }

    public static OrderPersistenceEntity.OrderPersistenceEntityBuilder existingOrder() {
        return OrderPersistenceEntity.builder()
                .id(IdGenerator.generateTSID().toLong())
                .customer(CustomerPersistenceEntityTestDataBuilder.aCustomer().build())
                .totalItems(3)
                .totalAmount(new BigDecimal(1250))
                .billing(existingBilling())
                .shipping(existingshipping())
                .status("DRAFT")
                .paymentMethod("CREDIT_CARD")
                .placedAt(OffsetDateTime.now())
                .items(Set.of(
                        existingItem().build(),
                        existingItemAlt().build()));
    }

    private static BillingEmbeddable existingBilling() {
        return BillingEmbeddable.builder()
                .firstName("John")
                .lastName("Doe")
                .document("225-09-1992")
                .phone("123-111-9911")
                .email("jhon.doe@gmail.com")
                .address(existingAddress())
                .build();
    }

    private static ShippingEmbeddable existingshipping() {
        return ShippingEmbeddable.builder()
                .cost(new BigDecimal(10))
                .expectedDate(LocalDate.now().plusWeeks(1))
                .address(existingAddress())
                .recipient(RecipientEmbeddable.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .document("112-33-2321")
                        .phone("111-441-1244")
                        .build())
                .build();
    }

    private static AddressEmbeddable existingAddress() {
        return AddressEmbeddable.builder()
                .street("Bourbon Street")
                .number("1234")
                .complement("apt. 11")
                .neighborhood("North Ville")
                .city("Montfort")
                .state("South Carolina")
                .zipCode("79911")
                .build();
    }

    public static OrderItemPersistenceEntity.OrderItemPersistenceEntityBuilder existingItem() {
        return OrderItemPersistenceEntity.builder()
                .id(IdGenerator.generateTSID().toLong())
                .price(new BigDecimal(500))
                .quantity(2)
                .totalAmount(new BigDecimal(1000))
                .productName("Notebook")
                .productId(IdGenerator.generateTimeBasedUUID());
    }

    public static OrderItemPersistenceEntity.OrderItemPersistenceEntityBuilder existingItemAlt() {
        return OrderItemPersistenceEntity.builder()
                .id(IdGenerator.generateTSID().toLong())
                .price(new BigDecimal(250))
                .quantity(1)
                .totalAmount(new BigDecimal(250))
                .productName("Mouse pad")
                .productId(IdGenerator.generateTimeBasedUUID());
    }
}
