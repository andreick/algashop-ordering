package com.example.algashop.ordering.infrastructure.persistence.repository;

import com.example.algashop.ordering.domain.model.entity.CustomerTestDataBuilder;
import com.example.algashop.ordering.infrastructure.persistence.config.SpringDataAuditingConfig;
import com.example.algashop.ordering.infrastructure.persistence.entity.CustomerPersistenceEntity;
import com.example.algashop.ordering.infrastructure.persistence.entity.CustomerPersistenceEntityTestDataBuilder;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderPersistenceEntity;
import com.example.algashop.ordering.infrastructure.persistence.entity.OrderPersistenceEntityTestDataBuilder;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(SpringDataAuditingConfig.class)
class OrderPersistenceEntityRepositoryIT {

    private final OrderPersistenceEntityRepository orderPersistenceEntityRepository;
    private final CustomerPersistenceEntityRepository customerPersistenceEntityRepository;

    private CustomerPersistenceEntity customerPersistenceEntity;

    @Autowired
    public OrderPersistenceEntityRepositoryIT(OrderPersistenceEntityRepository orderPersistenceEntityRepository,
            CustomerPersistenceEntityRepository customerPersistenceEntityRepository) {
        this.orderPersistenceEntityRepository = orderPersistenceEntityRepository;
        this.customerPersistenceEntityRepository = customerPersistenceEntityRepository;
    }

    @BeforeEach
    @Transactional(TxType.REQUIRES_NEW)
    void setup() {
        if (!customerPersistenceEntityRepository.existsById(CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID.value())) {
            customerPersistenceEntity = customerPersistenceEntityRepository
                    .saveAndFlush(CustomerPersistenceEntityTestDataBuilder.aCustomer().build());
        }
    }

    @Test
    void shouldPersist() {
        OrderPersistenceEntity entity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .customer(customerPersistenceEntity)
                .build();
        orderPersistenceEntityRepository.saveAndFlush(entity);

        Assertions.assertThat(orderPersistenceEntityRepository.existsById(entity.getId())).isTrue();

        OrderPersistenceEntity savedEntity = orderPersistenceEntityRepository.findById(entity.getId()).orElseThrow();

        Assertions.assertThat(savedEntity.getItems()).isNotEmpty();
    }

    @Test
    void shouldCount() {
        long ordersCount = orderPersistenceEntityRepository.count();
        Assertions.assertThat(ordersCount).isZero();
    }

    @Test
    void shouldSetAuditingValues() {
        OrderPersistenceEntity entity = OrderPersistenceEntityTestDataBuilder.existingOrder()
                .customer(customerPersistenceEntity)
                .build();
        entity = orderPersistenceEntityRepository.saveAndFlush(entity);

        Assertions.assertThat(entity.getCreatedByUserId()).isNotNull();

        Assertions.assertThat(entity.getLastModifiedAt()).isNotNull();
        Assertions.assertThat(entity.getLastModifiedByUserId()).isNotNull();
    }
}
