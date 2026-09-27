package com.ttulka.ecommerce.portal.web;

import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttulka.ecommerce.identity.user.Username;
import com.ttulka.ecommerce.portal.CheckoutOrder;
import com.ttulka.ecommerce.portal.PlaceOrderFromCart;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.PlaceOrder;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Web controller for Order use-cases.
 */
@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
class OrderController {

    /**
     * Session attribute name for the checkout token.
     *
     * <p>This token is <strong>server-issued and session-bound</strong>. It serves two purposes:
     * <ol>
     *   <li><b>Idempotency key</b> — passed to {@link com.ttulka.ecommerce.sales.order.PlaceOrder}
     *       so that a retry of the same checkout attempt cannot create a second order row.</li>
     *   <li><b>Implicit CSRF mitigation</b> — because the token is generated server-side on
     *       {@code GET /order} and tied to the session, a cross-site request forged without a
     *       valid session cannot supply a matching token.</li>
     * </ol>
     * Do not remove or replace this token without preserving both properties (BR-007).
     * If Spring Security CSRF protection is added later, verify that this mechanism is either
     * retained or superseded before disabling it.
     */
    private static final String SESSION_CHECKOUT_TOKEN = "checkoutToken";

    // R-10: only codes that map to real i18n keys are accepted; anything else falls back to default.
    private static final Set<String> KNOWN_ERROR_CODES =
            Set.of("duplicate", "noitems", "requires", "outofstock", "productnotfound", "default");

    private final @NonNull RetrieveCart retrieveCart;
    private final @NonNull CheckoutOrder checkoutOrder;

    @GetMapping
    public String index(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(SESSION_CHECKOUT_TOKEN);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(SESSION_CHECKOUT_TOKEN, token);
        }
        model.addAttribute("checkoutToken", token);
        return "order";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String place(@RequestParam(required = false) String name,
                        @RequestParam(required = false) String address,
                        HttpServletRequest request, HttpServletResponse response, Model model,
                        RedirectAttributes redirectAttributes) {
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
        log.info("Checkout attempt by {} from {} cart={}",
                username != null ? username.value() : "guest",
                request.getRemoteAddr(),
                cart.id() != null ? cart.id().value() : "unknown");

        // R-02: use the server-issued session token as the idempotency key — the client-supplied
        // value is ignored so it cannot be tampered with or omitted to bypass duplicate protection.
        HttpSession session = request.getSession(false);
        String sessionToken = session != null ? (String) session.getAttribute(SESSION_CHECKOUT_TOKEN) : null;

        try {
            UUID orderId;
            if (username == null) {
                orderId = checkoutOrder.checkout(cart, deliveryAddress, sessionToken);
            } else {
                orderId = checkoutOrder.checkout(cart, deliveryAddress, new Customer(username.value()), sessionToken);
            }
            // BR-010: pass the order reference to the success page so guests can note it down.
            if (orderId != null) {
                redirectAttributes.addFlashAttribute("orderId", orderId.toString());
            }
        } catch (PlaceOrder.DuplicateOrderException e) {
            // R-03: first order succeeded — clear the cart so it reflects reality.
            cart.empty();
            // R-01: regenerate the session token so the user can navigate back and try again.
            String newToken = UUID.randomUUID().toString();
            request.getSession(true).setAttribute(SESSION_CHECKOUT_TOKEN, newToken);
            return "redirect:/order/error?message=duplicate";
        }
        request.getSession().removeAttribute(SESSION_CHECKOUT_TOKEN);
        return "redirect:/order/success";
    }

    @GetMapping("/success")
    public String success(Model model) {
        // orderId flash attribute is populated by place() for BR-010 (guest order reference).
        // If the page is reloaded directly, orderId will be absent — the template handles that gracefully.
        return "order-success";
    }

    // R-10: sanitise the message code before passing it to the template to prevent i18n key injection.
    @GetMapping("/error")
    public String error(@RequestParam(required = false) String message, Model model) {
        String safe = (message != null && KNOWN_ERROR_CODES.contains(message)) ? message : "default";
        model.addAttribute("messageCode", safe);
        return "order-error";
    }

    @ExceptionHandler({PlaceOrderFromCart.NoItemsToOrderException.class,
                        PlaceOrderFromCart.ProductNotFoundException.class,
                        CheckoutOrder.OutOfStockException.class,
                        IllegalArgumentException.class})
    String exception(Exception ex) {
        return "redirect:/order/error?message=" + errorCode(ex);
    }

    private String errorCode(Exception e) {
        if (e instanceof PlaceOrderFromCart.NoItemsToOrderException) {
            return "noitems";
        }
        if (e instanceof PlaceOrderFromCart.ProductNotFoundException) {
            return "productnotfound";
        }
        if (e instanceof CheckoutOrder.OutOfStockException) {
            return "outofstock";
        }
        if (e instanceof IllegalArgumentException) {
            return "requires";
        }
        return "default";
    }
}
