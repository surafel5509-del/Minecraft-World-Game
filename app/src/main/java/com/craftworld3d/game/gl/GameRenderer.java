package com.craftworld3d.game.gl;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.Weather;
import com.craftworld3d.core.world.World;
import com.craftworld3d.game.Settings;
import com.craftworld3d.game.engine.EntityView;
import com.craftworld3d.game.engine.GameEngine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 3.0 renderer: chunk meshes (built on a worker thread), frustum
 * culling, fog, day/night sky with sun and moon, entity billboards,
 * precipitation, particles and the block selection outline.
 */
public final class GameRenderer implements GLSurfaceView.Renderer {
    private final GameEngine engine;
    private final Settings settings;
    private final TextureAtlasGL atlas;

    // shader handles
    private int worldProg, colorProg;
    private int uMvp, uCamPos, uTex, uDaylight, uFogColor, uFogStart, uFogEnd, uAlpha;
    private int cMvp, cColor;

    // matrices
    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] vp = new float[16];
    private final Frustum frustum = new Frustum();

    // chunk meshes
    private final Map<Long, ChunkMeshGL> meshes = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<ChunkMesher.MeshData> uploadQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<long[]> meshJobs = new LinkedBlockingQueue<>();
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private Thread mesherThread;
    private volatile boolean running = true;

    private final QuadBatch batch = new QuadBatch();
    public final ParticleSystem particles = new ParticleSystem();

    private final FloatBuffer lineBuf = ByteBuffer.allocateDirect(24 * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int lineVbo = -1;

    private int width = 1, height = 1;
    public volatile int fps;
    private int frames;
    private long fpsWindowStart;
    private long lastFrameNanos;

    // reusable scratch (no allocations per frame)
    private final List<ChunkMeshGL> visible = new ArrayList<>(256);
    private final double[] cam = new double[3];

    public GameRenderer(GameEngine engine, Settings settings) {
        this.engine = engine;
        this.settings = settings;
        this.atlas = new TextureAtlasGL(engine.atlasData);
        engine.setParticles(particles);
    }

    // ---------------- GLSurfaceView.Renderer ----------------

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        worldProg = GLShaders.buildProgram(GLShaders.WORLD_VS, GLShaders.WORLD_FS);
        uMvp = GLES30.glGetUniformLocation(worldProg, "uMvp");
        uCamPos = GLES30.glGetUniformLocation(worldProg, "uCamPos");
        uTex = GLES30.glGetUniformLocation(worldProg, "uTex");
        uDaylight = GLES30.glGetUniformLocation(worldProg, "uDaylight");
        uFogColor = GLES30.glGetUniformLocation(worldProg, "uFogColor");
        uFogStart = GLES30.glGetUniformLocation(worldProg, "uFogStart");
        uFogEnd = GLES30.glGetUniformLocation(worldProg, "uFogEnd");
        uAlpha = GLES30.glGetUniformLocation(worldProg, "uAlpha");

        colorProg = GLShaders.buildProgram(GLShaders.COLOR_VS, GLShaders.COLOR_FS);
        cMvp = GLES30.glGetUniformLocation(colorProg, "uMvp");
        cColor = GLES30.glGetUniformLocation(colorProg, "uColor");

        atlas.uploadToGl();
        batch.createGl();
        int[] id = new int[1];
        GLES30.glGenBuffers(1, id, 0);
        lineVbo = id[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 24 * 3 * 4, null, GLES30.GL_DYNAMIC_DRAW);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);

        // context (re)created: all GPU meshes are gone
        meshes.clear();
        inFlight.clear();
        uploadQueue.clear();
        World w = engine.renderWorld();
        if (w != null) for (Chunk c : w.loadedChunks()) c.dirty = true;

        if (mesherThread == null || !mesherThread.isAlive()) {
            running = true;
            mesherThread = new Thread(this::mesherLoop, "ChunkMesher");
            mesherThread.setDaemon(true);
            mesherThread.start();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastFrameNanos) / 1e9f);
        lastFrameNanos = now;
        frames++;
        if (now - fpsWindowStart > 1_000_000_000L) {
            fps = frames;
            engine.rendererFps = frames;
            frames = 0;
            fpsWindowStart = now;
        }

        World world = engine.renderWorld();
        Player player = engine.player();
        if (world == null || player == null) {
            GLES30.glClearColor(0, 0, 0, 1);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
            return;
        }

        drainUploads();
        scheduleDirtyChunks(world);
        dropUnloadedMeshes(world);

        // ---- camera ----
        float alpha = engine.tickAlpha();
        engine.playerEyePos(alpha, cam);
        // yaw/pitch are degrees; forward = (-sin yaw, 0, cos yaw) like Player.tick
        float yawRad = (float) Math.toRadians(player.yaw);
        float pitchRad = (float) Math.toRadians(player.pitch);
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        if (settings.cameraMode() == Settings.CAMERA_THIRD) {
            cam[0] -= dirX * 3.5;
            cam[1] -= dirY * 3.5 - 0.3;
            cam[2] -= dirZ * 3.5;
        }

        int renderDist = settings.renderDistance();
        float fogEnd = renderDist * 16f - 8f;
        float fogStart = fogEnd * 0.55f;
        float far = fogEnd + 48f;

        Matrix.perspectiveM(proj, 0, 70f, (float) width / height, 0.05f, far);
        Matrix.setLookAtM(view, 0,
                (float) cam[0], (float) cam[1], (float) cam[2],
                (float) (cam[0] + dirX), (float) (cam[1] + dirY), (float) (cam[2] + dirZ),
                0f, 1f, 0f);
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0);
        frustum.update(vp);

        // ---- sky ----
        float daylight = world.daylight();
        boolean depths = world.dimension == World.DIM_DEPTHS;
        float wMul = world.weather.isPrecipitating() ? 0.6f : 1f;
        float skyR, skyG, skyB;
        if (depths) {
            skyR = 0.14f; skyG = 0.03f; skyB = 0.05f;
        } else {
            skyR = lerp(0.015f, 0.47f, daylight) * wMul;
            skyG = lerp(0.02f, 0.66f, daylight) * wMul;
            skyB = lerp(0.07f, 1.0f, daylight) * wMul;
        }
        GLES30.glClearColor(skyR, skyG, skyB, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glUseProgram(worldProg);
        GLES30.glUniformMatrix4fv(uMvp, 1, false, vp, 0);
        GLES30.glUniform3f(uCamPos, (float) cam[0], (float) cam[1], (float) cam[2]);
        GLES30.glUniform1i(uTex, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlas.textureId);
        GLES30.glUniform1f(uDaylight, Math.max(daylight, depths ? 0.35f : 0f));
        GLES30.glUniform3f(uFogColor, skyR, skyG, skyB);
        GLES30.glUniform1f(uAlpha, 1f);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glEnableVertexAttribArray(2);

        // sun & moon (no fog, no depth write)
        if (!depths) {
            GLES30.glUniform1f(uFogStart, 1e6f);
            GLES30.glUniform1f(uFogEnd, 2e6f);
            GLES30.glDepthMask(false);
            GLES30.glDisable(GLES30.GL_CULL_FACE);
            batch.begin();
            drawCelestials(world);
            batch.draw(0, 1, 2);
            GLES30.glEnable(GLES30.GL_CULL_FACE);
            GLES30.glDepthMask(true);
        }

        GLES30.glUniform1f(uFogStart, fogStart);
        GLES30.glUniform1f(uFogEnd, fogEnd);

        // ---- opaque chunk pass ----
        collectVisible(world, renderDist);
        for (int i = 0; i < visible.size(); i++) {
            visible.get(i).drawOpaque(0, 1, 2);
        }

        // ---- entities, particles, precipitation ----
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        batch.begin();
        drawEntities(world, alpha, yawRad);
        if (settings.particles()) {
            particles.update(dt);
            particles.render(batch, atlas, yawRad);
        }
        if (!depths && world.weather.isPrecipitating()) {
            drawPrecipitation(world, daylight);
        }
        if (!batch.isEmpty()) batch.draw(0, 1, 2);
        GLES30.glEnable(GLES30.GL_CULL_FACE);

        // ---- translucent pass (back to front) ----
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glDepthMask(false);
        GLES30.glUniform1f(uAlpha, 0.78f);
        sortVisibleBackToFront();
        for (int i = 0; i < visible.size(); i++) {
            visible.get(i).drawTranslucent(0, 1, 2);
        }
        GLES30.glUniform1f(uAlpha, 1f);
        GLES30.glDepthMask(true);
        GLES30.glDisable(GLES30.GL_BLEND);

        GLES30.glDisableVertexAttribArray(1);
        GLES30.glDisableVertexAttribArray(2);

        // ---- selection outline ----
        int[] aim = engine.aimBlock();
        if (aim != null) drawSelection(aim);
        GLES30.glDisableVertexAttribArray(0);
    }

    // ---------------- chunk mesh management ----------------

    private void mesherLoop() {
        ChunkMesher mesher = new ChunkMesher(atlas);
        while (running) {
            try {
                long[] job = meshJobs.take();
                World world = engine.worldForDimension((int) job[1]);
                if (world == null) continue;
                int cx = (int) (job[0] >> 32);
                int cz = (int) job[0];
                Chunk chunk = world.getLoadedChunk(cx, cz);
                long k = job[0];
                if (chunk != null && chunk.ready) {
                    uploadQueue.offer(mesher.mesh(world, chunk));
                } else {
                    inFlight.remove(k);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                // never kill the mesher thread on a race with unloading
            }
        }
    }

    private void drainUploads() {
        ChunkMesher.MeshData d;
        int budget = 6; // uploads per frame to avoid hitches
        while (budget-- > 0 && (d = uploadQueue.poll()) != null) {
            ChunkMeshGL m = meshes.get(d.key);
            if (m == null) {
                meshes.put(d.key, new ChunkMeshGL(d));
            } else {
                m.upload(d);
            }
            inFlight.remove(d.key);
        }
    }

    private void scheduleDirtyChunks(World world) {
        for (Chunk c : world.loadedChunks()) {
            if (!c.ready) continue;
            long k = ChunkMesher.key(c.chunkX, c.chunkZ);
            if ((c.dirty || !meshes.containsKey(k)) && inFlight.add(k)) {
                c.dirty = false;
                meshJobs.offer(new long[]{k, world.dimension});
            }
        }
    }

    private void dropUnloadedMeshes(World world) {
        Iterator<Map.Entry<Long, ChunkMeshGL>> it = meshes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ChunkMeshGL> e = it.next();
            ChunkMeshGL m = e.getValue();
            if (world.getLoadedChunk(m.chunkX, m.chunkZ) == null) {
                m.delete();
                it.remove();
            }
        }
    }

    private void collectVisible(World world, int renderDist) {
        visible.clear();
        int pcx = ((int) Math.floor(cam[0])) >> 4;
        int pcz = ((int) Math.floor(cam[2])) >> 4;
        for (ChunkMeshGL m : meshes.values()) {
            int dx = m.chunkX - pcx, dz = m.chunkZ - pcz;
            if (dx * dx + dz * dz > renderDist * renderDist) continue;
            float minX = m.chunkX << 4, minZ = m.chunkZ << 4;
            if (frustum.intersectsBox(minX, 0, minZ, minX + 16, Chunk.SIZE_Y, minZ + 16)) {
                visible.add(m);
            }
        }
    }

    private void sortVisibleBackToFront() {
        visible.sort((a, b) -> {
            double da = sqDist(a), db = sqDist(b);
            return Double.compare(db, da);
        });
    }

    private double sqDist(ChunkMeshGL m) {
        double dx = (m.chunkX << 4) + 8 - cam[0];
        double dz = (m.chunkZ << 4) + 8 - cam[2];
        return dx * dx + dz * dz;
    }

    // ---------------- sky, entities, weather ----------------

    private void drawCelestials(World world) {
        double angle = (world.time % World.DAY_LENGTH) / (double) World.DAY_LENGTH * Math.PI * 2 - Math.PI / 2;
        drawCelestial(angle, atlas.data.tileIndex("sun"), 10f, 100f);
        drawCelestial(angle + Math.PI, atlas.data.tileIndex("moon"), 7f, 100f);
    }

    private void drawCelestial(double angle, int tile, float size, float dist) {
        float cx = (float) (cam[0] + Math.cos(angle) * dist);
        float cy = (float) (cam[1] + Math.sin(angle) * dist);
        float cz = (float) cam[2];
        batch.quad(atlas, tile, 1f, 1f,
                cx, cy - size / 2, cz - size / 2,
                cx, cy - size / 2, cz + size / 2,
                cx, cy + size / 2, cz + size / 2,
                cx, cy + size / 2, cz - size / 2);
    }

    private void drawEntities(World world, float alpha, float camYaw) {
        List<EntityView> views = engine.entityViews();
        float daylight = world.daylight();
        for (int i = 0; i < views.size(); i++) {
            EntityView v = views.get(i);
            float x = (float) lerpD(v.px, v.cx, alpha);
            float y = (float) lerpD(v.py, v.cy, alpha);
            float z = (float) lerpD(v.pz, v.cz, alpha);
            int light = world.getLight((int) Math.floor(x), (int) Math.floor(y + 0.5),
                    (int) Math.floor(z), daylight);
            float b = Math.max(0.15f, light / 15f);
            if (v.hurtFlash) b = 1f;
            batch.billboard(atlas, v.tile, b, v.hurtFlash ? 1f : b * 0.9f,
                    x, y, z, v.w, v.h, camYaw);
        }
    }

    private void drawPrecipitation(World world, float daylight) {
        // cold biomes show snowfall: detect via the surface block under the camera
        int camX = (int) Math.floor(cam[0]), camZ = (int) Math.floor(cam[2]);
        int surfY = world.surfaceHeight(camX, camZ);
        int surfId = world.getBlockId(camX, surfY, camZ);
        boolean snow = surfId == Blocks.SNOW_BLOCK.id || surfId == Blocks.ICE.id;
        int tile = atlas.data.tileIndex(snow ? "snow" : "water");
        float speed = snow ? 2.5f : 14f;
        long t = System.nanoTime() / 1_000_000L;
        for (int i = 0; i < 90; i++) {
            int h = i * 0x9E3779B9;
            float ox = ((h & 0xFF) / 255f - 0.5f) * 24f;
            float oz = (((h >> 8) & 0xFF) / 255f - 0.5f) * 24f;
            float phase = ((h >> 16) & 0xFF) / 255f * 20f;
            float fall = (t * speed / 1000f + phase) % 20f;
            float x = (float) cam[0] + ox;
            float z = (float) cam[2] + oz;
            float y = (float) cam[1] + 10f - fall;
            int gx = (int) Math.floor(x), gz = (int) Math.floor(z);
            if (world.surfaceHeight(gx, gz) > y) continue; // sheltered
            float w = snow ? 0.07f : 0.03f;
            float len = snow ? 0.07f : 0.6f;
            batch.billboard(atlas, tile, daylight * 0.9f, 0.2f, x, y, z, w * 8, len, 0);
        }
    }

    private void drawSelection(int[] aim) {
        float x = aim[0], y = aim[1], z = aim[2];
        float e = 0.003f;
        float x0 = x - e, y0 = y - e, z0 = z - e, x1 = x + 1 + e, y1 = y + 1 + e, z1 = z + 1 + e;
        lineBuf.position(0);
        putLine(x0, y0, z0, x1, y0, z0); putLine(x1, y0, z0, x1, y0, z1);
        putLine(x1, y0, z1, x0, y0, z1); putLine(x0, y0, z1, x0, y0, z0);
        putLine(x0, y1, z0, x1, y1, z0); putLine(x1, y1, z0, x1, y1, z1);
        putLine(x1, y1, z1, x0, y1, z1); putLine(x0, y1, z1, x0, y1, z0);
        putLine(x0, y0, z0, x0, y1, z0); putLine(x1, y0, z0, x1, y1, z0);
        putLine(x1, y0, z1, x1, y1, z1); putLine(x0, y0, z1, x0, y1, z1);
        lineBuf.position(0);

        GLES30.glUseProgram(colorProg);
        GLES30.glUniformMatrix4fv(cMvp, 1, false, vp, 0);
        GLES30.glUniform4f(cColor, 0.1f, 0.1f, 0.1f, 0.9f);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, 24 * 3 * 4, lineBuf);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glLineWidth(3f);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 24);
    }

    private void putLine(float ax, float ay, float az, float bx, float by, float bz) {
        lineBuf.put(ax).put(ay).put(az).put(bx).put(by).put(bz);
    }

    public void shutdown() {
        running = false;
        if (mesherThread != null) mesherThread.interrupt();
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static double lerpD(double a, double b, double t) { return a + (b - a) * t; }
}
