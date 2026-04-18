package com.ecommerce.order.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default ADMIN and CUSTOMER users on startup if they don't already exist.
 * Uses the runtime PasswordEncoder to guarantee correct BCrypt hashes
 * rather than relying on pre-computed strings in data.sql.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin123", "ADMIN");
        createUserIfNotExists("customer", "customer123", "CUSTOMER");
    }

    private void createUserIfNotExists(String username, String rawPassword, String role) {
        if (!userRepository.existsByUsername(username)) {
            AppUser user = AppUser.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Created default {} user: {}", role, username);
        } else {
            log.debug("User '{}' already exists, skipping seed", username);
        }
    }
}
