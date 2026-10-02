package com.craftworld3d.game;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.craftworld3d.core.progress.Achievement;
import com.craftworld3d.core.progress.AchievementManager;
import com.craftworld3d.core.save.WorldStorage;

import java.util.List;
import java.util.Map;

/** Offline achievements of the most recently played world. */
public final class AchievementsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(32), dp(12), dp(32), dp(24));
        col.setBackgroundColor(0xFF1B2A16);

        AchievementManager mgr = new AchievementManager();
        List<WorldStorage.WorldInfo> worlds = WorldStorage.listWorlds(AppPaths.worldsRoot(this));
        if (!worlds.isEmpty()) {
            WorldStorage storage = new WorldStorage(AppPaths.worldDir(this, worlds.get(0).id()));
            Map<String, Object> saved = storage.loadJson("achievements.json");
            if (saved != null) mgr.load(saved);
        }

        for (Achievement a : Achievement.values()) {
            boolean unlocked = mgr.isUnlocked(a);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(unlocked ? 0xFF2E5B2E : 0xFF2A2A2A);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), 0, dp(4));

            TextView title = new TextView(this);
            title.setText(GameActivity.achievementTitle(a));
            title.setTextSize(16);
            title.setTextColor(unlocked ? Color.WHITE : 0xFF888888);
            row.addView(title);

            TextView desc = new TextView(this);
            desc.setText(unlocked ? getString(GameActivity.achievementDesc(a))
                    : getString(GameActivity.achievementDesc(a)) + " — " + getString(R.string.locked));
            desc.setTextSize(12);
            desc.setTextColor(0xFFAAAAAA);
            row.addView(desc);

            col.addView(row, lp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
