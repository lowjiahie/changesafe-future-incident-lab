package com.ttulka.ecommerce.portal.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ttulka.ecommerce.common.primitives.Quantity;
import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.cart.CartId;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.cart.item.CartItem;
import com.ttulka.ecommerce.sales.cart.item.ProductId;
import com.ttulka.ecommerce.sales.cart.item.Title;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Web controller for Cart use-cases.
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
class CartController {

    private final @NonNull RetrieveCart retrieveCart;
    private final @NonNull FindProducts findProducts;

    @GetMapping
    public String index(Model model, HttpServletRequest request, HttpServletResponse response) {
        CartId cartId = new CartIdFromCookies(request, response).cartId();
        var cartItems = retrieveCart.byId(cartId).items();
        model.addAttribute(
                "items",
                cartItems.stream()
                        .map(item -> Map.of("id", item.productId().value(),
                                            "title", item.title().value(),
                                            "price", item.total().value(),
                                            "quantity", item.quantity().value()))
                        .toArray());
        model.addAttribute("total", cartItems.stream()
                .mapToDouble(item -> item.total().value()).sum());
        return "cart";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String add(@NonNull String productId, @NonNull Integer quantity,
                      HttpServletRequest request, HttpServletResponse response) {
        addItem(productId, quantity, request, response);
        return "redirect:/cart";
    }

    @ResponseBody
    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Integer> addWithoutRedirect(@NonNull String productId, @NonNull Integer quantity,
                                                    HttpServletRequest request, HttpServletResponse response) {
        Cart cart = addItem(productId, quantity, request, response);
        return Map.of("count", cart.items().stream()
                .mapToInt(item -> item.quantity().value()).sum());
    }

    private Cart addItem(String productId, Integer quantity,
                         HttpServletRequest request, HttpServletResponse response) {
        if (quantity < 1 || quantity > 1000) {
            throw new IllegalArgumentException("Quantity must be between 1 and 1000!");
        }
        var product = findProducts.byId(new com.ttulka.ecommerce.sales.catalog.product.ProductId(productId));
        if (!product.id().value().equals(productId)) {
            throw new IllegalArgumentException("Product does not exist!");
        }
        CartId cartId = new CartIdFromCookies(request, response).cartId();
        Cart cart = retrieveCart.byId(cartId);
        cart.add(new CartItem(
                new ProductId(productId),
                new Title(product.title().value()),
                product.price(),
                new Quantity(quantity)));

        return cart;
    }

    @GetMapping("/remove")
    public String remove(@NonNull String productId,
                         HttpServletRequest request, HttpServletResponse response) {
        CartId cartId = new CartIdFromCookies(request, response).cartId();
        retrieveCart.byId(cartId).remove(new ProductId(productId));

        return "redirect:/cart";
    }

}
