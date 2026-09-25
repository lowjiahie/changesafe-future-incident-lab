package com.ttulka.ecommerce.portal;

import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CheckoutOrderTest {

    private final PlaceOrderFromCart placeOrder = mock(PlaceOrderFromCart.class);
    private final PrepareOrderDelivery prepareDelivery = mock(PrepareOrderDelivery.class);
    private final CheckoutOrder checkoutOrder = new CheckoutOrder(placeOrder, prepareDelivery);
    private final Cart cart = mock(Cart.class);
    private final Address address = new Address(new Person("Alex Chen"), new Place("Demo Street 1"));

    @Test
    void order_delivery_and_cart_clear_happen_in_sequence() {
        UUID orderId = checkoutOrder.checkout(cart, address);

        InOrder steps = inOrder(placeOrder, prepareDelivery, cart);
        steps.verify(placeOrder).placeOrder(orderId, cart);
        steps.verify(prepareDelivery).prepareDelivery(orderId, address);
        steps.verify(cart).empty();
    }

    @Test
    void cart_is_not_cleared_when_delivery_cannot_be_prepared() {
        doThrow(new IllegalStateException("Delivery unavailable"))
                .when(prepareDelivery).prepareDelivery(any(UUID.class), eq(address));

        assertThrows(IllegalStateException.class, () -> checkoutOrder.checkout(cart, address));
        verify(cart, never()).empty();
    }
}
