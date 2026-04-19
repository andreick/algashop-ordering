package com.example.algashop.ordering.infrastructure.persistence.shoppingcart;

import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCart;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartId;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCarts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartsPersistenceProvider implements ShoppingCarts {

    private final ShoppingCartPersistenceEntityRepository persistenceRepository;
    private final ShoppingCartPersistenceEntityAssembler assembler;
    private final ShoppingCartPersistenceEntityDisassembler disassembler;

    @Override
    public Optional<ShoppingCart> ofId(ShoppingCartId shoppingCartId) {
        return persistenceRepository.findById(shoppingCartId.value())
                .map(disassembler::toDomainEntity);
    }

    @Override
    public boolean exists(ShoppingCartId shoppingCartId) {
        return persistenceRepository.existsById(shoppingCartId.value());
    }

    @Override
    public long count() {
        return persistenceRepository.count();
    }

    @Override
    @Transactional(readOnly = false)
    public void remove(ShoppingCart shoppingCart) {
        this.persistenceRepository.deleteById(shoppingCart.id().value());
    }

    @Override
    @Transactional(readOnly = false)
    public void remove(ShoppingCartId shoppingCartId) {
        this.persistenceRepository.deleteById(shoppingCartId.value());
    }

    @Override
    public Optional<ShoppingCart> ofCustomer(CustomerId customerId) {
        return persistenceRepository.findByCustomer_Id(customerId.value())
                .map(disassembler::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = false)
    public void add(ShoppingCart aggregateRoot) {
        persistenceRepository.findById(aggregateRoot.id().value())
                .ifPresentOrElse(
                        persistenceEntity -> update(aggregateRoot, persistenceEntity),
                        () -> insert(aggregateRoot));
    }

    private void update(ShoppingCart aggregateRoot, ShoppingCartPersistenceEntity persistenceEntity) {
        assembler.merge(persistenceEntity, aggregateRoot);
        persistenceRepository.flush();
    }

    private void insert(ShoppingCart aggregateRoot) {
        ShoppingCartPersistenceEntity persistenceEntity = assembler.fromDomain(aggregateRoot);
        persistenceRepository.saveAndFlush(persistenceEntity);
    }
}
