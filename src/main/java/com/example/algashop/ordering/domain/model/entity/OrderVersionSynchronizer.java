package com.example.algashop.ordering.domain.model.entity;

import com.example.algashop.ordering.domain.model.repository.VersionSynchronizer;
import org.springframework.stereotype.Component;

@Component
public class OrderVersionSynchronizer implements VersionSynchronizer<Order> {

    @Override
    public void synchronizeVersion(Order order, Long version) {
        order.setVersion(version);
    }
}
