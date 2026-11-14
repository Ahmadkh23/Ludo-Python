package com.eduludo.security;

import java.time.Instant;

/**
 * Proof that the admin logged in. Only {@link AuthService} can create one, and every protected
 * operation asks the AuthService whether the session is still valid (not expired, not logged out).
 */
public final class AdminSession {

    private final String token;
    private final Instant expiresAt;

    AdminSession(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}

