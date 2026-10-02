package com.craftworld.game;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.craftworld.game.engine.GameRenderer;
import com.craftworld.game.engine.GameSurface;
import com.craftworld.game.ui.HudView;

/** Single-activity, immersive game shell. Rendering and simulation are independent of Android UI. */
public final class MainActivity extends AppCompatActivity {
    private GameSurface surface;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        FrameLayout root = new FrameLayout(this);
        GameRenderer renderer = new GameRenderer(this);
        surface = new GameSurface(this, renderer);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        root.addView(new HudView(this, renderer.getGame()), new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }
    @Override protected void onResume(){ super.onResume(); surface.onResume(); }
    @Override protected void onPause(){ surface.onPause(); super.onPause(); }
}
