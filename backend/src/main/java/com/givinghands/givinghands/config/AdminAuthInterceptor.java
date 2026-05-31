package com.givinghands.givinghands.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * Protects /api/admin/** by requiring a logged-in user with ADMIN role.
 * Frontend must send {@code X-User-Email} from localStorage after login.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthInterceptor.class);

    public static final String ADMIN_USER_EMAIL_HEADER = "X-User-Email";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        log.info("[AdminAuthInterceptor] {} {}", request.getMethod(), request.getRequestURI());

        // 1. Try X-User-Email header first
        String email = request.getHeader(ADMIN_USER_EMAIL_HEADER);
        log.info("[AdminAuthInterceptor] X-User-Email header value: '{}'", email);

        // 2. Fall back to JWT principal set by JwtAuthenticationFilter
        if (email == null || email.isBlank()) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String principalEmail) {
                email = principalEmail;
                log.info("[AdminAuthInterceptor] Resolved email from JWT SecurityContext: '{}'", email);
            }
        }

        if (email == null || email.isBlank()) {
            log.warn("[AdminAuthInterceptor] No email resolved from header or JWT — returning 401");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Admin authentication required. Please log in as admin.");
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        log.info("[AdminAuthInterceptor] Looking up user by email: '{}'", normalizedEmail);

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.warn("[AdminAuthInterceptor] No user found in DB for email: '{}' — returning 403", normalizedEmail);
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. User not found: " + normalizedEmail);
            return false;
        }

        User user = userOpt.get();
        String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "null";
        log.info("[AdminAuthInterceptor] Found user id={} email='{}' role='{}'", user.getId(), user.getEmail(), role);

        boolean isAdmin = "ADMIN".equals(role);
        if (!isAdmin) {
            log.warn("[AdminAuthInterceptor] Role '{}' is not ADMIN — returning 403", role);
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. Admin role required. Your role: " + role);
            return false;
        }

        log.info("[AdminAuthInterceptor] Admin check passed for user id={}", user.getId());
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
