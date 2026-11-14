package com.eduludo.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Protects the question management wizard (admin only).
 *
 * <ul>
 *   <li>The admin password is stored only as a salted PBKDF2 hash ({@link PasswordHasher}).</li>
 *   <li>On the first visit, the admin creates the password (at least 8 characters, letters and digits).</li>
 *   <li>Five wrong passwords lock the login for 60 seconds (brute-force protection).</li>
 *   <li>A successful login returns an {@link AdminSession} that expires after 15 minutes. The question
 *       service checks it on every protected operation, so the protection is in the logic layer and not
 *       only in the UI.</li>
 * </ul>
 */
public final class AuthService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCKOUT = Duration.ofSeconds(60);
    public static final Duration SESSION_TTL = Duration.ofMinutes(15);
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final Path credentialsFile;
    private final Clock clock;
    private final int iterations;
    private final Map<String, Instant> sessions = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private int failedAttempts;
    private Instant lockedUntil = Instant.EPOCH;

    public AuthService(Path credentialsFile) {
        this(credentialsFile, Clock.systemUTC(), PasswordHasher.DEFAULT_ITERATIONS);
    }

    /** For tests: custom clock and a lower iteration count. */
    public AuthService(Path credentialsFile, Clock clock, int iterations) {
        this.credentialsFile = credentialsFile;
        this.clock = clock;
        this.iterations = iterations;
    }

    /** True once an admin password exists. */
    public boolean isConfigured() {
        return Files.exists(credentialsFile);
    }

    /** Returns null if the password is acceptable, otherwise the reason. */
    public static String validateNewPassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            return "The password must have at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        if (!letter || !digit) {
            return "The password must contain both letters and digits.";
        }
        return null;
    }

    /** First-time setup. Only allowed while no admin password exists. */
    public synchronized void createAdmin(char[] password) {
        if (isConfigured()) {
            throw new SecurityException("An admin password already exists");
        }
        String problem = validateNewPassword(password);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        store(password);
    }

    /**
     * Checks the password.
     *
     * @throws SecurityException with a user-readable message if it is wrong or the login is locked
     */
    public synchronized AdminSession login(char[] password) {
        Instant now = clock.instant();
        if (now.isBefore(lockedUntil)) {
            long secs = Duration.between(now, lockedUntil).toSeconds() + 1;
            throw new SecurityException("Too many wrong attempts. Try again in " + secs + " seconds.");
        }
        if (!isConfigured()) {
            throw new SecurityException("No admin password has been set yet");
        }
        Properties p = load();
        byte[] salt = Base64.getDecoder().decode(p.getProperty("salt"));
        byte[] hash = Base64.getDecoder().decode(p.getProperty("hash"));
        int iters = Integer.parseInt(p.getProperty("iterations"));
        if (!PasswordHasher.verify(password, salt, iters, hash)) {
            failedAttempts++;
            if (failedAttempts >= MAX_ATTEMPTS) {
                failedAttempts = 0;
                lockedUntil = now.plus(LOCKOUT);
                throw new SecurityException("Wrong password. Login is locked for " + LOCKOUT.toSeconds() + " seconds.");
            }
            throw new SecurityException("Wrong password (" + (MAX_ATTEMPTS - failedAttempts) + " attempts left).");
        }
        failedAttempts = 0;
        byte[] tokenBytes = new byte[24];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expires = now.plus(SESSION_TTL);
        sessions.put(token, expires);
        return new AdminSession(token, expires);
    }

    public synchronized boolean isValid(AdminSession session) {
        if (session == null) {
            return false;
        }
        Instant expires = sessions.get(session.token());
        if (expires == null) {
            return false;
        }
        if (!clock.instant().isBefore(expires)) {
            sessions.remove(session.token());
            return false;
        }
        return true;
    }

    /** @throws SecurityException if the session is missing, expired or logged out */
    public void requireValid(AdminSession session) {
        if (!isValid(session)) {
            throw new SecurityException("Admin login required (the session is missing or expired)");
        }
    }

    public synchronized void logout(AdminSession session) {
        if (session != null) {
            sessions.remove(session.token());
        }
    }

    /** Changes the admin password. Needs a valid session and the current password. */
    public synchronized void changePassword(AdminSession session, char[] currentPassword, char[] newPassword) {
        requireValid(session);
        Properties p = load();
        byte[] salt = Base64.getDecoder().decode(p.getProperty("salt"));
        byte[] hash = Base64.getDecoder().decode(p.getProperty("hash"));
        int iters = Integer.parseInt(p.getProperty("iterations"));
        if (!PasswordHasher.verify(currentPassword, salt, iters, hash)) {
            throw new SecurityException("The current password is wrong");
        }
        String problem = validateNewPassword(newPassword);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        store(newPassword);
    }

    private void store(char[] password) {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash(password, salt, iterations);
        Properties p = new Properties();
        p.setProperty("algorithm", "PBKDF2WithHmacSHA256");
        p.setProperty("iterations", Integer.toString(iterations));
        p.setProperty("salt", Base64.getEncoder().encodeToString(salt));
        p.setProperty("hash", Base64.getEncoder().encodeToString(hash));
        Arrays.fill(hash, (byte) 0);
        try {
            Files.createDirectories(credentialsFile.toAbsolutePath().getParent());
            Path tmp = credentialsFile.resolveSibling(credentialsFile.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "EduLudo admin credentials - salted PBKDF2 hash, never the plain password");
            }
            Files.move(tmp, credentialsFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save admin credentials", e);
        }
    }

    private Properties load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(credentialsFile)) {
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read admin credentials", e);
        }
        if (p.getProperty("salt") == null || p.getProperty("hash") == null || p.getProperty("iterations") == null) {
            throw new SecurityException("The admin credentials file is damaged");
        }
        return p;
    }
}

