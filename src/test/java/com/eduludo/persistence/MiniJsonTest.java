package com.eduludo.persistence;

import com.eduludo.testing.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.eduludo.testing.Assert.assertEquals;
import static com.eduludo.testing.Assert.assertThrows;
import static com.eduludo.testing.Assert.assertTrue;

/** Edge cases of the built-in JSON reader and writer. */
public class MiniJsonTest {

    @Test
    void wholeNumbersBecomeLongsAndDecimalsBecomeDoubles() {
        List<?> list = (List<?>) MiniJson.parse("[0, -7, 42, 3.5, -0.25, 1e3, 2E-2]");
        assertEquals(0L, list.get(0), "zero");
        assertEquals(-7L, list.get(1), "negative long");
        assertEquals(42L, list.get(2), "positive long");
        assertEquals(3.5, list.get(3), "double");
        assertEquals(-0.25, list.get(4), "negative double");
        assertEquals(1000.0, list.get(5), "exponent");
        assertEquals(0.02, list.get(6), "negative exponent");
    }

    @Test
    void emptyContainersAndNestedValuesParse() {
        assertEquals(Map.of(), MiniJson.parse("{}"), "empty object");
        assertEquals(List.of(), MiniJson.parse("[]"), "empty array");
        assertEquals(List.of(List.of()), MiniJson.parse("[[]]"), "nested empty array");
        Map<?, ?> m = (Map<?, ?>) MiniJson.parse("{\"a\": {\"b\": [1, {\"c\": null}]}}");
        Map<?, ?> a = (Map<?, ?>) m.get("a");
        List<?> b = (List<?>) a.get("b");
        assertTrue(((Map<?, ?>) b.get(1)).containsKey("c"), "a null value keeps its key");
    }

    @Test
    void whitespaceAroundValuesIsIgnored() {
        assertEquals(List.of(1L, 2L), MiniJson.parse(" \n\t[ 1 ,\r\n 2 ] \n"), "whitespace everywhere");
    }

    @Test
    void allStringEscapesAreDecoded() {
        String parsed = (String) MiniJson.parse("\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u05e9\"");
        assertEquals("q\" b\\ s/ \b\f\n\r\t ש", parsed, "every escape type");
    }

    @Test
    void objectKeysKeepTheirOrder() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("z", 1L);
        m.put("a", 2L);
        m.put("m", 3L);
        Map<?, ?> back = (Map<?, ?>) MiniJson.parse(MiniJson.write(m));
        assertEquals(List.of("z", "a", "m"), List.copyOf(back.keySet()), "insertion order kept");
    }

    @Test
    void controlCharactersAreEscapedWhenWriting() {
        String written = MiniJson.write("a\u0001b\tc");
        assertEquals("\"a\\u0001b\\tc\"", written, "control characters escaped");
        assertEquals("a\u0001b\tc", MiniJson.parse(written), "and read back");
    }

    @Test
    void malformedJsonIsRejected() {
        String[] bad = {
            "", "{", "[1, 2", "\"unterminated", "{\"a\" 1}", "{a: 1}", "[1 2]", "{\"a\": 1,}",
            "tru", "nul", "\"bad \\x escape\"", "\"\\u12\"", "@", "1.2.3"
        };
        for (String json : bad) {
            assertThrows(IllegalArgumentException.class, () -> MiniJson.parse(json), "should reject: " + json);
        }
    }

    @Test
    void errorsNameThePosition() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MiniJson.parse("[1, x]"), "bad value");
        assertTrue(e.getMessage().contains("position"), "message includes the position: " + e.getMessage());
    }

    @Test
    void unsupportedTypesCannotBeWritten() {
        assertThrows(IllegalArgumentException.class, () -> MiniJson.write(new Object()), "plain object");
        assertThrows(IllegalArgumentException.class, () -> MiniJson.write(List.of('c')), "a char inside a list");
    }
}

