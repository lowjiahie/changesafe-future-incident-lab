package com.ttulka.ecommerce.portal;

import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.shipping.delivery.Address;

import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Coordinates the synchronous checkout writes as one unit of work.
 * OrderPlaced listeners run after the transaction commits.
 */
@RequiredArgsConstructor
public class CheckoutOrder {

    private final @NonNull PlaceOrderFromCart placeOrderFromCart;
    private final @NonNull PrepareOrderDelivery prepareOrderDelivery;
    private final @NonNull AssignOrderToCustomer assignOrderToCustomer;

    /**
     * Demo-only: simulates slow downstream work (payment, delivery preparation) so a double click
     * in the browser reliably overlaps. Zero (the default) disables it.
     */
    @Setter(AccessLevel.PACKAGE)
    private long simulatedLatencyMillis;

    /**
     * Checks out a guest order that is not linked to any customer.
     */
    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress) {
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
        simulateSlowCheckout();
        cart.empty();
        return orderId;
    }

    /**
     * Checks out an order and links it to the customer so it shows up in their order history.
     */
    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress, @NonNull Customer customer) {
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart);
        assignOrderToCustomer.assign(new OrderId(orderId), customer);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
        simulateSlowCheckout();
        cart.empty();
        return orderId;
    }

    private void simulateSlowCheckout() {
        if (simulatedLatencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedLatencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
