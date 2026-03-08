package com.example.algashop.ordering.domain.valueobject;

import lombok.NonNull;

import java.io.Serializable;

public record Quantity(@NonNull Integer value) implements Serializable, Comparable<Quantity> {

    public static final Quantity ZERO = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
    }

    public Quantity add(@NonNull Quantity quantity) {
        return new Quantity(this.value + quantity.value());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(Quantity o) {
        return this.value.compareTo(o.value);
    }
}
