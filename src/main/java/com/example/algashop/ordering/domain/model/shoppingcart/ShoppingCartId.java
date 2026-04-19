package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.IdGenerator;
import lombok.NonNull;

import java.util.UUID;

public record ShoppingCartId(@NonNull UUID value) {

    public ShoppingCartId() {
        this(IdGenerator.generateTimeBasedUUID());
    }

    public ShoppingCartId(String value) {
        this(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
