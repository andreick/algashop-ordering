package com.example.algashop.ordering.application.customer.loyaltypoints;

import com.example.algashop.ordering.domain.model.customer.Customer;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.customer.CustomerLoyaltyPointsService;
import com.example.algashop.ordering.domain.model.customer.CustomerNotFoundException;
import com.example.algashop.ordering.domain.model.customer.Customers;
import com.example.algashop.ordering.domain.model.order.Order;
import com.example.algashop.ordering.domain.model.order.OrderId;
import com.example.algashop.ordering.domain.model.order.OrderNotFoundException;
import com.example.algashop.ordering.domain.model.order.Orders;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerLoyaltyPointsApplicationService {

    private final CustomerLoyaltyPointsService customerLoyaltyPointsService;
    private final Orders orders;
    private final Customers customers;

    @Transactional
    public void addLoyaltyPoints(@NonNull UUID rawCustomerId, @NonNull String rawOrderId) {
        CustomerId customerId = new CustomerId(rawCustomerId);
        Customer customer = customers.ofId(customerId)
                .orElseThrow(CustomerNotFoundException::new);

        OrderId orderId = new OrderId(rawOrderId);
        Order order = orders.ofId(orderId)
                .orElseThrow(OrderNotFoundException::new);

        customerLoyaltyPointsService.addPoints(customer, order);

        customers.add(customer);
    }
}
