package com.ttulka.ecommerce.sales.order;

import java.util.List;

/**
 * Find Orders use-case.
 */
public interface FindOrders {

    /**
     * Finds an order by the order ID.
     *
     * @param id the order ID
     * @return the order
     */
    Order byId(OrderId id);

    /**
     * Finds all orders assigned to a customer, the most recently placed first.
     *
     * @param customer the customer
     * @return the customer's orders, empty when the customer has none
     */
    List<Order> byCustomer(Customer customer);
}
