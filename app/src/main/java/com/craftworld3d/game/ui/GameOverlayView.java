package com.craftworld3d.game.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.craftworld3d.core.crafting.Recipe;
import com.craftworld3d.core.crafting.RecipeRegistry;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.entity.Villager;
import com.craftworld3d.core.entity.vehicle.Vehicle;
import com.craftworld3d.core.entity.vehicle.VehicleType;
import com.craftworld3d.core.entity.DroppedItem;
import com.craftworld3d.core.inventory.Inventory;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.ChestEntity;
import com.craftworld3d.core.world.blockentity.FurnaceEntity;
import com.craftworld3d.game.R;
import com.craftworld3d.game.Settings;
import com.craftworld3d.game.engine.ControlState;
import com.craftworld3d.game.engine.GameEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Touch controls and canvas HUD drawn over the GLSurfaceView:
 * joystick, look area, action buttons, hotbar, health/hunger/stamina bars,
 * plus modal screens (inventory+crafting, furnace, chest, trading, pause,
 * death). All inventory mutations are posted to the tick thread.
 */
public final class GameOverlayView extends View {
    public enum Screen { NONE, INVENTORY, FURNACE, CHEST, TRADING, PAUSE, DEATH }

    public interface Host {
        void onResumeGame();
        void onSaveAndQuit();
    }

    private final GameEngine engine;
    private final Settings settings;
    private final ControlState controls;
    private final Bitmap atlasBmp;
    private final float dp;
    private Host host;

    // paints (allocated once)
    private final Paint fill = new Paint();
    private final Paint stroke = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint();
    private final Rect src = new Rect();
    private final RectF dst = new RectF();

    // screens
    private volatile Screen screen = Screen.NONE;
    private FurnaceEntity furnace;
    private ChestEntity chest;
    private Villager villager;
    private int craftSize = 2;
    private final Inventory craftGrid = new Inventory(9);
    private ItemStack cursor = ItemStack.EMPTY;   // mutated on tick thread only

    // touch state
    private int joyPointer = -1, lookPointer = -1;
    private float joyCx, joyCy, joyX, joyY;
    private float lookLastX, lookLastY;
    private long lookDownTime;
    private float lookDownX, lookDownY;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<SlotHit> slotHits = new ArrayList<>();
    private final List<TradeHit> tradeHits = new ArrayList<>();

    // toasts
    private String toast;
    private long toastUntil;

    private boolean crouchToggle;
    private boolean lightsToggle;

    private static final class Btn {
        final String id;
        final String label;
        final RectF rect = new RectF();
        boolean pressed;
        int pointer = -1;
        Btn(String id, String label) { this.id = id; this.label = label; }
    }

    private static final class SlotHit {
        final RectF rect = new RectF();
        Inventory inv;          // null for furnace special slots
        int slot;               // index, or 0=input 1=fuel 2=output for furnace
        int kind;               // 0 inventory, 1 craft grid, 2 craft result, 3 furnace
    }

    private static final class TradeHit {
        final RectF rect = new RectF();
        Villager.Trade trade;
    }

    public GameOverlayView(Context ctx, GameEngine engine, Settings settings) {
        super(ctx);
        this.engine = engine;
        this.settings = settings;
        this.controls = engine.controls;
        this.atlasBmp = engine.atlasData.bitmap;
        dp = ctx.getResources().getDisplayMetrics().density;
        text.setColor(Color.WHITE);
        text.setShadowLayer(2, 1, 1, Color.BLACK);
        icon.setFilterBitmap(false);
        makeButtons();
    }

    public void setHost(Host h) { host = h; }

    private void makeButtons() {
        buttons.clear();
        for (String[] b : new String[][]{
                {"jump", "⤒"}, {"sneak", "⇩"}, {"sprint", "»"},
                {"break", "⛏"}, {"use", "✛"},
                {"inv", "…"}, {"pause", "II"},
                {"up", "▲"}, {"down", "▼"}, {"horn", "📯"}, {"lights", "💡"}, {"exit", "⏏"}}) {
            buttons.add(new Btn(b[0], b[1]));
        }
    }

    private Btn btn(String id) {
        for (Btn b : buttons) if (b.id.equals(id)) return b;
        return null;
    }

    // ---------------- screens API (called from activity / engine callbacks) ----------------

    public Screen screen() { return screen; }

    public void showScreen(Screen s) {
        screen = s;
        if (s == Screen.NONE) {
            furnace = null; chest = null; villager = null;
        } else {
            releaseWorldControls();
        }
        postInvalidate();
    }

    public void openCrafting(boolean big) {
        craftSize = big ? 3 : 2;
        showScreen(Screen.INVENTORY);
    }

