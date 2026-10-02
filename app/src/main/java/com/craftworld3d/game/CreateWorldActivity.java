package com.craftworld3d.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.craftworld3d.game.engine.GameEngine;

import java.util.Random;

/** New world: name, seed, game mode, difficulty, world type. */
public final class CreateWorldActivity extends Activity {
    private EditText nameField, seedField;
    private Spinner modeSpinner, difficultySpinner, typeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(32), dp(16), dp(32), dp(16));
        col.setBackgroundColor(0xFF1B2A16);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        label(col, R.string.world_name);
        nameField = new EditText(this);
        nameField.setText("My World");
        nameField.setTextColor(Color.WHITE);
        col.addView(nameField, field());

        label(col, R.string.world_seed);
        seedField = new EditText(this);
        seedField.setTextColor(Color.WHITE);
        col.addView(seedField, field());

        label(col, R.string.world_gamemode);
        modeSpinner = spinner(col,
                getString(R.string.gamemode_survival), getString(R.string.gamemode_creative));

        label(col, R.string.world_difficulty);
        difficultySpinner = spinner(col,
                getString(R.string.difficulty_peaceful), getString(R.string.difficulty_easy),
                getString(R.string.difficulty_normal), getString(R.string.difficulty_hard));
        difficultySpinner.setSelection(new Settings(this).difficulty());

        label(col, R.string.world_type);
        typeSpinner = spinner(col,
                getString(R.string.world_type_default), getString(R.string.world_type_flat));

        Button create = new Button(this);
        create.setText(R.string.create);
        create.setTextColor(Color.WHITE);
        create.setBackgroundColor(0xFF3E5B2E);
        create.setOnClickListener(v -> create());
        LinearLayout.LayoutParams lp = field();
        lp.setMargins(0, dp(16), 0, dp(8));
        col.addView(create, lp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col);
        setContentView(scroll);
    }

    private void create() {
        String name = nameField.getText().toString().trim();
        if (name.isEmpty()) name = "World";
        String seedText = seedField.getText().toString().trim();
        long seed;
        if (seedText.isEmpty()) {
            seed = new Random().nextLong();
        } else {
            try { seed = Long.parseLong(seedText); }
            catch (NumberFormatException e) { seed = seedText.hashCode() * 31L + seedText.length(); }
        }
        String type = typeSpinner.getSelectedItemPosition() == 1 ? "flat" : "default";
        String id = GameEngine.createWorld(AppPaths.worldsRoot(this), name, seed,
                modeSpinner.getSelectedItemPosition(),
                difficultySpinner.getSelectedItemPosition(), type);
        if (id == null) { finish(); return; }
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_WORLD_ID, id);
        startActivity(i);
        finish();
    }

    private void label(LinearLayout col, int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextColor(0xFFAACF8E);
        t.setPadding(0, dp(10), 0, dp(2));
        col.addView(t, field());
    }

    private Spinner spinner(LinearLayout col, String... items) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        col.addView(s, field());
        return s;
    }

    private LinearLayout.LayoutParams field() {
        return new LinearLayout.LayoutParams(dp(360), LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
