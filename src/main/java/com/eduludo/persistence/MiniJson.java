package com.eduludo.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer (objects, arrays, strings, numbers, booleans, null), so the project
 * needs no external libraries and the runnable JAR works on any machine with Java.
 * Objects become {@code LinkedHashMap<String,Object>}, arrays {@code List<Object>}, numbers {@code Long} or {@code Double}.
 */
public final class MiniJson {

    private final String s;
    private int pos;

    private MiniJson(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        if (p.pos != p.s.length()) {
            throw p.error("Unexpected trailing characters");
        }
        return value;
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        return sb.toString();
    }

    // ---------------------------------------------------------------- reading

    private Object readValue() {
        if (pos >= s.length()) {
            throw error("Unexpected end of input");
        }
        char c = s.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWs();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWs();
            if (peek() != '"') {
                throw error("Expected a property name");
            }
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            map.put(key, readValue());
            skipWs();
            char c = next();
            if (c == '}') {
                return map;
            }
            if (c != ',') {
                throw error("Expected ',' or '}'");
            }
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWs();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWs();
            list.add(readValue());
            skipWs();
            char c = next();
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw error("Expected ',' or ']'");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("Bad unicode escape");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("Bad escape \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
            pos++;
        }
        String num = s.substring(start, pos);
        if (num.isEmpty()) {
            throw error("Unexpected character '" + s.charAt(start) + "'");
        }
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            return Long.parseLong(num);
        } catch (NumberFormatException ex) {
            throw error("Bad number " + num);
        }
    }

    private Object literal(String word, Object value) {
        if (!s.startsWith(word, pos)) {
            throw error("Expected " + word);
        }
        pos += word.length();
        return value;
    }

    private void skipWs() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= s.length()) {
            throw error("Unexpected end of input");
        }
        return s.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char c) {
        if (next() != c) {
            throw error("Expected '" + c + "'");
        }
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("Invalid JSON at position " + pos + ": " + msg);
    }

    // ---------------------------------------------------------------- writing

    private static void writeValue(StringBuilder sb, Object v, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String str) {
            writeString(sb, str);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                pad(sb, indent + 1);
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                writeValue(sb, e.getValue(), indent + 1);
                sb.append(++i < map.size() ? ",\n" : "\n");
            }
            pad(sb, indent);
            sb.append('}');
        } else if (v instanceof List<?> list) {
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                pad(sb, indent + 1);
                writeValue(sb, list.get(i), indent + 1);
                sb.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            pad(sb, indent);
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Cannot write " + v.getClass());
        }
    }

    private static void writeString(StringBuilder sb, String str) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static void pad(StringBuilder sb, int indent) {
        sb.append("  ".repeat(indent));
    }
}

