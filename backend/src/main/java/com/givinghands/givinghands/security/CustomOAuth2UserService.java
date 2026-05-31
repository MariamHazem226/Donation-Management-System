package com.givinghands.givinghands.security;

import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = String.valueOf(attributes.get("sub"));
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account did not provide an email address");
        }

        String requestedRole = resolveRequestedRole();
        User user = authService.findOrCreateGoogleUser(googleId, email, name, picture, requestedRole);

        String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "USER";
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + role)),
                attributes,
                "sub"
        );
    }

    private String resolveRequestedRole() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "USER";
        }
        HttpSession session = attrs.getRequest().getSession(false);
        if (session == null) {
            return "USER";
        }
        Object value = session.getAttribute(OAuth2RoleHolder.SESSION_ATTRIBUTE);
        if (value instanceof String role) {
            return role;
        }
        return "USER";
    }
}
