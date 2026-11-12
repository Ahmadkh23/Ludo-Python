package com.eduludo.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 and a random 16-byte salt per password.
 * The plain password is never stored. Comparison is constant-time to avoid timing attacks.
 */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(char[] password, byte[] salt, int iterations, byte[] expectedHash) {
        byte[] actual = hash(password, salt, iterations);
        try {
            return MessageDigest.isEqual(actual, expectedHash);
        } finally {
            Arrays.fill(actual, (byte) 0);
        }
    }
}

