package com.example.algashop.ordering.domain.model.repository;

import com.example.algashop.ordering.domain.model.entity.CustomerTestDataBuilder;
import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.OrderStatus;
import com.example.algashop.ordering.domain.model.entity.OrderTestDataBuilder;
import com.example.algashop.ordering.domain.model.entity.OrderVersionSynchronizer;
import com.example.algashop.ordering.domain.model.entity.ProductTestDataBuilder;
import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.domain.model.valueobject.id.OrderId;
import com.example.algashop.ordering.infrastructure.persistence.assembler.CustomerPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.assembler.OrderPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.config.SpringDataAuditingConfig;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.CustomerPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.OrderPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.provider.CustomersPersistenceProvider;
import com.example.algashop.ordering.infrastructure.persistence.provider.OrdersPersistenceProvider;
import com.example.algashop.ordering.infrastructure.persistence.repository.OrderPersistenceEntityRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Sql(statements = { "DELETE FROM order_item", "DELETE FROM \"order\"" })
@Import({
        OrdersPersistenceProvider.class, OrderVersionSynchronizer.class,
        OrderPersistenceEntityAssembler.class, OrderPersistenceEntityDisassembler.class,
        CustomersPersistenceProvider.class,
        CustomerPersistenceEntityAssembler.class, CustomerPersistenceEntityDisassembler.class,
        SpringDataAuditingConfig.class
})
class OrdersIT {

    private final OrderPersistenceEntityRepository entityRepository;
    private final Orders orders;
    private final Customers customers;
    private final TransactionTemplate newTransaction;

    @Autowired
    public OrdersIT(OrderPersistenceEntityRepository entityRepository, Orders orders, Customers customers,
            PlatformTransactionManager transactionManager) {
        this.entityRepository = entityRepository;
        this.orders = orders;
        this.customers = customers;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @BeforeEach
    void setup() {
        if (!customers.exists(CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID)) {
            inNewTransaction(() -> customers.add(CustomerTestDataBuilder.existingCustomer().build()));
        }
    }

    @Test
    void shouldPersistAndFind() {
        Order originalOrder = OrderTestDataBuilder.anOrder().build();
        OrderId orderId = originalOrder.id();
        orders.add(originalOrder);

        Optional<Order> possibleOrder = orders.ofId(orderId);

        assertThat(possibleOrder).isPresent();

        Order savedOrder = possibleOrder.get();

        assertThat(savedOrder).satisfies(
                s -> assertThat(s.id()).isEqualTo(orderId),
                s -> assertThat(s.customerId()).isEqualTo(originalOrder.customerId()),
                s -> assertThat(s.totalAmount()).isEqualTo(originalOrder.totalAmount()),
                s -> assertThat(s.totalItems()).isEqualTo(originalOrder.totalItems()),
                s -> assertThat(s.placedAt()).isEqualTo(originalOrder.placedAt()),
                s -> assertThat(s.paidAt()).isEqualTo(originalOrder.paidAt()),
                s -> assertThat(s.canceledAt()).isEqualTo(originalOrder.canceledAt()),
                s -> assertThat(s.readyAt()).isEqualTo(originalOrder.readyAt()),
                s -> assertThat(s.status()).isEqualTo(originalOrder.status()),
                s -> assertThat(s.paymentMethod()).isEqualTo(originalOrder.paymentMethod()));
    }

    @Test
    void shouldUpdateExistingOrder() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        orders.add(order);

        order = orders.ofId(order.id()).orElseThrow();
        order.markAsPaid();

        orders.add(order);

        order = orders.ofId(order.id()).orElseThrow();

        Assertions.assertThat(order.isPaid()).isTrue();

    }

    @Test
    void shouldDemonstrateLostUpdateInSingleTransaction() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();

        // Versão inicial de order é null
        Assertions.assertThat(order.version()).isNull();

        orders.add(order);

        // Após persistir, versão deve ser 0
        Assertions.assertThat(order.version()).isZero();

        Order order1 = orders.ofId(order.id()).orElseThrow();
        Order order2 = orders.ofId(order.id()).orElseThrow();

        order1.markAsPaid();
        orders.add(order1);

        // Após persistir, versão da primeira atualização deve ser 1
        Assertions.assertThat(order1.version()).isOne();

        order2.cancel();
        orders.add(order2);

        // Após persistir, versão da segunda atualização deve ser 2.
        // Não há conflito porque as duas atualizações ocorreram na mesma transação,
        // mas a atualização de cancelamento perdeu a atualização de pagamento
        Assertions.assertThat(order2.version()).isEqualTo(2);

