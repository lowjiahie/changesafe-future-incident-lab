package com.ttulka.ecommerce.portal.web;

import java.util.List;

import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.catalog.product.Product;
import com.ttulka.ecommerce.sales.catalog.product.ProductId;
import com.ttulka.ecommerce.sales.catalog.product.Title;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.FindOrders;
import com.ttulka.ecommerce.sales.order.Order;
import com.ttulka.ecommerce.sales.order.OrderId;
import com.ttulka.ecommerce.sales.order.item.OrderItem;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Delivery;
import com.ttulka.ecommerce.shipping.delivery.FindDeliveries;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderHistoryController.class)
class OrderHistoryControllerTest {

    private static final String SESSION_ATTRIBUTE = "LOGGED_IN_USERNAME";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindOrders findOrders;
    @MockBean
    private FindProducts findProducts;
    @MockBean
    private FindDeliveries findDeliveries;

    @BeforeEach
    void unknown_product_and_delivery_by_default() {
        // the real services answer unknown ids with null objects, never with null
        Product unknownProduct = mock(Product.class);
        when(unknownProduct.id()).thenReturn(new ProductId("0"));
        when(unknownProduct.title()).thenReturn(new Title("unknown product"));
        when(findProducts.byId(any())).thenReturn(unknownProduct);

        Delivery unknownDelivery = mock(Delivery.class);
        when(unknownDelivery.orderId()).thenReturn(new com.ttulka.ecommerce.shipping.delivery.OrderId(0));
        when(findDeliveries.byOrder(any())).thenReturn(unknownDelivery);
    }

    @Test
    void anonymous_visitor_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(findOrders);
    }

    @Test
    void logged_in_user_sees_only_their_own_orders() throws Exception {
        Order order = order("order-abc", 45.0f, new OrderItem(orderProductId("1"), new Quantity(2)));
        when(findOrders.byCustomer(any())).thenReturn(List.of(order));
        Product product = mock(Product.class);
        when(product.id()).thenReturn(new ProductId("1"));
        when(product.title()).thenReturn(new Title("Domain-Driven Design"));
        when(findProducts.byId(new ProductId("1"))).thenReturn(product);

        mockMvc.perform(get("/orders").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("order-abc")))
                .andExpect(content().string(containsString("Domain-Driven Design")))
                .andExpect(content().string(containsString("Qty 2")))
                .andExpect(content().string(containsString("$45.00")));

        verify(findOrders).byCustomer(new Customer("ami"));
    }

    @Test
    void user_without_orders_sees_an_empty_state() throws Exception {
        when(findOrders.byCustomer(any())).thenReturn(List.of());

        mockMvc.perform(get("/orders").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No orders yet")))
                .andExpect(content().string(not(containsString("order-list"))));
    }

    @Test
    void product_no_longer_in_the_catalog_is_shown_by_its_id() throws Exception {
        Order order = order("order-xyz", 5.0f, new OrderItem(orderProductId("999"), new Quantity(1)));
        when(findOrders.byCustomer(any())).thenReturn(List.of(order));
        Product unknown = mock(Product.class);
        when(unknown.id()).thenReturn(new ProductId("0"));
        when(unknown.title()).thenReturn(new Title("unknown product"));
        when(findProducts.byId(new ProductId("999"))).thenReturn(unknown);

        mockMvc.perform(get("/orders").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("999")))
                .andExpect(content().string(not(containsString("unknown product"))));
    }

    @Test
    void delivery_recipient_and_address_of_an_order_are_shown() throws Exception {
        Order order = order("order-abc", 5.0f, new OrderItem(orderProductId("1"), new Quantity(1)));
        when(findOrders.byCustomer(any())).thenReturn(List.of(order));
        stubDelivery("order-abc", "Alex Chen", "Demo Street 1");

        mockMvc.perform(get("/orders").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alex Chen")))
                .andExpect(content().string(containsString("Demo Street 1")));
    }

    @Test
    void delivery_is_not_shown_when_the_order_has_no_delivery() throws Exception {
        Order order = order("order-abc", 5.0f, new OrderItem(orderProductId("1"), new Quantity(1)));
        when(findOrders.byCustomer(any())).thenReturn(List.of(order));
        Delivery unknown = mock(Delivery.class);
        when(unknown.orderId()).thenReturn(new com.ttulka.ecommerce.shipping.delivery.OrderId(0));
        when(findDeliveries.byOrder(any())).thenReturn(unknown);

        mockMvc.perform(get("/orders").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("order-delivery"))));
    }

    private void stubDelivery(String orderId, String person, String place) {
        Delivery delivery = mock(Delivery.class);
        when(delivery.orderId()).thenReturn(new com.ttulka.ecommerce.shipping.delivery.OrderId(orderId));
        when(delivery.address()).thenReturn(new Address(new Person(person), new Place(place)));
        when(findDeliveries.byOrder(any())).thenReturn(delivery);
    }

    private static com.ttulka.ecommerce.sales.order.item.ProductId orderProductId(String id) {
        return new com.ttulka.ecommerce.sales.order.item.ProductId(id);
    }

    private static Order order(String id, float total, OrderItem... items) {
        Order order = mock(Order.class);
        when(order.id()).thenReturn(new OrderId(id));
        when(order.total()).thenReturn(new Money(total));
        when(order.items()).thenReturn(List.of(items));
        return order;
    }
}
