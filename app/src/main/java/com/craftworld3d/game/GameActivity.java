package com.craftworld3d.game;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.craftworld3d.core.entity.Villager;
import com.craftworld3d.core.progress.Achievement;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.BlockEntity;
import com.craftworld3d.core.world.blockentity.ChestEntity;
import com.craftworld3d.core.world.blockentity.FurnaceEntity;
import com.craftworld3d.game.assets.AtlasData;
import com.craftworld3d.game.audio.SoundManager;
import com.craftworld3d.game.engine.GameEngine;
import com.craftworld3d.game.gl.GameRenderer;
import com.craftworld3d.game.ui.GameOverlayView;

import java.io.File;

/** The in-game activity: GL world view + touch overlay, owning the engine. */
public final class GameActivity extends Activity
        implements GameEngine.Callbacks, GameOverlayView.Host {
    public static final String EXTRA_WORLD_ID = "world_id";

    private GameEngine engine;
    private GameRenderer renderer;
    private GLSurfaceView glView;
    private GameOverlayView overlay;
    private SoundManager sounds;
    private Settings settings;
    private Thread stopThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String worldId = getIntent().getStringExtra(EXTRA_WORLD_ID);
        settings = new Settings(this);
        if (worldId == null) worldId = settings.lastWorldId();
        if (worldId == null) { finish(); return; }
        settings.setLastWorldId(worldId);
        File dir = AppPaths.worldDir(this, worldId);

        sounds = new SoundManager(this, settings.soundVolume(), settings.musicVolume());
        AtlasData atlas = new AtlasData(this);
        engine = new GameEngine(dir, atlas, sounds);
        engine.setCallbacks(this);
        engine.setRenderDistance(settings.effectiveRenderDistance());
        engine.load();

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(3);
        renderer = new GameRenderer(engine, settings);
        glView.setRenderer(renderer);

        overlay = new GameOverlayView(this, engine, settings);
        overlay.setHost(this);

        FrameLayout root = new FrameLayout(this);
        root.addView(glView);
        root.addView(overlay);
        setContentView(root);

        engine.start();
        sounds.startMusic();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) glView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glView != null) glView.onPause();
        if (engine != null && overlay != null && overlay.screen() != GameOverlayView.Screen.DEATH) {
            engine.setPaused(true);
            overlay.showScreen(GameOverlayView.Screen.PAUSE);
            engine.saveRequested();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (renderer != null) renderer.shutdown();
        if (engine != null && stopThread == null) {
            GameEngine e = engine;
            new Thread(e::stop, "FinalSave").start();
        }
        if (sounds != null) sounds.release();
    }

    @Override
    public void onBackPressed() {
        if (overlay.screen() == GameOverlayView.Screen.NONE) {
            engine.setPaused(true);
            overlay.showScreen(GameOverlayView.Screen.PAUSE);
        } else if (overlay.screen() == GameOverlayView.Screen.PAUSE) {
            engine.setPaused(false);
            overlay.showScreen(GameOverlayView.Screen.NONE);
        } else if (overlay.screen() != GameOverlayView.Screen.DEATH) {
            overlay.closeScreen();
        }
    }

    private void tickFpsIntoEngine() {
        engine.rendererFps = renderer.fps;
    }

    // ---------------- engine callbacks (tick thread) ----------------

    @Override
    public void onOpenCrafting(boolean grid3x3) {
        runOnUiThread(() -> overlay.openCrafting(grid3x3));
    }

    @Override
    public void onOpenFurnace(int x, int y, int z) {
        runOnUiThread(() -> {
            World w = engine.renderWorld();
            BlockEntity be = w.getBlockEntity(x, y, z);
            if (be instanceof FurnaceEntity f) overlay.openFurnace(f);
        });
    }

    @Override
    public void onOpenChest(int x, int y, int z) {
        runOnUiThread(() -> {
            World w = engine.renderWorld();
            BlockEntity be = w.getBlockEntity(x, y, z);
            if (be instanceof ChestEntity ch) overlay.openChest(ch);
        });
    }

    @Override
    public void onOpenTrading(Villager villager) {
        runOnUiThread(() -> overlay.openTrading(villager));
    }

    @Override
    public void onDeath() {
        runOnUiThread(() -> overlay.showScreen(GameOverlayView.Screen.DEATH));
    }

    @Override
    public void onAchievement(Achievement a) {
        runOnUiThread(() -> {
            tickFpsIntoEngine();
            overlay.showToast(getString(R.string.achievement_unlocked,
                    getString(achievementTitle(a))));
        });
    }

    @Override
    public void onWorldSaved() {
        runOnUiThread(() -> {
            tickFpsIntoEngine();
            overlay.showToast(getString(R.string.saved));
        });
    }

    // ---------------- overlay host ----------------

    @Override
    public void onResumeGame() {
        // nothing extra; overlay already unpaused the engine
    }

    @Override
    public void onSaveAndQuit() {
        if (stopThread != null) return;
        overlay.setEnabled(false);
        GameEngine e = engine;
        stopThread = new Thread(() -> {
            e.stop();
            runOnUiThread(this::finish);
        }, "SaveQuit");
        stopThread.start();
    }

    static int achievementTitle(Achievement a) {
        return switch (a) {
            case FIRST_BLOCK -> R.string.achievement_first_block;
            case FIRST_CRAFT -> R.string.achievement_first_craft;
            case FIRST_TOOL -> R.string.achievement_first_tool;
            case FIRST_HOUSE -> R.string.achievement_first_house;
            case FIRST_DIAMOND -> R.string.achievement_first_diamond;
            case FIRST_VEHICLE -> R.string.achievement_first_vehicle;
            case FIRST_FLIGHT -> R.string.achievement_first_flight;
            case FIRST_DIMENSION -> R.string.achievement_first_dimension;
        };
    }

    static int achievementDesc(Achievement a) {
        return switch (a) {
            case FIRST_BLOCK -> R.string.achievement_first_block_desc;
            case FIRST_CRAFT -> R.string.achievement_first_craft_desc;
            case FIRST_TOOL -> R.string.achievement_first_tool_desc;
            case FIRST_HOUSE -> R.string.achievement_first_house_desc;
            case FIRST_DIAMOND -> R.string.achievement_first_diamond_desc;
            case FIRST_VEHICLE -> R.string.achievement_first_vehicle_desc;
            case FIRST_FLIGHT -> R.string.achievement_first_flight_desc;
            case FIRST_DIMENSION -> R.string.achievement_first_dimension_desc;
        };
    }
}
