package com.givinghands.givinghands.security;

/**
 * Session key used when starting Google OAuth from register (USER vs ORGANIZATION).
 */
public final class OAuth2RoleHolder {

    public static final String SESSION_ATTRIBUTE = "OAUTH_ROLE";

    private OAuth2RoleHolder() {
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        String r = role.trim().toUpperCase();
        if ("ORGANIZATION".equals(r) || "CREATOR".equals(r)) {
            return "ORGANIZATION";
        }
        if ("ADMIN".equals(r)) {
            throw new IllegalArgumentException("Admin accounts cannot be created via Google sign-up");
        }
        return "USER";
    }
}
