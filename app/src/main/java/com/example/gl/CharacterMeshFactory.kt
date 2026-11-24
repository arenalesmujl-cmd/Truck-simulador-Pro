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
    val cockpitLeftHandMesh: MeshBuffer,
    val cockpitRightHandMesh: MeshBuffer,
    val thighMesh: MeshBuffer,
    val shinBootMesh: MeshBuffer
)

object CharacterMeshFactory {

    /**
     * Anatomically proportioned 3D human driver (~1.84m tall).
     * - Continuous 36x16 sculpted head mesh with recessed orbital sockets, brow ridge,
     *   nose bridge/tip/alar wings, cheekbones, jawline & chin, UV-mapped 1:1 to driverFaceTex.
     * - Separate headAccMesh for anatomical angled ears, fuller hair & 5-panel pro trucker cap.
     * - Seamless spherical shoulder, elbow, hip, and knee joint caps.
     * - Natural curled-finger standing hands + steering-wheel gripping hands in cockpit!
     */
    fun buildDriverMeshes(): DriverMeshSet {
        val skinR = 0.91f; val skinG = 0.70f; val skinB = 0.57f
        val skinWarmR = 0.85f; val skinWarmG = 0.64f; val skinWarmB = 0.51f
        val skinShadowR = 0.74f; val skinShadowG = 0.53f; val skinShadowB = 0.42f

        val hairR = 0.14f; val hairG = 0.11f; val hairB = 0.09f

        // Calibrated lifted sRGB values so pow(c, 2.1) retains rich cloth folds & weave instead of crushing to black
        val jacketR = 0.22f; val jacketG = 0.26f; val jacketB = 0.32f
        val jacketTrimR = 0.29f; val jacketTrimG = 0.34f; val jacketTrimB = 0.41f
        val suedeR = 0.62f; val suedeG = 0.39f; val suedeB = 0.22f
        val henleyR = 0.80f; val henleyG = 0.83f; val henleyB = 0.87f
        val denimR = 0.21f; val denimG = 0.31f; val denimB = 0.48f
        val denimFadeR = 0.29f; val denimFadeG = 0.40f; val denimFadeB = 0.58f
        val leatherR = 0.42f; val leatherG = 0.25f; val leatherB = 0.14f

        // =========================================================================
        // 1A. CONTINUOUS 56x24 SCULPTED UV-MAPPED HEAD & NECK (rendered with driverFaceTex)
        // =========================================================================
        val headFace = buildSculptedUvHeadMesh()

        // =========================================================================
        // 1B. ANATOMICAL LOFTED EARS, TAPERED HAIR & SLEEK 6-PANEL TRUCKER CAP
        // =========================================================================
        val headAcc = DynamicMeshBuilder()
        for (side in listOf(-1f, 1f)) {
            // Curved anatomical outer helix & earlobe + recessed concha shadow
            headAcc.addLoftedProfileY(
                cx = side * 0.081f,
                sectors = 14,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.120f, -0.008f, 0.0070f, 0.009f, skinR, skinG, skinB),
                    LoftRingY(0.136f, -0.010f, 0.0095f, 0.014f, skinWarmR, skinWarmG, skinWarmB),
                    LoftRingY(0.156f, -0.012f, 0.0105f, 0.016f, skinWarmR, skinWarmG, skinWarmB),
                    LoftRingY(0.172f, -0.011f, 0.0080f, 0.012f, skinShadowR, skinShadowG, skinShadowB)
                )
            )
            headAcc.addEllipsoid(side * 0.0845f, 0.149f, -0.008f, 0.0048f, 0.012f, 0.008f, 8, 10, skinShadowR, skinShadowG, skinShadowB)
            // Tapered sideburn & temple hair flush with jawline
            headAcc.addLoftedProfileY(
                cx = side * 0.072f,
                sectors = 14,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.105f, -0.020f, 0.006f, 0.011f, hairR, hairG, hairB),
                    LoftRingY(0.140f, -0.024f, 0.011f, 0.022f, hairR, hairG, hairB),
                    LoftRingY(0.178f, -0.025f, 0.014f, 0.028f, hairR, hairG, hairB)
                )
            )
        }
        // Sculpted smooth occipital & tapered nape hair (replaces blocky rear hair balls)
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 28,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.068f, -0.056f, 0.044f, 0.018f, hairR, hairG, hairB, roundness = 0.90f),
                LoftRingY(0.100f, -0.046f, 0.062f, 0.042f, hairR, hairG, hairB, roundness = 0.92f),
                LoftRingY(0.138f, -0.036f, 0.078f, 0.062f, hairR, hairG, hairB, roundness = 0.94f),
                LoftRingY(0.184f, -0.028f, 0.084f, 0.070f, hairR, hairG, hairB, roundness = 0.96f)
            )
        )

        // Tailored 6-Panel Charcoal-Slate & Burnt-Amber Pro Trucker Cap (natural cranium fit)
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 36,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.188f, 0.000f, 0.088f, 0.092f, 0.17f, 0.20f, 0.25f, roundness = 0.92f),
                LoftRingY(0.214f, -0.003f, 0.089f, 0.093f, 0.21f, 0.25f, 0.31f, roundness = 0.94f),
                LoftRingY(0.242f, -0.006f, 0.076f, 0.080f, 0.21f, 0.25f, 0.31f, roundness = 0.96f),
                LoftRingY(0.258f, -0.008f, 0.042f, 0.046f, 0.18f, 0.22f, 0.28f, roundness = 1.0f)
            )
        )
        // Curved rear snapback strap & buckle arch + top squatcho button & front embroidered patch
        headAcc.addEllipsoid(0f, 0.261f, -0.008f, 0.009f, 0.004f, 0.009f, 8, 12, 0.95f, 0.44f, 0.08f)
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.188f, -0.084f, 0.036f, 0.012f, 0.24f, 0.27f, 0.32f, roundness = 0.85f),
                LoftRingY(0.200f, -0.084f, 0.036f, 0.012f, 0.24f, 0.27f, 0.32f, roundness = 0.85f)
            )
        )
        headAcc.addEllipsoid(0f, 0.194f, -0.095f, 0.012f, 0.0055f, 0.004f, 6, 10, 0.94f, 0.43f, 0.07f)
        headAcc.addEllipsoid(0f, 0.222f, 0.091f, 0.021f, 0.012f, 0.0045f, 8, 12, 0.95f, 0.44f, 0.08f)
        headAcc.addEllipsoid(0f, 0.222f, 0.094f, 0.012f, 0.007f, 0.0035f, 8, 10, 0.92f, 0.94f, 0.96f)
        // Sleek curved matte visor with subtle amber under-brim sandwich trim
        headAcc.addCurvedVisor(
            cy = 0.195f,
            cz = 0.085f,
            width = 0.154f,
            length = 0.078f,
            thickness = 0.0055f,
            archHeight = 0.018f,
            segs = 24,
            r = 0.18f, g = 0.21f, b = 0.26f
        )
        headAcc.addCurvedVisor(
            cy = 0.1925f,
            cz = 0.086f,
            width = 0.150f,
            length = 0.080f,
            thickness = 0.0025f,
            archHeight = 0.018f,
            segs = 24,
            r = 0.94f, g = 0.43f, b = 0.07f
        )

        // =========================================================================
        // 2. ATHLETIC TORSO: SEAMLESS 360-DEGREE JACKET + OPEN-CHEST HENLEY LAYER
        // =========================================================================
        val torso = DynamicMeshBuilder()
        // Lower selvedge denim hip/pelvis, saddle-leather belt & recessed base shirt core + trapezius neck taper
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 28,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.028f, 0.000f, 0.170f, 0.118f, denimR, denimG, denimB, roundness = 0.85f),
                LoftRingY(0.088f, 0.002f, 0.164f, 0.114f, denimR, denimG, denimB, roundness = 0.85f),
                LoftRingY(0.092f, 0.003f, 0.171f, 0.120f, leatherR, leatherG, leatherB, roundness = 0.85f),
                LoftRingY(0.132f, 0.004f, 0.171f, 0.120f, leatherR, leatherG, leatherB, roundness = 0.85f),
                // Narrow recessed inner torso cylinder cleanly contained inside jacket shell & matched to warm neck skin
                LoftRingY(0.138f, 0.004f, 0.146f, 0.104f, jacketR, jacketG, jacketB, roundness = 0.84f),
                LoftRingY(0.525f, 0.002f, 0.130f, 0.090f, henleyR, henleyG, henleyB, roundness = 0.88f),
                LoftRingY(0.554f, 0.000f, 0.086f, 0.068f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.92f),
                LoftRingY(0.585f, -0.002f, 0.061f, 0.056f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.95f)
            )
        )
        // Rib-knit Henley collar band framing the base of the neck cleanly
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 24,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.524f, 0.004f, 0.088f, 0.072f, henleyR * 0.92f, henleyG * 0.92f, henleyB * 0.92f, roundness = 0.90f),
                LoftRingY(0.548f, 0.002f, 0.078f, 0.066f, henleyR * 0.88f, henleyG * 0.88f, henleyB * 0.88f, roundness = 0.92f)
            )
        )

        // Fitted Heather Henley tee chest plate tucked neatly between front jacket lapels
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.136f, 0.044f, 0.066f, 0.072f, henleyR, henleyG, henleyB, roundness = 0.85f),
                LoftRingY(0.250f, 0.052f, 0.068f, 0.074f, henleyR, henleyG, henleyB, roundness = 0.84f),
                LoftRingY(0.380f, 0.058f, 0.074f, 0.078f, henleyR, henleyG, henleyB, roundness = 0.84f),
                LoftRingY(0.490f, 0.048f, 0.076f, 0.074f, henleyR, henleyG, henleyB, roundness = 0.86f),
                LoftRingY(0.546f, 0.015f, 0.068f, 0.064f, henleyR, henleyG, henleyB, roundness = 0.90f)
            )
        )
        // Front Henley placket buttons + brushed steel belt buckle & leather keeper loop
        torso.addEllipsoid(0f, 0.112f, 0.124f, 0.032f, 0.021f, 0.009f, 8, 14, 0.84f, 0.86f, 0.90f)
        torso.addEllipsoid(0f, 0.112f, 0.129f, 0.016f, 0.010f, 0.006f, 6, 10, leatherR, leatherG, leatherB)
        torso.addBox(-0.048f, 0.112f, 0.120f, 0.012f, 0.044f, 0.012f, leatherR * 0.85f, leatherG * 0.85f, leatherB * 0.85f)
        for (by in listOf(0.43f, 0.478f, 0.522f)) {
            torso.addEllipsoid(0f, by, 0.126f, 0.0055f, 0.0055f, 0.004f, 6, 8, 0.26f, 0.28f, 0.32f)
        }

        // Full 360-degree Tailored Canvas Trucker Jacket Shell wrapping back, ribs, shoulders & rolled collar
        // With clean open front chest notch so zero shirt clips through back or sides!
        torso.addOpenFrontJacketShell(
            jacketR = jacketR, jacketG = jacketG, jacketB = jacketB,
            trimR = jacketTrimR, trimG = jacketTrimG, trimB = jacketTrimB,
            suedeR = suedeR, suedeG = suedeG, suedeB = suedeB
        )
        // Back saddle-suede shoulder yoke panel + raised twin front chest flap pockets, brass snap studs & shoulder deltoid caps
        torso.addEllipsoid(0f, 0.492f, -0.066f, 0.185f, 0.056f, 0.068f, 8, 16, suedeR * 0.88f, suedeG * 0.88f, suedeB * 0.88f)
        for (side in listOf(-1f, 1f)) {
            torso.addEllipsoid(side * 0.120f, 0.398f, 0.125f, 0.032f, 0.035f, 0.011f, 6, 12, jacketTrimR, jacketTrimG, jacketTrimB)
            torso.addEllipsoid(side * 0.120f, 0.422f, 0.129f, 0.033f, 0.012f, 0.011f, 6, 10, jacketTrimR * 1.12f, jacketTrimG * 1.12f, jacketTrimB * 1.12f)
            torso.addEllipsoid(side * 0.120f, 0.420f, 0.136f, 0.0055f, 0.0055f, 0.005f, 4, 8, 0.86f, 0.72f, 0.38f)
            torso.addEllipsoid(side * 0.205f, 0.494f, -0.004f, 0.063f, 0.063f, 0.088f, 10, 14, jacketR, jacketG, jacketB)
            // Subtle warm suede epaulet shoulder accent
            torso.addEllipsoid(side * 0.152f, 0.526f, 0.002f, 0.044f, 0.018f, 0.042f, 6, 10, suedeR, suedeG, suedeB)
        }

        // =========================================================================
        // 3. UPPER ARM WITH SEAMLESS SHOULDER & ELBOW SPHERICAL CAPS
        // =========================================================================
        // Muscular biceps-deltoid taper
        val upperArm = DynamicMeshBuilder()
        upperArm.addEllipsoid(0f, 0f, 0f, 0.062f, 0.062f, 0.062f, 10, 14, jacketTrimR, jacketTrimG, jacketTrimB)
        upperArm.addLoftedProfileY(
            cx = 0f,
            sectors = 24,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.295f, 0f, 0.046f, 0.046f, jacketR, jacketG, jacketB),
                LoftRingY(-0.245f, 0f, 0.050f, 0.049f, jacketR, jacketG, jacketB),
                LoftRingY(-0.155f, 0.005f, 0.058f, 0.057f, jacketTrimR, jacketTrimG, jacketTrimB),
                LoftRingY(-0.080f, 0.006f, 0.063f, 0.061f, jacketTrimR, jacketTrimG, jacketTrimB),
                LoftRingY(-0.010f, 0f, 0.064f, 0.064f, jacketTrimR, jacketTrimG, jacketTrimB),
                LoftRingY(0.025f, 0f, 0.054f, 0.054f, jacketTrimR, jacketTrimG, jacketTrimB)
            )
        )
        upperArm.addEllipsoid(0f, -0.290f, 0f, 0.046f, 0.046f, 0.046f, 10, 14, jacketR, jacketG, jacketB)

        // =========================================================================
        // 4. LEFT & RIGHT FOREARMS + RELAXED CURLED HANDS vs STEERING WHEEL GRIP HANDS
        // =========================================================================
        fun buildForearmHand(isLeft: Boolean, grippingWheel: Boolean): MeshBuffer {
            val f = DynamicMeshBuilder()
            f.addEllipsoid(0f, 0f, 0f, 0.045f, 0.045f, 0.045f, 10, 14, jacketR, jacketG, jacketB)
            f.addLoftedProfileY(
                cx = 0f,
                sectors = 24,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(-0.258f, 0f, 0.030f, 0.023f, skinR, skinG, skinB),
                    LoftRingY(-0.220f, 0.002f, 0.036f, 0.032f, skinR, skinG, skinB),
                    LoftRingY(-0.194f, 0.001f, 0.043f, 0.041f, jacketTrimR, jacketTrimG, jacketTrimB),
                    LoftRingY(-0.172f, 0f, 0.045f, 0.044f, jacketTrimR, jacketTrimG, jacketTrimB),
                    LoftRingY(-0.168f, 0f, 0.047f, 0.046f, jacketR, jacketG, jacketB),
                    LoftRingY(-0.080f, 0.002f, 0.051f, 0.049f, jacketR, jacketG, jacketB),
                    LoftRingY(0.000f, 0f, 0.046f, 0.046f, jacketR, jacketG, jacketB)
                )
            )
            if (isLeft) {
                f.addTaperedCylinderY(0f, -0.242f, 0f, 0.035f, 0.035f, 0.017f, 16, 0.11f, 0.12f, 0.14f)
                f.addCylinderZ(0f, -0.233f, 0.023f, 0.036f, 0.015f, 14, 0.82f, 0.85f, 0.90f)
                f.addCylinderZ(0f, -0.233f, 0.035f, 0.039f, 0.0115f, 14, 0.06f, 0.16f, 0.24f)
                f.addEllipsoid(0f, -0.233f, 0.0395f, 0.003f, 0.003f, 0.002f, 4, 6, 0.94f, 0.46f, 0.10f)
            }

            val inwardSign = if (isLeft) -1f else 1f
            // Palm, wrist tendon & fingerless saddle-leather driving glove with knuckle pad
            f.addEllipsoid(0f, -0.278f, 0.002f, 0.019f, 0.035f, 0.031f, 10, 14, leatherR, leatherG, leatherB)
            f.addEllipsoid(0f, -0.294f, 0.003f, 0.016f, 0.013f, 0.031f, 6, 12, leatherR * 1.15f, leatherG * 1.15f, leatherB * 1.15f)
            f.addEllipsoid(-inwardSign * 0.016f, -0.281f, 0.002f, 0.006f, 0.021f, 0.026f, 6, 10, 0.22f, 0.14f, 0.09f)

            if (grippingWheel) {
                // Thumb rests cleanly atop front (+Z) rim bolster pointing slightly inward
                f.addEllipsoid(inwardSign * 0.016f, -0.278f, 0.031f, 0.009f, 0.019f, 0.010f, 8, 10, skinR, skinG, skinB)
                // Four fingers wrap tightly inward around steering wheel rim!
                for (fi in 0 until 4) {
                    val fz = 0.021f - fi * 0.0135f
                    val fLen = when (fi) {
                        1 -> 0.032f
                        2 -> 0.030f
                        0 -> 0.027f
                        else -> 0.024f
                    }
                    // Knuckle joint + inward wrapped distal phalanx hugging rim
                    f.addEllipsoid(inwardSign * 0.003f, -0.302f, fz, 0.0068f, 0.008f, 0.0068f, 6, 8, skinR, skinG, skinB)
                    f.addEllipsoid(inwardSign * (fLen * 0.42f), -0.312f, fz, 0.012f, 0.007f, 0.0062f, 6, 8, skinWarmR, skinWarmG, skinWarmB)
                    f.addEllipsoid(inwardSign * (fLen * 0.72f), -0.302f, fz, 0.006f, 0.008f, 0.0058f, 6, 8, skinR, skinG, skinB)
                }
            } else {
                // Relaxed natural standing/walking hand with softly curved two-segment fingers
                f.addEllipsoid(inwardSign * 0.015f, -0.280f, 0.031f, 0.0085f, 0.019f, 0.009f, 8, 10, skinR, skinG, skinB)
                for (fi in 0 until 4) {
                    val fz = 0.021f - fi * 0.0135f
                    val fLen = when (fi) {
                        1 -> 0.033f
                        2 -> 0.031f
                        0 -> 0.027f
                        else -> 0.024f
                    }
                    val curl = 0.0065f + fi * 0.0012f
                    f.addTaperedCylinderY(0f, -0.296f - fLen * 0.45f, fz, 0.0060f, 0.0072f, fLen * 0.45f, 8, skinWarmR, skinWarmG, skinWarmB)
                    f.addEllipsoid(inwardSign * curl, -0.296f - fLen * 0.68f, fz, 0.0060f, 0.0095f, 0.0058f, 6, 8, skinR, skinG, skinB)
                }
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
        // 6. SHIN + SEAMLESS KNEE CAP + GOODYEAR-WELT LUGGED LEATHER WORK BOOT
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
                LoftRingY(-0.354f, 0.000f, 0.051f, 0.053f, leatherR, leatherG, leatherB),
                LoftRingY(-0.336f, 0.000f, 0.058f, 0.060f, 0.24f, 0.32f, 0.48f),
                LoftRingY(-0.312f, 0.000f, 0.059f, 0.061f, 0.22f, 0.30f, 0.46f),
                LoftRingY(-0.306f, 0.000f, 0.055f, 0.057f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.230f, -0.002f, 0.054f, 0.056f, denimR, denimG, denimB),
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
        // Contrast Goodyear welt trim + rugged lugged Vibram sole
        shin.addLoftedProfileZ(
            cx = 0f,
            sectors = 18,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.066f, -0.460f, 0.054f, 0.015f, 0.16f, 0.16f, 0.19f, roundness = 0.65f),
                LoftRingZ(0.005f, -0.460f, 0.055f, 0.014f, 0.17f, 0.17f, 0.20f, roundness = 0.65f),
                LoftRingZ(0.130f, -0.459f, 0.054f, 0.013f, 0.17f, 0.17f, 0.20f, roundness = 0.72f),
                LoftRingZ(0.172f, -0.457f, 0.041f, 0.010f, 0.17f, 0.17f, 0.20f, roundness = 0.85f)
            )
        )
        for (ly in listOf(-0.368f, -0.386f, -0.402f)) {
            shin.addCylinderX(-0.019f, 0.019f, ly, 0.054f, 0.0038f, 8, 0.82f, 0.72f, 0.52f)
            for (sx in listOf(-0.020f, 0.020f)) {
                shin.addEllipsoid(sx, ly, 0.053f, 0.0042f, 0.0042f, 0.0035f, 4, 6, 0.86f, 0.72f, 0.36f)
            }
        }

        return DriverMeshSet(
            headFaceMesh = headFace,
            headAccMesh = headAcc.build(),
            torsoMesh = torso.build(),
            upperArmMesh = upperArm.build(),
            leftForearmHandMesh = buildForearmHand(isLeft = true, grippingWheel = false),
            rightForearmHandMesh = buildForearmHand(isLeft = false, grippingWheel = false),
            cockpitLeftHandMesh = buildForearmHand(isLeft = true, grippingWheel = true),
            cockpitRightHandMesh = buildForearmHand(isLeft = false, grippingWheel = true),
            thighMesh = thigh.build(),
            shinBootMesh = shin.build()
        )
    }

    /**
     * Generates a single seamless 56x24 anatomical head mesh with sculpted cheekbones,
     * nose bridge/tip/alar wings, brow ridge, orbital eye-socket hollows, and chin contour,
     * UV-anchored 1:1 to TextureFactory.buildDriverFaceTexture().
     */
    private fun buildSculptedUvHeadMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        // Pair each ring with exact UV-v matching TextureFactory.buildDriverFaceTexture() anchors
        val ringSpecs = listOf(
            Pair(LoftRingY(-0.028f, -0.004f, 0.063f, 0.057f, 1f, 1f, 1f, roundness = 0.95f), 0.000f),
            Pair(LoftRingY(-0.004f, -0.004f, 0.059f, 0.054f, 1f, 1f, 1f, roundness = 0.96f), 0.067f),
            Pair(LoftRingY(0.020f, -0.003f, 0.056f, 0.053f, 1f, 1f, 1f, roundness = 0.96f), 0.133f),
            Pair(LoftRingY(0.045f, 0.006f, 0.057f, 0.061f, 1f, 1f, 1f, roundness = 0.92f), 0.200f),
            Pair(LoftRingY(0.064f, 0.012f, 0.060f, 0.070f, 1f, 1f, 1f, roundness = 0.85f), 0.267f), // chin bottom
            Pair(LoftRingY(0.075f, 0.013f, 0.064f, 0.073f, 1f, 1f, 1f, roundness = 0.83f), 0.302f), // chin cleft
            Pair(LoftRingY(0.084f, 0.014f, 0.068f, 0.076f, 1f, 1f, 1f, roundness = 0.82f), 0.333f),
            Pair(LoftRingY(0.096f, 0.014f, 0.072f, 0.079f, 1f, 1f, 1f, roundness = 0.81f), 0.372f),
            Pair(LoftRingY(0.105f, 0.014f, 0.075f, 0.081f, 1f, 1f, 1f, roundness = 0.81f), 0.405f), // lower lip
            Pair(LoftRingY(0.114f, 0.013f, 0.077f, 0.083f, 1f, 1f, 1f, roundness = 0.82f), 0.436f), // upper lip
            Pair(LoftRingY(0.123f, 0.012f, 0.079f, 0.084f, 1f, 1f, 1f, roundness = 0.83f), 0.469f), // philtrum
            Pair(LoftRingY(0.132f, 0.011f, 0.081f, 0.085f, 1f, 1f, 1f, roundness = 0.84f), 0.506f), // nose bulb
            Pair(LoftRingY(0.141f, 0.011f, 0.082f, 0.086f, 1f, 1f, 1f, roundness = 0.85f), 0.542f), // nose bridge
            Pair(LoftRingY(0.149f, 0.010f, 0.0825f, 0.0865f, 1f, 1f, 1f, roundness = 0.86f), 0.573f),
            Pair(LoftRingY(0.156f, 0.010f, 0.083f, 0.087f, 1f, 1f, 1f, roundness = 0.87f), 0.600f), // eyes
            Pair(LoftRingY(0.165f, 0.009f, 0.0835f, 0.087f, 1f, 1f, 1f, roundness = 0.88f), 0.633f), // upper lid
            Pair(LoftRingY(0.174f, 0.008f, 0.084f, 0.087f, 1f, 1f, 1f, roundness = 0.89f), 0.667f), // brow
            Pair(LoftRingY(0.184f, 0.005f, 0.083f, 0.086f, 1f, 1f, 1f, roundness = 0.90f), 0.700f),
            Pair(LoftRingY(0.194f, 0.003f, 0.082f, 0.085f, 1f, 1f, 1f, roundness = 0.92f), 0.733f), // cap rim
            Pair(LoftRingY(0.204f, 0.000f, 0.080f, 0.083f, 1f, 1f, 1f, roundness = 0.93f), 0.766f),
            Pair(LoftRingY(0.214f, -0.002f, 0.078f, 0.081f, 1f, 1f, 1f, roundness = 0.94f), 0.800f),
            Pair(LoftRingY(0.234f, -0.006f, 0.070f, 0.074f, 1f, 1f, 1f, roundness = 0.96f), 0.867f),
            Pair(LoftRingY(0.246f, -0.008f, 0.056f, 0.060f, 1f, 1f, 1f, roundness = 0.98f), 0.933f),
            Pair(LoftRingY(0.254f, -0.010f, 0.036f, 0.040f, 1f, 1f, 1f, roundness = 1.00f), 1.000f)
        )
        val rings = ringSpecs.map { it.first }
        val ringVs = ringSpecs.map { it.second }
        val nRings = rings.size
        val sectors = 56
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
                if (sa > 0.22f) {
                    val frontMask = ((sa - 0.22f) / 0.78f).coerceIn(0f, 1f)
                    // Anatomical nose ridge, bulb & lateral alar flare centered at x = 0, y = 0.128..0.172
                    val noseX = exp(-(x * x) / (2f * 0.0105f * 0.0105f))
                    val noseY = exp(-((y - 0.132f) * (y - 0.132f)) / (2f * 0.0155f * 0.0155f))
                    val bridgeY = exp(-((y - 0.156f) * (y - 0.156f)) / (2f * 0.016f * 0.016f)) * 0.58f
                    z += (noseY * 0.0235f + bridgeY * 0.0135f) * noseX * frontMask

                    val alarY = exp(-((y - 0.130f) * (y - 0.130f)) / (2f * 0.0065f * 0.0065f))
                    for (side in listOf(-1f, 1f)) {
                        val dxAlar = x - side * 0.0118f
                        val alarX = exp(-(dxAlar * dxAlar) / (2f * 0.0048f * 0.0048f))
                        z += alarX * alarY * 0.0062f * frontMask
                    }

                    // Deeper orbital sockets, defined upper/lower eyelid creases & zygomatic cheekbones
                    for (side in listOf(-1f, 1f)) {
                        val dxEye = x - side * 0.0285f
                        val dyEye = y - 0.156f
                        val eyeSocket = exp(-(dxEye * dxEye) / (2f * 0.012f * 0.012f) - (dyEye * dyEye) / (2f * 0.0085f * 0.0085f))
                        z -= eyeSocket * 0.0058f * frontMask

                        val upperLid = exp(-(dxEye * dxEye) / (2f * 0.008f * 0.008f) - ((y - 0.163f) * (y - 0.163f)) / (2f * 0.0032f * 0.0032f))
                        z += upperLid * 0.0035f * frontMask

                        val dxCheek = x - side * 0.042f
                        val dyCheek = y - 0.128f
                        val cheek = exp(-(dxCheek * dxCheek) / (2f * 0.016f * 0.016f) - (dyCheek * dyCheek) / (2f * 0.012f * 0.012f))
                        z += cheek * 0.0058f * frontMask

                        // Strong masculine mandibular jaw angle
                        val dxJaw = x - side * 0.056f
                        val dyJaw = y - 0.072f
                        val jaw = exp(-(dxJaw * dxJaw) / (2f * 0.011f * 0.011f) - (dyJaw * dyJaw) / (2f * 0.012f * 0.012f))
                        z += jaw * 0.0050f * frontMask
                    }

                    // Upper & lower lip protrusion + clean philtrum recess
                    val lipX = exp(-(x * x) / (2f * 0.014f * 0.014f))
                    val upperLipY = exp(-((y - 0.114f) * (y - 0.114f)) / (2f * 0.0042f * 0.0042f))
                    val lowerLipY = exp(-((y - 0.104f) * (y - 0.104f)) / (2f * 0.0045f * 0.0045f))
                    z += (upperLipY * 0.0048f + lowerLipY * 0.0042f) * lipX * frontMask

                    val philtrumX = exp(-(x * x) / (2f * 0.0038f * 0.0038f))
                    val philtrumY = exp(-((y - 0.123f) * (y - 0.123f)) / (2f * 0.0042f * 0.0042f))
                    z -= philtrumX * philtrumY * 0.0024f * frontMask

                    // Gentle supraorbital brow ridge & masculine chin prominence
                    val browX = exp(-(x * x) / (2f * 0.039f * 0.039f))
                    val browY = exp(-((y - 0.174f) * (y - 0.174f)) / (2f * 0.0075f * 0.0075f))
                    z += browX * browY * 0.0060f * frontMask

                    val chinX = exp(-(x * x) / (2f * 0.024f * 0.024f))
                    val chinY = exp(-((y - 0.075f) * (y - 0.075f)) / (2f * 0.012f * 0.012f))
                    z += chinX * chinY * 0.0076f * frontMask
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
            val v0 = ringVs[ri]
            val v1 = ringVs[ri + 1]
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

    /**
     * Generates a seamless tailored C-wrap open-front Trucker Jacket shell wrapping 320 degrees around
     * the back, shoulders, and ribs while framing the inner Henley tee in front without any clipping.
     */
    private fun DynamicMeshBuilder.addOpenFrontJacketShell(
        jacketR: Float, jacketG: Float, jacketB: Float,
        trimR: Float, trimG: Float, trimB: Float,
        suedeR: Float, suedeG: Float, suedeB: Float
    ) {
        data class ShellRing(
            val y: Float,
            val cz: Float,
            val rx: Float,
            val rz: Float,
            val openRad: Float,
            val roundness: Float,
            val r: Float,
            val g: Float,
            val b: Float
        )

        val rings = listOf(
            ShellRing(0.132f, 0.002f, 0.175f, 0.123f, 0.22f, 0.82f, trimR, trimG, trimB),
            ShellRing(0.172f, 0.004f, 0.178f, 0.125f, 0.23f, 0.81f, jacketR, jacketG, jacketB),
            ShellRing(0.285f, 0.008f, 0.184f, 0.129f, 0.27f, 0.79f, jacketR, jacketG, jacketB),
            ShellRing(0.395f, 0.010f, 0.198f, 0.135f, 0.34f, 0.77f, jacketR, jacketG, jacketB),
            ShellRing(0.492f, 0.006f, 0.214f, 0.130f, 0.44f, 0.76f, trimR, trimG, trimB),
            ShellRing(0.556f, 0.000f, 0.132f, 0.095f, 0.54f, 0.86f, suedeR, suedeG, suedeB)
        )

        val nR = rings.size
        val segs = 28
        val px = Array(nR) { FloatArray(segs + 1) }
        val py = Array(nR) { FloatArray(segs + 1) }
        val pz = Array(nR) { FloatArray(segs + 1) }
        val nx = Array(nR) { FloatArray(segs + 1) }
        val ny = Array(nR) { FloatArray(segs + 1) }
        val nz = Array(nR) { FloatArray(segs + 1) }

        fun sPow(v: Float, p: Float): Float = sign(v) * abs(v).pow(p)

        for (ri in 0 until nR) {
            val ring = rings[ri]
            val startA = (PI * 0.5).toFloat() + ring.openRad
            val endA = (PI * 2.5).toFloat() - ring.openRad
            for (s in 0..segs) {
                val t = s.toFloat() / segs
                val a = startA + (endA - startA) * t
                val ca = sPow(cos(a), ring.roundness)
                val sa = sPow(sin(a), ring.roundness)
                px[ri][s] = ca * ring.rx
                py[ri][s] = ring.y
                pz[ri][s] = ring.cz + sa * ring.rz
            }
        }

        for (ri in 0 until nR) {
            val pR = (ri - 1).coerceAtLeast(0)
            val nR2 = (ri + 1).coerceAtMost(nR - 1)
            for (s in 0..segs) {
                val pS = (s - 1).coerceAtLeast(0)
                val nS = (s + 1).coerceAtMost(segs)
                val tx = px[ri][nS] - px[ri][pS]
                val ty = py[ri][nS] - py[ri][pS]
                val tz = pz[ri][nS] - pz[ri][pS]
                val bx = px[nR2][s] - px[pR][s]
                val by = py[nR2][s] - py[pR][s]
                val bz = pz[nR2][s] - pz[pR][s]
                var vx = by * tz - bz * ty
                var vy = bz * tx - bx * tz
                var vz = bx * ty - by * tx
                val len = sqrt(vx * vx + vy * vy + vz * vz).coerceAtLeast(0.0001f)
                vx /= len; vy /= len; vz /= len
                nx[ri][s] = vx; ny[ri][s] = vy; nz[ri][s] = vz
            }
        }

        for (ri in 0 until nR - 1) {
            val r0 = rings[ri]
            val r1 = rings[ri + 1]
            val v0 = ri.toFloat() / (nR - 1)
            val v1 = (ri + 1).toFloat() / (nR - 1)
            for (s in 0 until segs) {
                val u0 = s.toFloat() / segs
                val u1 = (s + 1).toFloat() / segs
                addVertex(px[ri][s], py[ri][s], pz[ri][s], nx[ri][s], ny[ri][s], nz[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(px[ri][s + 1], py[ri][s + 1], pz[ri][s + 1], nx[ri][s + 1], ny[ri][s + 1], nz[ri][s + 1], u1, v0, r0.r, r0.g, r0.b)
                addVertex(px[ri + 1][s + 1], py[ri + 1][s + 1], pz[ri + 1][s + 1], nx[ri + 1][s + 1], ny[ri + 1][s + 1], nz[ri + 1][s + 1], u1, v1, r1.r, r1.g, r1.b)

                addVertex(px[ri][s], py[ri][s], pz[ri][s], nx[ri][s], ny[ri][s], nz[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(px[ri + 1][s + 1], py[ri + 1][s + 1], pz[ri + 1][s + 1], nx[ri + 1][s + 1], ny[ri + 1][s + 1], nz[ri + 1][s + 1], u1, v1, r1.r, r1.g, r1.b)
                addVertex(px[ri + 1][s], py[ri + 1][s], pz[ri + 1][s], nx[ri + 1][s], ny[ri + 1][s], nz[ri + 1][s], u0, v1, r1.r, r1.g, r1.b)
            }
        }

        // Solid tailored front placket edges + folded suede lapel piping along s=0 and s=segs
        val edgeDepth = 0.016f
        for (ri in 0 until nR - 1) {
            val r0 = rings[ri]
            val r1 = rings[ri + 1]
            // Left lapel front thickness edge (s = 0, facing inward +X)
            val lx0 = px[ri][0]; val ly0 = py[ri][0]; val lz0 = pz[ri][0]
            val lx1 = px[ri + 1][0]; val ly1 = py[ri + 1][0]; val lz1 = pz[ri + 1][0]
            addQuad(
                lx0, ly0, lz0,
                lx0, ly0, lz0 - edgeDepth,
                lx1, ly1, lz1 - edgeDepth,
                lx1, ly1, lz1,
                1f, 0f, 0.25f, r0.r * 0.92f, r0.g * 0.92f, r0.b * 0.92f
            )
            // Brass zipper teeth strip along left & right front openings
            addEllipsoid((lx0 + lx1) * 0.5f + 0.003f, (ly0 + ly1) * 0.5f, (lz0 + lz1) * 0.5f - 0.002f, 0.0045f, 0.032f, 0.006f, 5, 8, 0.78f, 0.64f, 0.36f)

            // Right lapel front thickness edge (s = segs, facing inward -X)
            val rx0 = px[ri][segs]; val ry0 = py[ri][segs]; val rz0 = pz[ri][segs]
            val rx1 = px[ri + 1][segs]; val ry1 = py[ri + 1][segs]; val rz1 = pz[ri + 1][segs]
            addQuad(
                rx0, ry0, rz0 - edgeDepth,
                rx0, ry0, rz0,
                rx1, ry1, rz1,
                rx1, ry1, rz1 - edgeDepth,
                -1f, 0f, 0.25f, r0.r * 0.92f, r0.g * 0.92f, r0.b * 0.92f
            )
            addEllipsoid((rx0 + rx1) * 0.5f - 0.003f, (ry0 + ry1) * 0.5f, (rz0 + rz1) * 0.5f - 0.002f, 0.0045f, 0.032f, 0.006f, 5, 8, 0.78f, 0.64f, 0.36f)
        }
    }
}
