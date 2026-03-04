package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.exception.ErrorMessages;
import com.example.algashop.ordering.domain.validator.FieldValidations;

public record Email(String value) {

    public Email {
        FieldValidations.requiresValidEmail(value, ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
    }

    @Override
    public String toString() {
        return value;
    }
}
