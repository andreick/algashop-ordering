package com.example.algashop.ordering.domain.valueobject;

import com.example.algashop.ordering.domain.exception.ErrorMessages;
import lombok.NonNull;

public record LoyaltyPoints(@NonNull Integer value) implements Comparable<LoyaltyPoints> {

    public static final LoyaltyPoints ZERO = new LoyaltyPoints(0);

    public LoyaltyPoints() {
        this(0);
    }

    public LoyaltyPoints {
        if (value < 0) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE);
        }
    }

    public LoyaltyPoints add(@NonNull LoyaltyPoints loyaltyPoints) {
        return add(loyaltyPoints.value());
    }

    public LoyaltyPoints add(@NonNull Integer value) {
        if (value <= 0) {
            throw new IllegalArgumentException(ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_ZERO_OR_NEGATIVE);
        }
        return new LoyaltyPoints(this.value() + value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int compareTo(LoyaltyPoints o) {
        return this.value().compareTo(o.value());
    }
}
