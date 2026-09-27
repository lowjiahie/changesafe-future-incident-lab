package com.ttulka.ecommerce.portal;

import java.util.List;
import java.util.UUID;

import com.ttulka.ecommerce.common.events.EventPublisher;
import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.cart.item.ProductId;
import com.ttulka.ecommerce.sales.cart.item.Title;
import com.ttulka.ecommerce.sales.order.OrderPlaced;
import com.ttulka.ecommerce.shipping.delivery.PrepareDelivery;
import com.ttulka.ecommerce.warehouse.Warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@JdbcTest
@ContextConfiguration(classes = {PortalConfig.class, PlaceOrderFromCartTest.ServicesTestConfig.class})
@Sql(statements = "INSERT INTO products VALUES ('1', 'Test 1', 'Test', 1.00), ('2', 'Test 2', 'Test', 2.00);")
class PlaceOrderFromCartTest {

    @Autowired
    private PlaceOrderFromCart placeOrderFromCart;

    @Autowired
    private EventPublisher eventPublisher;

    @Test
    void order_is_placed() {
        Cart cart = mock(Cart.class);
        CartItem cartItem = new CartItem(new ProductId("1"), new Title("Test"), new Money(1.f), new Quantity(1));
        when(cart.hasItems()).thenReturn(true);
        when(cart.items()).thenReturn(List.of(cartItem));

        placeOrderFromCart.placeOrder(UUID.randomUUID(), cart, null);
    }

    @Test
    void empty_cart_throws_an_exception() {
        assertThrows(PlaceOrderFromCart.NoItemsToOrderException.class,
                     () -> placeOrderFromCart.placeOrder(UUID.randomUUID(), mock(Cart.class), null));
    }

    @Test
    void checkout_uses_catalog_price_not_cart_price() {
        // R-09 safety contract: catalog price for product "1" is 1.00; cart has a manipulated
        // price of 99.99. Order total must be catalogPrice(1.00) * qty(2) = 2.00, not 199.98.
        Cart cart = mock(Cart.class);
        CartItem manipulatedItem = new CartItem(new ProductId("1"), new Title("Test 1"), new Money(99.99f), new Quantity(2));
        when(cart.hasItems()).thenReturn(true);
        when(cart.items()).thenReturn(List.of(manipulatedItem));

        placeOrderFromCart.placeOrder(UUID.randomUUID(), cart, null);

        verify(eventPublisher).raise(argThat(event -> {
            assertThat(event).isInstanceOf(OrderPlaced.class);
            assertThat(((OrderPlaced) event).total).isCloseTo(2.00f, offset(0.01f));
            return true;
        }));
    }

    @Configuration
    @ComponentScan("com.ttulka.ecommerce.sales")
    static class ServicesTestConfig {
        @MockBean
        private PrepareDelivery prepareDelivery;
        @MockBean
        private Warehouse warehouse;
        @MockBean
        private EventPublisher eventPublisher;
    }
}
