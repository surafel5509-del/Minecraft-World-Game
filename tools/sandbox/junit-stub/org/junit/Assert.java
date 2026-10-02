package org.junit;

import java.util.Arrays;

/** JUnit4-compatible assertions (subset). See Test.java for why this exists. */
public final class Assert {
    private Assert() {}

    public static void assertTrue(boolean cond) { assertTrue(null, cond); }

    public static void assertTrue(String msg, boolean cond) {
        if (!cond) fail(msg == null ? "expected true" : msg);
    }

    public static void assertFalse(boolean cond) { assertFalse(null, cond); }

    public static void assertFalse(String msg, boolean cond) {
        if (cond) fail(msg == null ? "expected false" : msg);
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String msg, long expected, long actual) {
        if (expected != actual) fail(format(msg, expected, actual));
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(null, expected, actual, delta);
    }

    public static void assertEquals(String msg, double expected, double actual, double delta) {
        if (Double.compare(expected, actual) != 0 && Math.abs(expected - actual) > delta) {
            fail(format(msg, expected, actual));
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals((String) null, expected, actual);
    }

    public static void assertEquals(String msg, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(format(msg, expected, actual));
        }
    }

    public static void assertNotEquals(long unexpected, long actual) {
        if (unexpected == actual) fail("values should differ: " + actual);
    }

    public static void assertNotEquals(double unexpected, double actual, double delta) {
        if (Math.abs(unexpected - actual) <= delta) fail("values should differ: " + actual);
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        if (unexpected == null ? actual == null : unexpected.equals(actual)) {
            fail("values should differ: " + actual);
        }
    }

    public static void assertNull(Object o) { assertNull(null, o); }

    public static void assertNull(String msg, Object o) {
        if (o != null) fail(msg == null ? "expected null but was " + o : msg);
    }

    public static void assertNotNull(Object o) { assertNotNull(null, o); }

    public static void assertNotNull(String msg, Object o) {
        if (o == null) fail(msg == null ? "expected non-null" : msg);
    }

    public static void assertSame(Object expected, Object actual) {
        assertSame(null, expected, actual);
    }

    public static void assertSame(String msg, Object expected, Object actual) {
        if (expected != actual) fail(format(msg, expected, actual));
    }

    public static void assertArrayEquals(short[] expected, short[] actual) {
        assertArrayEquals(null, expected, actual);
    }

    public static void assertArrayEquals(String msg, short[] expected, short[] actual) {
        if (!Arrays.equals(expected, actual)) {
            fail(msg == null ? "arrays differ" : msg + ": arrays differ");
        }
    }

    public static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) fail("arrays differ");
    }

    public static void fail() { fail(null); }

    public static void fail(String msg) {
        throw new AssertionError(msg == null ? "assertion failed" : msg);
    }

    private static String format(String msg, Object expected, Object actual) {
        String base = "expected:<" + expected + "> but was:<" + actual + ">";
        return msg == null ? base : msg + " " + base;
    }
}
