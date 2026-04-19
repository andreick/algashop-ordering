package com.example.algashop.ordering.domain.model.service;

import com.example.algashop.ordering.domain.model.entity.Customer;
import com.example.algashop.ordering.domain.model.entity.Order;
import com.example.algashop.ordering.domain.model.exception.CantAddLoyaltyPointsOrderIsNotReady;
import com.example.algashop.ordering.domain.model.exception.OrderNotBelongsToCustomerException;
import com.example.algashop.ordering.domain.model.utility.DomainService;
import com.example.algashop.ordering.domain.model.valueobject.LoyaltyPoints;
import com.example.algashop.ordering.domain.model.valueobject.Money;
import lombok.NonNull;

@DomainService
public class CustomerLoyaltyPointsService {

    private static final LoyaltyPoints basePoints = new LoyaltyPoints(5);

    private static final Money expectedAmountToGivePoints = new Money("1000");

    public void addPoints(@NonNull Customer customer, @NonNull Order order) {
        if (!customer.id().equals(order.customerId())) {
            throw new OrderNotBelongsToCustomerException();
        }

        if (!order.isReady()) {
            throw new CantAddLoyaltyPointsOrderIsNotReady();
        }

        customer.addLoyaltyPoints(calculatePoints(order));
    }

    private LoyaltyPoints calculatePoints(Order order) {
        if (shouldGivePointsByAmount(order.totalAmount())) {
            Money result = order.totalAmount().divide(expectedAmountToGivePoints);
            return new LoyaltyPoints(result.value().intValue() * basePoints.value());
        }

        return LoyaltyPoints.ZERO;
    }

    private boolean shouldGivePointsByAmount(Money amount) {
        return amount.compareTo(expectedAmountToGivePoints) >= 0;
    }
}
