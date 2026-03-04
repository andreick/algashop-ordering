package com.example.algashop.ordering.domain.validator;

import lombok.NonNull;
import org.apache.commons.validator.routines.EmailValidator;

public class FieldValidations {

    private FieldValidations() {
    }

    public static String requiresNonBlank(String value) {
        return requiresNonBlank(value, null);
    }

    public static String requiresNonBlank(@NonNull String value, String errorMessage) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    public static String requiresValidEmail(String email) {
        return requiresValidEmail(email, null);
    }

    public static String requiresValidEmail(@NonNull String email, String errorMessage) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return email;
    }

    public static String requiresValidPhone(String phone) {
        return requiresValidPhone(phone, null);
    }

    public static String requiresValidPhone(@NonNull String phone, String errorMessage) {
        // Simple regex for phone validation
        String phoneRegex = "^\\+?[0-9. ()-]{7,25}$";
        if (!phone.matches(phoneRegex)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return phone;
    }
}
