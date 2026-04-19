package com.example.algashop.ordering.domain.model.product;

import com.example.algashop.ordering.domain.model.FieldValidations;

public record ProductName(String value) {

    public ProductName {
        value = FieldValidations.requiresNonBlank(value).trim();
    }

    @Override
    public final String toString() {
        return value;
    }
}
