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
    val uMaterialModeLoc: Int // 0=standard, 1=metallic paint, 2=chrome, 3=asphalt road, 4=glass

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
        uMaterialModeLoc = GLES30.glGetUniformLocation(programId, "uMaterialMode")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    companion object {
        const val MAT_STANDARD = 0f
        const val MAT_METALLIC_PAINT = 1f
        const val MAT_CHROME = 2f
        const val MAT_ROAD = 3f
        const val MAT_GLASS = 4f

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
            uniform float uMaterialMode;

            out vec4 fragColor;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            void main() {
                vec4 base = vColor * uColorTint;
                if (base.a < 0.02) discard;

                // Sky dome / Emissive elements
                if (uEmissive > 1.5) {
                    // Sky dome vertical atmospheric gradient
                    float h = clamp((vWorldPos.y - uCameraPos.y + 40.0) / 360.0, 0.0, 1.0);
                    vec3 zenith = uFogColor * vec3(0.68, 0.78, 0.96);
                    vec3 horizon = mix(uFogColor, uLightColor * 0.65 + uFogColor * 0.55, 0.25);
                    vec3 skyGrad = mix(horizon, zenith, pow(h, 0.7));
                    fragColor = vec4(skyGrad, 1.0);
                    return;
                }

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
                vec3 H = normalize(L + V);

                // Procedural asphalt micro-texture & wet puddle specular
                float localWet = uWetness;
                if (uMaterialMode > 2.5 && uMaterialMode < 3.5) {
                    float grain = hash21(floor(vWorldPos.xz * 2.5)) * 0.06 - 0.03;
                    base.rgb = clamp(base.rgb + vec3(grain), 0.02, 1.0);
                    if (uWetness > 0.1) {
                        float puddle = smoothstep(0.45, 0.85, sin(vWorldPos.x * 0.22) * cos(vWorldPos.z * 0.19) * 0.5 + 0.5);
                        localWet = mix(0.45, 1.0, puddle) * uWetness;
                    }
                }

                float diff = max(dot(N, L), 0.0);
                vec3 hemiAmbient = mix(uAmbientColor * 0.62, uAmbientColor * 1.18, N.y * 0.5 + 0.5);

                // Material-specific PBR-inspired specular & reflection
                float shininess = mix(18.0, 84.0, localWet);
                float specStrength = mix(0.08, 0.55, localWet);
                vec3 envReflect = vec3(0.0);

                if (uMaterialMode > 0.5 && uMaterialMode < 1.5) {
                    // Metallic clearcoat truck paint
                    shininess = 64.0;
                    specStrength = 0.52;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(uAmbientColor * 0.5, uFogColor * 1.15, clamp(R.y * 0.5 + 0.5, 0.0, 1.0));
                    envReflect = skyEnv * fresnel * 0.42;
                } else if (uMaterialMode > 1.5 && uMaterialMode < 2.5) {
                    // Polished Chrome
                    shininess = 128.0;
                    specStrength = 1.15;
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(vec3(0.18, 0.20, 0.24), uFogColor * 1.25 + uLightColor * 0.35, smoothstep(-0.15, 0.45, R.y));
                    envReflect = skyEnv * 0.58;
                    base.rgb *= 0.65;
                } else if (uMaterialMode > 3.5) {
                    // Automotive tinted glass
                    shininess = 110.0;
                    specStrength = 0.95;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.2);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(uAmbientColor * 0.4, uFogColor * 1.25, clamp(R.y * 0.5 + 0.5, 0.0, 1.0));
                    envReflect = skyEnv * (0.28 + fresnel * 0.55);
                }

                float specPower = pow(max(dot(N, H), 0.0), shininess) * specStrength;

                // Truck headlight spotlight cone
                vec3 spotContrib = vec3(0.0);
                if (uHeadlightIntensity > 0.01) {
                    vec3 toFrag = vWorldPos - uHeadlightPos;
                    float distSpot = length(toFrag);
                    if (distSpot < 120.0) {
                        vec3 dirToFrag = toFrag / max(distSpot, 0.001);
                        float cone = dot(dirToFrag, normalize(uHeadlightDir));
                        float spotMask = smoothstep(0.76, 0.96, cone);
                        float atten = 1.0 / (1.0 + 0.016 * distSpot + 0.0010 * distSpot * distSpot);
                        float nDotSpot = max(dot(N, -dirToFrag), 0.15);
                        spotContrib = vec3(1.0, 0.95, 0.82) * uHeadlightIntensity * spotMask * atten * nDotSpot * 2.8;
                    }
                }

                vec3 lit = base.rgb * (hemiAmbient + uLightColor * diff + spotContrib) + uLightColor * specPower + envReflect;

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
                vec2 edge = abs(vUv - 0.5) * 2.0;
                if (edge.x > 0.94 || edge.y > 0.94) {
                    fragColor = vec4(0.08, 0.10, 0.13, 1.0);
                } else {
                    fragColor = texture(uTexture, vUv);
                }
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
