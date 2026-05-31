package com.givinghands.givinghands.util;

public class ValidationUtil {

    // Validate email format
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Validate password length
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    // Sanitize string - remove dangerous characters
    public static String sanitize(String input) {
        if (input == null) return null;
        return input
            .replaceAll("<[^>]*>", "")        // Remove HTML tags
            .replaceAll("[<>\"'%;()&+]", "")  // Remove special chars
            .trim();
    }

    // Validate that string is not empty
    public static boolean notEmpty(String value) {
        return value != null && !value.isBlank();
    }
}