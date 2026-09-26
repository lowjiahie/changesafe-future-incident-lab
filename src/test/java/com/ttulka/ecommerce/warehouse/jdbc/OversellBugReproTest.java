package com.ttulka.ecommerce.warehouse.jdbc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ttulka.ecommerce.common.events.EventPublisher;
import com.ttulka.ecommerce.warehouse.Amount;
import com.ttulka.ecommerce.warehouse.FetchGoods;
import com.ttulka.ecommerce.warehouse.GoodsFetched;
import com.ttulka.ecommerce.warehouse.GoodsMissed;
import com.ttulka.ecommerce.warehouse.OrderId;
import com.ttulka.ecommerce.warehouse.ProductId;
import com.ttulka.ecommerce.warehouse.ToFetch;
import com.ttulka.ecommerce.warehouse.Warehouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * KNOWN-BUG REPRODUCTION: overselling when two customers order the same product concurrently.
 * <p>
 * These tests FAIL on purpose while the bug exists. They are skipped in the normal build; run them with:
 * <pre>
 *   ./mvnw test -Drepro=true -Dtest=OversellBugReproTest
 * </pre>
 * Root cause: {@code GoodsFetchingJdbc.fetch()} reads the stock with a plain SELECT, decides in Java how much can
 * be fetched, then runs an unguarded {@code UPDATE ... SET amount = amount - ?}. Nothing makes that
 * read-then-update atomic, and nothing checks stock when the order is placed.
 */
@JdbcTest
@ContextConfiguration(classes = WarehouseJdbcConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfSystemProperty(named = "repro", matches = "true")
class OversellBugReproTest {

    private static final int ROUNDS = 200;

    @Autowired
    private FetchGoods fetchGoods;
    @Autowired
    private Warehouse warehouse;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    void two_customers_ordering_the_last_unit_must_not_both_get_it() throws Exception {
        int oversoldRounds = 0;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                String productCode = UUID.randomUUID().toString();
                warehouse.putIntoStock(new ProductId(productCode), new Amount(1)); // only ONE unit left

                CyclicBarrier startTogether = new CyclicBarrier(2);
                Future<?> userA = pool.submit(() -> order(startTogether, "order-A-" + productCode, productCode));
                Future<?> userB = pool.submit(() -> order(startTogether, "order-B-" + productCode, productCode));
                userA.get();
                userB.get();

                Integer fetched = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(amount), 0) FROM fetched_products WHERE product_id = ?",
                        Integer.class, productCode);
                if (fetched > 1) {
                    oversoldRounds++;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(oversoldRounds)
                .as("rounds (out of %d) where 1 unit in stock was handed out to more than one order", ROUNDS)
                .isZero();
    }

    @Test
    void stock_must_never_go_negative_under_concurrent_orders() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                String productCode = UUID.randomUUID().toString();
                warehouse.putIntoStock(new ProductId(productCode), new Amount(5));

                CyclicBarrier startTogether = new CyclicBarrier(2);
                // two orders of 3 each against 5 in stock: at most one of them can be satisfied in full
                Future<?> userA = pool.submit(() -> order(startTogether, "order-A-" + productCode, productCode, 3));
                Future<?> userB = pool.submit(() -> order(startTogether, "order-B-" + productCode, productCode, 3));
                userA.get();
                userB.get();

                Integer fetched = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(amount), 0) FROM fetched_products WHERE product_id = ?",
                        Integer.class, productCode);
                assertThat(fetched)
                        .as("units handed out for product %s (round %d) must not exceed the 5 in stock", productCode, round)
                        .isLessThanOrEqualTo(5);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Not a race: even in a single thread, an order that cannot be fulfilled at all still raises
     * {@code GoodsFetched}, so the order carries on to dispatch. Nothing listens to {@code GoodsMissed}.
     */
    @Test
    void order_for_sold_out_product_must_not_be_reported_as_goods_fetched() {
        String productCode = UUID.randomUUID().toString();
        warehouse.putIntoStock(new ProductId(productCode), new Amount(0)); // sold out

        fetchGoods.fetchFromOrder(new OrderId("sold-out-order"), List.of(
                new ToFetch(new ProductId(productCode), new Amount(1))));

        verify(eventPublisher, atLeastOnce()).raise(isA(GoodsMissed.class)); // the shortage IS detected...
        verify(eventPublisher, org.mockito.Mockito.never()).raise(isA(GoodsFetched.class)); // ...but GoodsFetched still fires
    }

    private void order(CyclicBarrier startTogether, String orderId, String productCode) {
        order(startTogether, orderId, productCode, 1);
    }

    private void order(CyclicBarrier startTogether, String orderId, String productCode, int amount) {
        try {
            startTogether.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        fetchGoods.fetchFromOrder(new OrderId(orderId), List.of(
                new ToFetch(new ProductId(productCode), new Amount(amount))));
    }
}
