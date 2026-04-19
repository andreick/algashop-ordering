package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.VersionSynchronizer;
import org.springframework.stereotype.Component;

@Component
public class OrderVersionSynchronizer implements VersionSynchronizer<Order> {

    @Override
    public void synchronizeVersion(Order order, Long version) {
        order.setVersion(version);
    }
}
