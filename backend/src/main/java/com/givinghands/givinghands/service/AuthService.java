package com.givinghands.givinghands.service;

import com.givinghands.givinghands.entity.User;

public interface AuthService {

    User registerUser(String name, String email, String password, String role);

    User loginUser(String email, String password);

    boolean validateUser(String email, String password);

    /**
     * Finds an existing user by email or Google id, or creates one for first Google sign-in.
     */
    User findOrCreateGoogleUser(String googleId, String email, String name, String pictureUrl, String requestedRole);
}
