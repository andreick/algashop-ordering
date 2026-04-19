package com.example.algashop.ordering.infrastructure.persistence.customer;

import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.customer.CustomerTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPersistenceEntityAssemblerTest {

    private CustomerPersistenceEntityAssembler assembler;

    @BeforeEach
    void setup() {
        assembler = new CustomerPersistenceEntityAssembler();
    }

    @Test
    void shouldConvertToPersistenceEntity() {
        Customer customer = CustomerTestDataBuilder.existingCustomer().build();

        CustomerPersistenceEntity persistence = assembler.fromDomain(customer);

        assertThat(persistence).satisfies(
                p -> assertThat(p.getId()).isEqualTo(customer.id().value()),
                p -> assertThat(p.getFirstName()).isEqualTo(customer.fullName().firstName()),
                p -> assertThat(p.getLastName()).isEqualTo(customer.fullName().lastName()),
                p -> assertThat(p.getBirthDate()).isEqualTo(customer.birthDate().value()),
                p -> assertThat(p.getEmail()).isEqualTo(customer.email().value()),
                p -> assertThat(p.getPhone()).isEqualTo(customer.phone().value()),
                p -> assertThat(p.getDocument()).isEqualTo(customer.document().value()),
                p -> assertThat(p.getPromotionNotificationsAllowed())
                        .isEqualTo(customer.isPromotionNotificationsAllowed()),
                p -> assertThat(p.getArchived()).isEqualTo(customer.isArchived()),
                p -> assertThat(p.getRegisteredAt()).isEqualTo(customer.registeredAt()),
                p -> assertThat(p.getArchivedAt()).isEqualTo(customer.archivedAt()),
                p -> assertThat(p.getLoyaltyPoints()).isEqualTo(customer.loyaltyPoints().value()),
                p -> assertThat(p.getAddress()).satisfies(a -> {
                    assertThat(a.getStreet()).isEqualTo(customer.address().street());
                    assertThat(a.getNumber()).isEqualTo(customer.address().number());
                    assertThat(a.getNeighborhood()).isEqualTo(customer.address().neighborhood());
                    assertThat(a.getCity()).isEqualTo(customer.address().city());
                    assertThat(a.getState()).isEqualTo(customer.address().state());
                    assertThat(a.getZipCode()).isEqualTo(customer.address().zipCode().value());
                    assertThat(a.getComplement()).isEqualTo(customer.address().complement());
                }));
    }

    @Test
    void givenAnonymizedCustomer_shouldMapAnonymizedFields() {
        Customer customer = CustomerTestDataBuilder.existingAnonymizedCustomer().build();

        CustomerPersistenceEntity persistence = assembler.fromDomain(customer);

        assertThat(persistence.getBirthDate()).isNull();
        assertThat(persistence.getArchived()).isTrue();
        assertThat(persistence.getEmail()).isEqualTo(customer.email().value());
        assertThat(persistence.getPhone()).isEqualTo(customer.phone().value());
        assertThat(persistence.getDocument()).isEqualTo(customer.document().value());
        assertThat(persistence.getPromotionNotificationsAllowed())
                .isEqualTo(customer.isPromotionNotificationsAllowed());
        assertThat(persistence.getArchivedAt()).isNotNull();
        assertThat(persistence.getLoyaltyPoints()).isEqualTo(customer.loyaltyPoints().value());
    }
}
