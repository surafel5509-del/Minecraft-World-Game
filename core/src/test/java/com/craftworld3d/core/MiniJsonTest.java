package com.craftworld3d.core;

import com.craftworld3d.core.util.MiniJson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MiniJsonTest {

    @Test
    public void roundTripNestedStructure() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "Test \"World\"\nLine2");
        m.put("seed", 1234567890123L);
        m.put("health", 19.5);
        m.put("flag", true);
        m.put("nothing", null);
        List<Object> list = new ArrayList<>();
        list.add(1L);
        list.add("two");
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", -5L);
        list.add(inner);
        m.put("list", list);

        String json = MiniJson.write(m);
        Map<String, Object> parsed = MiniJson.parseObject(json);
        assertEquals("Test \"World\"\nLine2", MiniJson.getString(parsed, "name", ""));
        assertEquals(1234567890123L, MiniJson.getLong(parsed, "seed", 0));
        assertEquals(19.5, MiniJson.getDouble(parsed, "health", 0), 1e-9);
        assertTrue(MiniJson.getBool(parsed, "flag", false));
        assertNull(parsed.get("nothing"));
        @SuppressWarnings("unchecked")
        List<Object> plist = (List<Object>) parsed.get("list");
        assertEquals(3, plist.size());
        assertEquals(1L, plist.get(0));
        @SuppressWarnings("unchecked")
        Map<String, Object> pinner = (Map<String, Object>) plist.get(2);
        assertEquals(-5, MiniJson.getInt(pinner, "x", 0));
    }

    @Test
    public void parsesWhitespaceAndUnicode() {
        Map<String, Object> m = MiniJson.parseObject(" { \"a\" : [ 1 , 2.5 , \"\\u0041\" ] } ");
        @SuppressWarnings("unchecked")
        List<Object> a = (List<Object>) m.get("a");
        assertEquals("A", a.get(2));
    }

    @Test
    public void rejectsBrokenJson() {
        try {
            MiniJson.parse("{\"a\":}");
            fail("should throw");
        } catch (IllegalArgumentException expected) {
        }
        try {
            MiniJson.parse("{\"a\":1} trailing");
            fail("should throw");
        } catch (IllegalArgumentException expected) {
        }
    }
}
