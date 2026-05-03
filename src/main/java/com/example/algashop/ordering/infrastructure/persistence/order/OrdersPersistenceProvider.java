package com.example.algashop.ordering.infrastructure.persistence.order;

import com.example.algashop.ordering.domain.model.VersionSynchronizer;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.OrderId;
import com.example.algashop.ordering.domain.model.order.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdersPersistenceProvider implements Orders {

    private final OrderPersistenceEntityRepository persistenceRepository;
    private final OrderPersistenceEntityAssembler assembler;
    private final OrderPersistenceEntityDisassembler disassembler;
    private final VersionSynchronizer<Order> versionSynchronizer;

    @Override
    public Optional<Order> ofId(OrderId orderId) {
        Optional<OrderPersistenceEntity> possibleEntity = persistenceRepository.findById(
                orderId.value().toLong());
        return possibleEntity.map(disassembler::toDomainEntity);
    }

    @Override
    public boolean exists(OrderId orderId) {
        return persistenceRepository.existsById(orderId.value().toLong());
    }

    @Override
    public long count() {
        return persistenceRepository.count();
    }

    @Override
    public List<Order> placedByCustomerInYear(CustomerId customerId, Year year) {
        List<OrderPersistenceEntity> entities = persistenceRepository.placedByCustomerInYear(
                customerId.value(),
                year.getValue());

        return entities.stream().map(disassembler::toDomainEntity).toList();
    }

    @Override
    public long salesQuantityByCustomerInYear(CustomerId customerId, Year year) {
        return this.persistenceRepository.salesQuantityByCustomerInYear(customerId.value(), year.getValue());
    }

    @Override
    public Money totalSoldForCustomer(CustomerId customerId) {
        return new Money(this.persistenceRepository.totalSoldForCustomer(customerId.value()));
    }

    @Override
    @Transactional
    public void add(Order aggregateRoot) {
        long orderId = aggregateRoot.id().value().toLong();

        persistenceRepository.findById(orderId)
                .ifPresentOrElse(persistenceEntity -> update(aggregateRoot, persistenceEntity),
                        () -> insert(aggregateRoot));

        aggregateRoot.clearDomainEvents();
    }

    private void update(Order aggregateRoot, OrderPersistenceEntity persistenceEntity) {
        assembler.merge(persistenceEntity, aggregateRoot);
        persistenceRepository.saveAndFlush(persistenceEntity);
        versionSynchronizer.synchronizeVersion(aggregateRoot, persistenceEntity.getVersion());
    }

    private void insert(Order aggregateRoot) {
        OrderPersistenceEntity persistenceEntity = assembler.fromDomain(aggregateRoot);
        persistenceRepository.saveAndFlush(persistenceEntity);
        versionSynchronizer.synchronizeVersion(aggregateRoot, persistenceEntity.getVersion());
    }
}
