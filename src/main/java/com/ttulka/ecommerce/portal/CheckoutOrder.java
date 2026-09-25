package com.ttulka.ecommerce.portal;

import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
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

    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress) {
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
        cart.empty();
        return orderId;
    }
}
