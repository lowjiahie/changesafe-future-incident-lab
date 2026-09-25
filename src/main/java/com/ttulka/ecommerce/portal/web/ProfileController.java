package com.ttulka.ecommerce.portal.web;

import jakarta.servlet.http.HttpServletRequest;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FindUser;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.UpdateUserProfile;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Web controller for Profile use-cases.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
class ProfileController {

    private final @NonNull FindUser findUser;
    private final @NonNull UpdateUserProfile updateUserProfile;

    @GetMapping
    public String index(HttpServletRequest request, Model model) {
        Username username = LoggedInUserFromSession.username(request);
        if (username == null) {
            return "redirect:/login";
        }
        addUserToModel(model, findUser.byUsername(username));
        return "profile";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String update(@RequestParam(required = false) String firstName,
                         @RequestParam(required = false) String lastName,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String address,
                         @RequestParam(required = false) String email,
                         HttpServletRequest request, Model model) {
        Username username = LoggedInUserFromSession.username(request);
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username.value());
        model.addAttribute("firstName", nullToEmpty(firstName));
        model.addAttribute("lastName", nullToEmpty(lastName));
        model.addAttribute("phone", nullToEmpty(phone));
        model.addAttribute("address", nullToEmpty(address));
        model.addAttribute("email", nullToEmpty(email));

        boolean invalid = false;
        FirstName firstNameVal = null;
        try {
            firstNameVal = new FirstName(nullToEmpty(firstName));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("firstNameError", true);
            invalid = true;
        }
        LastName lastNameVal = null;
        try {
            lastNameVal = new LastName(nullToEmpty(lastName));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("lastNameError", true);
            invalid = true;
        }
        Phone phoneVal = null;
        try {
            phoneVal = new Phone(nullToEmpty(phone));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("phoneError", true);
            invalid = true;
        }
        Address addressVal = null;
        try {
            addressVal = new Address(nullToEmpty(address));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("addressError", true);
            invalid = true;
        }
        Email emailVal = null;
        try {
            emailVal = new Email(nullToEmpty(email));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("emailError", true);
            invalid = true;
        }
        if (invalid) {
            return "profile";
        }
        updateUserProfile.update(username, firstNameVal, lastNameVal, phoneVal, addressVal, emailVal);
        model.addAttribute("saved", true);
        return "profile";
    }

    private void addUserToModel(Model model, User user) {
        model.addAttribute("username", user.username().value());
        model.addAttribute("firstName", user.firstName().value());
        model.addAttribute("lastName", user.lastName().value());
        model.addAttribute("phone", user.phone().value());
        model.addAttribute("address", user.address().value());
        model.addAttribute("email", user.email().value());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
