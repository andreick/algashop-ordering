package com.example.algashop.ordering.domain.entity;

import com.example.algashop.ordering.domain.exception.CustomerArchivedException;
import com.example.algashop.ordering.domain.exception.ErrorMessages;
import com.example.algashop.ordering.domain.validator.FieldValidations;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Accessors(fluent = true)
@Getter
public class Customer {

    private UUID id;
    private String fullName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String document;
    private Boolean isPromotionNotificationsAllowed;
    private Boolean isArchived;
    private OffsetDateTime registeredAt;
    private OffsetDateTime archivedAt;
    private Integer loyaltyPoints;

    public Customer(UUID id, String fullName, LocalDate birthDate, String email,
            String phone, String document, Boolean isPromotionNotificationsAllowed,
            OffsetDateTime registeredAt) {
        this.setId(id);
        this.setFullName(fullName);
        this.setBirthDate(birthDate);
        this.setEmail(email);
        this.setPhone(phone);
        this.setDocument(document);
        this.setIsPromotionNotificationsAllowed(isPromotionNotificationsAllowed);
        this.setRegisteredAt(registeredAt);
        this.setIsArchived(false);
        this.setLoyaltyPoints(0);
    }

    public Customer(UUID id, String fullName, LocalDate birthDate, String email, String phone,
            String document, Boolean isPromotionNotificationsAllowed, Boolean isArchived,
            OffsetDateTime registeredAt, OffsetDateTime archivedAt, Integer loyaltyPoints) {
        this.setId(id);
        this.setFullName(fullName);
        this.setBirthDate(birthDate);
        this.setEmail(email);
        this.setPhone(phone);
        this.setDocument(document);
        this.setIsPromotionNotificationsAllowed(isPromotionNotificationsAllowed);
        this.setIsArchived(isArchived);
        this.setRegisteredAt(registeredAt);
        this.setArchivedAt(archivedAt);
        this.setLoyaltyPoints(loyaltyPoints);
    }

    public void addLoyaltyPoints(Integer loyaltyPointsAdded) {
        verifyIfChangeable();
        if (loyaltyPointsAdded <= 0) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE);
        }
        this.setLoyaltyPoints(this.loyaltyPoints() + loyaltyPointsAdded);
    }

    public void archive() {
        verifyIfChangeable();
        this.setIsArchived(true);
        this.setArchivedAt(OffsetDateTime.now());
        this.setFullName("Anonymous");
        this.setPhone("000-000-0000");
        this.setDocument("000-00-0000");
        this.setEmail(UUID.randomUUID() + "@anonymous.com");
        this.setBirthDate(null);
        this.setIsPromotionNotificationsAllowed(false);
    }

    public void enablePromotionNotifications() {
        verifyIfChangeable();
        this.setIsPromotionNotificationsAllowed(true);
    }

    public void disablePromotionNotifications() {
        verifyIfChangeable();
        this.setIsPromotionNotificationsAllowed(false);
    }

    public void changeName(String fullName) {
        verifyIfChangeable();
        this.setFullName(fullName);
    }

    public void changeEmail(String email) {
        verifyIfChangeable();
        this.setEmail(email);
    }

    public void changePhone(String phone) {
        verifyIfChangeable();
        this.setPhone(phone);
    }

    private void setId(UUID id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setFullName(String fullName) {
        Objects.requireNonNull(fullName, ErrorMessages.VALIDATION_ERROR_FULLNAME_IS_NULL);
        if (fullName.isBlank()) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_FULLNAME_IS_BLANK);
        }
        this.fullName = fullName;
    }

    private void setBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            this.birthDate = null;
            return;
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_BIRTHDATE_IN_FUTURE);
        }
        this.birthDate = birthDate;
    }

    private void setEmail(String email) {
        FieldValidations.requiresValidEmail(email, ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
        this.email = email;
    }

    private void setPhone(String phone) {
        Objects.requireNonNull(phone);
        this.phone = phone;
    }

    private void setDocument(String document) {
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

    private void setLoyaltyPoints(Integer loyaltyPoints) {
        Objects.requireNonNull(loyaltyPoints);
        if (loyaltyPoints < 0) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE);
        }
        this.loyaltyPoints = loyaltyPoints;
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
