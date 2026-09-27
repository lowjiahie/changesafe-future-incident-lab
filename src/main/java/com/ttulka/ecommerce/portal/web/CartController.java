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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        // BR-014: cap accumulated quantity per item at 1000
        int existingQty = cart.items().stream()
                .filter(i -> i.productId().value().equals(productId))
                .mapToInt(i -> i.quantity().value())
                .findFirst().orElse(0);
        if (existingQty + quantity > 1000) {
            throw new IllegalArgumentException("Cart quantity limit reached!");
        }
        cart.add(new CartItem(
                new ProductId(productId),
                new Title(product.title().value()),
                product.price(),
                new Quantity(quantity)));

        return cart;
    }

    // BR-015: remove uses POST to prevent CSRF / browser-prefetch mutations
    @PostMapping("/remove")
    public String remove(@NonNull String productId,
                         HttpServletRequest request, HttpServletResponse response) {
        CartId cartId = new CartIdFromCookies(request, response).cartId();
        retrieveCart.byId(cartId).remove(new ProductId(productId));

        return "redirect:/cart";
    }

    // BR-014, BR-015: update absolute quantities per item; remove-then-add strategy (OQ-3)
    @PostMapping("/update")
    public String update(@RequestParam Map<String, String> params,
                         HttpServletRequest request, HttpServletResponse response) {
        CartId cartId = new CartIdFromCookies(request, response).cartId();
        Cart cart = retrieveCart.byId(cartId);

        params.forEach((key, value) -> {
            if (!key.startsWith("quantity_")) return;
            String productId = key.substring("quantity_".length());
            int quantity;
            try {
                quantity = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity for product " + productId);
            }
            if (quantity < 0 || quantity > 1000) {
                throw new IllegalArgumentException("Quantity must be between 0 and 1000!");
            }
            ProductId pid = new ProductId(productId);
            cart.remove(pid);
            if (quantity > 0) {
                // Re-fetch product to get current price (consistent with BR-006 spirit)
                var product = findProducts.byId(new com.ttulka.ecommerce.sales.catalog.product.ProductId(productId));
                cart.add(new CartItem(pid, new Title(product.title().value()),
                        product.price(), new Quantity(quantity)));
            }
        });

        return "redirect:/cart";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

}
