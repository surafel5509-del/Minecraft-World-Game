package com.craftworld3d.game.gl;

/** View-frustum plane extraction and AABB testing (for chunk culling). */
public final class Frustum {
    // 6 planes * (a,b,c,d)
    private final float[] planes = new float[24];

    /** Extracts planes from a column-major view-projection matrix. */
    public void update(float[] vp) {
        // left, right, bottom, top, near, far
        setPlane(0, vp[3] + vp[0], vp[7] + vp[4], vp[11] + vp[8], vp[15] + vp[12]);
        setPlane(1, vp[3] - vp[0], vp[7] - vp[4], vp[11] - vp[8], vp[15] - vp[12]);
        setPlane(2, vp[3] + vp[1], vp[7] + vp[5], vp[11] + vp[9], vp[15] + vp[13]);
        setPlane(3, vp[3] - vp[1], vp[7] - vp[5], vp[11] - vp[9], vp[15] - vp[13]);
        setPlane(4, vp[3] + vp[2], vp[7] + vp[6], vp[11] + vp[10], vp[15] + vp[14]);
        setPlane(5, vp[3] - vp[2], vp[7] - vp[6], vp[11] - vp[10], vp[15] - vp[14]);
    }

    private void setPlane(int i, float a, float b, float c, float d) {
        float len = (float) Math.sqrt(a * a + b * b + c * c);
        if (len < 1e-8f) len = 1;
        int o = i * 4;
        planes[o] = a / len;
        planes[o + 1] = b / len;
        planes[o + 2] = c / len;
        planes[o + 3] = d / len;
    }

    /** True if the axis-aligned box intersects the frustum. */
    public boolean intersectsBox(float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float a = planes[o], b = planes[o + 1], c = planes[o + 2], d = planes[o + 3];
            // pick the box corner most aligned with the plane normal
            float px = a > 0 ? maxX : minX;
            float py = b > 0 ? maxY : minY;
            float pz = c > 0 ? maxZ : minZ;
            if (a * px + b * py + c * pz + d < 0) return false;
        }
        return true;
    }
}
