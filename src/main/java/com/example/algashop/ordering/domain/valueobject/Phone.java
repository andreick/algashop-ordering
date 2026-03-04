package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.exception.ErrorMessages;
import com.example.algashop.ordering.domain.validator.FieldValidations;

public record Phone(String value) {

    public Phone {
        FieldValidations.requiresValidPhone(value, ErrorMessages.VALIDATION_ERROR_PHONE_IS_INVALID);
    }

    @Override
    public String toString() {
        return value;
    }
}
