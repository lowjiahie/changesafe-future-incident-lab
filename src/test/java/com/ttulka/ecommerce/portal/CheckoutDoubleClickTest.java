package com.ttulka.ecommerce.portal;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.CartId;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.cart.item.ProductId;
import com.ttulka.ecommerce.sales.cart.item.Title;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.FindOrders;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces the checkout double-click bug: two "Place order" submits for the same cart by the same
 * logged-in user are processed concurrently, and each one creates its own order.
 * <p>
 * The barrier makes the race deterministic: both requests are forced to see the cart as non-empty
 * before either of them empties it, which is exactly what happens when a user double-clicks.
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql("/test-data-order-workflow.sql")
class CheckoutDoubleClickTest {

    @Autowired
    private CheckoutOrder checkoutOrder;
    @Autowired
    private RetrieveCart retrieveCart;
    @Autowired
    private FindOrders findOrders;

    @Test
    void double_click_on_place_order_creates_only_one_order_for_the_customer() throws Exception {
        CartId cartId = new CartId(UUID.randomUUID());
        Customer customer = new Customer("dbl-" + UUID.randomUUID().toString().substring(0, 8));
        Address address = new Address(new Person("Test Name"), new Place("Test Address 123"));
        retrieveCart.byId(cartId).add(
                new CartItem(new ProductId("p-1"), new Title("Prod 1"), new Money(1.f), new Quantity(1)));

        CyclicBarrier bothRequestsHaveReadTheCart = new CyclicBarrier(2);
        ExecutorService twoClicks = Executors.newFixedThreadPool(2);
        try {
            List<Future<UUID>> results = List.of(
                    twoClicks.submit(() -> checkoutOrder.checkout(
                            new SynchronizedCart(retrieveCart.byId(cartId), bothRequestsHaveReadTheCart),
                            address, customer)),
                    twoClicks.submit(() -> checkoutOrder.checkout(
                            new SynchronizedCart(retrieveCart.byId(cartId), bothRequestsHaveReadTheCart),
                            address, customer)));
            for (Future<UUID> result : results) {
                try {
                    result.get(10, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // a rejected duplicate is fine, the assertion below checks the outcome
                }
            }
        } finally {
            twoClicks.shutdownNow();
        }

        assertThat(findOrders.byCustomer(customer))
                .as("orders created by a single double-clicked checkout")
                .hasSize(1);
    }

    /**
     * Cart that waits for the other request to read it before letting the checkout continue.
     */
    private record SynchronizedCart(Cart delegate, CyclicBarrier barrier) implements Cart {

        @Override
        public CartId id() {
            return delegate.id();
        }

        @Override
        public List<CartItem> items() {
            return delegate.items();
        }

        @Override
        public boolean hasItems() {
            boolean hasItems = delegate.hasItems();
            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return hasItems;
        }

        @Override
        public void add(CartItem toAdd) {
            delegate.add(toAdd);
        }

        @Override
        public void remove(ProductId productId) {
            delegate.remove(productId);
        }

        @Override
        public void empty() {
            delegate.empty();
        }
    }
}
