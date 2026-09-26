package com.ttulka.ecommerce.portal;

import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.CartId;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KNOWN-BUG REPRODUCTION: double-clicking "Place order" creates more than one order from a single cart.
 * <p>
 * This test FAILS on purpose while the bug exists. It is skipped in the normal build; run it with:
 * <pre>
 *   ./mvnw test -Drepro=true -Dtest=DoubleCheckoutBugReproTest
 * </pre>
 * Root cause: the order form has no submit guard, each request gets a fresh random order ID (no idempotency key),
 * and {@code CheckoutOrder} checks {@code cart.hasItems()} with a plain SELECT and only empties the cart at the
 * very end of its transaction, so two overlapping requests for the same cart both see the items.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "repro", matches = "true")
class DoubleCheckoutBugReproTest {

    private static final int ROUNDS = 100;

    @Autowired
    private CheckoutOrder checkoutOrder;
    @Autowired
    private RetrieveCart retrieveCart;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Address address = new Address(new Person("Alex Chen"), new Place("Demo Street 1"));

    @Test
    void double_submit_of_the_same_cart_must_create_exactly_one_order() throws Exception {
        int duplicatedRounds = 0;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                String cartId = "double-click-" + UUID.randomUUID();
                jdbcTemplate.update("INSERT INTO cart_items VALUES (?, ?, ?, ?, ?)",
                                    "p-dbl-" + round, "Demo item", 9.99, 1, cartId);
                int ordersBefore = countOrders();

                CyclicBarrier startTogether = new CyclicBarrier(2);
                Future<Boolean> click1 = pool.submit(() -> clickPlaceOrder(startTogether, cartId));
                Future<Boolean> click2 = pool.submit(() -> clickPlaceOrder(startTogether, cartId));
                click1.get();
                click2.get();

                if (countOrders() - ordersBefore > 1) {
                    duplicatedRounds++;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(duplicatedRounds)
                .as("rounds (out of %d) where a double click on 'Place order' produced more than one order", ROUNDS)
                .isZero();
    }

    /** Returns true when the checkout went through, false when the cart was already empty (the safe outcome). */
    private boolean clickPlaceOrder(CyclicBarrier startTogether, String cartId) throws Exception {
        startTogether.await();
        Cart cart = retrieveCart.byId(new CartId(cartId));
        try {
            checkoutOrder.checkout(cart, address);
            return true;
        } catch (PlaceOrderFromCart.NoItemsToOrderException e) {
            return false;
        }
    }

    private int countOrders() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
    }
}
