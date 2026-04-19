package com.example.algashop.ordering.domain.model.customer;

import com.example.algashop.ordering.domain.model.IdGenerator;
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
