package com.ttulka.ecommerce.sales.order.jdbc;

import java.sql.Timestamp;
import java.time.Instant;

import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.OrderId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC implementation for Assign Order To Customer use-case.
 */
@RequiredArgsConstructor
@Slf4j
class AssignOrderToCustomerJdbc implements AssignOrderToCustomer {

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public void assign(@NonNull OrderId orderId, @NonNull Customer customer) {
        jdbcTemplate.update("INSERT INTO customer_orders VALUES (?, ?, ?)",
                            orderId.value(), customer.value(), Timestamp.from(Instant.now()));
        log.info("Order {} assigned to customer {}", orderId, customer);
    }
}
