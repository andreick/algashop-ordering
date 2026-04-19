package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.Repository;
import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.customer.CustomerId;

import java.time.Year;
import java.util.List;

public interface Orders extends Repository<Order, OrderId> {

    List<Order> placedByCustomerInYear(CustomerId customerId, Year year);

    long salesQuantityByCustomerInYear(CustomerId customerId, Year year);

    Money totalSoldForCustomer(CustomerId customerId);
}
