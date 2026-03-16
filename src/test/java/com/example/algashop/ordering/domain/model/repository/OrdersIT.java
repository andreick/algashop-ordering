package com.example.algashop.ordering.domain.model.repository;

import com.example.algashop.ordering.domain.model.entity.CustomerTestDataBuilder;
import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.entity.OrderStatus;
import com.example.algashop.ordering.domain.model.entity.OrderTestDataBuilder;
import com.example.algashop.ordering.domain.model.entity.ProductTestDataBuilder;
import com.example.algashop.ordering.domain.model.valueobject.Quantity;
import com.example.algashop.ordering.domain.model.valueobject.id.OrderId;
import com.example.algashop.ordering.infrastructure.persistence.assembler.CustomerPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.assembler.OrderPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.CustomerPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.OrderPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.provider.CustomersPersistenceProvider;
import com.example.algashop.ordering.infrastructure.persistence.provider.OrdersPersistenceProvider;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Sql(statements = { "DELETE FROM order_item", "DELETE FROM \"order\"" })
@Import({
        OrdersPersistenceProvider.class, OrderPersistenceEntityAssembler.class,
        OrderPersistenceEntityDisassembler.class, CustomersPersistenceProvider.class,
        CustomerPersistenceEntityAssembler.class, CustomerPersistenceEntityDisassembler.class
})
class OrdersIT {

    private final Orders orders;
    private final Customers customers;
    private final TransactionTemplate newTransaction;

    @Autowired
    public OrdersIT(Orders orders, Customers customers, PlatformTransactionManager transactionManager) {
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
        orders.add(order);

        Order order1 = orders.ofId(order.id()).orElseThrow();
        Order order2 = orders.ofId(order.id()).orElseThrow();

        order1.markAsPaid();
        orders.add(order1);

        order2.cancel();

        orders.add(order2);

        Order savedOrder = orders.ofId(order.id()).orElseThrow();

        Assertions.assertThat(savedOrder.canceledAt()).isNotNull();
        Assertions.assertThat(savedOrder.paidAt()).isNull();
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

    private <T> T inNewTransaction(Supplier<T> callback) {
        return newTransaction.execute(status -> callback.get());
    }

    private void inNewTransaction(Runnable callback) {
        newTransaction.executeWithoutResult(status -> callback.run());
    }
}
