package com.example.algashop.ordering.domain.exception;

import com.example.algashop.ordering.domain.valueobject.id.ProductId;

public class ProductOutOfStockException extends DomainException {

    public ProductOutOfStockException(ProductId id) {
        super(String.format(ErrorMessages.ERROR_PRODUCT_IS_OUT_OF_STOCK, id));
    }
}
