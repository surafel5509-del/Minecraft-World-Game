package com.craftworld3d.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.craftworld3d.core.save.WorldStorage;

import java.util.List;

/** Main menu: Play / Create / Load / Settings / Achievements / Statistics / About / Exit. */
public final class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(16), dp(24), dp(16));
        col.setBackgroundColor(0xFF1B2A16);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(34);
        title.setTextColor(0xFF8BC34A);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(16));
        col.addView(title);

        addButton(col, R.string.menu_play, v -> play());
        addButton(col, R.string.menu_create_world, v ->
                startActivity(new Intent(this, CreateWorldActivity.class)));
        addButton(col, R.string.menu_load_world, v ->
                startActivity(new Intent(this, WorldListActivity.class)));
        addButton(col, R.string.menu_settings, v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        addButton(col, R.string.menu_achievements, v ->
                startActivity(new Intent(this, AchievementsActivity.class)));
        addButton(col, R.string.menu_statistics, v ->
                startActivity(new Intent(this, StatisticsActivity.class)));
        addButton(col, R.string.menu_about, v ->
                startActivity(new Intent(this, AboutActivity.class)));
        addButton(col, R.string.menu_exit, v -> finishAffinity());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    /** Play = continue the most recent world, else open world creation. */
    private void play() {
        Settings settings = new Settings(this);
        String last = settings.lastWorldId();
        if (last != null && AppPaths.worldDir(this, last).isDirectory()) {
            startGame(last);
            return;
        }
        List<WorldStorage.WorldInfo> worlds = WorldStorage.listWorlds(AppPaths.worldsRoot(this));
        if (!worlds.isEmpty()) {
            startGame(worlds.get(0).id());
        } else {
            startActivity(new Intent(this, CreateWorldActivity.class));
        }
    }

    private void startGame(String worldId) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_WORLD_ID, worldId);
        startActivity(i);
    }

    private void addButton(LinearLayout col, int textRes, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(textRes);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(0xFF3E5B2E);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        col.addView(b, lp);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
