package com.ttulka.ecommerce.portal;

import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.order.AssignOrderToCustomer;
import com.ttulka.ecommerce.sales.order.PlaceOrder;
import com.ttulka.ecommerce.shipping.delivery.PrepareDelivery;
import com.ttulka.ecommerce.warehouse.Warehouse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Portal component.
 */
@Configuration
class PortalConfig {

    @Bean
    PlaceOrderFromCart placeOrderFromCart(PlaceOrder placeOrder, FindProducts findProducts) {
        return new PlaceOrderFromCart(placeOrder, findProducts);
    }

    @Bean
    PrepareOrderDelivery prepareOrderDelivery(PrepareDelivery prepareDelivery) {
        return new PrepareOrderDelivery(prepareDelivery);
    }

    @Bean
    CheckoutOrder checkoutOrder(PlaceOrderFromCart placeOrderFromCart,
                                PrepareOrderDelivery prepareOrderDelivery,
                                AssignOrderToCustomer assignOrderToCustomer,
                                Warehouse warehouse) {
        return new CheckoutOrder(placeOrderFromCart, prepareOrderDelivery, assignOrderToCustomer, warehouse);
    }
}
