package com.ttulka.ecommerce.portal.web;

import com.ttulka.ecommerce.portal.CheckoutOrder;
import com.ttulka.ecommerce.portal.PlaceOrderFromCart;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.PlaceOrder;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckoutOrder checkoutOrder;
    @MockBean
    private RetrieveCart retrieveCart;

    @Test
    void index_shows_the_order_form_with_name_and_address_input_fields() throws Exception {
        mockMvc.perform(
                get("/order"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<form")))
                .andExpect(content().string(containsString("action=\"/order\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"address\"")));
    }

    @Test
    void order_is_placed() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Test Name")
                        .param("address", "Test Address 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/success"));

        verify(checkoutOrder).checkout(eq(cart),
                eq(new Address(new Person("Test Name"), new Place("Test Address 123"))),
                (String) eq(null));
    }

    @Test
    void order_of_a_logged_in_user_is_linked_to_them() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                post("/order")
                        .sessionAttr("LOGGED_IN_USERNAME", "ami")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Test Name")
                        .param("address", "Test Address 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/success"));

        verify(checkoutOrder).checkout(eq(cart),
                eq(new Address(new Person("Test Name"), new Place("Test Address 123"))),
                eq(new Customer("ami")),
                (String) eq(null));
    }

    @Test
    void international_names_are_accepted() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "张三")
                        .param("address", "Test Address 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/success"));

        verify(checkoutOrder).checkout(eq(cart),
                eq(new Address(new Person("张三"), new Place("Test Address 123"))),
                (String) eq(null));
    }

    @Test
    void order_form_is_not_filled() throws Exception {
        mockMvc.perform(
                post("/order").contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isOk())
                .andExpect(model().attribute("nameError", true))
                .andExpect(model().attribute("addressError", true));

        verifyNoInteractions(checkoutOrder, retrieveCart);
    }

    @Test
    void invalid_delivery_details_do_not_create_an_order() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Alex123")
                        .param("address", "Demo Street 1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("nameError", true))
                .andExpect(model().attribute("name", "Alex123"));

        verifyNoInteractions(checkoutOrder, retrieveCart);
    }

    @Test
    void success_is_shown() throws Exception {
        mockMvc.perform(get("/order/success"))
                .andExpect(status().isOk());
    }

    @Test
    void error_message_is_shown() throws Exception {
        mockMvc.perform(get("/order/error")
                                .param("message", "testmessage"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("messageCode", "testmessage"));
    }

    @Test
    void error_is_shown_for_no_items() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        doThrow(mock(PlaceOrderFromCart.NoItemsToOrderException.class))
                .when(checkoutOrder).checkout(any(Cart.class), any(Address.class), any());

        mockMvc.perform(
                post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Test Name")
                        .param("address", "Test Address 123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/error?message=noitems"));
    }

    @Test
    void duplicate_order_redirects_to_error_page_with_duplicate_message() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        doThrow(new PlaceOrder.DuplicateOrderException())
                .when(checkoutOrder).checkout(any(Cart.class), any(Address.class), any());

        mockMvc.perform(
                post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Test Name")
                        .param("address", "Test Address 123")
                        .param("idempotencyKey", "test-key-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/error?message=duplicate"));
    }

    @Test
    void idempotency_key_is_passed_to_checkout() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                post("/order")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("name", "Test Name")
                        .param("address", "Test Address 123")
                        .param("idempotencyKey", "my-key-abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/success"));

        verify(checkoutOrder).checkout(eq(cart),
                eq(new Address(new Person("Test Name"), new Place("Test Address 123"))),
                eq("my-key-abc"));
    }
}