    public void openFurnace(FurnaceEntity f) { furnace = f; showScreen(Screen.FURNACE); }

    public void openChest(ChestEntity c) { chest = c; showScreen(Screen.CHEST); }

    public void openTrading(Villager v) { villager = v; showScreen(Screen.TRADING); }

    public void showToast(String msg) {
        toast = msg;
        toastUntil = System.currentTimeMillis() + 2500;
        postInvalidate();
    }

    /** Returns leftover grid items to the inventory and closes any screen. */
    public void closeScreen() {
        engine.runOnTick(() -> {
            Player p = engine.player();
            for (int i = 0; i < craftGrid.size(); i++) {
                ItemStack s = craftGrid.get(i);
                if (!s.isEmpty()) {
                    p.inventory.add(s);
                    craftGrid.set(i, ItemStack.EMPTY);
                }
            }
            if (!cursor.isEmpty()) {
                p.inventory.add(cursor);
                cursor = ItemStack.EMPTY;
            }
        });
        craftSize = 2;
        showScreen(Screen.NONE);
    }

    private void releaseWorldControls() {
        controls.reset();
        joyPointer = -1;
        lookPointer = -1;
        for (Btn b : buttons) { b.pressed = false; b.pointer = -1; }
        pushVehicleControls();
    }

    // ---------------- drawing ----------------

