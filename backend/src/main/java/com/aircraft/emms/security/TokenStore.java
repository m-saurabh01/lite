package com.aircraft.emms.security;

import lombok.Getter;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token store for offline desktop app.
 * Tokens are random, cryptographically secure strings.
 */
public class TokenStore {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();
    private final long validityHours;

    public TokenStore(long validityHours) {
        this.validityHours = validityHours;
    }

    public String generateToken(String serviceId, String role) {
        byte[] bytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokens.put(token, new TokenEntry(serviceId, role, LocalDateTime.now().plusHours(validityHours)));
        return token;
    }

    public TokenEntry validateToken(String token) {
        if (token == null) return null;
        TokenEntry entry = tokens.get(token);
        if (entry == null) return null;
        if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return null;
        }
        return entry;
    }

    public void invalidateToken(String token) {
        tokens.remove(token);
    }

    public void invalidateAllForUser(String serviceId) {
        tokens.entrySet().removeIf(e -> e.getValue().getServiceId().equals(serviceId));
    }

    @Getter
    public static class TokenEntry {
        private final String serviceId;
        private final String role;
        private final LocalDateTime expiresAt;

        public TokenEntry(String serviceId, String role, LocalDateTime expiresAt) {
            this.serviceId = serviceId;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
}
