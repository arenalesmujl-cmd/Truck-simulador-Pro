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
    val uMaterialModeLoc: Int
    val uTextureLoc: Int

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
        uTextureLoc = GLES30.glGetUniformLocation(programId, "uTexture")
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
        const val MAT_SKIN_FABRIC = 5f

        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aUv;
            layout(location = 3) in vec4 aColor;

            uniform mat4 uMVP;
            uniform mat4 uModel;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec2 vUv;
            out vec4 vColor;

            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                vColor = aColor;
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec2 vUv;
            in vec4 vColor;

            uniform sampler2D uTexture;
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

            vec3 acesToneMap(vec3 x) {
                float a = 2.51;
                float b = 0.03;
                float c = 2.43;
                float d = 0.59;
                float e = 0.14;
                return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
            }

            vec3 srgbToLinear(vec3 c) {
                return pow(max(c, vec3(0.0001)), vec3(2.1));
            }

            vec3 linearToSrgb(vec3 c) {
                return pow(max(c, vec3(0.0001)), vec3(1.0 / 2.1));
            }

            void main() {
                vec4 texSample = texture(uTexture, vUv);
                vec4 base = texSample * vColor * uColorTint;
                if (base.a < 0.02) discard;

                // Soft radial contact shadow or corona glow billboard (uEmissive > 2.5)
                if (uEmissive > 2.5) {
                    fragColor = base;
                    return;
                }

                // Sky dome atmospheric Rayleigh/Mie scatter gradient (uEmissive > 1.5)
                if (uEmissive > 1.5) {
                    vec3 viewDir = normalize(vWorldPos - uCameraPos);
                    float h = clamp(viewDir.y * 1.15 + 0.04, 0.0, 1.0);
                    vec3 zenith = uFogColor * vec3(0.36, 0.58, 1.02);
                    vec3 horizon = mix(uFogColor * 1.08, uLightColor * 0.58 + uFogColor * 0.65, 0.42);
                    vec3 skyGrad = mix(horizon, zenith, pow(h, 0.58));
                    float sunDot = max(dot(viewDir, normalize(uLightDir)), 0.0);
                    float sunDisc = smoothstep(0.9965, 0.9994, sunDot) * 1.4;
                    float sunHalo = pow(sunDot, 20.0) * 0.46 + pow(sunDot, 4.5) * 0.14;
                    skyGrad += uLightColor * (sunDisc + sunHalo);
                    fragColor = vec4(clamp(skyGrad, 0.0, 1.0), 1.0);
                    return;
                }

                // Pure emissive lights / stars / LEDs
                if (uEmissive > 0.5) {
                    float dist = length(vWorldPos - uCameraPos);
                    float fog = 1.0 - exp(-pow(dist * uFogDensity * 0.48, 2.0));
                    vec3 finalRgb = mix(base.rgb * 1.16, uFogColor, clamp(fog, 0.0, 0.85));
                    fragColor = vec4(finalRgb, base.a);
                    return;
                }

                vec3 albedo = srgbToLinear(base.rgb);
                vec3 lightLin = srgbToLinear(uLightColor);
                vec3 ambLin = srgbToLinear(uAmbientColor);
                vec3 fogLin = srgbToLinear(uFogColor);

                vec3 N = normalize(vNormal);
                vec3 L = normalize(uLightDir);
                vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 H = normalize(L + V);

                float localWet = uWetness;
                if (uMaterialMode > 2.5 && uMaterialMode < 3.5 && uWetness > 0.1) {
                    float puddle = smoothstep(0.38, 0.86, sin(vWorldPos.x * 0.17) * cos(vWorldPos.z * 0.15) * 0.5 + 0.5);
                    localWet = mix(0.45, 1.0, puddle) * uWetness;
                }

                float ndl = dot(N, L);
                float diff = max(ndl, 0.0);

                // Warm Subsurface Scattering (SSS) wrap fill on human skin & fabric
                vec3 sssGlow = vec3(0.0);
                if (uMaterialMode > 4.5) {
                    diff = max((ndl + 0.42) / 1.42, 0.0);
                    float wrapBand = smoothstep(-0.40, 0.28, ndl) * (1.0 - smoothstep(0.08, 0.72, ndl));
                    sssGlow = vec3(0.24, 0.09, 0.04) * wrapBand * lightLin;
                }

                // Two-tone hemisphere sky bounce + ground bounce (adds natural global illumination)
                vec3 skyBounce = ambLin * 1.45;
                vec3 groundBounce = ambLin * vec3(0.58, 0.54, 0.48);
                vec3 hemiAmbient = mix(groundBounce, skyBounce, N.y * 0.5 + 0.5);

                // Soft cool fill light from opposite sky quadrant so shadowed sides keep rich 3D sculpting
                vec3 fillDir = normalize(vec3(-L.x, 0.55, -L.z));
                float fillDiff = max(dot(N, fillDir), 0.0) * 0.28;
                vec3 fillLight = mix(ambLin, fogLin, 0.5) * fillDiff;

                float shininess = mix(24.0, 110.0, localWet);
                float specStrength = mix(0.05, 0.65, localWet);
                vec3 envReflect = vec3(0.0);

                if (uMaterialMode > 0.5 && uMaterialMode < 1.5) {
                    // Dual-lobe Metallic Clearcoat Paint
                    shininess = 96.0;
                    specStrength = 0.62;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.4);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(groundBounce * 0.6, fogLin * 1.35 + lightLin * 0.35, smoothstep(-0.15, 0.45, R.y));
                    envReflect = skyEnv * (0.06 + fresnel * 0.48);
                } else if (uMaterialMode > 1.5 && uMaterialMode < 2.5) {
                    // Mirror-Polished Chrome with crisp horizon band
                    shininess = 160.0;
                    specStrength = 1.40;
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(vec3(0.06, 0.08, 0.12), fogLin * 1.35 + lightLin * 0.55, smoothstep(-0.14, 0.34, R.y));
                    envReflect = skyEnv * 0.72;
                    albedo *= 0.45;
                } else if (uMaterialMode > 3.5 && uMaterialMode < 4.5) {
                    // Deep Tinted Reflective Automotive Glass
                    shininess = 150.0;
                    specStrength = 1.25;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.2);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(vec3(0.03, 0.05, 0.09), fogLin * 1.35 + lightLin * 0.30, smoothstep(-0.10, 0.42, R.y));
                    envReflect = skyEnv * (0.34 + fresnel * 0.64);
                } else if (uMaterialMode > 4.5) {
                    // Skin / Denim / Leather silhouette rim light
                    shininess = 22.0;
                    specStrength = 0.09;
                    float rim = pow(1.0 - max(dot(N, V), 0.0), 3.4);
                    envReflect = (lightLin * 0.45 + skyBounce) * rim * 0.12;
                } else if (uMaterialMode > 2.5 && uMaterialMode < 3.5 && localWet > 0.15) {
                    // Wet asphalt puddle mirror sheen
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(ambLin * 0.3, fogLin * 1.25, clamp(R.y * 0.5 + 0.5, 0.0, 1.0));
                    envReflect = skyEnv * localWet * 0.38;
                }

                float specPower = pow(max(dot(N, H), 0.0), shininess) * specStrength;

                // Dual headlight spotlight beam
                vec3 spotContrib = vec3(0.0);
                if (uHeadlightIntensity > 0.01) {
                    vec3 toFrag = vWorldPos - uHeadlightPos;
                    float distSpot = length(toFrag);
                    if (distSpot < 140.0) {
                        vec3 dirToFrag = toFrag / max(distSpot, 0.001);
                        float cone = dot(dirToFrag, normalize(uHeadlightDir));
                        float spotMask = smoothstep(0.72, 0.96, cone);
                        float atten = 1.0 / (1.0 + 0.012 * distSpot + 0.0008 * distSpot * distSpot);
                        float nDotSpot = max(dot(N, -dirToFrag), 0.18);
                        spotContrib = vec3(1.0, 0.92, 0.78) * uHeadlightIntensity * spotMask * atten * nDotSpot * 3.4;
                    }
                }

                vec3 hdr = albedo * (hemiAmbient + fillLight + lightLin * diff * 1.35 + sssGlow + spotContrib)
                         + lightLin * specPower
                         + envReflect;
                vec3 toneMapped = acesToneMap(hdr * 1.15);
                vec3 litSrgb = linearToSrgb(toneMapped);

                float distCam = length(vWorldPos - uCameraPos);
                float fogFactor = clamp(1.0 - exp(-pow(distCam * uFogDensity, 2.0)), 0.0, 1.0);
                vec3 finalRgb = mix(litSrgb, uFogColor, fogFactor);

                fragColor = vec4(finalRgb, base.a);
            }
        """
    }
}

class MirrorQuadShaderProgram {
    val programId: Int
    val uTextureLoc: Int
    val uRectLoc: Int

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
                vec2 p = abs(vUv - 0.5) * 2.0;
                // Rounded aerodynamic side-mirror housing bezel + glossy reflection glass
                float corner = length(max(p - vec2(0.76, 0.80), 0.0));
                if (corner > 0.24) {
                    discard;
                }
                if (corner > 0.16 || p.x > 0.90 || p.y > 0.92) {
                    fragColor = vec4(0.08, 0.10, 0.13, 1.0);
                } else if (p.x > 0.86 || p.y > 0.88) {
                    fragColor = vec4(0.58, 0.62, 0.68, 1.0);
                } else {
                    vec4 mirrorSample = texture(uTexture, vUv);
                    float glassSheen = smoothstep(0.0, 1.0, (vUv.x + vUv.y) * 0.22);
                    fragColor = vec4(mirrorSample.rgb * 0.94 + vec3(0.04, 0.06, 0.09) * glassSheen, 1.0);
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
