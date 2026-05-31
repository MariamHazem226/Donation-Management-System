package com.givinghands.givinghands.security;

import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.oauth2.frontend-redirect-url}")
    private String frontendRedirectUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        User user = userRepository.findByEmail(email != null ? email.trim().toLowerCase() : "")
                .orElseThrow(() -> new IOException("Authenticated Google user not found in database"));

        String token = jwtService.generateToken(user);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(OAuth2RoleHolder.SESSION_ATTRIBUTE);
        }

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", token)
                .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
