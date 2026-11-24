package com.example.gl

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

data class DriverMeshSet(
    val headFaceMesh: MeshBuffer,
    val headAccMesh: MeshBuffer,
    val torsoMesh: MeshBuffer,
    val upperArmMesh: MeshBuffer,
    val leftForearmHandMesh: MeshBuffer,
    val rightForearmHandMesh: MeshBuffer,
    val thighMesh: MeshBuffer,
    val shinBootMesh: MeshBuffer
)

object CharacterMeshFactory {

    /**
     * Anatomically proportioned 3D human driver (~1.84m tall).
     * - Continuous 36x16 sculpted head mesh with recessed orbital sockets, brow ridge,
     *   nose bridge/tip, cheekbones, jawline & chin, UV-mapped to driverFaceTex (zero popping blobs).
     * - Separate headAccMesh for sculpted anatomical ears, hair volume, and 5-panel pro trucker cap.
     * - Seamless spherical shoulder, elbow, hip, and knee joint caps to prevent gaps under articulation.
     * - Distinct left & right sculpted leather-gloved hands with proper thumb chirality & left chronograph watch.
     */
    fun buildDriverMeshes(): DriverMeshSet {
        val skinR = 0.89f; val skinG = 0.68f; val skinB = 0.55f
        val skinWarmR = 0.84f; val skinWarmG = 0.62f; val skinWarmB = 0.49f
        val skinShadowR = 0.74f; val skinShadowG = 0.53f; val skinShadowB = 0.42f

        val hairR = 0.12f; val hairG = 0.09f; val hairB = 0.07f

        val jacketR = 0.13f; val jacketG = 0.15f; val jacketB = 0.19f
        val jacketTrimR = 0.18f; val jacketTrimG = 0.21f; val jacketTrimB = 0.26f
        val suedeR = 0.48f; val suedeG = 0.30f; val suedeB = 0.17f
        val henleyR = 0.90f; val henleyG = 0.91f; val henleyB = 0.93f
        val denimR = 0.14f; val denimG = 0.21f; val denimB = 0.35f
        val denimFadeR = 0.20f; val denimFadeG = 0.28f; val denimFadeB = 0.43f
        val leatherR = 0.32f; val leatherG = 0.18f; val leatherB = 0.10f

        // =========================================================================
        // 1A. CONTINUOUS SCULPTED UV-MAPPED HEAD & NECK (rendered with driverFaceTex)
        // =========================================================================
        val headFace = buildSculptedUvHeadMesh()

        // =========================================================================
        // 1B. ANATOMICAL EARS, LAYERED HAIR & 5-PANEL PRO TRUCKER CAP (whiteTex)
        // =========================================================================
        val headAcc = DynamicMeshBuilder()
        for (side in listOf(-1f, 1f)) {
            headAcc.addEllipsoid(side * 0.083f, 0.148f, -0.004f, 0.011f, 0.025f, 0.016f, 8, 12, skinWarmR, skinWarmG, skinWarmB)
            headAcc.addEllipsoid(side * 0.085f, 0.148f, 0.000f, 0.006f, 0.015f, 0.010f, 6, 10, skinShadowR, skinShadowG, skinShadowB)
            headAcc.addEllipsoid(side * 0.077f, 0.148f, -0.018f, 0.012f, 0.036f, 0.024f, 6, 10, hairR, hairG, hairB)
        }
        // Sculpted fuller hair volume at back & nape underneath cap band
        headAcc.addEllipsoid(0f, 0.162f, -0.026f, 0.084f, 0.078f, 0.074f, 12, 18, hairR, hairG, hairB)

        // Tailored 5-Panel Charcoal & Burnt-Amber Pro Trucker Cap
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 28,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.194f, -0.006f, 0.087f, 0.093f, 0.11f, 0.13f, 0.18f, roundness = 0.92f),
                LoftRingY(0.220f, -0.006f, 0.088f, 0.094f, 0.14f, 0.17f, 0.22f, roundness = 0.94f),
                LoftRingY(0.252f, -0.008f, 0.078f, 0.084f, 0.14f, 0.17f, 0.22f, roundness = 0.96f),
                LoftRingY(0.274f, -0.010f, 0.044f, 0.048f, 0.12f, 0.15f, 0.19f, roundness = 1.0f)
            )
        )
        headAcc.addEllipsoid(0f, 0.277f, -0.010f, 0.010f, 0.0045f, 0.010f, 6, 10, 0.95f, 0.44f, 0.08f)
        headAcc.addEllipsoid(0f, 0.228f, 0.089f, 0.021f, 0.012f, 0.005f, 6, 12, 0.95f, 0.44f, 0.08f)
        headAcc.addCurvedVisor(
            cy = 0.200f,
            cz = 0.081f,
            width = 0.160f,
            length = 0.090f,
            thickness = 0.0075f,
            archHeight = 0.021f,
            segs = 16,
            r = 0.94f, g = 0.43f, b = 0.07f
        )

        // =========================================================================
        // 2. ATHLETIC TORSO, TAILORED CANVAS/SUEDE TRUCKER JACKET, BELT & HENLEY TEE
        // =========================================================================
        val torso = DynamicMeshBuilder()
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 28,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.025f, 0.000f, 0.170f, 0.118f, denimR, denimG, denimB, roundness = 0.85f),
                LoftRingY(0.088f, 0.002f, 0.164f, 0.114f, denimR, denimG, denimB, roundness = 0.85f),
                LoftRingY(0.092f, 0.003f, 0.172f, 0.121f, leatherR, leatherG, leatherB, roundness = 0.85f),
                LoftRingY(0.132f, 0.004f, 0.172f, 0.121f, leatherR, leatherG, leatherB, roundness = 0.85f),
                LoftRingY(0.138f, 0.006f, 0.160f, 0.114f, henleyR, henleyG, henleyB, roundness = 0.82f),
                LoftRingY(0.240f, 0.010f, 0.168f, 0.122f, henleyR, henleyG, henleyB, roundness = 0.80f),
                LoftRingY(0.365f, 0.014f, 0.188f, 0.134f, henleyR, henleyG, henleyB, roundness = 0.78f),
                LoftRingY(0.475f, 0.010f, 0.204f, 0.126f, henleyR, henleyG, henleyB, roundness = 0.78f),
                LoftRingY(0.545f, 0.002f, 0.128f, 0.090f, henleyR, henleyG, henleyB, roundness = 0.88f),
                LoftRingY(0.580f, 0.000f, 0.070f, 0.068f, skinShadowR, skinShadowG, skinShadowB, roundness = 0.94f)
            )
        )
        // Brushed steel belt buckle & center placket buttons
        torso.addEllipsoid(0f, 0.112f, 0.122f, 0.030f, 0.020f, 0.009f, 8, 14, 0.82f, 0.84f, 0.88f)
        torso.addEllipsoid(0f, 0.112f, 0.127f, 0.016f, 0.010f, 0.006f, 6, 10, leatherR, leatherG, leatherB)
        for (by in listOf(0.44f, 0.485f, 0.528f)) {
            torso.addEllipsoid(0f, by, 0.132f, 0.006f, 0.006f, 0.004f, 6, 8, 0.26f, 0.28f, 0.32f)
        }

        // Open Charcoal Canvas Jacket Back/Shoulders + Suede Collar
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 28,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.134f, -0.024f, 0.174f, 0.100f, jacketTrimR, jacketTrimG, jacketTrimB, roundness = 0.78f),
                LoftRingY(0.175f, -0.024f, 0.177f, 0.102f, jacketR, jacketG, jacketB, roundness = 0.78f),
                LoftRingY(0.280f, -0.022f, 0.184f, 0.105f, jacketR, jacketG, jacketB, roundness = 0.77f),
                LoftRingY(0.390f, -0.019f, 0.200f, 0.110f, jacketR, jacketG, jacketB, roundness = 0.75f),
                LoftRingY(0.495f, -0.014f, 0.218f, 0.112f, jacketTrimR, jacketTrimG, jacketTrimB, roundness = 0.74f),
                LoftRingY(0.552f, -0.008f, 0.136f, 0.088f, suedeR, suedeG, suedeB, roundness = 0.86f)
            )
        )
        // Left & right open front jacket lapels + rolled suede collar tips + chest pockets + deltoid caps
        for (side in listOf(-1f, 1f)) {
            torso.addLoftedProfileY(
                cx = side * 0.116f,
                sectors = 18,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.136f, 0.036f, 0.062f, 0.076f, jacketTrimR, jacketTrimG, jacketTrimB, roundness = 0.78f),
                    LoftRingY(0.175f, 0.038f, 0.064f, 0.078f, jacketR, jacketG, jacketB, roundness = 0.78f),
                    LoftRingY(0.290f, 0.044f, 0.068f, 0.080f, jacketR, jacketG, jacketB, roundness = 0.77f),
                    LoftRingY(0.410f, 0.052f, 0.074f, 0.076f, jacketR, jacketG, jacketB, roundness = 0.76f),
                    LoftRingY(0.494f, 0.042f, 0.078f, 0.068f, suedeR, suedeG, suedeB, roundness = 0.76f)
                )
            )
            torso.addEllipsoid(side * 0.056f, 0.310f, 0.112f, 0.005f, 0.155f, 0.008f, 6, 8, 0.78f, 0.64f, 0.36f)
            torso.addEllipsoid(side * 0.114f, 0.402f, 0.128f, 0.032f, 0.036f, 0.010f, 6, 12, jacketTrimR, jacketTrimG, jacketTrimB)
            torso.addEllipsoid(side * 0.114f, 0.420f, 0.135f, 0.0055f, 0.0055f, 0.005f, 4, 8, 0.82f, 0.68f, 0.38f)
            torso.addEllipsoid(side * 0.194f, 0.495f, 0f, 0.058f, 0.062f, 0.090f, 10, 14, jacketR, jacketG, jacketB)
        }

        // =========================================================================
        // 3. UPPER ARM WITH SEAMLESS SHOULDER & ELBOW SPHERICAL CAPS
        // =========================================================================
        val upperArm = DynamicMeshBuilder()
        upperArm.addEllipsoid(0f, 0f, 0f, 0.062f, 0.062f, 0.062f, 10, 14, jacketTrimR, jacketTrimG, jacketTrimB)
        upperArm.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.295f, 0f, 0.048f, 0.048f, jacketR, jacketG, jacketB),
                LoftRingY(-0.245f, 0f, 0.051f, 0.050f, jacketR, jacketG, jacketB),
                LoftRingY(-0.150f, 0.004f, 0.057f, 0.056f, jacketR, jacketG, jacketB),
                LoftRingY(-0.075f, 0.005f, 0.062f, 0.060f, jacketTrimR, jacketTrimG, jacketTrimB),
                LoftRingY(-0.010f, 0f, 0.064f, 0.064f, jacketTrimR, jacketTrimG, jacketTrimB),
                LoftRingY(0.025f, 0f, 0.054f, 0.054f, jacketTrimR, jacketTrimG, jacketTrimB)
            )
        )
        // Spherical elbow joint cap centered exactly at -0.290m so 46deg flexion stays 100% sealed
        upperArm.addEllipsoid(0f, -0.290f, 0f, 0.047f, 0.047f, 0.047f, 10, 14, jacketR, jacketG, jacketB)

        // =========================================================================
        // 4. LEFT & RIGHT FOREARMS + ANATOMICAL GLOVED HANDS (+ WATCH ON LEFT WRIST)
        // =========================================================================
        fun buildForearmHand(isLeft: Boolean): MeshBuffer {
            val f = DynamicMeshBuilder()
            // Seamless elbow socket sphere at (0,0,0) matching upperArm elbow pivot
            f.addEllipsoid(0f, 0f, 0f, 0.046f, 0.046f, 0.046f, 10, 14, jacketR, jacketG, jacketB)
            f.addLoftedProfileY(
                cx = 0f,
                sectors = 20,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(-0.258f, 0f, 0.031f, 0.024f, skinR, skinG, skinB),
                    LoftRingY(-0.215f, 0f, 0.038f, 0.033f, skinR, skinG, skinB),
                    LoftRingY(-0.192f, 0f, 0.043f, 0.041f, jacketTrimR, jacketTrimG, jacketTrimB),
                    LoftRingY(-0.172f, 0f, 0.045f, 0.044f, jacketTrimR, jacketTrimG, jacketTrimB),
                    LoftRingY(-0.168f, 0f, 0.046f, 0.045f, jacketR, jacketG, jacketB),
                    LoftRingY(-0.075f, 0f, 0.050f, 0.049f, jacketR, jacketG, jacketB),
                    LoftRingY(0.000f, 0f, 0.047f, 0.047f, jacketR, jacketG, jacketB)
                )
            )
            if (isLeft) {
                f.addTaperedCylinderY(0f, -0.242f, 0f, 0.035f, 0.035f, 0.017f, 16, 0.11f, 0.12f, 0.14f)
                f.addCylinderZ(0f, -0.233f, 0.023f, 0.038f, 0.015f, 14, 0.80f, 0.83f, 0.88f)
                f.addCylinderZ(0f, -0.233f, 0.036f, 0.040f, 0.011f, 14, 0.06f, 0.14f, 0.20f)
            }

            // Palm & fingerless leather driving glove (palm faces inward toward leg, thumb along +Z front edge!)
            val inwardSign = if (isLeft) -1f else 1f
            f.addEllipsoid(0f, -0.282f, 0.002f, 0.020f, 0.036f, 0.032f, 10, 14, leatherR, leatherG, leatherB)
            f.addEllipsoid(0f, -0.295f, 0.003f, 0.016f, 0.013f, 0.031f, 6, 12, leatherR * 1.15f, leatherG * 1.15f, leatherB * 1.15f)
            // Thumb sits naturally along the front (+Z) edge angled slightly inward
            f.addEllipsoid(inwardSign * 0.015f, -0.278f, 0.031f, 0.0085f, 0.019f, 0.009f, 8, 10, skinR, skinG, skinB)
            for (fi in 0 until 4) {
                val fz = 0.021f - fi * 0.0135f
                val fLen = when (fi) {
                    1 -> 0.034f
                    2 -> 0.032f
                    0 -> 0.028f
                    else -> 0.025f
                }
                f.addTaperedCylinderY(0f, -0.296f - fLen * 0.65f, fz, 0.0058f, 0.0072f, fLen * 0.65f, 8, skinWarmR, skinWarmG, skinWarmB)
                f.addEllipsoid(inwardSign * 0.004f, -0.296f - fLen * 0.72f, fz, 0.0062f, 0.0085f, 0.0058f, 6, 8, skinR, skinG, skinB)
            }
            return f.build()
        }

        // =========================================================================
        // 5. THIGH WITH HIP & KNEE SPHERICAL CAPS
        // =========================================================================
        val thigh = DynamicMeshBuilder()
        thigh.addEllipsoid(0f, 0f, 0f, 0.078f, 0.074f, 0.078f, 10, 14, denimR, denimG, denimB)
        thigh.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.422f, 0f, 0.056f, 0.058f, denimR, denimG, denimB),
                LoftRingY(-0.385f, 0.004f, 0.059f, 0.061f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.270f, 0.006f, 0.067f, 0.069f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.140f, 0.005f, 0.075f, 0.077f, denimR, denimG, denimB),
                LoftRingY(-0.015f, 0f, 0.080f, 0.080f, denimR, denimG, denimB),
                LoftRingY(0.025f, 0f, 0.070f, 0.070f, denimR, denimG, denimB)
            )
        )
        thigh.addEllipsoid(0f, -0.420f, 0f, 0.056f, 0.056f, 0.058f, 10, 14, denimR, denimG, denimB)
        thigh.addEllipsoid(0.064f, -0.195f, 0f, 0.011f, 0.050f, 0.042f, 8, 10, denimR * 0.9f, denimG * 0.9f, denimB * 0.9f)

        // =========================================================================
        // 6. SHIN + SEAMLESS KNEE CAP + RUGGED LEATHER WORK BOOT
        // =========================================================================
        val shin = DynamicMeshBuilder()
        shin.addEllipsoid(0f, 0f, 0f, 0.055f, 0.055f, 0.057f, 10, 14, denimR, denimG, denimB)
        shin.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.424f, 0.002f, 0.052f, 0.055f, leatherR, leatherG, leatherB),
                LoftRingY(-0.352f, 0.000f, 0.051f, 0.053f, leatherR, leatherG, leatherB),
                LoftRingY(-0.334f, 0.000f, 0.056f, 0.058f, 0.16f, 0.11f, 0.09f),
                LoftRingY(-0.324f, 0.000f, 0.055f, 0.057f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.240f, -0.002f, 0.054f, 0.056f, denimR, denimG, denimB),
                LoftRingY(-0.125f, -0.004f, 0.060f, 0.061f, denimR, denimG, denimB),
                LoftRingY(-0.005f, 0.001f, 0.057f, 0.058f, denimR, denimG, denimB)
            )
        )
        shin.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.064f, -0.420f, 0.051f, 0.034f, leatherR * 0.9f, leatherG * 0.9f, leatherB * 0.9f, roundness = 0.85f),
                LoftRingZ(-0.025f, -0.418f, 0.054f, 0.038f, leatherR, leatherG, leatherB, roundness = 0.85f),
                LoftRingZ(0.055f, -0.422f, 0.053f, 0.035f, leatherR, leatherG, leatherB, roundness = 0.88f),
                LoftRingZ(0.125f, -0.428f, 0.052f, 0.031f, leatherR * 0.95f, leatherG * 0.95f, leatherB * 0.95f, roundness = 0.92f),
                LoftRingZ(0.168f, -0.436f, 0.039f, 0.021f, leatherR * 0.86f, leatherG * 0.86f, leatherB * 0.86f, roundness = 0.96f)
            )
        )
        shin.addLoftedProfileZ(
            cx = 0f,
            sectors = 18,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.066f, -0.460f, 0.054f, 0.015f, 0.09f, 0.09f, 0.11f, roundness = 0.65f),
                LoftRingZ(0.005f, -0.460f, 0.055f, 0.014f, 0.10f, 0.10f, 0.12f, roundness = 0.65f),
                LoftRingZ(0.130f, -0.459f, 0.054f, 0.013f, 0.10f, 0.10f, 0.12f, roundness = 0.72f),
                LoftRingZ(0.172f, -0.457f, 0.041f, 0.010f, 0.10f, 0.10f, 0.12f, roundness = 0.85f)
            )
        )
        for (ly in listOf(-0.368f, -0.386f, -0.402f)) {
            shin.addCylinderX(-0.019f, 0.019f, ly, 0.054f, 0.0038f, 8, 0.82f, 0.72f, 0.52f)
        }

        return DriverMeshSet(
            headFaceMesh = headFace,
            headAccMesh = headAcc.build(),
            torsoMesh = torso.build(),
            upperArmMesh = upperArm.build(),
            leftForearmHandMesh = buildForearmHand(isLeft = true),
            rightForearmHandMesh = buildForearmHand(isLeft = false),
            thighMesh = thigh.build(),
            shinBootMesh = shin.build()
        )
    }

    /**
     * Generates a single seamless 36x16 anatomical head mesh with sculpted nose bridge,
     * tip, brow ridge, subtle orbital eye-socket hollows, and chin contour,
     * mapped 1:1 to TextureFactory.buildDriverFaceTexture().
     */
    private fun buildSculptedUvHeadMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val rings = listOf(
            LoftRingY(-0.028f, -0.004f, 0.064f, 0.058f, 1f, 1f, 1f, roundness = 0.95f),
            LoftRingY(-0.004f, -0.004f, 0.060f, 0.055f, 1f, 1f, 1f, roundness = 0.96f),
            LoftRingY(0.020f, -0.003f, 0.056f, 0.053f, 1f, 1f, 1f, roundness = 0.96f),
            LoftRingY(0.045f, 0.006f, 0.057f, 0.060f, 1f, 1f, 1f, roundness = 0.92f),
            LoftRingY(0.064f, 0.012f, 0.060f, 0.068f, 1f, 1f, 1f, roundness = 0.86f),
            LoftRingY(0.084f, 0.014f, 0.068f, 0.075f, 1f, 1f, 1f, roundness = 0.82f),
            LoftRingY(0.104f, 0.014f, 0.075f, 0.081f, 1f, 1f, 1f, roundness = 0.81f),
            LoftRingY(0.122f, 0.012f, 0.079f, 0.084f, 1f, 1f, 1f, roundness = 0.83f),
            LoftRingY(0.138f, 0.011f, 0.082f, 0.086f, 1f, 1f, 1f, roundness = 0.85f),
            LoftRingY(0.156f, 0.010f, 0.083f, 0.087f, 1f, 1f, 1f, roundness = 0.87f),
            LoftRingY(0.174f, 0.008f, 0.084f, 0.087f, 1f, 1f, 1f, roundness = 0.89f),
            LoftRingY(0.194f, 0.003f, 0.082f, 0.085f, 1f, 1f, 1f, roundness = 0.92f),
            LoftRingY(0.214f, -0.002f, 0.079f, 0.082f, 1f, 1f, 1f, roundness = 0.94f),
            LoftRingY(0.234f, -0.006f, 0.070f, 0.074f, 1f, 1f, 1f, roundness = 0.96f),
            LoftRingY(0.246f, -0.008f, 0.056f, 0.060f, 1f, 1f, 1f, roundness = 0.98f),
            LoftRingY(0.254f, -0.010f, 0.036f, 0.040f, 1f, 1f, 1f, roundness = 1.00f)
        )
        val nRings = rings.size
        val sectors = 36
        val posX = Array(nRings) { FloatArray(sectors) }
        val posY = Array(nRings) { FloatArray(sectors) }
        val posZ = Array(nRings) { FloatArray(sectors) }
        val norX = Array(nRings) { FloatArray(sectors) }
        val norY = Array(nRings) { FloatArray(sectors) }
        val norZ = Array(nRings) { FloatArray(sectors) }

        fun sPow(v: Float, p: Float): Float = sign(v) * abs(v).pow(p)

        for (ri in 0 until nRings) {
            val ring = rings[ri]
            for (s in 0 until sectors) {
                val a = (s.toFloat() / sectors) * 2f * PI.toFloat()
                val ca = sPow(cos(a), ring.roundness)
                val sa = sPow(sin(a), ring.roundness)
                val x = ca * ring.rx
                val y = ring.y
                var z = ring.centerZ + sa * ring.rz

                // Sculpted facial reliefs on the front hemisphere (sin(a) > 0)
                if (sa > 0.25f) {
                    val frontMask = ((sa - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    // Anatomical nose ridge & tip centered at x = 0, y = 0.118..0.176
                    val noseX = exp(-(x * x) / (2f * 0.0115f * 0.0115f))
                    val noseY = exp(-((y - 0.132f) * (y - 0.132f)) / (2f * 0.017f * 0.017f))
                    val bridgeY = exp(-((y - 0.156f) * (y - 0.156f)) / (2f * 0.016f * 0.016f)) * 0.55f
                    z += (noseY * 0.022f + bridgeY * 0.013f) * noseX * frontMask

                    // Subtle orbital eye socket hollows around x = +-0.029, y = 0.156
                    for (side in listOf(-1f, 1f)) {
                        val dxEye = x - side * 0.0285f
                        val dyEye = y - 0.157f
                        val eyeSocket = exp(-(dxEye * dxEye) / (2f * 0.012f * 0.012f) - (dyEye * dyEye) / (2f * 0.0085f * 0.0085f))
                        z -= eyeSocket * 0.0042f * frontMask
                    }

                    // Gentle supraorbital brow ridge & chin prominence
                    val browX = exp(-(x * x) / (2f * 0.038f * 0.038f))
                    val browY = exp(-((y - 0.175f) * (y - 0.175f)) / (2f * 0.008f * 0.008f))
                    z += browX * browY * 0.0045f * frontMask

                    val chinX = exp(-(x * x) / (2f * 0.024f * 0.024f))
                    val chinY = exp(-((y - 0.078f) * (y - 0.078f)) / (2f * 0.012f * 0.012f))
                    z += chinX * chinY * 0.0055f * frontMask
                }

                posX[ri][s] = x
                posY[ri][s] = y
                posZ[ri][s] = z
            }
        }

        for (ri in 0 until nRings) {
            val prevR = (ri - 1).coerceAtLeast(0)
            val nextR = (ri + 1).coerceAtMost(nRings - 1)
            for (s in 0 until sectors) {
                val prevS = (s - 1 + sectors) % sectors
                val nextS = (s + 1) % sectors
                val tx = posX[ri][nextS] - posX[ri][prevS]
                val ty = posY[ri][nextS] - posY[ri][prevS]
                val tz = posZ[ri][nextS] - posZ[ri][prevS]
                val bx = posX[nextR][s] - posX[prevR][s]
                val by = posY[nextR][s] - posY[prevR][s]
                val bz = posZ[nextR][s] - posZ[prevR][s]
                var nx = by * tz - bz * ty
                var ny = bz * tx - bx * tz
                var nz = bx * ty - by * tx
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
                nx /= len; ny /= len; nz /= len
                norX[ri][s] = nx; norY[ri][s] = ny; norZ[ri][s] = nz
            }
        }

        for (ri in 0 until nRings - 1) {
            val v0 = ri.toFloat() / (nRings - 1)
            val v1 = (ri + 1).toFloat() / (nRings - 1)
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                val u0 = s.toFloat() / sectors
                val u1 = (s + 1).toFloat() / sectors

                mb.addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, 1f, 1f, 1f)
                mb.addVertex(posX[ri][sNext], posY[ri][sNext], posZ[ri][sNext], norX[ri][sNext], norY[ri][sNext], norZ[ri][sNext], u1, v0, 1f, 1f, 1f)
                mb.addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, 1f, 1f, 1f)

                mb.addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, 1f, 1f, 1f)
                mb.addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, 1f, 1f, 1f)
                mb.addVertex(posX[ri + 1][s], posY[ri + 1][s], posZ[ri + 1][s], norX[ri + 1][s], norY[ri + 1][s], norZ[ri + 1][s], u0, v1, 1f, 1f, 1f)
            }
        }
        // Crown & neck caps
        val top = rings.last()
        val lastIdx = nRings - 1
        for (s in 0 until sectors) {
            val sNext = (s + 1) % sectors
            mb.addVertex(0f, top.y, top.centerZ, 0f, 1f, 0f, 0.75f, 0.96f, 1f, 1f, 1f)
            mb.addVertex(posX[lastIdx][s], top.y, posZ[lastIdx][s], 0f, 1f, 0f, 0.75f, 0.96f, 1f, 1f, 1f)
            mb.addVertex(posX[lastIdx][sNext], top.y, posZ[lastIdx][sNext], 0f, 1f, 0f, 0.75f, 0.96f, 1f, 1f, 1f)
        }
        val bot = rings.first()
        for (s in 0 until sectors) {
            val sNext = (s + 1) % sectors
            mb.addVertex(0f, bot.y, bot.centerZ, 0f, -1f, 0f, 0.25f, 0.02f, 0.85f, 0.85f, 0.85f)
            mb.addVertex(posX[0][sNext], bot.y, posZ[0][sNext], 0f, -1f, 0f, 0.25f, 0.02f, 0.85f, 0.85f, 0.85f)
            mb.addVertex(posX[0][s], bot.y, posZ[0][s], 0f, -1f, 0f, 0.25f, 0.02f, 0.85f, 0.85f, 0.85f)
        }
        return mb.build()
    }
}
