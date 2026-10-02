package com.craftworld3d.game;

import android.content.Context;

import java.io.File;

/** Local storage layout: &lt;app files&gt;/CraftWorld3D/worlds/&lt;world_id&gt;/ */
public final class AppPaths {
    private AppPaths() {}

    public static File worldsRoot(Context ctx) {
        File base = ctx.getExternalFilesDir(null);
        if (base == null) base = ctx.getFilesDir();
        File root = new File(base, "CraftWorld3D/worlds");
        //noinspection ResultOfMethodCallIgnored
        root.mkdirs();
        return root;
    }

    public static File worldDir(Context ctx, String id) {
        return new File(worldsRoot(ctx), id);
    }

    public static void deleteRecursive(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursive(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
