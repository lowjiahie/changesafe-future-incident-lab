package com.ttulka.ecommerce.portal.web;

import java.util.Arrays;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ttulka.ecommerce.sales.cart.CartId;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Retrieve and save Cart ID from/to HTTP cookies.
 */
@RequiredArgsConstructor
final class CartIdFromCookies {

    private final static String COOKIE_NAME = "CART_ID";
    private final static String REQUEST_CART_ID = CartIdFromCookies.class.getName() + ".cartId";

    private final @NonNull HttpServletRequest request;
    private final @NonNull HttpServletResponse response;

    private CartId cartId;

    public CartId cartId() {
        if (cartId == null) {
            Object existing = request.getAttribute(REQUEST_CART_ID);
            if (existing instanceof CartId) {
                cartId = (CartId) existing;
                return cartId;
            }
            cartId = new CartId(
                    request.getCookies() != null ?
                    Arrays.stream(request.getCookies())
                            .filter(cookie -> COOKIE_NAME.equalsIgnoreCase(cookie.getName()))
                            .map(Cookie::getValue)
                            .findAny()
                            .orElseGet(() -> UUID.randomUUID().toString())
                    : UUID.randomUUID().toString());

            request.setAttribute(REQUEST_CART_ID, cartId);
            saveCookie(cartId.value());
        }
        return cartId;
    }

    private void saveCookie(String value) {
        response.addCookie(asCookie(COOKIE_NAME, value));
    }

    private Cookie asCookie(String name, String value) {
        var cookie = new Cookie(name, value);
        cookie.setPath("/");
        return cookie;
    }
}
