package com.ttulka.ecommerce.sales.order.jdbc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ttulka.ecommerce.common.events.EventPublisher;
import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.FindOrders;
import com.ttulka.ecommerce.sales.order.Order;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.sales.order.item.OrderItem;
import com.ttulka.ecommerce.sales.order.item.ProductId;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC implementation for Find Orders use-cases.
 */
@RequiredArgsConstructor
@Slf4j
final class FindOrdersJdbc implements FindOrders {

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final @NonNull EventPublisher eventPublisher;

    @Override
    public Order byId(OrderId id) {
        var items = jdbcTemplate.queryForList(
                "SELECT product_id, quantity FROM order_items WHERE order_id = ?",
                id.value());

        var order = jdbcTemplate.queryForList(
                "SELECT id, total FROM orders WHERE id = ?",
                id.value())
                .stream().findAny();

        return order
                .map(o -> toOrder(o, items.stream()
                        .map(this::toOrderItem)
                        .collect(Collectors.toList())))
                .orElseGet(UnknownOrder::new);
    }

    @Override
    public List<Order> byCustomer(Customer customer) {
        var orders = jdbcTemplate.queryForList(
                "SELECT o.id, o.total FROM orders o " +
                "JOIN customer_orders c ON c.order_id = o.id " +
                "WHERE c.username = ? ORDER BY c.placed_at DESC, o.id",
                customer.value());

        var itemsByOrderId = jdbcTemplate.queryForList(
                "SELECT i.order_id, i.product_id, i.quantity FROM order_items i " +
                "JOIN customer_orders c ON c.order_id = i.order_id " +
                "WHERE c.username = ?",
                customer.value())
                .stream().collect(Collectors.groupingBy(
                        item -> (String) item.get("order_id"),
                        Collectors.mapping(this::toOrderItem, Collectors.toList())));

        var result = new ArrayList<Order>();
        for (var order : orders) {
            var items = itemsByOrderId.get((String) order.get("id"));
            if (items != null) {
                result.add(toOrder(order, items));
            }
        }
        return result;
    }

    private Order toOrder(Map<String, Object> order, List<OrderItem> items) {
        return new OrderJdbc(
                new OrderId(order.get("id")),
                new Money(((BigDecimal) order.get("total")).floatValue()),
                items,
                jdbcTemplate,
                eventPublisher);
    }

    private OrderItem toOrderItem(Map<String, Object> item) {
        return new OrderItem(
                new ProductId(item.get("product_id")),
                new Quantity((Integer) item.get("quantity")));
    }
}
