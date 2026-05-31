package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.notification.NotificationEvents;
import com.givinghands.givinghands.notification.NotificationPublisher;
import com.givinghands.givinghands.service.AuthService;
import com.givinghands.givinghands.security.OAuth2RoleHolder;
import com.givinghands.givinghands.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private NotificationPublisher notificationPublisher;

    @Override
    public User registerUser(String name, String email, String password, String role) {


        if (!ValidationUtil.notEmpty(name))
            throw new RuntimeException("Name is required");

        if (!ValidationUtil.isValidEmail(email))
            throw new RuntimeException("Invalid email format");

        if (!ValidationUtil.isValidPassword(password))
            throw new RuntimeException("Password must be at least 6 characters");


        name = ValidationUtil.sanitize(name);
        email = email.trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent())
            throw new RuntimeException("Email already registered");


        String normalizedRole = (role == null ? "USER" : role.trim().toUpperCase());

        if (normalizedRole.equals("DONOR") || normalizedRole.equals("USER"))
            normalizedRole = "USER";
        else if (normalizedRole.equals("VOLUNTEER"))
            normalizedRole = "USER";
        else if (normalizedRole.equals("CREATOR") || normalizedRole.equals("ORGANIZATION"))
            normalizedRole = "ORGANIZATION";
        else if (normalizedRole.equals("ADMIN"))
            throw new RuntimeException("Admin accounts cannot be created via public registration");
        else
            normalizedRole = "USER";

        User user = new User();
        user.setName(name);
        user.setEmail(email);

        user.setPassword(passwordEncoder.encode(password));

        user.setRole(normalizedRole);

        User saved = userRepository.save(user);

        if ("ORGANIZATION".equals(normalizedRole)) {
            Map<String, Object> orgPayload = Map.of(NotificationEvents.ORGANIZATION_ID, saved.getId());
            notificationPublisher.publish(NotificationEvents.ORG_REGISTRATION_WELCOME, orgPayload);
            notificationPublisher.publish(NotificationEvents.ORG_WELCOME_APPROVED, orgPayload);
            notificationPublisher.publish(NotificationEvents.ADMIN_NEW_ORG_REGISTERED, orgPayload);
        }

        return saved;
    }

    @Override
    public User loginUser(String email, String password) {

        if (!ValidationUtil.isValidEmail(email))
            throw new RuntimeException("Invalid email format");

        if (!ValidationUtil.notEmpty(password))
            throw new RuntimeException("Password is required");

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("google".equalsIgnoreCase(user.getOauthProvider())) {
            throw new RuntimeException("This account uses Google sign-in. Please continue with Google.");
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            throw new RuntimeException("Invalid password");
        }

        if (!passwordEncoder.matches(password, storedPassword)) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getRole() != null) {
            user.setRole(user.getRole().trim().toUpperCase());
        }

        return user;
    }

    @Override
    public boolean validateUser(String email, String password) {

        if (!ValidationUtil.isValidEmail(email) || !ValidationUtil.notEmpty(password))
            return false;

        String normalizedEmail = email.trim().toLowerCase();

        return userRepository.findByEmail(normalizedEmail)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    @Override
    public User findOrCreateGoogleUser(String googleId, String email, String name, String pictureUrl, String requestedRole) {
        String normalizedEmail = email.trim().toLowerCase();
        String role = OAuth2RoleHolder.normalizeRole(requestedRole);

        return userRepository.findByEmail(normalizedEmail)
                .map(existing -> updateGoogleProfile(existing, googleId, name, pictureUrl))
                .orElseGet(() -> createGoogleUser(googleId, normalizedEmail, name, pictureUrl, role));
    }

    private User updateGoogleProfile(User user, String googleId, String name, String pictureUrl) {
        if (googleId != null && !googleId.isBlank()) {
            user.setGoogleId(googleId);
        }
        user.setOauthProvider("google");
        if (name != null && !name.isBlank()) {
            user.setName(ValidationUtil.sanitize(name));
        }
        if (pictureUrl != null && !pictureUrl.isBlank()) {
            user.setAvatarUrl(pictureUrl);
        }
        return userRepository.save(user);
    }

    private User createGoogleUser(String googleId, String email, String name, String pictureUrl, String role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name != null && !name.isBlank() ? ValidationUtil.sanitize(name) : email.split("@")[0]);
        user.setRole(role);
        user.setGoogleId(googleId);
        user.setOauthProvider("google");
        user.setAvatarUrl(pictureUrl);
        user.setPassword(passwordEncoder.encode("OAUTH_" + UUID.randomUUID()));

        User saved = userRepository.save(user);

        if ("ORGANIZATION".equals(role)) {
            Map<String, Object> orgPayload = Map.of(NotificationEvents.ORGANIZATION_ID, saved.getId());
            notificationPublisher.publish(NotificationEvents.ORG_REGISTRATION_WELCOME, orgPayload);
            notificationPublisher.publish(NotificationEvents.ORG_WELCOME_APPROVED, orgPayload);
            notificationPublisher.publish(NotificationEvents.ADMIN_NEW_ORG_REGISTERED, orgPayload);
        }

        return saved;
    }
}