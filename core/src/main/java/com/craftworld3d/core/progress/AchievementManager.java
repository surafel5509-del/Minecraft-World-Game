package com.craftworld3d.core.progress;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/** Tracks unlocked achievements; persists to the world save. */
public final class AchievementManager {
    public interface Listener {
        void onUnlocked(Achievement achievement);
    }

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private Listener listener;

    public void setListener(Listener l) { this.listener = l; }

    public boolean isUnlocked(Achievement a) { return unlocked.contains(a); }

    /** Unlocks if new; returns true when freshly unlocked. */
    public boolean unlock(Achievement a) {
        if (!unlocked.add(a)) return false;
        if (listener != null) listener.onUnlocked(a);
        return true;
    }

    public int unlockedCount() { return unlocked.size(); }

    public Map<String, Object> save() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> list = new ArrayList<>();
        for (Achievement a : unlocked) list.add(a.name());
        m.put("unlocked", list);
        return m;
    }

    @SuppressWarnings("unchecked")
    public void load(Map<String, Object> m) {
        unlocked.clear();
        Object list = m.get("unlocked");
        if (list instanceof List) {
            for (Object o : (List<Object>) list) {
                try {
                    unlocked.add(Achievement.valueOf(String.valueOf(o)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
