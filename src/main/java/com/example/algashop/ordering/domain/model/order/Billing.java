package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.commons.Address;
import com.example.algashop.ordering.domain.model.commons.Document;
import com.example.algashop.ordering.domain.model.commons.Email;
import com.example.algashop.ordering.domain.model.commons.FullName;
import com.example.algashop.ordering.domain.model.commons.Phone;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record Billing(
        @NonNull FullName fullName,
        @NonNull Document document,
        @NonNull Phone phone,
        @NonNull Email email,
        @NonNull Address address) {
}