    @SuppressLint("DefaultLocale")
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (engine.player() == null) return;
        switch (screen) {
            case NONE -> drawHud(c);
            case INVENTORY -> { drawHud(c); drawInventoryScreen(c); }
            case FURNACE -> { drawHud(c); drawFurnaceScreen(c); }
            case CHEST -> { drawHud(c); drawChestScreen(c); }
            case TRADING -> { drawHud(c); drawTradingScreen(c); }
            case PAUSE -> drawPauseScreen(c);
            case DEATH -> drawDeathScreen(c);
        }
        drawToast(c);
        postInvalidateDelayed(33);
    }

    private void drawHud(Canvas c) {
        int w = getWidth(), h = getHeight();
        Player p = engine.player();
        boolean mounted = engine.isMounted();

        // crosshair
        stroke.setColor(0xCCFFFFFF);
        stroke.setStrokeWidth(2 * dp);
        c.drawLine(w / 2f - 10 * dp, h / 2f, w / 2f + 10 * dp, h / 2f, stroke);
        c.drawLine(w / 2f, h / 2f - 10 * dp, w / 2f, h / 2f + 10 * dp, stroke);

        // break progress ring
        float bp = engine.breakProgress;
        if (bp > 0.01f) {
            stroke.setColor(0xDDFFFFFF);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(4 * dp);
            dst.set(w / 2f - 18 * dp, h / 2f - 18 * dp, w / 2f + 18 * dp, h / 2f + 18 * dp);
            c.drawArc(dst, -90, bp * 360, false, stroke);
            stroke.setStyle(Paint.Style.FILL);
        }

        // joystick
        float jr = 70 * dp;
        float jcx = joyPointer >= 0 ? joyCx : 120 * dp;
        float jcy = joyPointer >= 0 ? joyCy : h - 120 * dp;
        fill.setColor(0x33FFFFFF);
        c.drawCircle(jcx, jcy, jr, fill);
        fill.setColor(0x66FFFFFF);
        c.drawCircle(jcx + joyX * jr * 0.6f, jcy + joyY * jr * 0.6f, 28 * dp, fill);

        // buttons
        layoutButtons(w, h, mounted);
        text.setTextAlign(Paint.Align.CENTER);
        for (Btn b : buttons) {
            if (b.rect.isEmpty()) continue;
            fill.setColor(b.pressed ? 0x88FFFFFF : 0x44000000);
            c.drawRoundRect(b.rect, 10 * dp, 10 * dp, fill);
            text.setTextSize(b.rect.height() * 0.42f);
            c.drawText(b.label, b.rect.centerX(),
                    b.rect.centerY() + text.getTextSize() * 0.35f, text);
        }

        // hotbar
        float slot = Math.min(52 * dp, w / 11.5f);
        float hx = w / 2f - slot * 4.5f, hy = h - slot - 8 * dp;
        slotHits.clear();
        Inventory inv = p.inventory;
        for (int i = 0; i < 9; i++) {
            RectF r = new RectF(hx + i * slot, hy, hx + (i + 1) * slot - 3 * dp, hy + slot - 3 * dp);
            fill.setColor(i == inv.selectedSlot ? 0xAAFFFFFF : 0x66000000);
            c.drawRoundRect(r, 6 * dp, 6 * dp, fill);
            drawStack(c, inv.get(i), r);
            SlotHit sh = new SlotHit();
            sh.rect.set(r);
            sh.inv = inv;
            sh.slot = i;
            sh.kind = 0;
            slotHits.add(sh);
        }

        // health / hunger / stamina above hotbar
        if (!mounted && p.gamemode == Player.GAMEMODE_SURVIVAL) {
            drawPips(c, hx, hy - 20 * dp, p.health / 20f, 0xFFE53935);
            drawPips(c, hx + slot * 5f, hy - 20 * dp, p.hunger / 20f, 0xFFFB8C00);
            fill.setColor(0x66000000);
            c.drawRect(hx, hy - 30 * dp, hx + slot * 9 - 3 * dp, hy - 26 * dp, fill);
            fill.setColor(0xFF4CAF50);
            c.drawRect(hx, hy - 30 * dp, hx + (slot * 9 - 3 * dp) * (p.stamina / 20f), hy - 26 * dp, fill);
        }

        // vehicle HUD
        if (mounted) {
            Vehicle v = engine.mountedVehicle();
            if (v != null) drawVehicleHud(c, v, w);
        }

        // FPS + day/time
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(13 * dp);
        if (settings.showFps()) {
            c.drawText("FPS " + engine.rendererFps, 10 * dp, 20 * dp, text);
        }
    }

    private void drawVehicleHud(Canvas c, Vehicle v, int w) {
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(14 * dp);
        float y = 24 * dp;
        c.drawText(String.format("%s  %.0f km/h", v.type.name(), Math.abs(v.speed) * 3.6f),
                w / 2f - 100 * dp, y, text);
        if (v.type.kind == VehicleType.Kind.PLANE || v.type.kind == VehicleType.Kind.HELICOPTER) {
            c.drawText(String.format("ALT %.0f m", v.y - 62), w / 2f + 40 * dp, y, text);
        }
        // fuel + health bars
        if (v.type.needsFuel()) {
            fill.setColor(0x66000000);
            c.drawRect(w / 2f - 100 * dp, y + 6 * dp, w / 2f + 100 * dp, y + 12 * dp, fill);
            fill.setColor(0xFFFFC107);
            c.drawRect(w / 2f - 100 * dp, y + 6 * dp,
                    w / 2f - 100 * dp + 200 * dp * v.fuelFraction(), y + 12 * dp, fill);
        }
        fill.setColor(0x66000000);
        c.drawRect(w / 2f - 100 * dp, y + 14 * dp, w / 2f + 100 * dp, y + 20 * dp, fill);
        fill.setColor(0xFFE53935);
        c.drawRect(w / 2f - 100 * dp, y + 14 * dp,
                w / 2f - 100 * dp + 200 * dp * (v.health / v.type.maxHealth), y + 20 * dp, fill);
    }

    private void drawPips(Canvas c, float x, float y, float frac, int color) {
        for (int i = 0; i < 10; i++) {
            fill.setColor((i + 0.99f) / 10f <= frac ? color : 0x66000000);
            c.drawCircle(x + i * 14 * dp + 6 * dp, y, 5.5f * dp, fill);
        }
    }

    private void layoutButtons(int w, int h, boolean mounted) {
        for (Btn b : buttons) b.rect.setEmpty();
        float s = 56 * dp, gap = 10 * dp;
        float rx = w - s - 14 * dp, by = h - s - 70 * dp;
        if (!mounted) {
            btn("jump").rect.set(rx, by, rx + s, by + s);
            btn("sneak").rect.set(rx - s - gap, by + 20 * dp, rx - gap, by + 20 * dp + s);
            btn("sprint").rect.set(rx - s - gap, by - s - gap + 20 * dp, rx - gap, by - gap + 20 * dp);
            btn("break").rect.set(rx - 0.2f * s, by - s - gap - 0.4f * s, rx + s, by - gap - 0.2f * s);
            btn("use").rect.set(rx - s - gap - 0.4f * s, by - 2.1f * s - 2 * gap,
                    rx + 0.2f * s, by - 1.2f * s - 2 * gap);
        } else {
            Vehicle v = engine.mountedVehicle();
            btn("exit").rect.set(rx, by, rx + s, by + s);
            btn("horn").rect.set(rx - s - gap, by, rx - gap, by + s);
            btn("lights").rect.set(rx - 2 * (s + gap), by, rx - s - 2 * gap, by + s);
            if (v != null && (v.type.kind == VehicleType.Kind.PLANE
                    || v.type.kind == VehicleType.Kind.HELICOPTER)) {
                btn("up").rect.set(rx, by - 2 * (s + gap), rx + s, by - s - 2 * gap);
                btn("down").rect.set(rx, by - s - gap, rx + s, by - gap);
            }
        }
        float ts = 42 * dp;
        btn("pause").rect.set(w - ts - 10 * dp, 10 * dp, w - 10 * dp, 10 * dp + ts);
        btn("inv").rect.set(w - 2 * ts - 20 * dp, 10 * dp, w - ts - 20 * dp, 10 * dp + ts);
    }

    private void drawStack(Canvas c, ItemStack s, RectF r) {
        if (s == null || s.isEmpty()) return;
        int tile = engine.atlasData.iconTileForItem(s.itemId);
        Rect tr = engine.atlasData.tileRect(tile);
        src.set(tr);
        dst.set(r.left + r.width() * 0.12f, r.top + r.height() * 0.12f,
                r.right - r.width() * 0.12f, r.bottom - r.height() * 0.12f);
        c.drawBitmap(atlasBmp, src, dst, icon);
        if (s.count > 1) {
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(r.height() * 0.32f);
            c.drawText(String.valueOf(s.count), r.right - 3 * dp, r.bottom - 3 * dp, text);
        }
        int maxDur = s.item() != null ? s.item().maxDurability() : 0;
        if (maxDur > 0 && s.damage > 0) {
            float frac = 1f - (float) s.damage / maxDur;
            fill.setColor(frac > 0.4f ? 0xFF4CAF50 : 0xFFE53935);
            c.drawRect(r.left + 3 * dp, r.bottom - 5 * dp,
                    r.left + 3 * dp + (r.width() - 6 * dp) * frac, r.bottom - 3 * dp, fill);
        }
    }

    // ---------------- modal screens ----------------

    private RectF panel(Canvas c, float wFrac, float hFrac) {
        int w = getWidth(), h = getHeight();
        fill.setColor(0x99000000);
        c.drawRect(0, 0, w, h, fill);
        float pw = w * wFrac, ph = h * hFrac;
        RectF r = new RectF((w - pw) / 2, (h - ph) / 2, (w + pw) / 2, (h + ph) / 2);
        fill.setColor(0xEE3A3A3A);
        c.drawRoundRect(r, 12 * dp, 12 * dp, fill);
        return r;
    }

    private float slotSize() { return Math.min(52 * dp, getWidth() / 13f); }

    /** Draws the player's 36 slots into the given panel bottom; registers hits. */
    private float drawPlayerInventory(Canvas c, RectF p) {
        float s = slotSize();
        float x0 = p.centerX() - s * 4.5f;
        float y = p.bottom - 4 * (s + 3 * dp) - 8 * dp;
        Inventory inv = engine.player().inventory;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = row == 3 ? col : 9 + row * 9 + col; // hotbar drawn last row
                float yy = y + row * (s + 3 * dp) + (row == 3 ? 6 * dp : 0);
                RectF r = new RectF(x0 + col * (s + 3 * dp), yy, x0 + col * (s + 3 * dp) + s, yy + s);
                fill.setColor(idx == inv.selectedSlot && idx < 9 ? 0x88FFFFFF : 0x66000000);
                c.drawRoundRect(r, 5 * dp, 5 * dp, fill);
                drawStack(c, inv.get(idx), r);
                SlotHit sh = new SlotHit();
                sh.rect.set(r);
                sh.inv = inv;
                sh.slot = idx;
                sh.kind = 0;
                slotHits.add(sh);
            }
        }
        return y;
    }

    private void drawInventoryScreen(Canvas c) {
        slotHits.clear();
        RectF p = panel(c, 0.72f, 0.92f);
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(16 * dp);
        c.drawText(getContext().getString(craftSize == 3 ? R.string.crafting : R.string.inventory),
                p.left + 12 * dp, p.top + 22 * dp, text);

        float s = slotSize();
        float gx = p.centerX() - (craftSize * (s + 3 * dp) + s + 30 * dp) / 2;
        float gy = p.top + 36 * dp;
        for (int row = 0; row < craftSize; row++) {
            for (int col = 0; col < craftSize; col++) {
                RectF r = new RectF(gx + col * (s + 3 * dp), gy + row * (s + 3 * dp),
                        gx + col * (s + 3 * dp) + s, gy + row * (s + 3 * dp) + s);
                fill.setColor(0x66000000);
                c.drawRoundRect(r, 5 * dp, 5 * dp, fill);
                int idx = row * craftSize + col;
                drawStack(c, craftGrid.get(idx), r);
                SlotHit sh = new SlotHit();
                sh.rect.set(r);
                sh.inv = craftGrid;
                sh.slot = idx;
                sh.kind = 1;
                slotHits.add(sh);
            }
        }
        // result slot
        float ry = gy + (craftSize * (s + 3 * dp) - s) / 2;
        RectF rr = new RectF(gx + craftSize * (s + 3 * dp) + 26 * dp, ry,
                gx + craftSize * (s + 3 * dp) + 26 * dp + s, ry + s);
        fill.setColor(0x88333355);
        c.drawRoundRect(rr, 5 * dp, 5 * dp, fill);
        text.setTextSize(s * 0.5f);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText("→", rr.left - 16 * dp, rr.centerY() + 6 * dp, text);
        Recipe match = currentRecipe();
        if (match != null) drawStack(c, match.result(), rr);
        SlotHit res = new SlotHit();
        res.rect.set(rr);
        res.kind = 2;
        slotHits.add(res);

        drawPlayerInventory(c, p);
        drawCursorAndClose(c, p);
    }

    private Recipe currentRecipe() {
        int[] grid = new int[craftSize * craftSize];
        for (int i = 0; i < grid.length; i++) grid[i] = craftGrid.get(i).itemId;
        return RecipeRegistry.match(grid, craftSize);
    }

    private void drawFurnaceScreen(Canvas c) {
        slotHits.clear();
        RectF p = panel(c, 0.72f, 0.92f);
        FurnaceEntity f = furnace;
        if (f == null) return;
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(16 * dp);
        c.drawText(getContext().getString(R.string.furnace), p.left + 12 * dp, p.top + 22 * dp, text);

        float s = slotSize();
        float cx = p.centerX(), ty = p.top + 40 * dp;
        RectF in = new RectF(cx - s * 2.2f, ty, cx - s * 1.2f, ty + s);
        RectF fu = new RectF(cx - s * 2.2f, ty + s * 1.6f, cx - s * 1.2f, ty + s * 2.6f);
        RectF out = new RectF(cx + s * 1.2f, ty + s * 0.8f, cx + s * 2.2f, ty + s * 1.8f);
        for (int i = 0; i < 3; i++) {
            RectF r = i == 0 ? in : i == 1 ? fu : out;
            fill.setColor(0x66000000);
            c.drawRoundRect(r, 5 * dp, 5 * dp, fill);
            SlotHit sh = new SlotHit();
            sh.rect.set(r);
            sh.kind = 3;
            sh.slot = i;
            slotHits.add(sh);
        }
        drawStack(c, f.input, in);
        drawStack(c, f.fuel, fu);
        drawStack(c, f.output, out);

        // flame + progress arrow
        fill.setColor(f.isBurning() ? 0xFFFF7043 : 0x44FFFFFF);
        c.drawRect(fu.left + s * 0.3f, fu.top - s * 0.45f, fu.right - s * 0.3f, fu.top - s * 0.1f, fill);
        float prog = f.cookTicks / (float) FurnaceEntity.SMELT_TICKS;
        fill.setColor(0x66000000);
        c.drawRect(cx - s, ty + s * 1.15f, cx + s, ty + s * 1.45f, fill);
        fill.setColor(0xFFFFC107);
        c.drawRect(cx - s, ty + s * 1.15f, cx - s + 2 * s * Math.min(1, prog), ty + s * 1.45f, fill);

        drawPlayerInventory(c, p);
        drawCursorAndClose(c, p);
    }

    private void drawChestScreen(Canvas c) {
        slotHits.clear();
        RectF p = panel(c, 0.72f, 0.95f);
        ChestEntity ch = chest;
        if (ch == null) return;
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(16 * dp);
        c.drawText(getContext().getString(R.string.chest), p.left + 12 * dp, p.top + 22 * dp, text);
        float s = slotSize();
        float x0 = p.centerX() - s * 4.5f, y0 = p.top + 32 * dp;
        for (int i = 0; i < ChestEntity.SIZE; i++) {
            int row = i / 9, col = i % 9;
            RectF r = new RectF(x0 + col * (s + 3 * dp), y0 + row * (s + 3 * dp),
                    x0 + col * (s + 3 * dp) + s, y0 + row * (s + 3 * dp) + s);
            fill.setColor(0x66000000);
            c.drawRoundRect(r, 5 * dp, 5 * dp, fill);
            drawStack(c, ch.inventory.get(i), r);
            SlotHit sh = new SlotHit();
            sh.rect.set(r);
            sh.inv = ch.inventory;
            sh.slot = i;
            sh.kind = 0;
            slotHits.add(sh);
        }
        drawPlayerInventory(c, p);
        drawCursorAndClose(c, p);
    }

    private void drawTradingScreen(Canvas c) {
        slotHits.clear();
        tradeHits.clear();
        RectF p = panel(c, 0.6f, 0.9f);
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(16 * dp);
        c.drawText(getContext().getString(R.string.trading), p.left + 12 * dp, p.top + 22 * dp, text);
        float s = 34 * dp;
        float y = p.top + 40 * dp;
        for (Villager.Trade t : Villager.TRADES) {
            RectF row = new RectF(p.left + 10 * dp, y, p.right - 10 * dp, y + s + 8 * dp);
            fill.setColor(0x44000000);
            c.drawRoundRect(row, 6 * dp, 6 * dp, fill);
            drawTradeIcon(c, t.costItem(), t.costCount(), row.left + 6 * dp, y + 4 * dp, s);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(s * 0.5f);
            c.drawText("→", row.centerX(), y + s * 0.7f, text);
            drawTradeIcon(c, t.resultItem(), t.resultCount(), row.centerX() + 20 * dp, y + 4 * dp, s);
            TradeHit th = new TradeHit();
            th.rect.set(row);
            th.trade = t;
            tradeHits.add(th);
            y += s + 14 * dp;
        }
        drawCursorAndClose(c, p);
    }

    private void drawTradeIcon(Canvas c, int itemId, int count, float x, float y, float s) {
        Rect tr = engine.atlasData.tileRect(engine.atlasData.iconTileForItem(itemId));
        src.set(tr);
        dst.set(x, y, x + s, y + s);
        c.drawBitmap(atlasBmp, src, dst, icon);
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(s * 0.45f);
        c.drawText("x" + count, x + s + 3 * dp, y + s * 0.75f, text);
    }

    private final RectF closeRect = new RectF();
    private final RectF dropRect = new RectF();

    private void drawCursorAndClose(Canvas c, RectF p) {
        // close button
        closeRect.set(p.right - 34 * dp, p.top + 6 * dp, p.right - 6 * dp, p.top + 34 * dp);
        fill.setColor(0x88AA3333);
        c.drawRoundRect(closeRect, 6 * dp, 6 * dp, fill);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16 * dp);
        c.drawText("✕", closeRect.centerX(), closeRect.centerY() + 6 * dp, text);

        // drop button
        dropRect.set(p.left + 6 * dp, p.top + 6 * dp, p.left + 70 * dp, p.top + 34 * dp);
        fill.setColor(0x88555555);
        c.drawRoundRect(dropRect, 6 * dp, 6 * dp, fill);
        text.setTextSize(12 * dp);
        c.drawText("Drop", dropRect.centerX(), dropRect.centerY() + 4 * dp, text);

        // cursor stack follows last touch
        ItemStack cur = cursor;
        if (!cur.isEmpty()) {
            float s = slotSize();
            RectF r = new RectF(lookLastX - s / 2, lookLastY - s / 2, lookLastX + s / 2, lookLastY + s / 2);
            drawStack(c, cur, r);
        }
    }

    private void drawPauseScreen(Canvas c) {
        RectF p = panel(c, 0.45f, 0.6f);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(20 * dp);
        c.drawText(getContext().getString(R.string.paused), p.centerX(), p.top + 36 * dp, text);
        drawMenuButton(c, p, 0, getContext().getString(R.string.pause_resume));
        drawMenuButton(c, p, 1, getContext().getString(R.string.pause_save_quit));
    }

    private void drawDeathScreen(Canvas c) {
        int w = getWidth(), h = getHeight();
        fill.setColor(0xAA660000);
        c.drawRect(0, 0, w, h, fill);
        RectF p = new RectF(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.75f);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(24 * dp);
        c.drawText(getContext().getString(R.string.you_died), w / 2f, p.top + 20 * dp, text);
        drawMenuButton(c, p, 1, getContext().getString(R.string.respawn));
    }

    private final RectF[] menuRects = {new RectF(), new RectF()};

    private void drawMenuButton(Canvas c, RectF p, int index, String label) {
        float bw = p.width() * 0.8f, bh = 46 * dp;
        float x = p.centerX() - bw / 2;
        float y = p.top + 60 * dp + index * (bh + 16 * dp);
        menuRects[index].set(x, y, x + bw, y + bh);
        fill.setColor(0x885577AA);
        c.drawRoundRect(menuRects[index], 8 * dp, 8 * dp, fill);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(15 * dp);
        c.drawText(label, p.centerX(), y + bh / 2 + 5 * dp, text);
    }

    private void drawToast(Canvas c) {
        if (toast == null || System.currentTimeMillis() > toastUntil) return;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(14 * dp);
        fill.setColor(0xAA222222);
        float tw = text.measureText(toast) + 30 * dp;
        RectF r = new RectF(getWidth() / 2f - tw / 2, 54 * dp, getWidth() / 2f + tw / 2, 86 * dp);
        c.drawRoundRect(r, 8 * dp, 8 * dp, fill);
        c.drawText(toast, getWidth() / 2f, 75 * dp, text);
    }

    // ---------------- touch handling ----------------

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        int index = ev.getActionIndex();
        int id = ev.getPointerId(index);
        float x = ev.getX(index), y = ev.getY(index);

        if (screen == Screen.PAUSE || screen == Screen.DEATH) {
            if (action == MotionEvent.ACTION_DOWN) handleMenuTap(x, y);
            return true;
        }
        if (screen != Screen.NONE) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                lookLastX = x; lookLastY = y;
                handleScreenTap(x, y);
            } else if (action == MotionEvent.ACTION_MOVE) {
                lookLastX = ev.getX(0);
                lookLastY = ev.getY(0);
            }
            postInvalidate();
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(id, x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++) {
                    onPointerMove(ev.getPointerId(i), ev.getX(i), ev.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                onPointerUp(id, x, y);
                break;
        }
        return true;
    }

    private void onPointerDown(int id, float x, float y) {
        // hotbar?
        for (SlotHit sh : slotHits) {
            if (sh.kind == 0 && sh.inv == engine.player().inventory && sh.slot < 9
                    && sh.rect.contains(x, y)) {
                engine.selectHotbarSlot(sh.slot);
                return;
            }
        }
        // buttons?
        for (Btn b : buttons) {
            if (!b.rect.isEmpty() && b.rect.contains(x, y)) {
                b.pressed = true;
                b.pointer = id;
                onButtonDown(b);
                return;
            }
        }
        // joystick zone: left 40% of screen
        if (x < getWidth() * 0.4f && joyPointer < 0) {
            joyPointer = id;
            joyCx = x;
            joyCy = y;
            joyX = joyY = 0;
            return;
        }
        // look area
        if (lookPointer < 0) {
            lookPointer = id;
            lookLastX = x;
            lookLastY = y;
            lookDownTime = System.currentTimeMillis();
            lookDownX = x;
            lookDownY = y;
        }
    }

    private void onPointerMove(int id, float x, float y) {
        if (id == joyPointer) {
            float r = 70 * dp;
            joyX = clamp((x - joyCx) / r, -1, 1);
            joyY = clamp((y - joyCy) / r, -1, 1);
            if (!engine.isMounted()) {
                controls.moveX = joyX;
                controls.moveZ = -joyY;
                // push past the edge to sprint
                controls.sprint = btnPressed("sprint") || (joyY < -0.95f);
            } else {
                pushVehicleControls();
            }
        } else if (id == lookPointer) {
            float sens = 0.22f * settings.sensitivity();
            engine.addLook(-(x - lookLastX) * sens, -(y - lookLastY) * sens);
            lookLastX = x;
            lookLastY = y;
        }
    }

    private void onPointerUp(int id, float x, float y) {
        if (id == joyPointer) {
            joyPointer = -1;
            joyX = joyY = 0;
            controls.moveX = controls.moveZ = 0;
            if (!btnPressed("sprint")) controls.sprint = false;
            pushVehicleControls();
            return;
        }
        if (id == lookPointer) {
            lookPointer = -1;
            // short tap on the look area = use/interact
            if (System.currentTimeMillis() - lookDownTime < 220
                    && Math.hypot(x - lookDownX, y - lookDownY) < 14 * dp) {
                engine.useTapped();
            }
            return;
        }
        for (Btn b : buttons) {
            if (b.pointer == id) {
                b.pressed = false;
                b.pointer = -1;
                onButtonUp(b);
            }
        }
    }

    private boolean btnPressed(String id) {
        Btn b = btn(id);
        return b != null && b.pressed;
    }

    private void onButtonDown(Btn b) {
        switch (b.id) {
            case "jump" -> controls.jump = true;
            case "sprint" -> controls.sprint = true;
            case "sneak" -> {
                crouchToggle = !crouchToggle;
                controls.crouch = crouchToggle;
            }
            case "break" -> {
                controls.breaking = true;
                engine.attackTapped();
            }
            case "use" -> engine.useTapped();
            case "inv" -> {
                craftSize = 2;
                showScreen(Screen.INVENTORY);
            }
            case "pause" -> {
                engine.setPaused(true);
                showScreen(Screen.PAUSE);
            }
            case "exit" -> engine.dismountRequested();
            case "horn" -> controls.horn = true;
            case "lights" -> {
                lightsToggle = !lightsToggle;
                controls.headlights = lightsToggle;
            }
            case "up", "down" -> pushVehicleControls();
        }
        pushVehicleControls();
    }

    private void onButtonUp(Btn b) {
        switch (b.id) {
            case "jump" -> controls.jump = false;
            case "sprint" -> controls.sprint = joyY < -0.95f;
            case "break" -> controls.breaking = false;
            case "horn" -> controls.horn = false;
        }
        pushVehicleControls();
    }

    /** Maps joystick + up/down buttons onto the mounted vehicle's controls. */
    private void pushVehicleControls() {
        if (!engine.isMounted()) return;
        Vehicle v = engine.mountedVehicle();
        if (v == null) return;
        float up = btnPressed("up") ? 1 : 0;
        float down = btnPressed("down") ? 1 : 0;
        switch (v.type.kind) {
            case PLANE -> {
                controls.vehicleThrottle = Math.max(0, -joyY);
                controls.vehicleSteer = joyX;
                controls.vehiclePitch = down - up;   // negative pitch climbs
            }
            case HELICOPTER -> {
                controls.vehicleThrottle = up - down;
                controls.vehicleSteer = joyX;
                controls.vehiclePitch = joyY;        // joystick up = forward
            }
            default -> {
                controls.vehicleThrottle = -joyY;
                controls.vehicleSteer = joyX;
                controls.vehiclePitch = 0;
            }
        }
    }

    private void handleMenuTap(float x, float y) {
        if (screen == Screen.PAUSE) {
            if (menuRects[0].contains(x, y)) {
                engine.setPaused(false);
                showScreen(Screen.NONE);
                if (host != null) host.onResumeGame();
            } else if (menuRects[1].contains(x, y)) {
                if (host != null) host.onSaveAndQuit();
            }
        } else if (screen == Screen.DEATH) {
            if (menuRects[1].contains(x, y)) {
                engine.respawnRequested();
                showScreen(Screen.NONE);
            }
        }
    }

    private void handleScreenTap(float x, float y) {
        if (closeRect.contains(x, y)) {
            closeScreen();
            return;
        }
        if (dropRect.contains(x, y)) {
            engine.runOnTick(this::dropCursorOrSelected);
            return;
        }
        for (TradeHit th : tradeHits) {
            if (th.rect.contains(x, y)) {
                Villager.Trade t = th.trade;
                engine.runOnTick(() -> {
                    if (Villager.doTrade(engine.player(), t)) {
                        engine.onItemCrafted(new ItemStack(t.resultItem(), t.resultCount()));
                    }
                });
                return;
            }
        }
        for (SlotHit sh : slotHits) {
            if (sh.rect.contains(x, y)) {
                final SlotHit hit = sh;
                engine.runOnTick(() -> clickSlot(hit));
                return;
            }
        }
    }

    /** Runs on the tick thread. */
    private void clickSlot(SlotHit sh) {
        switch (sh.kind) {
            case 0, 1 -> cursor = sh.inv.clickSlot(sh.slot, cursor);
            case 2 -> takeCraftResult();
            case 3 -> clickFurnaceSlot(sh.slot);
        }
    }

    private void takeCraftResult() {
        Recipe r = currentRecipe();
        if (r == null) return;
        ItemStack result = r.result().copy();
        if (!cursor.isEmpty()) {
            if (!cursor.canStackWith(result)
                    || cursor.count + result.count > cursor.maxStack()) return;
            cursor.count += result.count;
        } else {
            cursor = result;
        }
        for (int i = 0; i < craftSize * craftSize; i++) {
            if (!craftGrid.get(i).isEmpty()) craftGrid.shrink(i);
        }
        engine.onItemCrafted(result);
    }

    private void clickFurnaceSlot(int which) {
        FurnaceEntity f = furnace;
        if (f == null) return;
        switch (which) {
            case 0 -> { ItemStack t = f.input; f.input = cursor; cursor = t == null ? ItemStack.EMPTY : t; }
            case 1 -> { ItemStack t = f.fuel; f.fuel = cursor; cursor = t == null ? ItemStack.EMPTY : t; }
            case 2 -> {
                if (f.output != null && !f.output.isEmpty() && cursor.isEmpty()) {
                    cursor = f.output;
                    f.output = ItemStack.EMPTY;
                }
            }
        }
    }

    private void dropCursorOrSelected() {
        Player p = engine.player();
        World w = engine.renderWorld();
        ItemStack toDrop;
        if (!cursor.isEmpty()) {
            toDrop = cursor;
            cursor = ItemStack.EMPTY;
        } else {
            toDrop = p.inventory.selected();
            if (toDrop.isEmpty()) return;
            p.inventory.set(p.inventory.selectedSlot, ItemStack.EMPTY);
        }
        double yawRad = Math.toRadians(p.yaw);
        DroppedItem di = new DroppedItem(toDrop,
                p.x - Math.sin(yawRad) * 1.2, p.eyeY() - 0.3, p.z + Math.cos(yawRad) * 1.2);
        di.vx = -Math.sin(yawRad) * 4;
        di.vz = Math.cos(yawRad) * 4;
        w.addEntity(di);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
