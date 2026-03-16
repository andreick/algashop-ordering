package com.example.algashop.ordering.domain.model.repository;

import com.example.algashop.ordering.domain.model.entity.Customer;
import com.example.algashop.ordering.domain.model.entity.CustomerTestDataBuilder;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.infrastructure.persistence.assembler.CustomerPersistenceEntityAssembler;
import com.example.algashop.ordering.infrastructure.persistence.disassembler.CustomerPersistenceEntityDisassembler;
import com.example.algashop.ordering.infrastructure.persistence.provider.CustomersPersistenceProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@DataJpaTest
@Import({ CustomersPersistenceProvider.class,
        CustomerPersistenceEntityAssembler.class,
        CustomerPersistenceEntityDisassembler.class })
class CustomersIT {

    private Customers customers;

    @Autowired
    CustomersIT(Customers customers) {
        this.customers = customers;
    }

    @Test
    void shouldPersistAndFind() {
        Customer originalCustomer = CustomerTestDataBuilder.brandNewCustomer().build();
        CustomerId customerId = originalCustomer.id();
        customers.add(originalCustomer);

        Optional<Customer> possibleCustomer = customers.ofId(customerId);

        Assertions.assertThat(possibleCustomer).isPresent();

        Customer savedCustomer = possibleCustomer.get();

        Assertions.assertThat(savedCustomer).satisfies(
                s -> Assertions.assertThat(s.id()).isEqualTo(customerId));
    }

    @Test
    void shouldUpdateExistingCustomer() {
        Customer customer = CustomerTestDataBuilder.brandNewCustomer().build();
        customers.add(customer);

        customer = customers.ofId(customer.id()).orElseThrow();
        customer.archive();

        customers.add(customer);

        Customer savedCustomer = customers.ofId(customer.id()).orElseThrow();

        Assertions.assertThat(savedCustomer.archivedAt()).isNotNull();
        Assertions.assertThat(savedCustomer.isArchived()).isTrue();

    }

    @Test
    void shouldCountExistingOrders() {
        Assertions.assertThat(customers.count()).isZero();

        Customer customer1 = CustomerTestDataBuilder.brandNewCustomer().build();
        customers.add(customer1);

        Customer customer2 = CustomerTestDataBuilder.brandNewCustomer().build();
        customers.add(customer2);

        Assertions.assertThat(customers.count()).isEqualTo(2L);
    }

    @Test
    void shouldReturnValidateIfOrderExists() {
        Customer customer = CustomerTestDataBuilder.brandNewCustomer().build();
        customers.add(customer);

        Assertions.assertThat(customers.exists(customer.id())).isTrue();
        Assertions.assertThat(customers.exists(new CustomerId())).isFalse();
    }
}
