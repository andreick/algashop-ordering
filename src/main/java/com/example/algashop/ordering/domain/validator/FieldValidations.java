package com.example.algashop.ordering.domain.validator;

import org.apache.commons.validator.routines.EmailValidator;

public class FieldValidations {

    private FieldValidations() {
    }

    public static void requiresValidEmail(String email) {
        requiresValidEmail(email, null);
    }

    public static void requiresValidEmail(String email, String errorMessage) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
