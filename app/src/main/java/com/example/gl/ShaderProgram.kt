package com.example.gl

import android.opengl.GLES30
import android.util.Log

class WorldShaderProgram {
    val programId: Int

    val uMvpLoc: Int
    val uModelLoc: Int
    val uLightDirLoc: Int
    val uLightColorLoc: Int
    val uAmbientColorLoc: Int
    val uCameraPosLoc: Int
    val uFogColorLoc: Int
    val uFogDensityLoc: Int
    val uHeadlightPosLoc: Int
    val uHeadlightDirLoc: Int
    val uHeadlightIntensityLoc: Int
    val uWetnessLoc: Int
    val uEmissiveLoc: Int
    val uColorTintLoc: Int

    init {
        programId = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uMvpLoc = GLES30.glGetUniformLocation(programId, "uMVP")
        uModelLoc = GLES30.glGetUniformLocation(programId, "uModel")
        uLightDirLoc = GLES30.glGetUniformLocation(programId, "uLightDir")
        uLightColorLoc = GLES30.glGetUniformLocation(programId, "uLightColor")
        uAmbientColorLoc = GLES30.glGetUniformLocation(programId, "uAmbientColor")
        uCameraPosLoc = GLES30.glGetUniformLocation(programId, "uCameraPos")
        uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLoc = GLES30.glGetUniformLocation(programId, "uFogDensity")
        uHeadlightPosLoc = GLES30.glGetUniformLocation(programId, "uHeadlightPos")
        uHeadlightDirLoc = GLES30.glGetUniformLocation(programId, "uHeadlightDir")
        uHeadlightIntensityLoc = GLES30.glGetUniformLocation(programId, "uHeadlightIntensity")
        uWetnessLoc = GLES30.glGetUniformLocation(programId, "uWetness")
        uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
        uColorTintLoc = GLES30.glGetUniformLocation(programId, "uColorTint")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec4 aColor;

            uniform mat4 uMVP;
            uniform mat4 uModel;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec4 vColor;

            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vColor = aColor;
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec4 vColor;

            uniform vec3 uLightDir;
            uniform vec3 uLightColor;
            uniform vec3 uAmbientColor;
            uniform vec3 uCameraPos;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uHeadlightPos;
            uniform vec3 uHeadlightDir;
            uniform float uHeadlightIntensity;
            uniform float uWetness;
            uniform float uEmissive;
            uniform vec4 uColorTint;

            out vec4 fragColor;

            void main() {
                vec4 base = vColor * uColorTint;
                if (base.a < 0.02) discard;

                if (uEmissive > 0.5) {
                    float dist = length(vWorldPos - uCameraPos);
                    float fog = 1.0 - exp(-pow(dist * uFogDensity * 0.65, 2.0));
                    vec3 finalRgb = mix(base.rgb, uFogColor, clamp(fog, 0.0, 0.9));
                    fragColor = vec4(finalRgb, base.a);
                    return;
                }

                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                vec3 V = normalize(uCameraPos - vWorldPos);

                float diff = max(dot(N, L), 0.0);
                vec3 hemiAmbient = mix(uAmbientColor * 0.65, uAmbientColor * 1.15, N.y * 0.5 + 0.5);

                // Blinn-Phong specular (stronger on wet asphalt)
                vec3 H = normalize(L + V);
                float shininess = mix(16.0, 72.0, uWetness);
                float specPower = pow(max(dot(N, H), 0.0), shininess) * mix(0.08, 0.55, uWetness);

                // Truck headlight spotlight cone
                vec3 spotContrib = vec3(0.0);
                if (uHeadlightIntensity > 0.01) {
                    vec3 toFrag = vWorldPos - uHeadlightPos;
                    float distSpot = length(toFrag);
                    if (distSpot < 110.0) {
                        vec3 dirToFrag = toFrag / max(distSpot, 0.001);
                        float cone = dot(dirToFrag, normalize(uHeadlightDir));
                        float spotMask = smoothstep(0.78, 0.96, cone);
                        float atten = 1.0 / (1.0 + 0.018 * distSpot + 0.0012 * distSpot * distSpot);
                        float nDotSpot = max(dot(N, -dirToFrag), 0.15);
                        spotContrib = vec3(1.0, 0.95, 0.80) * uHeadlightIntensity * spotMask * atten * nDotSpot * 2.6;
                    }
                }

                vec3 lit = base.rgb * (hemiAmbient + uLightColor * diff + spotContrib) + uLightColor * specPower;

                // Subtle asphalt micro-grain based on world coordinates when near road height
                float distCam = length(vWorldPos - uCameraPos);
                float fogFactor = clamp(1.0 - exp(-pow(distCam * uFogDensity, 2.0)), 0.0, 1.0);
                vec3 finalRgb = mix(lit, uFogColor, fogFactor);

                fragColor = vec4(finalRgb, base.a);
            }
        """
    }
}

class MirrorQuadShaderProgram {
    val programId: Int
    val uTextureLoc: Int
    val uRectLoc: Int // (x, y, width, height) in NDC [-1, 1]

    init {
        programId = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
        uRectLoc = GLES30.glGetUniformLocation(programId, "uRect")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUv;

            uniform vec4 uRect;
            out vec2 vUv;

            void main() {
                vec2 ndc = uRect.xy + aPos * uRect.zw;
                // Mirror horizontally for realistic side mirror reflection
                vUv = vec2(1.0 - aUv.x, aUv.y);
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;
            in vec2 vUv;
            uniform sampler2D uTexture;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTexture, vUv);
            }
        """
    }
}

private fun compileShader(type: Int, source: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, source)
    GLES30.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES30.glGetShaderInfoLog(shader)
        Log.e("ShaderProgram", "Shader compile error: $log")
        GLES30.glDeleteShader(shader)
        return 0
    }
    return shader
}

private fun linkProgram(vertSrc: String, fragSrc: String): Int {
    val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
    val prog = GLES30.glCreateProgram()
    GLES30.glAttachShader(prog, vs)
    GLES30.glAttachShader(prog, fs)
    GLES30.glLinkProgram(prog)
    val linked = IntArray(1)
    GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linked, 0)
    if (linked[0] == 0) {
        val log = GLES30.glGetProgramInfoLog(prog)
        Log.e("ShaderProgram", "Program link error: $log")
    }
    GLES30.glDeleteShader(vs)
    GLES30.glDeleteShader(fs)
    return prog
}
