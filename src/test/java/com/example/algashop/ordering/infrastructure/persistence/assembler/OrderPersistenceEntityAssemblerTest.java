package com.example.algashop.ordering.infrastructure.persistence.assembler;

import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.OrderItem;
import com.example.algashop.ordering.domain.model.entity.OrderTestDataBuilder;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderItemPersistenceEntity;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderPersistenceEntity;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderPersistenceEntityTestDataBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceEntityAssemblerTest {

    private final OrderPersistenceEntityAssembler assembler = new OrderPersistenceEntityAssembler();

    @Test
    void shouldConvertToDomain() {
        Order order = OrderTestDataBuilder.anOrder().build();
        OrderPersistenceEntity orderPersistenceEntity = assembler.fromDomain(order);
        assertThat(orderPersistenceEntity).satisfies(
                p -> assertThat(p.getId()).isEqualTo(order.id().value().toLong()),
                p -> assertThat(p.getCustomerId()).isEqualTo(order.customerId().value()),
                p -> assertThat(p.getTotalAmount()).isEqualTo(order.totalAmount().value()),
                p -> assertThat(p.getTotalItems()).isEqualTo(order.totalItems().value()),
                p -> assertThat(p.getStatus()).isEqualTo(order.status().name()),
                p -> assertThat(p.getPaymentMethod()).isEqualTo(order.paymentMethod().name()),
                p -> assertThat(p.getPlacedAt()).isEqualTo(order.placedAt()),
                p -> assertThat(p.getPaidAt()).isEqualTo(order.paidAt()),
                p -> assertThat(p.getCanceledAt()).isEqualTo(order.canceledAt()),
                p -> assertThat(p.getReadyAt()).isEqualTo(order.readyAt()));
    }

    @Test
    void givenOrderWithNotItems_shouldRemovePersistenceEntityItems() {
        Order order = OrderTestDataBuilder.anOrder().withItems(false).build();
        OrderPersistenceEntity orderPersistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder().build();

        assertThat(order.items()).isEmpty();
        assertThat(orderPersistenceEntity.getItems()).isNotEmpty();

        assembler.merge(orderPersistenceEntity, order);

        assertThat(orderPersistenceEntity.getItems()).isEmpty();
    }

    @Test
    void givenOrderWithItems_shouldAddToPersistenceEntity() {
        Order order = OrderTestDataBuilder.anOrder().withItems(true).build();
        OrderPersistenceEntity persistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .items(new HashSet<>()).build();

        assertThat(order.items()).isNotEmpty();
        assertThat(persistenceEntity.getItems()).isEmpty();

        assembler.merge(persistenceEntity, order);

        assertThat(persistenceEntity.getItems()).isNotEmpty();
        assertThat(persistenceEntity.getItems()).hasSameSizeAs(order.items());
    }

    @Test
    void givenOrderWithItems_whenMerge_shouldRemoveMergeCorrectly() {
        Order order = OrderTestDataBuilder.anOrder().withItems(true).build();

        assertThat(order.items()).hasSize(2);

        Set<OrderItemPersistenceEntity> orderItemPersistenceEntities = order.items().stream()
                .map(assembler::fromDomain)
                .collect(Collectors.toSet());

        OrderPersistenceEntity persistenceEntity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .items(orderItemPersistenceEntities)
                .build();

        OrderItem orderItem = order.items().iterator().next();
        order.removeItem(orderItem.id());

        assembler.merge(persistenceEntity, order);

        assertThat(persistenceEntity.getItems()).isNotEmpty();
        assertThat(persistenceEntity.getItems()).hasSameSizeAs(order.items());
    }
}
