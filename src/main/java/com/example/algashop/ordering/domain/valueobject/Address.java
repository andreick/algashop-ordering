package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.validator.FieldValidations;
import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
public record Address(
        @NonNull String street,
        String complement,
        @NonNull String neighborhood,
        @NonNull String number,
        @NonNull String city,
        @NonNull String state,
        @NonNull ZipCode zipCode) {

    public Address {
        street = FieldValidations.requiresNonBlank(street).trim();
        neighborhood = FieldValidations.requiresNonBlank(neighborhood).trim();
        number = FieldValidations.requiresNonBlank(number).trim();
        city = FieldValidations.requiresNonBlank(city).trim();
        state = FieldValidations.requiresNonBlank(state).trim();
    }
}
