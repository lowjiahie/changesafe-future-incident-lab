package com.ttulka.ecommerce.portal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ttulka.ecommerce.portal.CheckoutOrder;
import com.ttulka.ecommerce.portal.PlaceOrderFromCart;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.identity.user.Username;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.shipping.delivery.Address;
import com.ttulka.ecommerce.shipping.delivery.Person;
import com.ttulka.ecommerce.shipping.delivery.Place;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Web controller for Order use-cases.
 */
@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
class OrderController {

    private final @NonNull RetrieveCart retrieveCart;
    private final @NonNull CheckoutOrder checkoutOrder;

    @GetMapping
    public String index() {
        return "order";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String place(@RequestParam(required = false) String name,
                        @RequestParam(required = false) String address,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute("name", name == null ? "" : name);
        model.addAttribute("address", address == null ? "" : address);

        // Validate all user input before any order/payment event can be raised.
        boolean invalid = false;
        Person person = null;
        try {
            person = new Person(name);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("nameError", true);
            invalid = true;
        }
        Place place = null;
        try {
            place = new Place(address);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("addressError", true);
            invalid = true;
        }
        if (invalid) {
            return "order";
        }
        Cart cart = retrieveCart.byId(new CartIdFromCookies(request, response).cartId());
        Address deliveryAddress = new Address(person, place);
        Username username = LoggedInUserFromSession.username(request);
        if (username == null) {
            checkoutOrder.checkout(cart, deliveryAddress);
        } else {
            checkoutOrder.checkout(cart, deliveryAddress, new Customer(username.value()));
        }

        return "redirect:/order/success";
    }

    @GetMapping("/success")
    public String success(HttpServletRequest request, HttpServletResponse response) {
        return "order-success";
    }

    @GetMapping("/error")
    public String error(String message, Model model) {
        model.addAttribute("messageCode", message);
        return "order-error";
    }

    @ExceptionHandler({PlaceOrderFromCart.NoItemsToOrderException.class, IllegalArgumentException.class})
    String exception(Exception ex) {
        return "redirect:/order/error?message=" + errorCode(ex);
    }

    private String errorCode(Exception e) {
        if (e instanceof PlaceOrderFromCart.NoItemsToOrderException) {
            return "noitems";
        }
        if (e instanceof IllegalArgumentException) {
            return "requires";
        }
        return "default";
    }
}