        var persistenceEntity = entityRepository.findById(order.id().value().toLong()).orElseThrow();

        Assertions.assertThat(persistenceEntity.getVersion()).isEqualTo(2);
        Assertions.assertThat(persistenceEntity.getCanceledAt()).isNotNull();
        Assertions.assertThat(persistenceEntity.getPaidAt()).isNull();
    }

    @Test
    void shouldKeepDomainAndPersistenceVersionSynchronized() {
        OrderId orderId = inNewTransaction(() -> {
            Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();

            // Versão inicial deve ser null
            Assertions.assertThat(order.version()).isNull();

            orders.add(order);

            // Após persistir, versão deve ser 0
            Assertions.assertThat(order.version()).isZero();

            order.markAsPaid();
            orders.add(order);

            // Após persistir, versão deve ser 1
            Assertions.assertThat(order.version()).isOne();

            // Versão do domínio deve estar sincronizada com a versão de persistência
            var persistenceEntity = entityRepository.findById(order.id().value().toLong()).orElseThrow();
            Assertions.assertThat(order.version()).isEqualTo(persistenceEntity.getVersion());

            return order.id();
        });

        Order existingOrder = inNewTransaction(() -> {
            Order order = orders.ofId(orderId).orElseThrow();

            // Versão recuperada do domínio deve ser 1
            Assertions.assertThat(order.version()).isOne();

            order.markAsReady();
            orders.add(order);

            // Após persistir, versão deve ser 2
            Assertions.assertThat(order.version()).isEqualTo(2);

            // Versão do domínio deve estar sincronizada com a versão de persistência
            var persistenceEntity = entityRepository.findById(orderId.value().toLong()).orElseThrow();
            Assertions.assertThat(order.version()).isEqualTo(persistenceEntity.getVersion());

            return order;
        });

        // Versão do domínio deve estar sincronizada com a versão de persistência
        var persistenceEntity = entityRepository.findById(orderId.value().toLong()).orElseThrow();
        Assertions.assertThat(existingOrder.version()).isEqualTo(persistenceEntity.getVersion());
    }

    @Test
    void shouldNotAllowStaleUpdates() {
        OrderId orderId = inNewTransaction(() -> {
            Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
            orders.add(order);
            return order.id();
        });

        Assertions.assertThatThrownBy(() -> inNewTransaction(() -> {
            Order staleOrder = orders.ofId(orderId).orElseThrow();

            inNewTransaction(() -> {
                Order freshOrder = orders.ofId(orderId).orElseThrow();
                Assertions.assertThat(freshOrder).isNotSameAs(staleOrder);
                Assertions.assertThat(freshOrder.version()).isEqualTo(staleOrder.version());
                freshOrder.markAsPaid();
                orders.add(freshOrder);
            });

            staleOrder.cancel();
            orders.add(staleOrder);
        })).isInstanceOf(OptimisticLockingFailureException.class);

        Order savedOrder = orders.ofId(orderId).orElseThrow();

        Assertions.assertThat(savedOrder.canceledAt()).isNull();
        Assertions.assertThat(savedOrder.paidAt()).isNotNull();
    }

    @Test
    void shouldCountExistingOrders() {
        Assertions.assertThat(orders.count()).isZero();

        Order order1 = OrderTestDataBuilder.anOrder().build();
        Order order2 = OrderTestDataBuilder.anOrder().build();

        orders.add(order1);
        orders.add(order2);

        Assertions.assertThat(orders.count()).isEqualTo(2L);
    }

    @Test
    void shouldReturnIfOrderExists() {
        Order order = OrderTestDataBuilder.anOrder().build();
        orders.add(order);

        Assertions.assertThat(orders.exists(order.id())).isTrue();
        Assertions.assertThat(orders.exists(new OrderId())).isFalse();
    }

    @Test
    void shouldListExistingOrdersByYear() {
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build());
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build());
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.CANCELED).build());
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.DRAFT).build());

        CustomerId customerId = CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID;

        List<Order> listedOrders = orders.placedByCustomerInYear(customerId, Year.now());
        Assertions.assertThat(listedOrders).hasSize(2);

        listedOrders = orders.placedByCustomerInYear(customerId, Year.now().minusYears(1));
        Assertions.assertThat(listedOrders).isEmpty();

        listedOrders = orders.placedByCustomerInYear(new CustomerId(), Year.now());
        Assertions.assertThat(listedOrders).isEmpty();
    }

    @Test
    void shouldReturnTotalSoldByCustomer() {
        Order order1 = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();
        Order order2 = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();

        orders.add(order1);
        orders.add(order2);

        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.CANCELED).build());
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build());

        Money expectedTotalAmount = order1.totalAmount().add(order2.totalAmount());

        CustomerId customerId = CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID;

        Assertions.assertThat(orders.totalSoldForCustomer(customerId)).isEqualTo(expectedTotalAmount);
        Assertions.assertThat(orders.totalSoldForCustomer(new CustomerId())).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldReturnSalesQuantityByCustomer() {
        Order order1 = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();
        Order order2 = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();

        orders.add(order1);
        orders.add(order2);

        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.CANCELED).build());
        orders.add(OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build());

        CustomerId customerId = CustomerTestDataBuilder.DEFAULT_CUSTOMER_ID;

        Assertions.assertThat(orders.salesQuantityByCustomerInYear(customerId, Year.now())).isEqualTo(2L);
        Assertions.assertThat(orders.salesQuantityByCustomerInYear(customerId, Year.now().minusYears(1))).isZero();
    }

    @Test
    void shouldRemovePersistenceEntityItemsBetweenTransactions() {
        OrderId orderId = inNewTransaction(() -> {
            Order order = OrderTestDataBuilder.anOrder().withItems(true).build();
            orders.add(order);
            return order.id();
        });

        inNewTransaction(() -> {
            Order order = orders.ofId(orderId).orElseThrow();
            order.items().stream()
                    .map(i -> i.id())
                    .toList()
                    .forEach(order::removeItem);
            orders.add(order);
        });

        Order updatedOrder = inNewTransaction(() -> orders.ofId(orderId).orElseThrow());
        Assertions.assertThat(updatedOrder.items()).isEmpty();
    }

    @Test
    void shouldAddItemsToPersistenceEntityBetweenTransactions() {
        OrderId orderId = inNewTransaction(() -> {
            Order order = OrderTestDataBuilder.anOrder().withItems(false).build();
            orders.add(order);
            return order.id();
        });

        inNewTransaction(() -> {
            Order order = orders.ofId(orderId).orElseThrow();
            order.addItem(ProductTestDataBuilder.aProduct().build(), new Quantity(2));
            order.addItem(ProductTestDataBuilder.aProductAltRamMemory().build(), new Quantity(1));
            orders.add(order);
        });

        Order updatedOrder = inNewTransaction(() -> orders.ofId(orderId).orElseThrow());
        Assertions.assertThat(updatedOrder.items()).hasSize(2);
    }

    @Test
    void shouldRemoveMergedItemCorrectlyBetweenTransactions() {
        OrderId orderId = inNewTransaction(() -> {
            Order order = OrderTestDataBuilder.anOrder().withItems(true).build();
            orders.add(order);
            return order.id();
        });

        var removedItemId = inNewTransaction(() -> {
            Order order = orders.ofId(orderId).orElseThrow();
            var orderItemId = order.items().iterator().next().id();
            order.removeItem(orderItemId);
            orders.add(order);
            return orderItemId;
        });

        Order updatedOrder = inNewTransaction(() -> orders.ofId(orderId).orElseThrow());
        Assertions.assertThat(updatedOrder.items()).hasSize(1);
        Assertions.assertThat(updatedOrder.items()).noneMatch(i -> i.id().equals(removedItemId));
    }

    @Test
    void shouldUpdateAndKeepPersistenceEntityState() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        long orderId = order.id().value().toLong();
        orders.add(order);

        var persistenceEntity = entityRepository.findById(orderId).orElseThrow();

        Assertions.assertThat(persistenceEntity.getStatus()).isEqualTo(OrderStatus.PLACED.name());

        Assertions.assertThat(persistenceEntity.getCreatedByUserId()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedAt()).isNotNull();
        Assertions.assertThat(persistenceEntity.getLastModifiedByUserId()).isNotNull();

        order = orders.ofId(order.id()).orElseThrow();
        order.markAsPaid();
        orders.add(order);

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
        orders.add(order);

        Assertions.assertThatNoException().isThrownBy(() -> orders.ofId(order.id()).orElseThrow());
    }

    private <T> T inNewTransaction(Supplier<T> callback) {
        return newTransaction.execute(status -> callback.get());
    }

    private void inNewTransaction(Runnable callback) {
        newTransaction.executeWithoutResult(status -> callback.run());
    }
}
