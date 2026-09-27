package com.ttulka.ecommerce.portal.web;

import com.ttulka.ecommerce.portal.CheckoutOrder;
import com.ttulka.ecommerce.sales.cart.Cart;
import com.ttulka.ecommerce.sales.cart.RetrieveCart;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Safety contract: a valid customer name does not make a missing delivery address valid.
 * Direct controller unit test: no Spring context or database is started.
 */
class OrderAddressRequiredTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void missing_or_blank_address_prevents_checkout(String address) {
        RetrieveCart retrieveCart = mock(RetrieveCart.class);
        CheckoutOrder checkoutOrder = mock(CheckoutOrder.class);
        when(retrieveCart.byId(any())).thenReturn(mock(Cart.class));
        OrderController controller = new OrderController(retrieveCart, checkoutOrder);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("checkoutToken", "address-required-test");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = assertDoesNotThrow(() -> controller.place(
                "Test Name", address, request, new MockHttpServletResponse(), model,
                new RedirectAttributesModelMap()),
                "Missing address must show validation feedback, not throw an exception");

        assertAll(
                () -> assertThat(view).isEqualTo("order"),
                () -> assertThat(model.get("addressError")).isEqualTo(true),
                () -> verifyNoInteractions(checkoutOrder, retrieveCart));
    }
}
