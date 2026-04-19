package com.example.algashop.ordering.domain.model.service;

import com.example.algashop.ordering.domain.model.valueobject.Money;
import com.example.algashop.ordering.domain.model.valueobject.id.ProductId;

public interface ShoppingCartProductAdjustmentService {
    void adjustPrice(ProductId productId, Money updatedPrice);

    void changeAvailability(ProductId productId, boolean available);
}
