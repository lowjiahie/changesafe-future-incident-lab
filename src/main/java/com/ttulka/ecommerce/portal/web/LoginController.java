package com.ttulka.ecommerce.portal.web;

import jakarta.servlet.http.HttpServletRequest;

import com.ttulka.ecommerce.identity.user.AuthenticateUser;
import com.ttulka.ecommerce.identity.user.RawPassword;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Web controller for Login/Logout use-cases.
 */
@Controller
@RequiredArgsConstructor
class LoginController {

    private final @NonNull AuthenticateUser authenticateUser;

    @GetMapping("/login")
    public String index(HttpServletRequest request) {
        if (LoggedInUserFromSession.username(request) != null) {
            return "redirect:/profile";
        }
        return "login";
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String login(@RequestParam(required = false) String username,
                        @RequestParam(required = false) String password,
                        HttpServletRequest request, Model model) {
        model.addAttribute("username", username == null ? "" : username);
        try {
            User user = authenticateUser.authenticate(
                    new Username(username == null ? "" : username),
                    new RawPassword(password == null ? "" : password));
            LoggedInUserFromSession.login(request, user.username());
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("loginError", true);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        LoggedInUserFromSession.logout(request);
        return "redirect:/";
    }
}
