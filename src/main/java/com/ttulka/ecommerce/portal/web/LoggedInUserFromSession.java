package com.ttulka.ecommerce.portal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.ttulka.ecommerce.identity.user.Username;

/**
 * Retrieve and save the logged-in user's Username from/to the HTTP session.
 */
final class LoggedInUserFromSession {

    private static final String SESSION_ATTRIBUTE = "LOGGED_IN_USERNAME";

    private LoggedInUserFromSession() {
    }

    static void login(HttpServletRequest request, Username username) {
        request.getSession(true).setAttribute(SESSION_ATTRIBUTE, username.value());
    }

    static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    static Username username(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value == null ? null : new Username((String) value);
    }
}
