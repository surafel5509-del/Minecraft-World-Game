package com.craftworld3d.core.save;

import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.BlockEntity;
import com.craftworld3d.core.world.blockentity.ChestEntity;
import com.craftworld3d.core.world.blockentity.FurnaceEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Local world persistence under worlds/<world_id>/:
 *
 *   level.json          seed, player, time, weather, entities, metadata
 *   stats.json          statistics
 *   achievements.json   achievements
 *   chunks/<dim>/c.X.Z.dat   RLE+gzip chunk blocks and block entities
 *
 * All writes are atomic (tmp file + rename), keep a .bak backup of the
 * previous version, and chunk payloads carry a CRC32 so corrupted files are
 * detected and recovered from backup (or regenerated from the seed).
 */
public final class WorldStorage {
    private static final int MAGIC = 0x43573344; // "CW3D"
    private static final byte VERSION = 1;

    private final File worldDir;

    public WorldStorage(File worldDir) {
        this.worldDir = worldDir;
        //noinspection ResultOfMethodCallIgnored
        worldDir.mkdirs();
    }

    public File dir() { return worldDir; }

    // ---------------- Chunks ----------------

    private File chunkFile(int dimension, int cx, int cz) {
        File dimDir = new File(worldDir, "chunks/" + dimension);
        //noinspection ResultOfMethodCallIgnored
        dimDir.mkdirs();
        return new File(dimDir, "c." + cx + "." + cz + ".dat");
    }

    public boolean chunkExists(int dimension, int cx, int cz) {
        return chunkFile(dimension, cx, cz).isFile();
    }

