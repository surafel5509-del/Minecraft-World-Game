package com.craftworld3d.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.craftworld3d.core.save.WorldStorage;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Saved worlds: name, seed, mode, last played and play time; tap to load. */
public final class WorldListActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rebuild();
    }

    private void rebuild() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(12), dp(24), dp(12));
        col.setBackgroundColor(0xFF1B2A16);

        List<WorldStorage.WorldInfo> worlds = WorldStorage.listWorlds(AppPaths.worldsRoot(this));
        if (worlds.isEmpty()) {
            TextView t = new TextView(this);
            t.setText(R.string.no_worlds);
            t.setTextColor(Color.WHITE);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(48), 0, 0);
            col.addView(t);
        }
        for (WorldStorage.WorldInfo info : worlds) {
            col.addView(worldRow(info));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private LinearLayout worldRow(WorldStorage.WorldInfo info) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xFF2C421F);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(rowLp);

        LinearLayout info2 = new LinearLayout(this);
        info2.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info2.setLayoutParams(lp);

        TextView name = new TextView(this);
        name.setText(info.name());
        name.setTextSize(18);
        name.setTextColor(Color.WHITE);
        info2.addView(name);

        TextView meta = new TextView(this);
        String mode = getString(info.gamemode() == 1
                ? R.string.gamemode_creative : R.string.gamemode_survival);
        String played = formatDuration(info.playTimeMs());
        meta.setText(getString(R.string.world_info, String.valueOf(info.seed()), mode, played)
                + "\n" + DateFormat.getDateTimeInstance().format(new Date(info.lastPlayed())));
        meta.setTextColor(0xFFAACF8E);
        meta.setTextSize(12);
        info2.addView(meta);
        row.addView(info2);

        Button del = new Button(this);
        del.setText(R.string.delete_world);
        del.setTextColor(Color.WHITE);
        del.setBackgroundColor(0xFF7A2E2E);
        del.setOnClickListener(v -> confirmDelete(info.id()));
        row.addView(del);

        row.setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_WORLD_ID, info.id());
            startActivity(i);
        });
        return row;
    }

    private void confirmDelete(String id) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_world_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    AppPaths.deleteRecursive(AppPaths.worldDir(this, id));
                    rebuild();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static String formatDuration(long ms) {
        long m = ms / 60000;
        return m >= 60 ? (m / 60) + "h " + (m % 60) + "m" : m + "m";
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
