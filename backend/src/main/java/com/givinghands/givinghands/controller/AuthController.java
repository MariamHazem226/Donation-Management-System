package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.AuthResponseDTO;
import com.givinghands.givinghands.dto.UserResponseDTO;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.security.JwtService;
import com.givinghands.givinghands.security.OAuth2RoleHolder;
import com.givinghands.givinghands.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody java.util.Map<String, String> request) {
        try {
            User user = authService.registerUser(
                    request.get("name"),
                    request.get("email"),
                    request.get("password"),
                    request.get("role")
            );

            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(new AuthResponseDTO(token, UserResponseDTO.from(user)));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody java.util.Map<String, String> request) {
        try {
            User user = authService.loginUser(
                    request.get("email"),
                    request.get("password")
            );

            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(new AuthResponseDTO(token, UserResponseDTO.from(user)));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // CURRENT USER
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal String email) {

        if (email == null || email.isBlank()) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String s) {
                email = s;
            }
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401)
                    .body(java.util.Map.of("error", "Not authenticated"));
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .<ResponseEntity<?>>map(user ->
                        ResponseEntity.ok(UserResponseDTO.from(user))
                )
                .orElseGet(() ->
                        ResponseEntity.status(401)
                                .body(java.util.Map.of("error", "User not found"))
                );
    }

    // GOOGLE OAUTH
    @GetMapping("/oauth/google")
    public void googleOAuth(
            @RequestParam(defaultValue = "USER") String role,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        try {
            session.setAttribute(
                    OAuth2RoleHolder.SESSION_ATTRIBUTE,
                    OAuth2RoleHolder.normalizeRole(role)
            );
        } catch (IllegalArgumentException e) {
            response.sendRedirect("/register.html?oauthError=" + e.getMessage());
            return;
        }

        response.sendRedirect("/oauth2/authorization/google");
    }
}