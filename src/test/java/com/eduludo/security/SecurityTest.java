package com.eduludo.security;

import com.eduludo.testing.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertFalse;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Admin password storage, login lockout, session expiry and file encryption. */
public class SecurityTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private Path tempFile() throws IOException {
        Path dir = Files.createTempDirectory("eduludo-test");
        return dir.resolve("admin.properties");
    }

    @Test
    void thePasswordIsStoredOnlyAsASaltedHash() throws IOException {
        Path file = tempFile();
        AuthService auth = new AuthService(file, Clock.systemUTC(), 1000);
        auth.createAdmin("secret123".toCharArray());
        String content = Files.readString(file);
        assertFalse(content.contains("secret123"), "plain password not stored");
        assertTrue(content.contains("salt=") && content.contains("hash="), "salt and hash stored");
    }

    @Test
    void loginWorksOnlyWithTheRightPassword() throws IOException {
        AuthService auth = new AuthService(tempFile(), Clock.systemUTC(), 1000);
        auth.createAdmin("secret123".toCharArray());
        AdminSession s = auth.login("secret123".toCharArray());
        assertTrue(auth.isValid(s), "valid session");
        assertThrows(SecurityException.class, () -> auth.login("wrong123".toCharArray()), "wrong password rejected");
        auth.logout(s);
        assertFalse(auth.isValid(s), "logged out");
    }

    @Test
    void weakPasswordsAreRejectedAndSetupHappensOnce() throws IOException {
        AuthService auth = new AuthService(tempFile(), Clock.systemUTC(), 1000);
        assertThrows(IllegalArgumentException.class, () -> auth.createAdmin("short1".toCharArray()), "too short");
        assertThrows(IllegalArgumentException.class, () -> auth.createAdmin("onlyletters".toCharArray()), "needs digits");
        auth.createAdmin("secret123".toCharArray());
        assertThrows(SecurityException.class, () -> auth.createAdmin("another123".toCharArray()), "only once");
    }

    @Test
    void fiveWrongPasswordsLockTheLogin() throws IOException {
        MutableClock clock = new MutableClock();
        AuthService auth = new AuthService(tempFile(), clock, 1000);
        auth.createAdmin("secret123".toCharArray());
        for (int i = 0; i < AuthService.MAX_ATTEMPTS; i++) {
            assertThrows(SecurityException.class, () -> auth.login("bad".toCharArray()), "wrong attempt");
        }
        SecurityException locked = assertThrows(SecurityException.class, () -> auth.login("secret123".toCharArray()),
                "even the right password is refused while locked");
        assertTrue(locked.getMessage().contains("Try again"), "lock message");
        clock.advance(AuthService.LOCKOUT.plusSeconds(1));
        assertTrue(auth.isValid(auth.login("secret123".toCharArray())), "works again after the lockout");
    }

    @Test
    void sessionsExpire() throws IOException {
        MutableClock clock = new MutableClock();
        AuthService auth = new AuthService(tempFile(), clock, 1000);
        auth.createAdmin("secret123".toCharArray());
        AdminSession s = auth.login("secret123".toCharArray());
        clock.advance(AuthService.SESSION_TTL.plusSeconds(1));
        assertFalse(auth.isValid(s), "expired session");
        assertThrows(SecurityException.class, () -> auth.requireValid(s), "expired session refused");
        assertThrows(SecurityException.class, () -> auth.requireValid(null), "no session refused");
    }

    @Test
    void thePasswordCanBeChanged() throws IOException {
        AuthService auth = new AuthService(tempFile(), Clock.systemUTC(), 1000);
        auth.createAdmin("secret123".toCharArray());
        AdminSession s = auth.login("secret123".toCharArray());
        assertThrows(SecurityException.class,
                () -> auth.changePassword(s, "wrong999".toCharArray(), "newpass123".toCharArray()), "needs current password");
        auth.changePassword(s, "secret123".toCharArray(), "newpass123".toCharArray());
        assertThrows(SecurityException.class, () -> auth.login("secret123".toCharArray()), "old password no longer works");
        assertTrue(auth.isValid(auth.login("newpass123".toCharArray())), "new password works");
    }

    @Test
    void encryptedDataRoundTripsAndTamperingIsDetected() {
        CryptoBox box = new CryptoBox("key-1".toCharArray());
        String secret = "{\"correct\": 2, \"text\": \"שלום\"}";
        String enc = box.encrypt(secret);
        assertFalse(enc.contains("correct"), "ciphertext hides the content");
        assertEquals(secret, box.decrypt(enc), "round trip (including Hebrew text)");
        char[] chars = enc.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = chars[mid] == 'A' ? 'B' : 'A';
        assertThrows(SecurityException.class, () -> box.decrypt(new String(chars)), "tampered file rejected");
        assertThrows(SecurityException.class, () -> new CryptoBox("key-2".toCharArray()).decrypt(enc), "wrong key rejected");
        assertThrows(SecurityException.class, () -> box.decrypt("not encrypted at all"), "garbage rejected");
    }
}

