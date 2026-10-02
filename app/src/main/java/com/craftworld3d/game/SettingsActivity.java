package com.craftworld3d.game;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;

/** All options are stored locally in SharedPreferences. */
public final class SettingsActivity extends Activity {
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(this);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(32), dp(12), dp(32), dp(24));
        col.setBackgroundColor(0xFF1B2A16);

        addSpinner(col, R.string.settings_graphics,
                new int[]{R.string.graphics_low, R.string.graphics_medium, R.string.graphics_high},
                settings.graphicsQuality(), settings::setGraphicsQuality);

        addSeek(col, R.string.settings_render_distance, 2, 12, settings.renderDistance(),
                v -> settings.setRenderDistance(v), true);

        addSwitch(col, R.string.settings_show_fps, settings.showFps(), settings::setShowFps);
        addSwitch(col, R.string.settings_particles, settings.particles(), settings::setParticles);

        addSeek(col, R.string.settings_sensitivity, 2, 20,
                (int) (settings.sensitivity() * 10),
                v -> settings.setSensitivity(v / 10f), false);
        addSeek(col, R.string.settings_sound_volume, 0, 10,
                (int) (settings.soundVolume() * 10),
                v -> settings.setSoundVolume(v / 10f), false);
        addSeek(col, R.string.settings_music_volume, 0, 10,
                (int) (settings.musicVolume() * 10),
                v -> settings.setMusicVolume(v / 10f), false);

        addSpinner(col, R.string.settings_difficulty,
                new int[]{R.string.difficulty_peaceful, R.string.difficulty_easy,
                        R.string.difficulty_normal, R.string.difficulty_hard},
                settings.difficulty(), settings::setDifficulty);

        addSpinner(col, R.string.settings_camera,
                new int[]{R.string.camera_first, R.string.camera_third},
                settings.cameraMode(), settings::setCameraMode);

        addSwitch(col, R.string.settings_vibration, settings.vibration(), settings::setVibration);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private interface IntConsumer { void accept(int v); }
    private interface BoolConsumer { void accept(boolean v); }

    private void addSpinner(LinearLayout col, int labelRes, int[] options,
                            int selected, IntConsumer onChange) {
        label(col, labelRes);
        String[] items = new String[options.length];
        for (int i = 0; i < options.length; i++) items[i] = getString(options[i]);
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        s.setSelection(Math.min(selected, options.length - 1));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                onChange.accept(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        col.addView(s, params());
    }

    private void addSeek(LinearLayout col, int labelRes, int min, int max,
                         int value, IntConsumer onChange, boolean showValue) {
        TextView t = label(col, labelRes);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, value - min)));
        if (showValue) t.setText(getString(labelRes) + ": " + value);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                int v = p + min;
                onChange.accept(v);
                if (showValue) t.setText(getString(labelRes) + ": " + v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        col.addView(sb, params());
    }

    private void addSwitch(LinearLayout col, int labelRes, boolean value, BoolConsumer onChange) {
        Switch sw = new Switch(this);
        sw.setText(labelRes);
        sw.setTextColor(Color.WHITE);
        sw.setChecked(value);
        sw.setPadding(0, dp(10), 0, dp(4));
        sw.setOnCheckedChangeListener((b, checked) -> onChange.accept(checked));
        col.addView(sw, params());
    }

    private TextView label(LinearLayout col, int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextColor(0xFFAACF8E);
        t.setPadding(0, dp(12), 0, dp(2));
        col.addView(t, params());
        return t;
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(dp(400), LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
