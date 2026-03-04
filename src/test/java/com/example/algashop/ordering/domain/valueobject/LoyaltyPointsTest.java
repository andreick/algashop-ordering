package com.example.algashop.ordering.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LoyaltyPointsTest {

    @Test
    void given_negativeValue_whenCreate_shouldGenerateException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new LoyaltyPoints(-10));
    }

    @Test
    void given_validValue_whenCreate_shouldCreateSuccessfully() {
        LoyaltyPoints points = new LoyaltyPoints(10);

        assertThat(points.value()).isEqualTo(10);
    }

    @Test
    void given_validPoints_whenAddInvalidPoints_shouldGenerateException() {
        LoyaltyPoints points = new LoyaltyPoints(10);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> points.add(LoyaltyPoints.ZERO));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> points.add(-10));
    }
}
