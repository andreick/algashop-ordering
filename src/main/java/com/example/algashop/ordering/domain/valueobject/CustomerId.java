package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.utility.IdGenerator;
import lombok.NonNull;

import java.util.UUID;

public record CustomerId(@NonNull UUID value) {

    public CustomerId() {
        this(IdGenerator.generateTimeBasedUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
