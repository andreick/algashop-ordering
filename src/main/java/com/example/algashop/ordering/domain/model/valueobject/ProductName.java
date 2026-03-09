package com.example.algashop.ordering.domain.model.valueobject;

import com.example.algashop.ordering.domain.model.validator.FieldValidations;

public record ProductName(String value) {

    public ProductName {
        value = FieldValidations.requiresNonBlank(value).trim();
    }

    @Override
    public final String toString() {
        return value;
    }
}
