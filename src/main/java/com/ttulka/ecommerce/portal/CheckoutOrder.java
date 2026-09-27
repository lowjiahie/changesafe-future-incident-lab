package com.ttulka.ecommerce.portal;

import java.util.UUID;

import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.warehouse.Amount;
import com.ttulka.ecommerce.warehouse.ProductId;
import com.ttulka.ecommerce.warehouse.Warehouse;

import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the synchronous checkout writes as one unit of work.
 * OrderPlaced listeners run after the transaction commits.
 */
@RequiredArgsConstructor
public class CheckoutOrder {

    private final @NonNull PlaceOrderFromCart placeOrderFromCart;
    private final @NonNull PrepareOrderDelivery prepareOrderDelivery;
    private final @NonNull AssignOrderToCustomer assignOrderToCustomer;
    private final @NonNull Warehouse warehouse;

    /**
     * Checks out a guest order that is not linked to any customer.
     */
    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress, String idempotencyKey) {
        checkStock(cart);
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart, idempotencyKey);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
        cart.empty();
        return orderId;
    }

    /**
     * Checks out an order and links it to the customer so it shows up in their order history.
     */
    @Transactional
    public UUID checkout(@NonNull Cart cart, @NonNull Address deliveryAddress, @NonNull Customer customer, String idempotencyKey) {
        checkStock(cart);
        UUID orderId = UUID.randomUUID();
        placeOrderFromCart.placeOrder(orderId, cart, idempotencyKey);
        assignOrderToCustomer.assign(new OrderId(orderId), customer);
        prepareOrderDelivery.prepareDelivery(orderId, deliveryAddress);
        cart.empty();
        return orderId;
    }

    // R-05: reject checkout early when any item is out of stock (BR-003).
    // BR-013: uses leftInStockForUpdate (SELECT ... FOR UPDATE) so concurrent checkouts block
    // on the lock rather than both reading "in stock" before either has committed.
    private void checkStock(@NonNull Cart cart) {
        for (CartItem item : cart.items()) {
            ProductId productId = new ProductId(item.productId().value());
            if (!warehouse.leftInStockForUpdate(productId).hasEnough(new Amount(item.quantity().value()))) {
                throw new CheckoutOrder.OutOfStockException();
            }
        }
    }

    /**
     * OutOfStockException is thrown when one or more cart items are not available in sufficient
     * quantity to fulfil the order.
     */
    public static class OutOfStockException extends RuntimeException {
    }
}
