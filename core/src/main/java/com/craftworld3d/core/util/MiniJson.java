package com.craftworld3d.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free JSON reader/writer used by the save system.
 * Supports objects, arrays, strings, numbers (as Long/Double), booleans and null.
 */
public final class MiniJson {
    private MiniJson() {}

    // ---------------- Writing ----------------

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder(256);
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof String s) { writeString(sb, s); return; }
        if (v instanceof Boolean || v instanceof Long || v instanceof Integer) { sb.append(v); return; }
        if (v instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) sb.append('0');
            else sb.append(d.doubleValue());
            return;
        }
        if (v instanceof Float f) { sb.append(f.floatValue()); return; }
        if (v instanceof Number n) { sb.append(n); return; }
        if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, e.getKey());
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (v instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object o : (List<Object>) v) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, o);
            }
            sb.append(']');
            return;
        }
        writeString(sb, String.valueOf(v));
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------------- Parsing ----------------

    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.eof()) throw new IllegalArgumentException("Trailing JSON content at " + p.pos);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        return (Map<String, Object>) parse(text);
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) { this.s = s; }

        boolean eof() { return pos >= s.length(); }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        char peek() {
            if (eof()) throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(pos);
        }

        void expect(char c) {
            if (eof() || s.charAt(pos) != c)
                throw new IllegalArgumentException("Expected '" + c + "' at " + pos);
            pos++;
        }

        Object parseValue() {
            skipWs();
            char c = peek();
            if (c == '{') return parseObj();
            if (c == '[') return parseArr();
            if (c == '"') return parseStr();
            if (c == 't') { literal("true"); return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null"); return null; }
            return parseNum();
        }

        void literal(String lit) {
            if (!s.startsWith(lit, pos)) throw new IllegalArgumentException("Bad literal at " + pos);
            pos += lit.length();
        }

        Map<String, Object> parseObj() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            skipWs();
            if (!eof() && peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String key = parseStr();
                skipWs();
                expect(':');
                m.put(key, parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; return m; }
                throw new IllegalArgumentException("Expected ',' or '}' at " + pos);
            }
        }

        List<Object> parseArr() {
            expect('[');
            List<Object> l = new ArrayList<>();
            skipWs();
            if (!eof() && peek() == ']') { pos++; return l; }
            while (true) {
                l.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; return l; }
                throw new IllegalArgumentException("Expected ',' or ']' at " + pos);
            }
        }

        String parseStr() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) throw new IllegalArgumentException("Unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException("Bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object parseNum() {
            int start = pos;
            boolean isDouble = false;
            while (!eof()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+') pos++;
                else if (c == '.' || c == 'e' || c == 'E') { isDouble = true; pos++; }
                else break;
            }
            String num = s.substring(start, pos);
            if (num.isEmpty()) throw new IllegalArgumentException("Bad number at " + start);
            return isDouble ? (Object) Double.parseDouble(num) : (Object) Long.parseLong(num);
        }
    }

    // ---------------- Typed accessors ----------------

    public static long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : def;
    }

    public static int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    public static double getDouble(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public static float getFloat(Map<String, Object> m, String key, float def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.floatValue() : def;
    }

    public static boolean getBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    public static String getString(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v instanceof String s ? s : def;
    }
}
