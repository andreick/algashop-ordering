package com.example.algashop.ordering.domain.model.valueobject;

import lombok.Builder;
import lombok.NonNull;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record Shipping(
                @NonNull Money cost,
                @NonNull LocalDate expectedDate,
                @NonNull Recipient recipient,
                @NonNull Address address) {
}
