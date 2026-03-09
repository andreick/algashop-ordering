package com.example.algashop.ordering.domain.model.valueobject;

import com.example.algashop.ordering.domain.model.validator.FieldValidations;

public record Document(String value) {

    public Document {
        FieldValidations.requiresNonBlank(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
