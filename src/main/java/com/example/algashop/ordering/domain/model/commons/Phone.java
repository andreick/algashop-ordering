package com.example.algashop.ordering.domain.model.commons;

import com.example.algashop.ordering.domain.model.ErrorMessages;
import com.example.algashop.ordering.domain.model.FieldValidations;

public record Phone(String value) {

    public Phone {
        FieldValidations.requiresValidPhone(value, ErrorMessages.VALIDATION_ERROR_PHONE_IS_INVALID);
    }

    @Override
    public String toString() {
        return value;
    }
}
