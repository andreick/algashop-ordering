package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.exception.ErrorMessages;
import lombok.NonNull;

import java.time.LocalDate;

public record BirthDate(@NonNull LocalDate value) {

    public BirthDate {
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_BIRTHDATE_FROM_FUTURE);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
