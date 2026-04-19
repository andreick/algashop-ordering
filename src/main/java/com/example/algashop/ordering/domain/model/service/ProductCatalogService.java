package com.example.algashop.ordering.domain.model.service;

import com.example.algashop.ordering.domain.model.valueobject.Product;
import com.example.algashop.ordering.domain.model.valueobject.id.ProductId;

import java.util.Optional;

public interface ProductCatalogService {
    Optional<Product> ofId(ProductId productId);
}