    public void saveChunk(World world, Chunk chunk) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(16384);
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw))) {
            out.writeInt(chunk.chunkX);
            out.writeInt(chunk.chunkZ);
            writeRle(out, chunk.rawBlocks());
            writeBlockEntities(out, world, chunk);
        }
        byte[] payload = raw.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(payload);

        File target = chunkFile(world.dimension, chunk.chunkX, chunk.chunkZ);
        File tmp = new File(target.getPath() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmp))) {
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            out.writeInt((int) crc.getValue());
            out.writeInt(payload.length);
            out.write(payload);
        }
        atomicReplace(tmp, target);
        chunk.modified = false;
    }

    /** Loads a chunk; returns null when missing or unrecoverably corrupted. */
    public Chunk loadChunk(World world, int cx, int cz) {
        File target = chunkFile(world.dimension, cx, cz);
        Chunk chunk = tryLoadChunkFile(world, target, cx, cz);
        if (chunk == null) {
            File bak = new File(target.getPath() + ".bak");
            chunk = tryLoadChunkFile(world, bak, cx, cz);
        }
        return chunk;
    }

    private Chunk tryLoadChunkFile(World world, File file, int cx, int cz) {
        if (!file.isFile()) return null;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (in.readInt() != MAGIC) return null;
            byte version = in.readByte();
            if (version != VERSION) return null;
            int expectedCrc = in.readInt();
            int len = in.readInt();
            if (len <= 0 || len > 1 << 24) return null;
            byte[] payload = in.readNBytes(len);
            if (payload.length != len) return null;
            CRC32 crc = new CRC32();
            crc.update(payload);
            if ((int) crc.getValue() != expectedCrc) return null;

            try (DataInputStream data = new DataInputStream(
                new GZIPInputStream(new ByteArrayInputStream(payload)))) {
                int fileCx = data.readInt();
                int fileCz = data.readInt();
                if (fileCx != cx || fileCz != cz) return null;
                Chunk chunk = new Chunk(cx, cz);
                readRle(data, chunk.rawBlocks());
                readBlockEntities(data, world);
                chunk.recalcHeightMap();
                chunk.recalcLight();
                chunk.ready = true;
                chunk.modified = false;
                return chunk;
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void writeRle(DataOutputStream out, short[] blocks) throws IOException {
        // Count runs.
        int runs = 0;
        for (int i = 0; i < blocks.length; ) {
            int j = i;
            while (j < blocks.length && blocks[j] == blocks[i]) j++;
            runs++;
            i = j;
        }
        out.writeInt(runs);
        for (int i = 0; i < blocks.length; ) {
            int j = i;
            while (j < blocks.length && blocks[j] == blocks[i]) j++;
            out.writeShort(blocks[i]);
            out.writeInt(j - i);
            i = j;
        }
    }

    private static void readRle(DataInputStream in, short[] blocks) throws IOException {
        int runs = in.readInt();
        int pos = 0;
        for (int r = 0; r < runs; r++) {
            short id = in.readShort();
            int len = in.readInt();
            if (len < 0 || pos + len > blocks.length) throw new IOException("Bad RLE data");
            for (int i = 0; i < len; i++) blocks[pos++] = id;
        }
        if (pos != blocks.length) throw new IOException("Incomplete RLE data");
    }

    private void writeBlockEntities(DataOutputStream out, World world, Chunk chunk) throws IOException {
        List<BlockEntity> list = new ArrayList<>();
        for (BlockEntity be : world.blockEntities()) {
            if ((be.x >> 4) == chunk.chunkX && (be.z >> 4) == chunk.chunkZ) list.add(be);
        }
        out.writeInt(list.size());
        for (BlockEntity be : list) {
            out.writeInt(be.x);
            out.writeInt(be.y);
            out.writeInt(be.z);
            out.writeUTF(be.type());
            byte[] json = MiniJson.write(be.saveData()).getBytes(StandardCharsets.UTF_8);
            out.writeInt(json.length);
            out.write(json);
        }
    }

    private void readBlockEntities(DataInputStream in, World world) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 65536) throw new IOException("Bad block entity count");
        for (int i = 0; i < count; i++) {
            int x = in.readInt(), y = in.readInt(), z = in.readInt();
            String type = in.readUTF();
            int len = in.readInt();
            if (len < 0 || len > 1 << 20) throw new IOException("Bad block entity data");
            String json = new String(in.readNBytes(len), StandardCharsets.UTF_8);
            BlockEntity be = switch (type) {
                case "chest" -> new ChestEntity(x, y, z);
                case "furnace" -> new FurnaceEntity(x, y, z);
                default -> null;
            };
            if (be != null) {
                try {
                    be.loadData(MiniJson.parseObject(json));
                    world.setBlockEntity(be);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    // ---------------- JSON files (level, stats, achievements) ----------------

    public void saveJson(String name, Map<String, Object> data) throws IOException {
        File target = new File(worldDir, name);
        File tmp = new File(target.getPath() + ".tmp");
        byte[] bytes = MiniJson.write(data).getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(bytes);
            out.getFD().sync();
        }
        atomicReplace(tmp, target);
    }

    /** Loads a JSON file; falls back to .bak if the main file is corrupted. */
    public Map<String, Object> loadJson(String name) {
        File target = new File(worldDir, name);
        Map<String, Object> m = tryLoadJson(target);
        if (m == null) m = tryLoadJson(new File(target.getPath() + ".bak"));
        return m;
    }

    private Map<String, Object> tryLoadJson(File file) {
        if (!file.isFile()) return null;
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return MiniJson.parseObject(text);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** tmp -> target, keeping the previous target as .bak. */
    private static void atomicReplace(File tmp, File target) throws IOException {
        if (target.isFile()) {
            File bak = new File(target.getPath() + ".bak");
            Files.copy(target.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------------- World listing ----------------

    public record WorldInfo(String id, String name, long seed, int gamemode,
                            long lastPlayed, long playTimeMs, String worldType) {}

    /** Lists worlds under the given root (reads each level.json). */
    public static List<WorldInfo> listWorlds(File worldsRoot) {
        List<WorldInfo> out = new ArrayList<>();
        File[] dirs = worldsRoot.listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File dir : dirs) {
            WorldStorage storage = new WorldStorage(dir);
            Map<String, Object> level = storage.loadJson("level.json");
            if (level == null) continue;
            out.add(new WorldInfo(
                dir.getName(),
                MiniJson.getString(level, "name", dir.getName()),
                MiniJson.getLong(level, "seed", 0),
                MiniJson.getInt(level, "gamemode", 0),
                MiniJson.getLong(level, "lastPlayed", 0),
                MiniJson.getLong(level, "playTimeMs", 0),
                MiniJson.getString(level, "worldType", "default")));
        }
        out.sort((a, b) -> Long.compare(b.lastPlayed(), a.lastPlayed()));
        return out;
    }
}
