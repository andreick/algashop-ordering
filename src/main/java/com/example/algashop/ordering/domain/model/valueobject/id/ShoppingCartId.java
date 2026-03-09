package com.example.algashop.ordering.domain.model.valueobject.id;

import com.example.algashop.ordering.domain.model.utility.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.NonNull;

public record ShoppingCartId(@NonNull TSID value) {

    public ShoppingCartId() {
        this(IdGenerator.gererateTSID());
    }

    public ShoppingCartId(Long value) {
        this(TSID.from(value));
    }

    public ShoppingCartId(String value) {
        this(TSID.from(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
