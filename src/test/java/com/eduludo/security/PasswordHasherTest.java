package com.eduludo.security;

import com.eduludo.testing.Test;

import java.util.Arrays;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertTrue;

/** PBKDF2 password hashing and the randomness of the encryption. */
public class PasswordHasherTest {

    private static final int ITERATIONS = 1000;

    @Test
    void theRightPasswordVerifiesAndAWrongOneDoesNot() {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash("secret123".toCharArray(), salt, ITERATIONS);
        assertTrue(PasswordHasher.verify("secret123".toCharArray(), salt, ITERATIONS, hash), "right password");
        assertFalse(PasswordHasher.verify("secret124".toCharArray(), salt, ITERATIONS, hash), "one character off");
        assertFalse(PasswordHasher.verify("SECRET123".toCharArray(), salt, ITERATIONS, hash), "passwords are case sensitive");
        assertFalse(PasswordHasher.verify("".toCharArray(), salt, ITERATIONS, hash), "empty password");
    }

    @Test
    void hashingIsRepeatableForTheSameSaltAndIterations() {
        byte[] salt = PasswordHasher.newSalt();
        byte[] a = PasswordHasher.hash("secret123".toCharArray(), salt, ITERATIONS);
        byte[] b = PasswordHasher.hash("secret123".toCharArray(), salt, ITERATIONS);
        assertTrue(Arrays.equals(a, b), "same input gives the same hash");
        assertEquals(32, a.length, "256-bit hash");
    }

    @Test
    void theSaltAndIterationCountChangeTheHash() {
        byte[] salt1 = PasswordHasher.newSalt();
        byte[] salt2 = PasswordHasher.newSalt();
        assertEquals(16, salt1.length, "16-byte salt");
        assertFalse(Arrays.equals(salt1, salt2), "every salt is random");
        byte[] h1 = PasswordHasher.hash("secret123".toCharArray(), salt1, ITERATIONS);
        byte[] h2 = PasswordHasher.hash("secret123".toCharArray(), salt2, ITERATIONS);
        assertFalse(Arrays.equals(h1, h2), "same password with different salts gives different hashes");
        assertFalse(PasswordHasher.verify("secret123".toCharArray(), salt1, ITERATIONS + 1, h1),
                "a different iteration count does not verify");
    }

    @Test
    void encryptingTheSameTextTwiceGivesDifferentCiphertext() {
        CryptoBox box = new CryptoBox("key".toCharArray());
        String a = box.encrypt("same text");
        String b = box.encrypt("same text");
        assertFalse(a.equals(b), "a fresh random IV each time hides repeated content");
        assertEquals("same text", box.decrypt(a), "first decrypts");
        assertEquals("same text", box.decrypt(b), "second decrypts");
        assertEquals("", box.decrypt(box.encrypt("")), "empty text round trips");
    }
}

