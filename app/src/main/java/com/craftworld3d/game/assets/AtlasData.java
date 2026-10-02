package com.craftworld3d.game.assets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.core.block.RenderShape;
import com.craftworld3d.core.item.Item;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.util.MiniJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the bundled texture atlas (assets/textures/atlas.png) and its
 * name-to-tile-index table (tiles.json). Shared by the GL renderer (UV lookup)
 * and the canvas HUD (item icons). All assets are local; nothing is downloaded.
 */
public final class AtlasData {
    public static final int GRID = 16;        // 16x16 tiles in the atlas
    public final Bitmap bitmap;
    public final int tilePx;
    private final Map<String, Integer> tiles = new HashMap<>();
    private final int missingTile;

    public AtlasData(Context ctx) {
        try {
            try (InputStream in = ctx.getAssets().open("textures/atlas.png")) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inScaled = false;
                bitmap = BitmapFactory.decodeStream(in, null, o);
            }
            try (InputStream in = ctx.getAssets().open("textures/tiles.json")) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                Map<String, Object> m = MiniJson.parseObject(
                        new String(bos.toByteArray(), StandardCharsets.UTF_8));
                for (Map.Entry<String, Object> e : m.entrySet()) {
                    tiles.put(e.getKey(), (int) ((Number) e.getValue()).doubleValue());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled texture atlas", e);
        }
        tilePx = bitmap.getWidth() / GRID;
        Integer missing = tiles.get("missing");
        missingTile = missing != null ? missing : 0;
    }

    public int tileIndex(String name) {
        Integer i = tiles.get(name);
        return i != null ? i : missingTile;
    }

    public Rect tileRect(int index) {
        int tx = (index % GRID) * tilePx;
        int ty = (index / GRID) * tilePx;
        return new Rect(tx, ty, tx + tilePx, ty + tilePx);
    }

    /** Tile used as the 2D inventory icon for an item id. */
    public int iconTileForItem(int itemId) {
        Item item = ItemRegistry.get(itemId);
        if (item != null && item.icon != null && tiles.containsKey(item.icon)) {
            return tiles.get(item.icon);
        }
        if (itemId > 0 && itemId < BlockRegistry.MAX_BLOCKS) {
            Block b = BlockRegistry.get(itemId);
            if (b != null) {
                String tex = b.shape == RenderShape.CUBE ? b.texSide : b.texTop;
                if (tex == null) tex = b.texSide;
                if (tex != null && tiles.containsKey(tex)) return tiles.get(tex);
            }
        }
        return missingTile;
    }
}
