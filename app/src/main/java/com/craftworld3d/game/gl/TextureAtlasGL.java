package com.craftworld3d.game.gl;

import android.opengl.GLES30;
import android.opengl.GLUtils;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.game.assets.AtlasData;

/**
 * GPU side of the texture atlas: one GL texture plus precomputed tile UVs and
 * per-block face tile indices (top/side/bottom).
 */
public final class TextureAtlasGL {
    public static final int TILES = AtlasData.GRID * AtlasData.GRID;

    public final AtlasData data;
    public int textureId = -1;

    /** uv of each tile's top-left corner, with a small inset against bleeding. */
    public final float[] u0 = new float[TILES];
    public final float[] v0 = new float[TILES];
    public final float tileUv;
    public final float inset;

    public final int[] topTile = new int[BlockRegistry.MAX_BLOCKS];
    public final int[] sideTile = new int[BlockRegistry.MAX_BLOCKS];
    public final int[] bottomTile = new int[BlockRegistry.MAX_BLOCKS];

    public TextureAtlasGL(AtlasData data) {
        this.data = data;
        float grid = AtlasData.GRID;
        inset = 0.5f / data.bitmap.getWidth();
        tileUv = 1f / grid - 2 * inset;
        for (int i = 0; i < TILES; i++) {
            u0[i] = (i % AtlasData.GRID) / grid + inset;
            v0[i] = (i / AtlasData.GRID) / grid + inset;
        }
        for (Block b : BlockRegistry.all()) {
            if (b.id >= BlockRegistry.MAX_BLOCKS) continue;
            topTile[b.id] = data.tileIndex(b.texTop != null ? b.texTop : b.texSide);
            sideTile[b.id] = data.tileIndex(b.texSide);
            bottomTile[b.id] = data.tileIndex(b.texBottom != null ? b.texBottom : b.texSide);
        }
    }

    /** (Re)creates the GL texture. Must run on the GL thread. */
    public void uploadToGl() {
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, data.bitmap, 0);
    }
}
