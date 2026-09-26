package com.ttulka.ecommerce.sales.order;

/**
 * Assign Order To Customer use-case.
 */
public interface AssignOrderToCustomer {

    /**
     * Assigns an order to a customer so the customer can find it later.
     *
     * @param orderId  the order ID
     * @param customer the customer the order belongs to
     */
    void assign(OrderId orderId, Customer customer);
}
