package com.example.algashop.ordering.domain.model.order;

import com.example.algashop.ordering.domain.model.commons.Money;
import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductId;
import com.example.algashop.ordering.domain.model.product.ProductName;
import com.example.algashop.ordering.domain.model.product.ProductOutOfStockException;
import com.example.algashop.ordering.domain.model.product.ProductTestDataBuilder;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

class OrderTest {

    @Test
    void shouldGenerateDraftOrder() {
        CustomerId customerId = new CustomerId();
        Order order = Order.draft(customerId);

        Assertions.assertWith(order,
                o -> Assertions.assertThat(o.id()).isNotNull(),
                o -> Assertions.assertThat(o.customerId()).isEqualTo(customerId),
                o -> Assertions.assertThat(o.totalAmount()).isEqualTo(Money.ZERO),
                o -> Assertions.assertThat(o.totalItems()).isEqualTo(Quantity.ZERO),
                o -> Assertions.assertThat(o.isDraft()).isTrue(),
                o -> Assertions.assertThat(o.items()).isEmpty(),

                o -> Assertions.assertThat(o.placedAt()).isNull(),
                o -> Assertions.assertThat(o.paidAt()).isNull(),
                o -> Assertions.assertThat(o.canceledAt()).isNull(),
                o -> Assertions.assertThat(o.readyAt()).isNull(),
                o -> Assertions.assertThat(o.billing()).isNull(),
                o -> Assertions.assertThat(o.shipping()).isNull(),
                o -> Assertions.assertThat(o.paymentMethod()).isNull());
    }

    @Test
    void shouldAddItem() {
        Order order = Order.draft(new CustomerId());
        Product product = ProductTestDataBuilder.aProductAltMousePad().build();
        ProductId productId = product.id();

        order.addItem(product, new Quantity(1));

        Assertions.assertThat(order.items()).hasSize(1);

        OrderItem orderItem = order.items().iterator().next();

        Assertions.assertWith(orderItem,
                i -> Assertions.assertThat(i.id()).isNotNull(),
                i -> Assertions.assertThat(i.productName()).isEqualTo(new ProductName("Mouse Pad")),
                i -> Assertions.assertThat(i.productId()).isEqualTo(productId),
                i -> Assertions.assertThat(i.price()).isEqualTo(new Money("100")),
                i -> Assertions.assertThat(i.quantity()).isEqualTo(new Quantity(1)));
    }

    @Test
    void shouldGenerateExceptionWhenTryToChangeItemSet() {
        Order order = Order.draft(new CustomerId());
        Product product = ProductTestDataBuilder.aProductAltMousePad().build();

        order.addItem(product, new Quantity(1));

        Set<OrderItem> items = order.items();

        Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(items::clear);
    }

    @Test
    void shouldCalculateTotals() {
        Order order = Order.draft(new CustomerId());

        order.addItem(
                ProductTestDataBuilder.aProductAltMousePad().build(),
                new Quantity(2));

        order.addItem(
                ProductTestDataBuilder.aProductAltRamMemory().build(),
                new Quantity(1));

        Assertions.assertThat(order.totalAmount()).isEqualTo(new Money("400"));
        Assertions.assertThat(order.totalItems()).isEqualTo(new Quantity(3));
    }

    @Test
    void givenDraftOrder_whenPlace_shouldChangeToPlaced() {
        Order order = OrderTestDataBuilder.anOrder().build();
        order.place();
        Assertions.assertThat(order.isPlaced()).isTrue();
    }

