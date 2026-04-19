package com.example.algashop.ordering.domain.model.product;

import com.example.algashop.ordering.domain.model.DomainException;
import com.example.algashop.ordering.domain.model.ErrorMessages;

public class ProductOutOfStockException extends DomainException {

    public ProductOutOfStockException(ProductId id) {
        super(String.format(ErrorMessages.ERROR_PRODUCT_IS_OUT_OF_STOCK, id));
    }
}
