package com.ttulka.ecommerce.portal.web;

import java.util.List;
import java.util.Map;

import com.ttulka.ecommerce.common.primitives.Money;
import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.catalog.product.Product;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.cart.item.ProductId;
import com.ttulka.ecommerce.sales.cart.item.Title;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RetrieveCart retrieveCart;
    @MockBean
    private FindProducts findProducts;

    @Test
    void index_shows_the_cart_items() throws Exception {
        Cart cart = mock(Cart.class);
        when(cart.items()).thenReturn(List.of(
                new CartItem(new ProductId("test-1"), new Title("Test"), new Money(1.f), new Quantity(123))));
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                get("/cart"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", new Object[]{
                        Map.of("id", "test-1",
                               "title", "Test",
                               "price", 1.f * 123,
                               "quantity", 123)}));
    }

    @Test
    void item_is_added_into_the_cart() throws Exception {
        Cart cart = mock(Cart.class);
        Product product = mock(Product.class);
        when(retrieveCart.byId(any())).thenReturn(cart);
        when(findProducts.byId(any())).thenReturn(product);
        when(product.id()).thenReturn(new com.ttulka.ecommerce.sales.catalog.product.ProductId("test-1"));
        when(product.title()).thenReturn(new com.ttulka.ecommerce.sales.catalog.product.Title("Test"));
        when(product.price()).thenReturn(new Money(10.5f));

        mockMvc.perform(
                post("/cart")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("productId", "test-1")
                        .param("title", "Forged title")
                        .param("price", "0.01")
                        .param("quantity", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        ArgumentCaptor<CartItem> added = ArgumentCaptor.forClass(CartItem.class);
        verify(cart).add(added.capture());
        assertThat(added.getValue().title()).isEqualTo(new Title("Test"));
        assertThat(added.getValue().unitPrice()).isEqualTo(new Money(10.5f));
        assertThat(added.getValue().quantity()).isEqualTo(new Quantity(123));
    }

    @Test
    void ajax_add_returns_cart_quantity_without_redirect() throws Exception {
        Cart cart = mock(Cart.class);
        Product product = mock(Product.class);
        when(retrieveCart.byId(any())).thenReturn(cart);
        when(findProducts.byId(any())).thenReturn(product);
        when(product.id()).thenReturn(new com.ttulka.ecommerce.sales.catalog.product.ProductId("test-1"));
        when(product.title()).thenReturn(new com.ttulka.ecommerce.sales.catalog.product.Title("Test"));
        when(product.price()).thenReturn(new Money(10.5f));
        when(cart.items()).thenReturn(List.of(new CartItem(new ProductId("test-1"),
                new Title("Test"), new Money(10.5f), new Quantity(4))));

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("productId", "test-1")
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void item_is_removed_from_the_cart() throws Exception {
        Cart cart = mock(Cart.class);
        when(retrieveCart.byId(any())).thenReturn(cart);

        mockMvc.perform(
                get("/cart/remove")
                        .param("productId", "test-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }
}
