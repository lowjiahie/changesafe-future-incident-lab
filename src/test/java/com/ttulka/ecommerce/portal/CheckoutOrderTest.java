package com.ttulka.ecommerce.portal;

import java.util.List;
import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.cart.item.ProductId;
import com.ttulka.ecommerce.sales.cart.item.Title;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;
import com.ttulka.ecommerce.warehouse.Amount;
import com.ttulka.ecommerce.warehouse.InStock;
import com.ttulka.ecommerce.warehouse.Warehouse;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutOrderTest {

    private final PlaceOrderFromCart placeOrder = mock(PlaceOrderFromCart.class);
    private final PrepareOrderDelivery prepareDelivery = mock(PrepareOrderDelivery.class);
    private final AssignOrderToCustomer assignToCustomer = mock(AssignOrderToCustomer.class);
    private final Warehouse warehouse = mock(Warehouse.class);
    private final CheckoutOrder checkoutOrder = new CheckoutOrder(placeOrder, prepareDelivery, assignToCustomer, warehouse);
    private final Cart cart = mock(Cart.class);
    private final Address address = new Address(new Person("Alex Chen"), new Place("Demo Street 1"));

    @Test
    void order_delivery_and_cart_clear_happen_in_sequence() {
        when(cart.items()).thenReturn(List.of());

        UUID orderId = checkoutOrder.checkout(cart, address, null);

        InOrder steps = inOrder(placeOrder, prepareDelivery, cart);
        steps.verify(placeOrder).placeOrder(orderId, cart, null);
        steps.verify(prepareDelivery).prepareDelivery(orderId, address);
        steps.verify(cart).empty();
    }

    @Test
    void cart_is_not_cleared_when_delivery_cannot_be_prepared() {
        when(cart.items()).thenReturn(List.of());
        doThrow(new IllegalStateException("Delivery unavailable"))
                .when(prepareDelivery).prepareDelivery(any(UUID.class), eq(address));

        assertThrows(IllegalStateException.class, () -> checkoutOrder.checkout(cart, address, null));
        verify(cart, never()).empty();
    }

    @Test
    void guest_checkout_is_not_assigned_to_a_customer() {
        when(cart.items()).thenReturn(List.of());

        checkoutOrder.checkout(cart, address, null);

        verifyNoInteractions(assignToCustomer);
    }

    @Test
    void customer_order_is_assigned_between_placing_and_delivery() {
        Customer customer = new Customer("ami");
        when(cart.items()).thenReturn(List.of());

        UUID orderId = checkoutOrder.checkout(cart, address, customer, null);

        InOrder steps = inOrder(placeOrder, assignToCustomer, prepareDelivery, cart);
        steps.verify(placeOrder).placeOrder(orderId, cart, null);
        steps.verify(assignToCustomer).assign(new OrderId(orderId), customer);
        steps.verify(prepareDelivery).prepareDelivery(orderId, address);
        steps.verify(cart).empty();
    }

    @Test
    void cart_is_not_cleared_when_the_order_cannot_be_assigned() {
        when(cart.items()).thenReturn(List.of());
        doThrow(new IllegalStateException("Assignment failed"))
                .when(assignToCustomer).assign(any(OrderId.class), eq(new Customer("ami")));

        assertThrows(IllegalStateException.class, () -> checkoutOrder.checkout(cart, address, new Customer("ami"), null));
        verify(cart, never()).empty();
        verifyNoInteractions(prepareDelivery);
    }

    @Test
    void checkout_is_blocked_when_item_is_out_of_stock() {
        // R-05: warehouse says only 5 in stock; cart requests 10
        CartItem item = new CartItem(new ProductId("P-OOS"), new Title("Test"), new Money(1f), new Quantity(10));
        when(cart.items()).thenReturn(List.of(item));
        when(warehouse.leftInStock(any())).thenReturn(new InStock(new Amount(5)));

        assertThrows(CheckoutOrder.OutOfStockException.class,
                     () -> checkoutOrder.checkout(cart, address, null));
        verifyNoInteractions(placeOrder);
        verify(cart, never()).empty();
    }
}
