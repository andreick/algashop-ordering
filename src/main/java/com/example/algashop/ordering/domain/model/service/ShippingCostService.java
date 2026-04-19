package com.example.algashop.ordering.domain.model.service;

import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.ZipCode;
import lombok.Builder;

import java.time.LocalDate;

public interface ShippingCostService {

    CalculationResult calculate(CalculationRequest request);

    @Builder
    record CalculationRequest(ZipCode origin, ZipCode destination) {
    }

    @Builder
    record CalculationResult(Money cost, LocalDate expectedDate) {
    }
}
