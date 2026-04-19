package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.commons.Address;
import com.example.algashop.ordering.domain.model.commons.Money;
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
