package com.example.algashop.ordering.infrastructure.persistence.customer;

import com.example.algashop.ordering.domain.model.commons.Email;
import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.customer.Customers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomersPersistenceProvider implements Customers {

    private final CustomerPersistenceEntityRepository persistenceRepository;
    private final CustomerPersistenceEntityAssembler assembler;
    private final CustomerPersistenceEntityDisassembler disassembler;

    @Override
    public Optional<Customer> ofId(CustomerId customerId) {
        Optional<CustomerPersistenceEntity> possibleEntity = persistenceRepository.findById(customerId.value());
        return possibleEntity.map(disassembler::toDomainEntity);
    }

    @Override
    public boolean exists(CustomerId customerId) {
        return persistenceRepository.existsById(customerId.value());
    }

    @Override
    public long count() {
        return persistenceRepository.count();
    }

    @Override
    public Optional<Customer> ofEmail(Email email) {
        return persistenceRepository.findByEmail(email.value())
                .map(disassembler::toDomainEntity);
    }

    @Override
    public boolean isEmailUnique(Email email, CustomerId exceptCustomerId) {
        return !persistenceRepository.existsByEmailAndIdNot(email.value(), exceptCustomerId.value());
    }

    @Override
    @Transactional
    public void add(Customer aggregateRoot) {
        UUID customerId = aggregateRoot.id().value();

        persistenceRepository.findById(customerId)
                .ifPresentOrElse(persistenceEntity -> update(aggregateRoot, persistenceEntity),
                        () -> insert(aggregateRoot));

        aggregateRoot.clearDomainEvents();
    }

    private void update(Customer aggregateRoot, CustomerPersistenceEntity persistenceEntity) {
        assembler.merge(persistenceEntity, aggregateRoot);
        persistenceRepository.saveAndFlush(persistenceEntity);
    }

    private void insert(Customer aggregateRoot) {
        CustomerPersistenceEntity persistenceEntity = assembler.fromDomain(aggregateRoot);
        persistenceRepository.saveAndFlush(persistenceEntity);
    }
}
