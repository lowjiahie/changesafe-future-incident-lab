package com.ttulka.ecommerce.portal;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.catalog.product.ProductId;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.sales.order.PlaceOrder;
import com.ttulka.ecommerce.sales.order.item.OrderItem;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Place Order From Cart use-case.
 */
@RequiredArgsConstructor
public class PlaceOrderFromCart {

    private final @NonNull PlaceOrder placeOrder;
    private final @NonNull FindProducts findProducts;

    /**
     * Places a new order created from the cart.
     *
     * @param orderId the order ID value
     * @param cart    the cart
     */
    public void placeOrder(@NonNull UUID orderId, @NonNull Cart cart, String idempotencyKey) {
        if (!cart.hasItems()) {
            throw new PlaceOrderFromCart.NoItemsToOrderException();
        }
        List<CartItem> items = cart.items();
        // R-09: total is re-priced from the catalog — the client-supplied cart price is never
        // used for billing (safety contract: total = sum(catalogPrice × quantity)).
        Money total = items.stream()
                .map(item -> findProducts
                        .byId(new ProductId(item.productId().value()))
                        .price()
                        .multi(item.quantity().value()))
                .reduce(Money::add)
                .orElse(Money.ZERO);
        // here a command message PlaceOrder could be sent for lower coupling
        placeOrder.place(new OrderId(orderId),
                         items.stream()
                                 .map(this::toOrderItem)
                                 .collect(Collectors.toList()),
                         total,
                         idempotencyKey);
    }

    private OrderItem toOrderItem(CartItem cartItem) {
        return new OrderItem(
                new com.ttulka.ecommerce.sales.order.item.ProductId(cartItem.productId().value()),
                cartItem.quantity());
    }

    /**
     * NoItemsToOrderException is thrown when there are no items in the cart to be ordered.
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class NoItemsToOrderException extends RuntimeException {
    }
}
