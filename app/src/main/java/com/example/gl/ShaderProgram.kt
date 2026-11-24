package com.example.gl

import android.opengl.GLES30
import android.util.Log

class ShadowDepthShaderProgram {
    val programId: Int
    val uLightMvpLoc: Int

    init {
        programId = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uLightMvpLoc = GLES30.glGetUniformLocation(programId, "uLightMVP")
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            precision highp float;
            layout(location = 0) in vec3 aPos;
            uniform mat4 uLightMVP;
            void main() {
                gl_Position = uLightMVP * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision highp float;
            void main() {
            }
        """
    }
}

class WorldShaderProgram {
    val programId: Int

    val uMvpLoc: Int
    val uModelLoc: Int
    val uLightVpLoc: Int
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
    val uShadowMapLoc: Int
    val uShadowEnabledLoc: Int
    val uTimeLoc: Int
    val uStarIntensityLoc: Int

    init {
        programId = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uMvpLoc = GLES30.glGetUniformLocation(programId, "uMVP")
        uModelLoc = GLES30.glGetUniformLocation(programId, "uModel")
        uLightVpLoc = GLES30.glGetUniformLocation(programId, "uLightVP")
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
        uShadowMapLoc = GLES30.glGetUniformLocation(programId, "uShadowMap")
        uShadowEnabledLoc = GLES30.glGetUniformLocation(programId, "uShadowEnabled")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "uTime")
        uStarIntensityLoc = GLES30.glGetUniformLocation(programId, "uStarIntensity")
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
            precision highp float;
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aUv;
            layout(location = 3) in vec4 aColor;

            uniform mat4 uMVP;
            uniform mat4 uModel;
            uniform mat4 uLightVP;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec2 vUv;
            out vec4 vColor;
            out vec4 vShadowCoord;

            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                vColor = aColor;
                vShadowCoord = uLightVP * worldPos;
                gl_Position = uMVP * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision highp float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec2 vUv;
            in vec4 vColor;
            in vec4 vShadowCoord;

            uniform sampler2D uTexture;
            uniform sampler2D uShadowMap;
            uniform float uShadowEnabled;
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
            uniform float uTime;
            uniform float uStarIntensity;

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

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            float valueNoise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + vec2(1.0, 0.0));
                float c = hash21(i + vec2(0.0, 1.0));
                float d = hash21(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            float fbmClouds(vec2 p) {
                float v = 0.0;
                float amp = 0.52;
                for (int i = 0; i < 4; i++) {
                    v += amp * valueNoise(p);
                    p = p * 2.04 + vec2(17.1, -9.4);
                    amp *= 0.5;
                }
                return v;
            }

            float sampleDirectionalShadow(vec3 N, vec3 L) {
                if (uShadowEnabled < 0.5) return 1.0;
                vec3 proj = vShadowCoord.xyz / max(vShadowCoord.w, 0.0001);
                proj = proj * 0.5 + 0.5;
                if (proj.z > 1.0 || proj.z < 0.0) return 1.0;
                float edge = max(abs(proj.x - 0.5), abs(proj.y - 0.5));
                if (edge >= 0.495) return 1.0;

                float bias = max(0.0022 * (1.0 - max(dot(N, L), 0.0)), 0.00075);
                float texel = 1.0 / 1024.0;
                float occ = 0.0;
                occ += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(-texel, -texel)).r) ? 1.0 : 0.0;
                occ += (proj.z - bias > texture(uShadowMap, proj.xy + vec2( texel, -texel)).r) ? 1.0 : 0.0;
                occ += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(-texel,  texel)).r) ? 1.0 : 0.0;
                occ += (proj.z - bias > texture(uShadowMap, proj.xy + vec2( texel,  texel)).r) ? 1.0 : 0.0;
                float rawShadow = 1.0 - (occ * 0.25) * 0.68;
                float fade = 1.0 - smoothstep(0.36, 0.49, edge);
                return mix(1.0, rawShadow, fade);
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

                // Sky dome atmospheric Rayleigh/Mie scatter + volumetric FBM clouds + twinkling stars (uEmissive > 1.5)
                if (uEmissive > 1.5) {
                    vec3 viewDir = normalize(vWorldPos - uCameraPos);
                    float h = clamp(viewDir.y * 1.18 + 0.04, 0.0, 1.0);
                    vec3 zenith = uFogColor * vec3(0.30, 0.52, 1.08);
                    vec3 horizon = mix(uFogColor * 1.12, uLightColor * 0.64 + uFogColor * 0.60, 0.46);
                    vec3 skyGrad = mix(horizon, zenith, pow(h, 0.55));
                    vec3 L = normalize(uLightDir);
                    float sunDot = max(dot(viewDir, L), 0.0);
                    float sunDisc = smoothstep(0.9968, 0.9994, sunDot) * 1.50;
                    float sunHalo = pow(sunDot, 22.0) * 0.48 + pow(sunDot, 4.5) * 0.16;
                    skyGrad += uLightColor * (sunDisc + sunHalo);

                    // Crisp twinkling starfield & faint Milky Way band at night
                    if (uStarIntensity > 0.02 && viewDir.y > 0.04) {
                        vec2 starUv = viewDir.xz / (viewDir.y + 0.28) * 32.0;
                        vec2 cell = floor(starUv);
                        vec2 f = fract(starUv) - 0.5;
                        float rnd = hash21(cell);
                        if (rnd > 0.968) {
                            float d = length(f - (vec2(hash21(cell + 3.1), hash21(cell + 7.7)) - 0.5) * 0.55);
                            float twinkle = 0.68 + 0.32 * sin(uTime * 2.8 + rnd * 90.0);
                            float starBright = smoothstep(0.16, 0.02, d) * twinkle * uStarIntensity * smoothstep(0.04, 0.28, viewDir.y);
                            skyGrad += vec3(0.86, 0.92, 1.0) * starBright * 1.25;
                        }
                    }

                    // Multi-octave drifting procedural cumulus cloud deck
                    if (viewDir.y > 0.025) {
                        vec2 cloudProj = viewDir.xz / (viewDir.y + 0.16) * 1.85 + vec2(uTime * 0.012, uTime * 0.005);
                        float density = fbmClouds(cloudProj);
                        float detail = fbmClouds(cloudProj * 2.35 - vec2(uTime * 0.008, 0.0));
                        float coverage = mix(0.46, 0.30, clamp(uWetness * 0.85, 0.0, 1.0));
                        float cloudAlpha = smoothstep(coverage, coverage + 0.34, density * 0.78 + detail * 0.22);
                        cloudAlpha *= smoothstep(0.025, 0.16, viewDir.y);
                        vec3 cloudUnder = mix(uFogColor * 0.62, uAmbientColor * 1.15, 0.45);
                        vec3 cloudLit = mix(uFogColor * 1.18, uLightColor * 1.12, 0.55 + 0.40 * pow(sunDot, 3.0));
                        vec3 cloudColor = mix(cloudUnder, cloudLit, smoothstep(0.25, 0.78, detail));
                        skyGrad = mix(skyGrad, cloudColor, cloudAlpha * 0.88);
                    }

                    fragColor = vec4(clamp(skyGrad, 0.0, 1.0), 1.0);
                    return;
                }

                // Pure emissive lights / LEDs
                if (uEmissive > 0.5) {
                    float dist = length(vWorldPos - uCameraPos);
                    float fog = 1.0 - exp(-pow(dist * uFogDensity * 0.48, 2.0));
                    vec3 finalRgb = mix(base.rgb * 1.22, uFogColor, clamp(fog, 0.0, 0.85));
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

                bool isOceanWater = (uMaterialMode < 0.5 && vWorldPos.x > 870.0 && vWorldPos.z < -180.0 && vWorldPos.y < -0.15 && N.y > 0.90);
                if (isOceanWater) {
                    float w1 = sin(vWorldPos.x * 0.28 + uTime * 1.65) * cos(vWorldPos.z * 0.24 - uTime * 1.25);
                    float w2 = cos(vWorldPos.x * 0.52 - uTime * 2.1 + vWorldPos.z * 0.41);
                    N = normalize(vec3((w1 * 0.16 + w2 * 0.09), 1.0, (w2 * 0.16 - w1 * 0.09)));
                    float shoreDist = vWorldPos.x - 870.0;
                    float foamWave = smoothstep(0.58, 0.95, sin(shoreDist * 0.55 - uTime * 2.2) * 0.5 + 0.5);
                    float shoreMask = clamp(1.0 - shoreDist / 16.0, 0.0, 1.0);
                    albedo = mix(srgbToLinear(vec3(0.04, 0.28, 0.54)), srgbToLinear(vec3(0.88, 0.94, 0.99)), shoreMask * (0.45 + 0.48 * foamWave));
                } else if (uMaterialMode < 0.5 && N.y > 0.12) {
                    float macroPatch = sin(vWorldPos.x * 0.045) * cos(vWorldPos.z * 0.041)
                                     + 0.5 * sin(vWorldPos.x * 0.13 + vWorldPos.z * 0.11);
                    float fineGrain = sin(vWorldPos.x * 0.42 - vWorldPos.z * 0.37) * 0.038;
                    if (N.y < 0.78) {
                        float rockFactor = smoothstep(0.78, 0.46, N.y);
                        float strata = sin(vWorldPos.y * 1.45 + vWorldPos.x * 0.09 + vWorldPos.z * 0.07) * 0.14;
                        vec3 granite = srgbToLinear(vec3(0.38, 0.37, 0.36)) * (1.0 + strata + fineGrain * 2.0);
                        albedo = mix(albedo * (1.0 + macroPatch * 0.095 + fineGrain), granite, rockFactor * 0.72);
                    } else {
                        albedo *= (1.0 + macroPatch * 0.095 + fineGrain);
                    }
                }

                float localWet = uWetness;
                if (uMaterialMode > 2.5 && uMaterialMode < 3.5) {
                    float grain = sin(vWorldPos.x * 2.4) * cos(vWorldPos.z * 2.3) * 0.05;
                    float rippleX = cos(vWorldPos.x * 5.2 + vWorldPos.z * 2.7) * 0.045;
                    float rippleZ = sin(vWorldPos.z * 5.1 - vWorldPos.x * 2.3) * 0.045;
                    albedo *= (1.0 + grain);
                    if (uWetness > 0.1) {
                        float puddle = smoothstep(0.38, 0.86, sin(vWorldPos.x * 0.17) * cos(vWorldPos.z * 0.15) * 0.5 + 0.5);
                        localWet = mix(0.45, 1.0, puddle) * uWetness;
                        N = normalize(N + vec3(rippleX, 0.0, rippleZ) * (1.0 - puddle * 0.7));
                    } else {
                        N = normalize(N + vec3(rippleX, 0.0, rippleZ) * 0.65);
                    }
                }

                vec3 H = normalize(L + V);
                float shadowFactor = sampleDirectionalShadow(N, L);

                float ndl = dot(N, L);
                float diff = max(ndl, 0.0);

                // Warm Subsurface Scattering (SSS) wrap fill on human skin & fabric
                vec3 sssGlow = vec3(0.0);
                if (uMaterialMode > 4.5) {
                    diff = max((ndl + 0.50) / 1.50, 0.0);
                    float wrapBand = smoothstep(-0.44, 0.34, ndl) * (1.0 - smoothstep(0.10, 0.78, ndl));
                    sssGlow = vec3(0.30, 0.115, 0.052) * wrapBand * lightLin;
                }

                vec3 skyBounce = ambLin * 1.52;
                vec3 groundBounce = ambLin * vec3(0.56, 0.52, 0.46);
                vec3 hemiAmbient = mix(groundBounce, skyBounce, N.y * 0.5 + 0.5);

                vec3 fillDir = normalize(vec3(-L.x, 0.58, -L.z));
                float fillDiff = max(dot(N, fillDir), 0.0) * 0.30;
                vec3 fillLight = mix(ambLin, fogLin, 0.5) * fillDiff;

                float shininess = mix(24.0, 118.0, localWet);
                float specStrength = mix(0.05, 0.72, localWet);
                vec3 envReflect = vec3(0.0);

                if (isOceanWater) {
                    shininess = 148.0;
                    specStrength = 1.30;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(ambLin * 0.45, fogLin * 1.48 + lightLin * 0.48, smoothstep(-0.08, 0.45, R.y));
                    envReflect = skyEnv * (0.22 + fresnel * 0.70);
                } else if (uMaterialMode > 0.5 && uMaterialMode < 1.5) {
                    // Dual-lobe automotive clearcoat + subtle metallic base flake shimmer
                    shininess = 88.0;
                    specStrength = 0.62;
                    float flake = hash21(floor(vWorldPos.xz * 260.0 + vWorldPos.y * 190.0));
                    float flakeGlint = smoothstep(0.76, 0.99, flake) * 0.24 * max(ndl, 0.0);
                    float clearcoat = pow(max(dot(N, H), 0.0), 210.0) * 0.78 * shadowFactor;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(groundBounce * 0.52, fogLin * 1.48 + lightLin * 0.48, smoothstep(-0.15, 0.44, R.y));
                    envReflect = skyEnv * (0.09 + fresnel * 0.58) + lightLin * (clearcoat + flakeGlint);
                } else if (uMaterialMode > 1.5 && uMaterialMode < 2.5) {
                    shininess = 175.0;
                    specStrength = 1.55;
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(vec3(0.06, 0.08, 0.12), fogLin * 1.44 + lightLin * 0.60, smoothstep(-0.14, 0.34, R.y));
                    envReflect = skyEnv * 0.78;
                    albedo *= 0.45;
                } else if (uMaterialMode > 3.5 && uMaterialMode < 4.5) {
                    shininess = 165.0;
                    specStrength = 1.40;
                    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.1);
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(vec3(0.03, 0.05, 0.10), fogLin * 1.46 + lightLin * 0.36, smoothstep(-0.10, 0.42, R.y));
                    envReflect = skyEnv * (0.38 + fresnel * 0.62);
                } else if (uMaterialMode > 4.5) {
                    shininess = 24.0;
                    specStrength = 0.085;
                    float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0);
                    envReflect = (lightLin * 0.52 + skyBounce) * rim * 0.16;
                } else if (uMaterialMode > 2.5 && uMaterialMode < 3.5 && localWet > 0.15) {
                    vec3 R = reflect(-V, N);
                    vec3 skyEnv = mix(ambLin * 0.3, fogLin * 1.28, clamp(R.y * 0.5 + 0.5, 0.0, 1.0));
                    envReflect = skyEnv * localWet * 0.42;
                }

                float specPower = pow(max(dot(N, H), 0.0), shininess) * specStrength * shadowFactor;

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
                        spotContrib = vec3(1.0, 0.92, 0.78) * uHeadlightIntensity * spotMask * atten * nDotSpot * 3.5;
                    }
                }

                vec3 hdr = albedo * (hemiAmbient + fillLight + lightLin * diff * 1.38 * shadowFactor + sssGlow + spotContrib)
                         + lightLin * specPower
                         + envReflect;
                vec3 toneMapped = acesToneMap(hdr * 1.16);
                vec3 litSrgb = linearToSrgb(toneMapped);

                float distCam = length(vWorldPos - uCameraPos);
                float fogFactor = clamp(1.0 - exp(-pow(distCam * uFogDensity, 2.0)), 0.0, 1.0);
                float sunFwd = max(dot(-V, L), 0.0);
                vec3 aerialFog = mix(uFogColor, uLightColor * 0.92, pow(sunFwd, 5.0) * 0.32 * (1.0 - clamp(uWetness, 0.0, 1.0)));
                vec3 finalRgb = mix(litSrgb, aerialFog, fogFactor);

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
                float corner = length(max(p - vec2(0.76, 0.80), 0.0));
                if (corner > 0.24) {
                    discard;
                }
                if (corner > 0.16 || p.x > 0.90 || p.y > 0.92) {
                    float housingShade = 0.78 + 0.30 * (1.0 - vUv.y);
                    fragColor = vec4(vec3(0.085, 0.105, 0.138) * housingShade, 1.0);
                } else if (p.x > 0.86 || p.y > 0.88) {
                    float chromeSpec = 0.86 + 0.24 * sin((vUv.x + vUv.y) * 3.14159);
                    fragColor = vec4(vec3(0.56, 0.61, 0.68) * chromeSpec, 1.0);
                } else {
                    vec4 mirrorSample = texture(uTexture, vUv);
                    float glassSheen = smoothstep(0.0, 1.0, (vUv.x + vUv.y) * 0.22);
                    float vignette = 1.0 - smoothstep(0.55, 1.05, length(p * 0.55)) * 0.12;
                    fragColor = vec4((mirrorSample.rgb * 0.95 + vec3(0.04, 0.06, 0.09) * glassSheen) * vignette, 1.0);
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
