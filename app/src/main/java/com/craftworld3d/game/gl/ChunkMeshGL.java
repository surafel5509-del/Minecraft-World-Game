package com.craftworld3d.game.gl;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** GPU buffers for one chunk: an opaque VBO and a translucent VBO. */
public final class ChunkMeshGL {
    public final int chunkX, chunkZ;
    private int vboOpaque = -1, vboTranslucent = -1;
    public int opaqueVerts, translucentVerts;

    public ChunkMeshGL(ChunkMesher.MeshData data) {
        chunkX = data.chunkX;
        chunkZ = data.chunkZ;
        upload(data);
    }

    public void upload(ChunkMesher.MeshData data) {
        delete();
        opaqueVerts = data.opaqueVerts;
        translucentVerts = data.translucentVerts;
        if (opaqueVerts > 0) vboOpaque = makeVbo(data.opaque, data.opaqueVerts);
        if (translucentVerts > 0) vboTranslucent = makeVbo(data.translucent, data.translucentVerts);
    }

    private static int makeVbo(float[] verts, int count) {
        int bytes = count * ChunkMesher.FLOATS_PER_VERT * 4;
        FloatBuffer buf = ByteBuffer.allocateDirect(bytes)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(verts, 0, count * ChunkMesher.FLOATS_PER_VERT).position(0);
        int[] id = new int[1];
        GLES30.glGenBuffers(1, id, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, id[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bytes, buf, GLES30.GL_STATIC_DRAW);
        return id[0];
    }

    public void drawOpaque(int posLoc, int uvLoc, int lightLoc) {
        if (opaqueVerts > 0) draw(vboOpaque, opaqueVerts, posLoc, uvLoc, lightLoc);
    }

    public void drawTranslucent(int posLoc, int uvLoc, int lightLoc) {
        if (translucentVerts > 0) draw(vboTranslucent, translucentVerts, posLoc, uvLoc, lightLoc);
    }

    private static void draw(int vbo, int verts, int posLoc, int uvLoc, int lightLoc) {
        int stride = ChunkMesher.FLOATS_PER_VERT * 4;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glVertexAttribPointer(uvLoc, 2, GLES30.GL_FLOAT, false, stride, 12);
        GLES30.glVertexAttribPointer(lightLoc, 2, GLES30.GL_FLOAT, false, stride, 20);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, verts);
    }

    public void delete() {
        if (vboOpaque >= 0) {
            GLES30.glDeleteBuffers(1, new int[]{vboOpaque}, 0);
            vboOpaque = -1;
        }
        if (vboTranslucent >= 0) {
            GLES30.glDeleteBuffers(1, new int[]{vboTranslucent}, 0);
            vboTranslucent = -1;
        }
    }
}
