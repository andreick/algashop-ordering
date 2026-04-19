package com.example.algashop.ordering.domain.model.service;

import com.example.algashop.ordering.domain.model.entity.Customer;
import com.example.algashop.ordering.domain.model.exception.CustomerEmailIsInUseException;
import com.example.algashop.ordering.domain.model.repository.Customers;
import com.example.algashop.ordering.domain.model.utility.DomainService;
import com.example.algashop.ordering.domain.model.valueobject.Address;
import com.example.algashop.ordering.domain.model.valueobject.BirthDate;
import com.example.algashop.ordering.domain.model.valueobject.Document;
import com.example.algashop.ordering.domain.model.valueobject.Email;
import com.example.algashop.ordering.domain.model.valueobject.FullName;
import com.example.algashop.ordering.domain.model.valueobject.Phone;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final Customers customers;

    public Customer register(
            FullName fullName, BirthDate birthDate, Email email,
            Phone phone, Document document, Boolean promotionNotificationsAllowed,
            Address address) {

        Customer customer = Customer.brandNew()
                .fullName(fullName)
                .birthDate(birthDate)
                .email(email)
                .phone(phone)
                .document(document)
                .promotionNotificationsAllowed(promotionNotificationsAllowed)
                .address(address)
                .build();

        verifyEmailUniqueness(customer.email(), customer.id());

        return customer;
    }

    public void changeEmail(Customer customer, Email newEmail) {
        verifyEmailUniqueness(newEmail, customer.id());
        customer.changeEmail(newEmail);
    }

    private void verifyEmailUniqueness(Email email, CustomerId customerId) {
        if (!customers.isEmailUnique(email, customerId)) {
            throw new CustomerEmailIsInUseException();
        }
    }
}