    @Test
    void givenPlacedOrder_whenMarkAsPaid_shouldChangeToPaid() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        order.markAsPaid();
        Assertions.assertThat(order.isPaid()).isTrue();
        Assertions.assertThat(order.paidAt()).isNotNull();
    }

    @Test
    void givenPaidOrder_whenMarkAsReady_shouldChangeToReady() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();
        order.markAsReady();
        Assertions.assertThat(order.isReady()).isTrue();
        Assertions.assertThat(order.readyAt()).isNotNull();
    }

    @Test
    void givenNonPaidOrder_whenMarkAsReady_shouldThrowExceptionAndKeepState() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        OrderStatus beforeStatus = order.status();
        Assertions.assertThat(order.readyAt()).isNull();
        Assertions.assertThatExceptionOfType(OrderStatusCannotBeChangedException.class)
                .isThrownBy(order::markAsReady);
        Assertions.assertThat(order.status()).isEqualTo(beforeStatus);
        Assertions.assertThat(order.readyAt()).isNull();
    }

    @Test
    void givenPlacedOrder_whenTryToPlace_shouldGenerateException() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        Assertions.assertThatExceptionOfType(OrderStatusCannotBeChangedException.class)
                .isThrownBy(order::place);
    }

    @Test
    void givenDraftOrder_whenChangePaymentMethod_shouldAllowChange() {
        Order order = Order.draft(new CustomerId());
        order.changePaymentMethod(PaymentMethod.CREDIT_CARD);
        Assertions.assertWith(order.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void givenDraftOrder_whenChangeBilling_shouldAllowChange() {
        Billing billing = OrderTestDataBuilder.aBilling();
        Order order = Order.draft(new CustomerId());
        order.changeBilling(billing);

        Assertions.assertThat(order.billing()).isEqualTo(billing);
    }

    @Test
    void givenDraftOrder_whenChangeShipping_shouldAllowChange() {
        Shipping shipping = OrderTestDataBuilder.aShipping();
        Order order = Order.draft(new CustomerId());
        Money expectedTotalAmount = order.totalAmount().add(shipping.cost());

        order.changeShipping(shipping);

        Assertions.assertWith(order,
                o -> Assertions.assertThat(o.shipping()).isEqualTo(shipping),
                o -> Assertions.assertThat(o.totalAmount()).isEqualTo(expectedTotalAmount));
    }

    @Test
    void givenDraftOrderAndDeliveryDateInThePast_whenChangeShipping_shouldNotAllowChange() {
        LocalDate expectedDeliveryDate = LocalDate.now().minusDays(2);

        Shipping shipping = OrderTestDataBuilder.aShipping().toBuilder()
                .expectedDate(expectedDeliveryDate)
                .build();

        Order order = Order.draft(new CustomerId());

        Assertions.assertThatExceptionOfType(OrderInvalidShippingDeliveryDateException.class)
                .isThrownBy(() -> order.changeShipping(shipping));
    }

    @Test
    void givenDraftOrder_whenChangeItem_shouldRecalculate() {
        Order order = Order.draft(new CustomerId());

        order.addItem(
                ProductTestDataBuilder.aProductAltMousePad().build(),
                new Quantity(3));

        OrderItem orderItem = order.items().iterator().next();

        order.changeItemQuantity(orderItem.id(), new Quantity(5));

        Assertions.assertWith(order,
                o -> Assertions.assertThat(o.totalAmount()).isEqualTo(new Money("500")),
                o -> Assertions.assertThat(o.totalItems()).isEqualTo(new Quantity(5)));
    }

    @Test
    void givenOutOfStockProduct_whenTryToAddToAnOrder_shouldNotAllow() {
        Order order = Order.draft(new CustomerId());

        ThrowingCallable addItem = () -> order.addItem(
                ProductTestDataBuilder.aProductUnavailable().build(),
                new Quantity(1));

        Assertions.assertThatExceptionOfType(ProductOutOfStockException.class).isThrownBy(addItem);
    }

    @Test
    void givenDraftOrder_withTwoItems_whenRemoveItem_shouldRecalculateTotalsAndCount() {
        Order order = OrderTestDataBuilder.anOrder().build();
        int initialCount = order.items().size();
        Money initialTotal = order.totalAmount();
        Quantity initialItems = order.totalItems();

        OrderItem itemToRemove = order.items().iterator().next();

        order.removeItem(itemToRemove.id());

        Assertions.assertThat(order.items()).hasSize(initialCount - 1);

        Money expectedTotal = new Money(
                initialTotal.value().subtract(itemToRemove.totalAmount().value()));
        Quantity expectedQuantity = new Quantity(
                initialItems.value() - itemToRemove.quantity().value());

        Assertions.assertWith(order,
                o -> Assertions.assertThat(o.totalAmount()).isEqualTo(expectedTotal),
                o -> Assertions.assertThat(o.totalItems()).isEqualTo(expectedQuantity));
    }

    @Test
    void givenDraftOrder_whenRemoveNonexistentItem_shouldThrow() {
        Order order = OrderTestDataBuilder.anOrder().build();
        OrderItemId fakeId = new OrderItemId();

        Assertions.assertThatExceptionOfType(OrderDoesNotContainOrderItemException.class)
                .isThrownBy(() -> order.removeItem(fakeId));
    }

    @Test
    void givenPlacedOrder_whenRemoveItem_shouldThrowCannotBeEdited() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();
        OrderItem firstItem = order.items().iterator().next();

        ThrowingCallable removeItem = () -> order.removeItem(firstItem.id());
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(removeItem);
    }

    @Test
    void givenPlacedOrder_whenTryToEdit_shouldThrowCannotBeEdited() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PLACED).build();

        ThrowingCallable changePaymentMethod = () -> order.changePaymentMethod(PaymentMethod.CREDIT_CARD);
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changePaymentMethod);

        ThrowingCallable changeBilling = () -> order.changeBilling(OrderTestDataBuilder.aBilling());
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changeBilling);

        ThrowingCallable changeShipping = () -> order.changeShipping(OrderTestDataBuilder.aShippingAlt());
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changeShipping);

        // adding an item also should be forbidden once order is placed
        ThrowingCallable addAfterPlaced = () -> order.addItem(ProductTestDataBuilder.aProduct().build(),
                new Quantity(1));
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(addAfterPlaced);
    }

    @Test
    void givenPaidOrder_whenTryToEdit_shouldThrowCannotBeEdited() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.PAID).build();

        ThrowingCallable changePaymentMethod = () -> order.changePaymentMethod(PaymentMethod.CREDIT_CARD);
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changePaymentMethod);

        ThrowingCallable changeBilling = () -> order.changeBilling(OrderTestDataBuilder.aBilling());
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changeBilling);

        ThrowingCallable changeShipping = () -> order.changeShipping(OrderTestDataBuilder.aShippingAlt());
        Assertions.assertThatExceptionOfType(OrderCannotBeEditedException.class).isThrownBy(changeShipping);
    }

    @Test
    void givenOrderInVariousStates_whenCancel_shouldMoveToCanceledAndRecordDate() {
        for (OrderStatus state : java.util.Arrays.asList(OrderStatus.DRAFT, OrderStatus.PLACED, OrderStatus.PAID,
                OrderStatus.READY)) {
            Order order = OrderTestDataBuilder.anOrder().status(state).build();
            order.cancel();
            Assertions.assertThat(order.isCanceled()).isTrue();
            Assertions.assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
            Assertions.assertThat(order.canceledAt()).isNotNull();
        }
    }

    @Test
    void givenCanceledOrder_whenCancelAgain_shouldThrowAndKeepState() {
        Order order = OrderTestDataBuilder.anOrder().status(OrderStatus.CANCELED).build();
        java.time.OffsetDateTime before = order.canceledAt();
        Assertions.assertThatExceptionOfType(OrderStatusCannotBeChangedException.class)
                .isThrownBy(order::cancel);
        Assertions.assertThat(order.status()).isEqualTo(OrderStatus.CANCELED);
        Assertions.assertThat(order.canceledAt()).isEqualTo(before);
    }
}
