package com.givinghands.givinghands.config;

import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Upgrades legacy plain-text seeded passwords to BCrypt hashes on startup.
 */
@Component
@Order(2)
public class PasswordEncodingMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncodingMigration.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SchemaHealthChecker schemaHealthChecker;

    public PasswordEncodingMigration(UserRepository userRepository,
                                     BCryptPasswordEncoder passwordEncoder,
                                     SchemaHealthChecker schemaHealthChecker) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.schemaHealthChecker = schemaHealthChecker;
    }

    @Override
    public void run(String... args) {
        if (!schemaHealthChecker.check().readyForSeeding()) {
            log.error("[password-migration] SKIPPED — database schema is not valid");
            return;
        }

        int upgraded = 0;
        try {
            for (User user : userRepository.findAll()) {
                String stored = user.getPassword();
                if (stored == null || stored.isBlank()) {
                    continue;
                }
                if (isBcryptHash(stored)) {
                    continue;
                }
                user.setPassword(passwordEncoder.encode(stored));
                userRepository.save(user);
                upgraded++;
                log.info("Upgraded password encoding for user: {}", user.getEmail());
            }
            if (upgraded > 0) {
                log.info("Password migration complete. {} user(s) updated.", upgraded);
            }
        } catch (Exception e) {
            log.error("Password encoding migration failed (application will continue): {}", e.getMessage());
        }
    }

    static boolean isBcryptHash(String password) {
        return password != null
                && password.startsWith("$2")
                && password.length() == 60;
    }
}
