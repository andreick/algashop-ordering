package com.example.algashop.ordering.domain.model.commons;

import com.example.algashop.ordering.domain.model.FieldValidations;

public record Document(String value) {

    public Document {
        FieldValidations.requiresNonBlank(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
