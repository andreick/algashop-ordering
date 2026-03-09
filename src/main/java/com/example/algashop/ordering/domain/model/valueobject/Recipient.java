package com.example.algashop.ordering.domain.model.valueobject;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record Recipient(
                @NonNull FullName fullName,
                @NonNull Document document,
                @NonNull Phone phone) {
}
