package com.example.algashop.ordering.domain.model.shoppingcart;

import com.example.algashop.ordering.domain.model.IdGenerator;
import lombok.NonNull;

import java.util.UUID;

public record ShoppingCartItemId(@NonNull UUID value) {

    public ShoppingCartItemId() {
        this(IdGenerator.generateTimeBasedUUID());
    }

    public ShoppingCartItemId(String value) {
        this(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
