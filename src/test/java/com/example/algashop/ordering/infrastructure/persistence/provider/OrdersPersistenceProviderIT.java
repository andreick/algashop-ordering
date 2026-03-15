package com.example.algashop.ordering.infrastructure.persistence.provider;

import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.OrderStatus;
import com.example.algashop.ordering.domain.model.entity.OrderTestDataBuilder;
import com.example.algashop.ordering.infrastructure.persistence.assembler.OrderPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.config.SpringDataAuditingConfig;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.OrderPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.repository.OrderPersistenceEntityRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
        OrdersPersistenceProvider.class,
        OrderPersistenceEntityAssembler.class,
        OrderPersistenceEntityDisassembler.class,
        SpringDataAuditingConfig.class
})
class OrdersPersistenceProviderIT {

    private OrdersPersistenceProvider persistenceProvider;
    private OrderPersistenceEntityRepository entityRepository;

    @Autowired
    public OrdersPersistenceProviderIT(OrdersPersistenceProvider persistenceProvider,
            OrderPersistenceEntityRepository entityRepository) {
        this.persistenceProvider = persistenceProvider;
        this.entityRepository = entityRepository;
    }

    @Test
    void shouldUpdateAndKeepPersistenceEntityState() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        long orderId = order.id().value().toLong();
        persistenceProvider.add(order);

        var persistenceEntity = entityRepository.findById(orderId).orElseThrow();

        Assertions.assertThat(persistenceEntity.getStatus()).isEqualTo(OrderStatus.PLACED.name());

        Assertions.assertThat(persistenceEntity.getCreatedByUserId()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedAt()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedByUserId()).isNotNull();

        order = persistenceProvider.ofId(order.id()).orElseThrow();
        order.markAsPaid();
        persistenceProvider.add(order);

        persistenceEntity = entityRepository.findById(orderId).orElseThrow();

        Assertions.assertThat(persistenceEntity.getStatus()).isEqualTo(OrderStatus.PAID.name());

        Assertions.assertThat(persistenceEntity.getCreatedByUserId()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedAt()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedByUserId()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAddFindAndNotFailWhenNoTransaction() {
        Order order = OrderTestDataBuilder.anOrder().build();
        persistenceProvider.add(order);

        Assertions.assertThatNoException().isThrownBy(
                () -> persistenceProvider.ofId(order.id()).orElseThrow());
    }
}
