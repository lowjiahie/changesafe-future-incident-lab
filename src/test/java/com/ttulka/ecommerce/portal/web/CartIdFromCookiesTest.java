package com.ttulka.ecommerce.portal.web;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartIdFromCookiesTest {

    @Test
    void cart_cookie_has_http_only_and_same_site_flags() {
        // R-06: CART_ID cookie must carry HttpOnly and SameSite=Strict (BR-005).
        // Secure is tested separately because it depends on request.isSecure().
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getCookies()).thenReturn(null);
        when(request.isSecure()).thenReturn(false);

        new CartIdFromCookies(request, response).cartId();

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeastOnce()).addCookie(captor.capture());

        Cookie cookie = captor.getAllValues().stream()
                .filter(c -> "CART_ID".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CART_ID cookie not found"));

        assertThat(cookie.isHttpOnly()).as("HttpOnly flag must be set").isTrue();
        assertThat(cookie.getAttribute("SameSite")).as("SameSite attribute must be Strict")
                .isEqualTo("Strict");
    }

    @Test
    void cart_cookie_secure_flag_mirrors_request_security() {
        // R-06: Secure flag is on for HTTPS requests and off for HTTP (enables tests and local dev).
        HttpServletRequest httpsRequest = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(httpsRequest.getCookies()).thenReturn(null);
        when(httpsRequest.isSecure()).thenReturn(true);

        new CartIdFromCookies(httpsRequest, response).cartId();

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeastOnce()).addCookie(captor.capture());

        Cookie cookie = captor.getAllValues().stream()
                .filter(c -> "CART_ID".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseThrow();

        assertThat(cookie.getSecure()).as("Secure flag must be set for HTTPS").isTrue();
    }

    @Test
    void existing_cart_id_is_read_from_cookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie existing = new Cookie("CART_ID", "my-cart-123");
        when(request.getCookies()).thenReturn(new Cookie[]{existing});
        when(request.getAttribute(CartIdFromCookies.class.getName() + ".cartId")).thenReturn(null);

        var cartId = new CartIdFromCookies(request, response).cartId();

        assertThat(cartId.value()).isEqualTo("my-cart-123");
    }
}
