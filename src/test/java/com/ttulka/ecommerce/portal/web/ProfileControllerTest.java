package com.ttulka.ecommerce.portal.web;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FindUser;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.UpdateUserProfile;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    private static final String SESSION_ATTRIBUTE = "LOGGED_IN_USERNAME";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindUser findUser;
    @MockBean
    private UpdateUserProfile updateUserProfile;

    @Test
    void anonymous_visitor_is_redirected_to_login() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void logged_in_user_sees_their_profile() throws Exception {
        User user = testUser();
        when(findUser.byUsername(any())).thenReturn(user);

        mockMvc.perform(get("/profile").sessionAttr(SESSION_ATTRIBUTE, "ami"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Ami\"")))
                .andExpect(content().string(containsString("value=\"Tanaka\"")));
    }

    @Test
    void profile_is_updated_with_valid_values() throws Exception {
        mockMvc.perform(
                post("/profile")
                        .sessionAttr(SESSION_ATTRIBUTE, "ami")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("firstName", "Amelia")
                        .param("lastName", "Tanaka")
                        .param("phone", "+1 555 9999")
                        .param("address", "99 New Address")
                        .param("email", "amelia@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("profile was updated")));

        verify(updateUserProfile).update(
                new Username("ami"), new FirstName("Amelia"), new LastName("Tanaka"),
                new Phone("+1 555 9999"), new Address("99 New Address"), new Email("amelia@example.com"));
    }

    @Test
    void invalid_email_is_rejected_without_saving() throws Exception {
        mockMvc.perform(
                post("/profile")
                        .sessionAttr(SESSION_ATTRIBUTE, "ami")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("firstName", "Amelia")
                        .param("lastName", "Tanaka")
                        .param("phone", "+1 555 9999")
                        .param("address", "99 New Address")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid email")));

        verify(updateUserProfile, org.mockito.Mockito.never()).update(any(), any(), any(), any(), any(), any());
    }

    private User testUser() {
        User user = mock(User.class);
        when(user.username()).thenReturn(new Username("ami"));
        when(user.firstName()).thenReturn(new FirstName("Ami"));
        when(user.lastName()).thenReturn(new LastName("Tanaka"));
        when(user.phone()).thenReturn(new Phone("+1 555 0101"));
        when(user.address()).thenReturn(new Address("12 Sakura Lane"));
        when(user.email()).thenReturn(new Email("ami@example.com"));
        return user;
    }
}
