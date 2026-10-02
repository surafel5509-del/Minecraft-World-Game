package com.craftworld3d.game.gl;

import android.opengl.GLES30;

/** GLSL ES 3.0 shader programs used by the renderer. */
public final class GLShaders {

    /** World/entity shader: textured, per-vertex sky+block light, distance fog. */
    public static final String WORLD_VS = """
            #version 300 es
            uniform mat4 uMvp;
            uniform vec3 uCamPos;
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec2 aUv;
            layout(location = 2) in vec2 aLight;  // x = sky light * shade, y = block light * shade
            out vec2 vUv;
            out vec2 vLight;
            out float vDist;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vUv = aUv;
                vLight = aLight;
                vDist = length(aPos - uCamPos);
            }
            """;

    public static final String WORLD_FS = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTex;
            uniform float uDaylight;
            uniform vec3 uFogColor;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform float uAlpha;
            in vec2 vUv;
            in vec2 vLight;
            in float vDist;
            out vec4 fragColor;
            void main() {
                vec4 tex = texture(uTex, vUv);
                if (tex.a < 0.1) discard;
                float b = max(vLight.x * uDaylight, vLight.y);
                b = clamp(b, 0.04, 1.0);
                vec3 col = tex.rgb * b;
                float fog = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
                fragColor = vec4(mix(col, uFogColor, fog), tex.a * uAlpha);
            }
            """;

    /** Flat color shader for the block selection outline and simple shapes. */
    public static final String COLOR_VS = """
            #version 300 es
            uniform mat4 uMvp;
            layout(location = 0) in vec3 aPos;
            void main() { gl_Position = uMvp * vec4(aPos, 1.0); }
            """;

    public static final String COLOR_FS = """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() { fragColor = uColor; }
            """;

    private GLShaders() {}

    public static int buildProgram(String vs, String fs) {
        int v = compile(GLES30.GL_VERTEX_SHADER, vs);
        int f = compile(GLES30.GL_FRAGMENT_SHADER, fs);
        int p = GLES30.glCreateProgram();
        GLES30.glAttachShader(p, v);
        GLES30.glAttachShader(p, f);
        GLES30.glLinkProgram(p);
        int[] ok = new int[1];
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(p);
            GLES30.glDeleteProgram(p);
            throw new IllegalStateException("Shader link failed: " + log);
        }
        GLES30.glDeleteShader(v);
        GLES30.glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES30.glCreateShader(type);
        GLES30.glShaderSource(s, src);
        GLES30.glCompileShader(s);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(s);
            GLES30.glDeleteShader(s);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return s;
    }
}
