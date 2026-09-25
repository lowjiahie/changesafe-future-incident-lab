package com.ttulka.ecommerce.identity.user.jdbc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for JDBC implementation for User service.
 */
@Configuration
class UserJdbcConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    FindUserJdbc findUserJdbc(JdbcTemplate jdbcTemplate) {
        return new FindUserJdbc(jdbcTemplate);
    }

    @Bean
    AuthenticateUserJdbc authenticateUserJdbc(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, FindUserJdbc findUserJdbc) {
        return new AuthenticateUserJdbc(jdbcTemplate, passwordEncoder, findUserJdbc);
    }

    @Bean
    UpdateUserProfileJdbc updateUserProfileJdbc(FindUserJdbc findUserJdbc) {
        return new UpdateUserProfileJdbc(findUserJdbc);
    }
}
