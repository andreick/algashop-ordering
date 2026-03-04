package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.exception.ErrorMessages;
import lombok.NonNull;

public record ZipCode(@NonNull String value) {

    public ZipCode {
        if (value.length() != 5) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_ZIPCODE_LENGTH_INVALID);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
