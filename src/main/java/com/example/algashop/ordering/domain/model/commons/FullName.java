package com.example.algashop.ordering.domain.model.commons;

import com.example.algashop.ordering.domain.model.FieldValidations;

public record FullName(String firstName, String lastName) {

    public FullName {
        firstName = FieldValidations.requiresNonBlank(firstName).trim();
        lastName = FieldValidations.requiresNonBlank(lastName).trim();
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
