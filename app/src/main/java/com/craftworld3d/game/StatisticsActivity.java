package com.craftworld3d.game;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.craftworld3d.core.progress.Stats;
import com.craftworld3d.core.save.WorldStorage;

import java.util.List;
import java.util.Map;

/** Gameplay statistics of the most recently played world. */
public final class StatisticsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(32), dp(12), dp(32), dp(24));
        col.setBackgroundColor(0xFF1B2A16);

        Stats stats = new Stats();
        String worldName = null;
        List<WorldStorage.WorldInfo> worlds = WorldStorage.listWorlds(AppPaths.worldsRoot(this));
        if (!worlds.isEmpty()) {
            worldName = worlds.get(0).name();
            WorldStorage storage = new WorldStorage(AppPaths.worldDir(this, worlds.get(0).id()));
            Map<String, Object> saved = storage.loadJson("stats.json");
            if (saved != null) stats.load(saved);
        }

        TextView head = new TextView(this);
        head.setText(getString(R.string.stats_pick_world)
                + (worldName != null ? " " + worldName : ""));
        head.setTextColor(0xFFAACF8E);
        head.setPadding(0, 0, 0, dp(10));
        col.addView(head);

        addRow(col, R.string.stat_blocks_mined, String.valueOf(stats.get(Stats.Stat.BLOCKS_MINED)));
        addRow(col, R.string.stat_blocks_placed, String.valueOf(stats.get(Stats.Stat.BLOCKS_PLACED)));
        addRow(col, R.string.stat_distance, String.valueOf(stats.get(Stats.Stat.DISTANCE_TRAVELED_M)));
        addRow(col, R.string.stat_mobs, String.valueOf(stats.get(Stats.Stat.MOBS_DEFEATED)));
        addRow(col, R.string.stat_crafted, String.valueOf(stats.get(Stats.Stat.ITEMS_CRAFTED)));
        long sec = stats.get(Stats.Stat.TIME_PLAYED_SECONDS);
        addRow(col, R.string.stat_time, sec / 3600 + "h " + (sec % 3600) / 60 + "m");
        addRow(col, R.string.stat_deaths, String.valueOf(stats.get(Stats.Stat.DEATHS)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private void addRow(LinearLayout col, int labelRes, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        row.setBackgroundColor(0xFF2C421F);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(3), 0, dp(3));

        TextView l = new TextView(this);
        l.setText(labelRes);
        l.setTextColor(Color.WHITE);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(0xFF8BC34A);
        row.addView(v);

        col.addView(row, lp);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
