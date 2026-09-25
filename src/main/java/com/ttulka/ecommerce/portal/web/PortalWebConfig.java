package com.ttulka.ecommerce.portal.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ttulka.ecommerce.sales.cart.RetrieveCart;
import com.ttulka.ecommerce.sales.catalog.FindCategories;

import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Configuration for Portal Web.
 */
@Configuration
class PortalWebConfig {

    /**
     * Web Layout Advice for Portal.
     * <p>
     * Adds a list of Category items into the layout model.
     */
    @ControllerAdvice(basePackageClasses = PortalWebConfig.class)
    @RequiredArgsConstructor
    class WebLayoutAdvice {

        private final @NonNull FindCategories findCategories;
        private final @NonNull RetrieveCart retrieveCart;

        @ModelAttribute
        public void decorateWithCategories(Model model, HttpServletRequest request, HttpServletResponse response) {
            model.addAttribute("categories", findCategories.all().stream()
                    .map(category -> Map.of(
                            "uri", category.uri().value(),
                            "title", category.title().value()))
                    .toArray());
            var cartId = new CartIdFromCookies(request, response).cartId();
            model.addAttribute("cartCount", retrieveCart.byId(cartId).items().stream()
                    .mapToInt(item -> item.quantity().value()).sum());
        }
    }
}
