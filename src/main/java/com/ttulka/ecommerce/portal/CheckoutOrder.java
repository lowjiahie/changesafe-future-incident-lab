package com.ttulka.ecommerce.portal;

import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.shipping.delivery.Address;

import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
     * Checks out a guest order that is not linked to any customer.
     */
    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress) {
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
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
        cart.empty();
        return orderId;
    }
}
