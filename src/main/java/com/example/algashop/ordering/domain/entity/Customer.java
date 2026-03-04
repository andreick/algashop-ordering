package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.exception.CustomerArchivedException;
import com.example.algashop.ordering.domain.valueobject.Address;
import com.example.algashop.ordering.domain.valueobject.BirthDate;
import com.example.algashop.ordering.domain.valueobject.CustomerId;
import com.example.algashop.ordering.domain.valueobject.Document;
import com.example.algashop.ordering.domain.valueobject.Email;
import com.example.algashop.ordering.domain.valueobject.FullName;
import com.example.algashop.ordering.domain.valueobject.LoyaltyPoints;
import com.example.algashop.ordering.domain.valueobject.Phone;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Accessors(fluent = true)
@Getter
public class Customer {

    private CustomerId id;
    private FullName fullName;
    private BirthDate birthDate;
    private Email email;
    private Phone phone;
    private Document document;
    private Boolean isPromotionNotificationsAllowed;
    private Boolean isArchived;
    private OffsetDateTime registeredAt;
    private OffsetDateTime archivedAt;
    private LoyaltyPoints loyaltyPoints;
    private Address address;

    @Builder(builderClassName = "BrandNewCustomerBuild", builderMethodName = "brandNew")
    private static Customer createBrandNew(FullName fullName, BirthDate birthDate, Email email,
            Phone phone, Document document, Boolean promotionNotificationsAllowed,
            Address address) {
        return new Customer(new CustomerId(),
                fullName,
                birthDate,
                email,
                phone,
                document,
                promotionNotificationsAllowed,
                false,
                OffsetDateTime.now(),
                null,
                LoyaltyPoints.ZERO,
                address);
    }

    @Builder(builderClassName = "ExistingCustomerBuild", builderMethodName = "existing")
    private Customer(CustomerId id, FullName fullName, BirthDate birthDate, Email email, Phone phone,
            Document document, Boolean promotionNotificationsAllowed, Boolean archived,
            OffsetDateTime registeredAt, OffsetDateTime archivedAt, LoyaltyPoints loyaltyPoints, Address address) {
        this.setId(id);
        this.setFullName(fullName);
        this.setBirthDate(birthDate);
        this.setEmail(email);
        this.setPhone(phone);
        this.setDocument(document);
        this.setIsPromotionNotificationsAllowed(promotionNotificationsAllowed);
        this.setIsArchived(archived);
        this.setRegisteredAt(registeredAt);
        this.setArchivedAt(archivedAt);
        this.setLoyaltyPoints(loyaltyPoints);
        this.setAddress(address);
    }

    public void addLoyaltyPoints(LoyaltyPoints loyaltyPointsAdded) {
        verifyIfChangeable();
        this.setLoyaltyPoints(this.loyaltyPoints.add(loyaltyPointsAdded));
    }

    public void archive() {
        verifyIfChangeable();
        this.setIsArchived(true);
        this.setArchivedAt(OffsetDateTime.now());
        this.setFullName(new FullName("Anonymous", "Customer"));
        this.setPhone(new Phone("000-000-0000"));
        this.setDocument(new Document("000-00-0000"));
        this.setEmail(new Email(UUID.randomUUID() + "@anonymous.com"));
        this.setBirthDate(null);
        this.setIsPromotionNotificationsAllowed(false);
        this.setAddress(this.address.toBuilder()
                .number("Anonymized")
                .complement(null).build());
    }

    public void enablePromotionNotifications() {
        verifyIfChangeable();
        this.setIsPromotionNotificationsAllowed(true);
    }

    public void disablePromotionNotifications() {
        verifyIfChangeable();
        this.setIsPromotionNotificationsAllowed(false);
    }

    public void changeName(FullName fullName) {
        verifyIfChangeable();
        this.setFullName(fullName);
    }

    public void changeEmail(Email email) {
        verifyIfChangeable();
        this.setEmail(email);
    }

    public void changePhone(Phone phone) {
        verifyIfChangeable();
        this.setPhone(phone);
    }

    private void setId(CustomerId id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setFullName(FullName fullName) {
        Objects.requireNonNull(fullName);
        this.fullName = fullName;
    }

    private void setBirthDate(BirthDate birthDate) {
        this.birthDate = birthDate;
    }

    private void setEmail(Email email) {
        Objects.requireNonNull(email);
        this.email = email;
    }

    private void setPhone(Phone phone) {
        Objects.requireNonNull(phone);
        this.phone = phone;
    }

    private void setDocument(Document document) {
        Objects.requireNonNull(document);
        this.document = document;
    }

    private void setIsPromotionNotificationsAllowed(Boolean promotionNotificationsAllowed) {
        Objects.requireNonNull(promotionNotificationsAllowed);
        this.isPromotionNotificationsAllowed = promotionNotificationsAllowed;
    }

    private void setIsArchived(Boolean archived) {
        Objects.requireNonNull(archived);
        this.isArchived = archived;
    }

    private void setRegisteredAt(OffsetDateTime registeredAt) {
        Objects.requireNonNull(registeredAt);
        this.registeredAt = registeredAt;
    }

    private void setArchivedAt(OffsetDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    private void setLoyaltyPoints(LoyaltyPoints loyaltyPoints) {
        Objects.requireNonNull(loyaltyPoints);
        this.loyaltyPoints = loyaltyPoints;
    }

    private void setAddress(Address address) {
        Objects.requireNonNull(address);
        this.address = address;
    }

    private void verifyIfChangeable() {
        if (Boolean.TRUE.equals(this.isArchived)) {
            throw new CustomerArchivedException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
