package com.example.algashop.ordering.domain.model.customer;

import com.example.algashop.ordering.domain.model.commons.Address;
import com.example.algashop.ordering.domain.model.commons.Document;
import com.example.algashop.ordering.domain.model.commons.Email;
import com.example.algashop.ordering.domain.model.commons.FullName;
import com.example.algashop.ordering.domain.model.commons.Phone;
import com.example.algashop.ordering.domain.model.commons.ZipCode;
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
				c -> assertThat(c.fullName()).isEqualTo(new FullName("Anonymous", "Anonymous")),
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
								.build()));
	}

	@Test
	void given_archivedCustomer_whenTryToUpdate_shouldGenerateException() {
		Customer customer = CustomerTestDataBuilder.existingAnonymizedCustomer().build();

		assertThatExceptionOfType(CustomerArchivedException.class)
				.isThrownBy(customer::archive);

		Email newEmail = new Email("newemail@mail.com");
		assertThatExceptionOfType(CustomerArchivedException.class)
				.isThrownBy(() -> customer.changeEmail(newEmail));

		Phone newPhone = new Phone("123-123-1111");
		assertThatExceptionOfType(CustomerArchivedException.class)
				.isThrownBy(() -> customer.changePhone(newPhone));

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

	@Test
	void givenValidData_whenCreateBrandNewCustomer_shouldGenerateCustomerRegisteredEvent() {
		Customer customer = CustomerTestDataBuilder.brandNewCustomer().build();
		CustomerRegisteredEvent event = new CustomerRegisteredEvent(customer.id(),
				customer.registeredAt(), customer.fullName(), customer.email());
		assertThat(customer.domainEvents()).contains(event);
	}

	@Test
	void givenUnarchivedCustomer_whenArchive_shouldGenerateCustomerArchivedEvent() {
		Customer customer = CustomerTestDataBuilder.existingCustomer().archived(false).archivedAt(null).build();
		customer.archive();
		CustomerArchivedEvent event = new CustomerArchivedEvent(customer.id(), customer.archivedAt());
		assertThat(customer.domainEvents()).contains(event);
	}
}
