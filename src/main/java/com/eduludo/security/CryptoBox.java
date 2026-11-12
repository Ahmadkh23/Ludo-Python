package com.eduludo.security;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Authenticated encryption (AES-256-GCM) for data files such as the question bank.
 * <p>
 * File format: Base64( "EDL1" | salt(16) | iv(12) | ciphertext+tag ). The key is derived with PBKDF2 from a
 * secret and the per-file salt. GCM also detects any change to the file: a tampered file is rejected.
 * <p>
 * Purpose: players can't open the question file in a text editor to read the answers or edit questions
 * without the admin wizard. See SECURITY.md for the threat model and its limits.
 */
public final class CryptoBox {

    private static final byte[] MAGIC = "EDL1".getBytes(StandardCharsets.US_ASCII);
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int KDF_ITERATIONS = 65_536;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final char[] secret;

    public CryptoBox(char[] secret) {
        this.secret = secret.clone();
    }

    public String encrypt(String plaintext) {
        try {
            byte[] salt = new byte[SALT_LEN];
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(salt);
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(MAGIC);
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(MAGIC.length + SALT_LEN + IV_LEN + ct.length);
            buf.put(MAGIC).put(salt).put(iv).put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * @throws SecurityException if the data was modified, encrypted with another key, or is not ours
     */
    public String decrypt(String encoded) {
        byte[] all;
        try {
            all = Base64.getDecoder().decode(encoded.strip());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("The data file is damaged or was edited by hand", e);
        }
        if (all.length < MAGIC.length + SALT_LEN + IV_LEN + TAG_BITS / 8) {
            throw new SecurityException("The data file is damaged or was edited by hand");
        }
        ByteBuffer buf = ByteBuffer.wrap(all);
        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new SecurityException("Unknown data file format");
        }
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        buf.get(salt);
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(MAGIC);
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new SecurityException("The data file was modified outside the application", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private SecretKeySpec key(byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, KDF_ITERATIONS, 256);
        try {
            byte[] k = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return new SecretKeySpec(k, "AES");
        } finally {
            spec.clearPassword();
        }
    }
}

