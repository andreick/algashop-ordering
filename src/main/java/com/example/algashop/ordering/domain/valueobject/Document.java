package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.validator.FieldValidations;

public record Document(String value) {

    public Document {
        FieldValidations.requiresNonBlank(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
