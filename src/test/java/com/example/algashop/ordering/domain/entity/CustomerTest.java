package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.exception.CustomerArchivedException;
import com.example.algashop.ordering.domain.valueobject.Address;
import com.example.algashop.ordering.domain.valueobject.Document;
import com.example.algashop.ordering.domain.valueobject.Email;
import com.example.algashop.ordering.domain.valueobject.FullName;
import com.example.algashop.ordering.domain.valueobject.LoyaltyPoints;
import com.example.algashop.ordering.domain.valueobject.Phone;
import com.example.algashop.ordering.domain.valueobject.ZipCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertWith;

class CustomerTest {

    @Test
    void given_unarchivedCustomer_whenArchive_shouldAnonymize() {
        Customer customer = CustomerTestDataBuilder.existingCustomer().build();

        customer.archive();

        assertWith(customer,
            c -> assertThat(c.fullName()).isEqualTo(new FullName("Anonymous","Customer")),
            c -> assertThat(c.email()).isNotEqualTo(new Email("john.doe@gmail.com")),
            c -> assertThat(c.phone()).isEqualTo(new Phone("000-000-0000")),
            c -> assertThat(c.document()).isEqualTo(new Document("000-00-0000")),
            c -> assertThat(c.birthDate()).isNull(),
            c -> assertThat(c.isPromotionNotificationsAllowed()).isFalse(),
            c -> assertThat(c.address()).isEqualTo(
                    Address.builder()
                            .street("Bourbon Street")
                            .number("Anonymized")
                            .neighborhood("North Ville")
                            .city("York")
                            .state("South California")
                            .zipCode(new ZipCode("12345"))
                            .complement(null)
                            .build()
            )
        );
    }

    @Test
    void given_archivedCustomer_whenTryToUpdate_shouldGenerateException() {
        Customer customer = CustomerTestDataBuilder.existingAnonymizedCustomer().build();

        assertThatExceptionOfType(CustomerArchivedException.class)
                .isThrownBy(customer::archive);

        Email newEmail = new Email("newemail@mail.com");
        assertThatExceptionOfType(CustomerArchivedException.class)
                .isThrownBy(()-> customer.changeEmail(newEmail));

        Phone newPhone = new Phone("123-123-1111");
        assertThatExceptionOfType(CustomerArchivedException.class)
                .isThrownBy(()-> customer.changePhone(newPhone));

        assertThatExceptionOfType(CustomerArchivedException.class)
                .isThrownBy(customer::enablePromotionNotifications);

        assertThatExceptionOfType(CustomerArchivedException.class)
                .isThrownBy(customer::disablePromotionNotifications);
    }

    @Test
    void given_brandNewCustomer_whenAddLoyaltyPoints_shouldSumPoints() {
        Customer customer = CustomerTestDataBuilder.brandNewCustomer().build();

        customer.addLoyaltyPoints(new LoyaltyPoints(10));
        customer.addLoyaltyPoints(new LoyaltyPoints(20));

        assertThat(customer.loyaltyPoints()).isEqualTo(new LoyaltyPoints(30));
    }
}
