package com.example.algashop.ordering.domain.model.valueobject.id;

import com.example.algashop.ordering.domain.model.utility.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.NonNull;

public record ShoppingCartItemId(@NonNull TSID value) {

    public ShoppingCartItemId() {
        this(IdGenerator.gererateTSID());
    }

    public ShoppingCartItemId(Long value) {
        this(TSID.from(value));
    }

    public ShoppingCartItemId(String value) {
        this(TSID.from(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
