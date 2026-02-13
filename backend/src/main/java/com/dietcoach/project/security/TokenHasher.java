// com.dietcoach.project.security.TokenHasher
package com.dietcoach.project.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TokenHasher {
    private TokenHasher() {}

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Token hash failed", e);
        }
    }
}
