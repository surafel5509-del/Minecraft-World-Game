package com.craftworld3d.game.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Local audio: short effects via SoundPool, looping background music via
 * MediaPlayer. All sounds are bundled WAV files under assets/sounds/.
 */
public final class SoundManager {
    private static final String[] EFFECTS = {
            "step_grass", "step_stone", "step_wood", "step_sand",
            "dig_stone", "dig_wood", "dig_grass", "dig_sand", "dig_glass",
            "place", "splash", "eat", "hurt",
            "mob_pig", "mob_cow", "mob_sheep", "mob_chicken", "mob_zombie",
            "arrow", "creeper_fuse", "explosion",
            "rain", "thunder", "wind",
            "click", "achievement", "portal", "engine", "horn"
    };

    private final SoundPool pool;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Map<String, Integer> loopStreams = new HashMap<>();
    private MediaPlayer music;
    private final Context ctx;
    private volatile float volume;
    private volatile float musicVolume;

    public SoundManager(Context ctx, float volume, float musicVolume) {
        this.ctx = ctx.getApplicationContext();
        this.volume = volume;
        this.musicVolume = musicVolume;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build();
        for (String name : EFFECTS) {
            try (AssetFileDescriptor fd = this.ctx.getAssets().openFd("sounds/" + name + ".wav")) {
                ids.put(name, pool.load(fd, 1));
            } catch (Exception ignored) {
                // missing sound: skip silently, game remains playable
            }
        }
    }

    public void setVolume(float v) { volume = v; }

    public void setMusicVolume(float v) {
        musicVolume = v;
        MediaPlayer m = music;
        if (m != null) {
            try { m.setVolume(v, v); } catch (Exception ignored) {}
        }
    }

    public void play(String name) { play(name, 1f, 1f); }

    public void play(String name, float vol, float pitch) {
        Integer id = ids.get(name);
        if (id == null) return;
        float v = volume * vol;
        if (v <= 0.01f) return;
        pool.play(id, v, v, 1, 0, pitch);
    }

    /** Distance-attenuated effect. */
    public void playAt(String name, double dist) {
        if (dist > 24) return;
        float vol = (float) Math.max(0.05, 1.0 - dist / 24.0);
        play(name, vol, 0.9f + (float) (Math.random() * 0.2));
    }

    /** Starts (or keeps) a looping effect like rain or an engine. */
    public void loop(String name, float vol) {
        Integer id = ids.get(name);
        if (id == null) return;
        float v = volume * vol;
        Integer stream = loopStreams.get(name);
        if (stream != null) {
            pool.setVolume(stream, v, v);
            return;
        }
        int s = pool.play(id, v, v, 1, -1, 1f);
        if (s != 0) loopStreams.put(name, s);
    }

    public void stopLoop(String name) {
        Integer stream = loopStreams.remove(name);
        if (stream != null) pool.stop(stream);
    }

    public void stopAllLoops() {
        for (Integer s : loopStreams.values()) pool.stop(s);
        loopStreams.clear();
    }

    public void startMusic() {
        if (music != null || musicVolume <= 0.01f) return;
        try {
            AssetFileDescriptor fd = ctx.getAssets().openFd("sounds/music.wav");
            music = new MediaPlayer();
            music.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            music.setLooping(true);
            music.setVolume(musicVolume, musicVolume);
            music.prepare();
            music.start();
        } catch (Exception e) {
            music = null;
        }
    }

    public void stopMusic() {
        if (music != null) {
            try { music.stop(); music.release(); } catch (Exception ignored) {}
            music = null;
        }
    }

    public void release() {
        stopAllLoops();
        stopMusic();
        pool.release();
    }
}
