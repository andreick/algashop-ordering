package com.example.algashop.ordering.domain.model.product;

import com.example.algashop.ordering.domain.model.commons.Money;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record Product(
        @NonNull ProductId id,
        @NonNull ProductName name,
        @NonNull Money price,
        @NonNull Boolean inStock) {

    public void checkOutOfStock() {
        if (isOutOfStock()) {
            throw new ProductOutOfStockException(this.id());
        }
    }

    private boolean isOutOfStock() {
        return !inStock();
    }
}
