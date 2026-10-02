package com.craftworld3d.game;

import android.content.Context;
import android.content.SharedPreferences;

/** Local settings stored in SharedPreferences. 100% offline. */
public final class Settings {
    public static final int GRAPHICS_LOW = 0, GRAPHICS_MEDIUM = 1, GRAPHICS_HIGH = 2;
    public static final int CAMERA_FIRST = 0, CAMERA_THIRD = 1;

    private final SharedPreferences prefs;

    public Settings(Context ctx) {
        prefs = ctx.getSharedPreferences("craftworld_settings", Context.MODE_PRIVATE);
    }

    public int graphicsQuality() { return prefs.getInt("graphics", GRAPHICS_MEDIUM); }
    public void setGraphicsQuality(int v) { prefs.edit().putInt("graphics", v).apply(); }

    public int renderDistance() { return prefs.getInt("renderDistance", 5); }
    public void setRenderDistance(int v) { prefs.edit().putInt("renderDistance", clamp(v, 2, 12)).apply(); }

    public boolean showFps() { return prefs.getBoolean("showFps", false); }
    public void setShowFps(boolean v) { prefs.edit().putBoolean("showFps", v).apply(); }

    public boolean particles() { return prefs.getBoolean("particles", true); }
    public void setParticles(boolean v) { prefs.edit().putBoolean("particles", v).apply(); }

    /** Look sensitivity multiplier, 0.2 .. 2.0. */
    public float sensitivity() { return prefs.getFloat("sensitivity", 1.0f); }
    public void setSensitivity(float v) { prefs.edit().putFloat("sensitivity", clampF(v, 0.2f, 2.0f)).apply(); }

    public float soundVolume() { return prefs.getFloat("soundVolume", 0.8f); }
    public void setSoundVolume(float v) { prefs.edit().putFloat("soundVolume", clampF(v, 0f, 1f)).apply(); }

    public float musicVolume() { return prefs.getFloat("musicVolume", 0.4f); }
    public void setMusicVolume(float v) { prefs.edit().putFloat("musicVolume", clampF(v, 0f, 1f)).apply(); }

    /** 0 peaceful, 1 easy, 2 normal, 3 hard. */
    public int difficulty() { return prefs.getInt("difficulty", 2); }
    public void setDifficulty(int v) { prefs.edit().putInt("difficulty", clamp(v, 0, 3)).apply(); }

    public int cameraMode() { return prefs.getInt("cameraMode", CAMERA_FIRST); }
    public void setCameraMode(int v) { prefs.edit().putInt("cameraMode", v).apply(); }

    public boolean vibration() { return prefs.getBoolean("vibration", true); }
    public void setVibration(boolean v) { prefs.edit().putBoolean("vibration", v).apply(); }

    public String lastWorldId() { return prefs.getString("lastWorld", null); }
    public void setLastWorldId(String id) { prefs.edit().putString("lastWorld", id).apply(); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clampF(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
