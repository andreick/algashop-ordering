package com.example.algashop.ordering.infrastructure.persistence.order;

import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.OrderId;
import com.example.algashop.ordering.domain.model.order.OrderStatus;
import com.example.algashop.ordering.domain.model.order.PaymentMethod;
import com.example.algashop.ordering.domain.model.order.Shipping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceEntityDisassemblerTest {

    private final OrderPersistenceEntityDisassembler disassembler = new OrderPersistenceEntityDisassembler();

    @Test
    void shouldConvertFromPersistence() {
        OrderPersistenceEntity persistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder().build();

        Order domainEntity = disassembler.toDomainEntity(persistenceEntity);

        assertThat(domainEntity).isNotNull();
        assertThat(domainEntity)
                .extracting(
                        Order::id,
                        Order::customerId,
                        Order::totalAmount,
                        Order::totalItems,
                        Order::placedAt,
                        Order::paidAt,
                        Order::canceledAt,
                        Order::readyAt,
                        Order::status,
                        Order::paymentMethod)
                .containsExactly(
                        new OrderId(persistenceEntity.getId()),
                        new CustomerId(persistenceEntity.getCustomerId()),
                        new Money(persistenceEntity.getTotalAmount()),
                        new Quantity(persistenceEntity.getTotalItems()),
                        persistenceEntity.getPlacedAt(),
                        persistenceEntity.getPaidAt(),
                        persistenceEntity.getCanceledAt(),
                        persistenceEntity.getReadyAt(),
                        OrderStatus.valueOf(persistenceEntity.getStatus()),
                        PaymentMethod.valueOf(persistenceEntity.getPaymentMethod()));

        assertThat(domainEntity.billing())
                .extracting(
                        b -> b.fullName().firstName(),
                        b -> b.fullName().lastName(),
                        b -> b.document().value(),
                        b -> b.phone().value(),
                        b -> b.email().value(),
                        b -> b.address().street(),
                        b -> b.address().number(),
                        b -> b.address().complement())
                .containsExactly(
                        persistenceEntity.getBilling().getFirstName(),
                        persistenceEntity.getBilling().getLastName(),
                        persistenceEntity.getBilling().getDocument(),
                        persistenceEntity.getBilling().getPhone(),
                        persistenceEntity.getBilling().getEmail(),
                        persistenceEntity.getBilling().getAddress().getStreet(),
                        persistenceEntity.getBilling().getAddress().getNumber(),
                        persistenceEntity.getBilling().getAddress().getComplement());

        assertThat(domainEntity.shipping())
                .extracting(
                        Shipping::cost,
                        Shipping::expectedDate,
                        sh -> sh.address().street(),
                        sh -> sh.address().number(),
                        sh -> sh.address().complement(),
                        sh -> sh.recipient().fullName().firstName(),
                        sh -> sh.recipient().fullName().lastName(),
                        sh -> sh.recipient().document().value(),
                        sh -> sh.recipient().phone().value())
                .containsExactly(
                        new Money(persistenceEntity.getShipping().getCost()),
                        persistenceEntity.getShipping().getExpectedDate(),
                        persistenceEntity.getShipping().getAddress().getStreet(),
                        persistenceEntity.getShipping().getAddress().getNumber(),
                        persistenceEntity.getShipping().getAddress().getComplement(),
                        persistenceEntity.getShipping().getRecipient().getFirstName(),
                        persistenceEntity.getShipping().getRecipient().getLastName(),
                        persistenceEntity.getShipping().getRecipient().getDocument(),
                        persistenceEntity.getShipping().getRecipient().getPhone());

        assertThat(domainEntity.items()).hasSameSizeAs(persistenceEntity.getItems());
    }

    @Test
    void shouldMapNullWhenBillingIsNull() {
        OrderPersistenceEntity persistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .billing(null)
                .build();
        Order domainEntity = disassembler.toDomainEntity(persistenceEntity);

        assertThat(domainEntity.billing()).isNull();
    }

    @Test
    void shouldMapNullWhenShippingIsNull() {
        OrderPersistenceEntity persistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .shipping(null)
                .build();
        Order domainEntity = disassembler.toDomainEntity(persistenceEntity);

        assertThat(domainEntity.shipping()).isNull();
    }
}
