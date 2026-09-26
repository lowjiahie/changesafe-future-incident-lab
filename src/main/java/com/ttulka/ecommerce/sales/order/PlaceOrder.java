package com.ttulka.ecommerce.sales.order;

import java.util.Collection;

import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.sales.order.item.OrderItem;

/**
 * Place Order use-case.
 */
public interface PlaceOrder {

    /**
     * Places a new order.
     *
     * @param orderId        the order ID
     * @param items          the order items
     * @param total          the order total
     * @param idempotencyKey optional client-supplied key; if non-null and already present throws
     *                       {@link DuplicateOrderException}
     */
    void place(OrderId orderId, Collection<OrderItem> items, Money total, String idempotencyKey);

    /**
     * DuplicateOrderException is thrown when an order with the same idempotency key already exists.
     */
    final class DuplicateOrderException extends RuntimeException {
    }
}
