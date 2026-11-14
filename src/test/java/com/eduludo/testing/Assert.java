package com.eduludo.testing;

import java.util.Objects;

/** Assertions for the test suite. */
public final class Assert {

    private Assert() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message + " - value was null");
        }
    }

    /** Asserts that the code throws the given exception type and returns it. */
    public static <T extends Throwable> T assertThrows(Class<T> type, Runnable code, String message) {
        try {
            code.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            throw new AssertionError(message + " - expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError(message + " - expected " + type.getSimpleName() + " but nothing was thrown");
    }
}

