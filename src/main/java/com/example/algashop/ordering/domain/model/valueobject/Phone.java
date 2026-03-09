package com.example.algashop.ordering.domain.model.valueobject;

import com.example.algashop.ordering.domain.model.exception.ErrorMessages;
import com.example.algashop.ordering.domain.model.validator.FieldValidations;

public record Phone(String value) {

    public Phone {
        FieldValidations.requiresValidPhone(value, ErrorMessages.VALIDATION_ERROR_PHONE_IS_INVALID);
    }

    @Override
    public String toString() {
        return value;
    }
}
