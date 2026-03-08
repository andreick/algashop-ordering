package com.example.algashop.ordering.domain.valueobject.id;

import com.example.algashop.ordering.domain.utility.IdGenerator;
import lombok.NonNull;

import java.util.UUID;

public record ProductId(@NonNull UUID value) {

    public ProductId() {
        this(IdGenerator.generateTimeBasedUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
