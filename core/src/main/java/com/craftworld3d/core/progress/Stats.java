package com.craftworld3d.core.progress;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local gameplay statistics, persisted with the world. */
public final class Stats {
    public enum Stat {
        BLOCKS_MINED, BLOCKS_PLACED, DISTANCE_TRAVELED_M, MOBS_DEFEATED,
        ITEMS_CRAFTED, TIME_PLAYED_SECONDS, DEATHS
    }

    private final EnumMap<Stat, Long> values = new EnumMap<>(Stat.class);

    public void add(Stat stat, long amount) {
        values.merge(stat, amount, Long::sum);
    }

    public long get(Stat stat) {
        return values.getOrDefault(stat, 0L);
    }

    public Map<String, Object> save() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<Stat, Long> e : values.entrySet()) {
            m.put(e.getKey().name(), e.getValue());
        }
        return m;
    }

    public void load(Map<String, Object> m) {
        values.clear();
        for (Stat s : Stat.values()) {
            Object v = m.get(s.name());
            if (v instanceof Number n) values.put(s, n.longValue());
        }
    }
}
