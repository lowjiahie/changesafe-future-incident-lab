package com.ttulka.ecommerce.portal.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.ttulka.ecommerce.identity.user.Username;
import com.ttulka.ecommerce.sales.catalog.FindProducts;
import com.ttulka.ecommerce.sales.catalog.product.ProductId;
import com.ttulka.ecommerce.sales.order.Customer;
import com.ttulka.ecommerce.sales.order.FindOrders;
import com.ttulka.ecommerce.sales.order.Order;
import com.ttulka.ecommerce.shipping.delivery.Delivery;
import com.ttulka.ecommerce.shipping.delivery.FindDeliveries;
import com.ttulka.ecommerce.shipping.delivery.OrderId;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Web controller for Order History use-cases: a logged-in user sees their own orders.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
class OrderHistoryController {

    private final @NonNull FindOrders findOrders;
    private final @NonNull FindProducts findProducts;
    private final @NonNull FindDeliveries findDeliveries;

    @GetMapping
    public String index(HttpServletRequest request, Model model) {
        Username username = LoggedInUserFromSession.username(request);
        if (username == null) {
            return "redirect:/login";
        }
        Map<String, String> titles = new HashMap<>();
        model.addAttribute("orders", findOrders.byCustomer(new Customer(username.value())).stream()
                .map(order -> toModel(order, titles))
                .toList());
        return "orders";
    }

    private Map<String, Object> toModel(Order order, Map<String, String> titles) {
        List<Map<String, Object>> items = order.items().stream()
                .map(item -> Map.<String, Object>of(
                        "title", titles.computeIfAbsent(item.productId().value(), this::productTitle),
                        "quantity", item.quantity().value()))
                .toList();
        Map<String, Object> model = new HashMap<>(Map.of(
                "id", order.id().value(),
                "total", order.total().value(),
                "items", items));
        // the delivery is prepared with the order; the shipping domain answers unknown orders with a null object
        Delivery delivery = findDeliveries.byOrder(new OrderId(order.id().value()));
        if (delivery.orderId().value().equals(order.id().value())) {
            model.put("recipient", delivery.address().person().value());
            model.put("deliveryAddress", delivery.address().place().value());
        }
        return model;
    }

    private String productTitle(String productId) {
        var product = findProducts.byId(new ProductId(productId));
        // the catalog answers unknown ids with a null object; show the raw id instead of "unknown product"
        return product.id().value().equals(productId) ? product.title().value() : productId;
    }
}
