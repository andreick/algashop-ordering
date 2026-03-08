package com.example.algashop.ordering.domain.valueobject.id;

import com.example.algashop.ordering.domain.utility.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.NonNull;

public record OrderItemId(@NonNull TSID value) {

    public OrderItemId() {
        this(IdGenerator.gererateTSID());
    }

    public OrderItemId(Long value) {
        this(TSID.from(value));
    }

    public OrderItemId(String value) {
        this(TSID.from(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
