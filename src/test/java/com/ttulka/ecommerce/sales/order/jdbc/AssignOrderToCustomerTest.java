package com.ttulka.ecommerce.sales.order.jdbc;

import java.util.List;
import java.util.Map;

import com.ttulka.ecommerce.common.events.EventPublisher;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.FindOrders;
import com.ttulka.ecommerce.sales.order.OrderId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@ContextConfiguration(classes = OrderJdbcConfig.class)
@Sql("/test-data-sales-find-orders.sql")
class AssignOrderToCustomerTest {

    @Autowired
    private AssignOrderToCustomer assignOrderToCustomer;
    @Autowired
    private FindOrders findOrders;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EventPublisher eventPublisher;

    @Test
    void order_is_assigned_to_the_customer() {
        assignOrderToCustomer.assign(new OrderId(4), new Customer("ami"));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT username FROM customer_orders WHERE order_id = '4' AND username = 'ami'");
        assertThat(rows).hasSize(1);
        assertThat(findOrders.byCustomer(new Customer("ami")))
                .extracting(o -> o.id()).contains(new OrderId(4));
    }

    @Test
    void an_order_cannot_be_assigned_twice() {
        assertThrows(Exception.class,
                     () -> assignOrderToCustomer.assign(new OrderId(1), new Customer("jason")));
    }
}
