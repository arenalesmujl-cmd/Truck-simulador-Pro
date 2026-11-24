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
    val headGlassesFrameMesh: MeshBuffer,
    val headGlassesLensMesh: MeshBuffer,
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
            // Curved anatomical outer helix & earlobe + recessed concha shadow hugged closely to sculpted cranium
            headAcc.addLoftedProfileY(
                cx = side * 0.075f,
                sectors = 16,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.119f, -0.009f, 0.0066f, 0.0086f, skinR, skinG, skinB),
                    LoftRingY(0.135f, -0.012f, 0.0092f, 0.0136f, skinWarmR, skinWarmG, skinWarmB),
                    LoftRingY(0.155f, -0.014f, 0.0104f, 0.0154f, skinWarmR, skinWarmG, skinWarmB),
                    LoftRingY(0.172f, -0.013f, 0.0076f, 0.0115f, skinShadowR, skinShadowG, skinShadowB)
                )
            )
            headAcc.addLoftedProfileY(
                cx = side * 0.0785f,
                sectors = 12,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.132f, -0.010f, 0.0040f, 0.0072f, skinShadowR, skinShadowG, skinShadowB),
                    LoftRingY(0.150f, -0.010f, 0.0050f, 0.0090f, skinShadowR, skinShadowG, skinShadowB),
                    LoftRingY(0.164f, -0.010f, 0.0038f, 0.0068f, skinShadowR, skinShadowG, skinShadowB)
                )
            )
            // Smooth lofted anatomical sideburn hugging ear & cheekbone cleanly (no sausage tubes)
            headAcc.addLoftedProfileY(
                cx = side * 0.070f,
                sectors = 10,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.122f, 0.011f, 0.0022f, 0.0042f, hairR, hairG, hairB),
                    LoftRingY(0.145f, 0.013f, 0.0032f, 0.0074f, hairR, hairG, hairB),
                    LoftRingY(0.172f, 0.014f, 0.0042f, 0.0094f, hairR, hairG, hairB),
                    LoftRingY(0.190f, 0.014f, 0.0046f, 0.0102f, hairR, hairG, hairB)
                )
            )
        }

        // Sleek 6-Panel Charcoal-Slate & Burnt-Amber Pro Trucker Cap (clean smooth crown + flush embroidered crest)
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 36,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.189f, -0.002f, 0.080f, 0.095f, 0.16f, 0.19f, 0.24f, roundness = 0.90f),
                LoftRingY(0.214f, -0.005f, 0.081f, 0.096f, 0.21f, 0.25f, 0.31f, roundness = 0.92f),
                LoftRingY(0.241f, -0.008f, 0.068f, 0.082f, 0.21f, 0.25f, 0.31f, roundness = 0.95f),
                LoftRingY(0.256f, -0.010f, 0.036f, 0.044f, 0.18f, 0.22f, 0.28f, roundness = 1.0f)
            )
        )
        // Curved rear snapback strap & buckle arch + top squatcho button + flush woven patch crest
        headAcc.addEllipsoid(0f, 0.258f, -0.010f, 0.0072f, 0.0032f, 0.0072f, 8, 12, 0.94f, 0.44f, 0.08f)
        headAcc.addLoftedProfileY(
            cx = 0f,
            sectors = 20,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.189f, -0.090f, 0.032f, 0.010f, 0.24f, 0.27f, 0.32f, roundness = 0.85f),
                LoftRingY(0.199f, -0.090f, 0.032f, 0.010f, 0.24f, 0.27f, 0.32f, roundness = 0.85f)
            )
        )
        headAcc.addBeveledBox(0f, 0.194f, -0.099f, 0.022f, 0.010f, 0.005f, 0.0018f, 0.94f, 0.43f, 0.07f)
        headAcc.addBeveledBox(0f, 0.222f, 0.089f, 0.036f, 0.019f, 0.004f, 0.002f, 0.11f, 0.13f, 0.17f)
        headAcc.addBeveledBox(0f, 0.222f, 0.0905f, 0.028f, 0.013f, 0.003f, 0.0015f, 0.95f, 0.44f, 0.08f)
        headAcc.addBeveledBox(0f, 0.222f, 0.0918f, 0.016f, 0.005f, 0.002f, 0.001f, 0.94f, 0.96f, 0.98f)
        // Sleek curved matte visor with subtle amber under-brim sandwich trim
        headAcc.addCurvedVisor(
            cy = 0.195f,
            cz = 0.090f,
            width = 0.146f,
            length = 0.078f,
            thickness = 0.0055f,
            archHeight = 0.018f,
            segs = 24,
            r = 0.18f, g = 0.21f, b = 0.26f
        )
        headAcc.addCurvedVisor(
            cy = 0.1925f,
            cz = 0.091f,
            width = 0.142f,
            length = 0.080f,
            thickness = 0.0025f,
            archHeight = 0.018f,
            segs = 24,
            r = 0.94f, g = 0.43f, b = 0.07f
        )

        // =========================================================================
        // 1C. CLOSE-FITTED SPORT AVIATOR SUNGLASSES (THIN TITANIUM FRAME + CRYSTAL TINT)
        // =========================================================================
        val glassesFrame = DynamicMeshBuilder()
        val glassesLens = DynamicMeshBuilder()
        val fR = 0.30f; val fG = 0.34f; val fB = 0.40f
        // Contoured upper brow bar hugging brow ridge closely (z = 0.104..0.109)
        glassesFrame.addCurvedFingerChain(
            nodes = listOf(
                FingerNode(-0.059f, 0.165f, 0.087f, 0.0019f, fR, fG, fB),
                FingerNode(-0.032f, 0.169f, 0.105f, 0.0021f, fR, fG, fB),
                FingerNode(0.000f, 0.170f, 0.109f, 0.0022f, fR, fG, fB),
                FingerNode(0.032f, 0.169f, 0.105f, 0.0021f, fR, fG, fB),
                FingerNode(0.059f, 0.165f, 0.087f, 0.0019f, fR, fG, fB)
            ),
            sectors = 8
        )
        glassesFrame.addCurvedFingerChain(
            nodes = listOf(
                FingerNode(-0.013f, 0.159f, 0.107f, 0.0018f, fR, fG, fB),
                FingerNode(0.000f, 0.162f, 0.110f, 0.0019f, fR, fG, fB),
                FingerNode(0.013f, 0.159f, 0.107f, 0.0018f, fR, fG, fB)
            ),
            sectors = 8
        )
        for (side in listOf(-1f, 1f)) {
            // Slim temple arms resting cleanly against head & curving over top of ear helix
            glassesFrame.addCurvedFingerChain(
                nodes = listOf(
                    FingerNode(side * 0.059f, 0.165f, 0.087f, 0.0020f, fR, fG, fB),
                    FingerNode(side * 0.070f, 0.166f, 0.040f, 0.0018f, fR, fG, fB),
                    FingerNode(side * 0.074f, 0.167f, -0.006f, 0.0018f, 0.14f, 0.16f, 0.20f),
                    FingerNode(side * 0.072f, 0.153f, -0.024f, 0.0016f, 0.14f, 0.16f, 0.20f)
                ),
                sectors = 8
            )
            glassesFrame.addEllipsoid(side * 0.010f, 0.153f, 0.103f, 0.0018f, 0.0032f, 0.0015f, 4, 6, 0.72f, 0.76f, 0.82f)
            glassesFrame.addCurvedFingerChain(
                nodes = listOf(
                    FingerNode(side * 0.0125f, 0.158f, 0.106f, 0.0017f, fR, fG, fB),
                    FingerNode(side * 0.016f, 0.145f, 0.104f, 0.0017f, fR, fG, fB),
                    FingerNode(side * 0.034f, 0.143f, 0.101f, 0.0017f, fR, fG, fB),
                    FingerNode(side * 0.052f, 0.147f, 0.092f, 0.0017f, fR, fG, fB),
                    FingerNode(side * 0.059f, 0.162f, 0.087f, 0.0019f, fR, fG, fB)
                ),
                sectors = 8
            )

            // Crystal-tinted polarized smoke lens (alpha 0.44) showing crisp eyes & reflective sky sheen
            val lensXs = floatArrayOf(0.0125f, 0.0245f, 0.0370f, 0.0485f, 0.0578f)
            val lensZs = floatArrayOf(0.1058f, 0.1052f, 0.1026f, 0.0968f, 0.0882f)
            val lensBots = floatArrayOf(0.1460f, 0.1435f, 0.1428f, 0.1448f, 0.1486f)
            val lensTops = floatArrayOf(0.1670f, 0.1686f, 0.1690f, 0.1675f, 0.1646f)
            for (li in 0 until 4) {
                val x0 = side * lensXs[li]
                val x1 = side * lensXs[li + 1]
                val z0 = lensZs[li]
                val z1 = lensZs[li + 1]
                val b0 = lensBots[li]
                val b1 = lensBots[li + 1]
                val t0 = lensTops[li]
                val t1 = lensTops[li + 1]
                val nxAvg = side * (0.08f + li * 0.12f)
                val nzAvg = sqrt((1f - nxAvg * nxAvg).coerceAtLeast(0.1f))
                val shade = 1.0f - li * 0.05f
                if (side > 0f) {
                    glassesLens.addQuad(x0, b0, z0, x1, b1, z1, x1, t1, z1, x0, t0, z0, nxAvg, 0.06f, nzAvg, 0.06f * shade, 0.07f * shade, 0.09f * shade, 0.44f)
                } else {
                    glassesLens.addQuad(x1, b1, z1, x0, b0, z0, x0, t0, z0, x1, t1, z1, nxAvg, 0.06f, nzAvg, 0.06f * shade, 0.07f * shade, 0.09f * shade, 0.44f)
                }
            }
            glassesFrame.addBox(side * 0.060f, 0.165f, 0.086f, 0.003f, 0.004f, 0.006f, 0.94f, 0.44f, 0.08f)
        }

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
                LoftRingY(0.138f, 0.004f, 0.144f, 0.102f, jacketR, jacketG, jacketB, roundness = 0.84f),
                LoftRingY(0.525f, 0.002f, 0.126f, 0.088f, henleyR, henleyG, henleyB, roundness = 0.88f),
                LoftRingY(0.554f, 0.000f, 0.072f, 0.059f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.92f),
                LoftRingY(0.585f, -0.003f, 0.051f, 0.047f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.95f)
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
        // Front Henley placket buttons + chamfered brushed steel belt buckle & keeper loops
        torso.addBeveledBox(0f, 0.112f, 0.123f, 0.056f, 0.042f, 0.014f, 0.005f, 0.84f, 0.86f, 0.90f)
        torso.addBeveledBox(0f, 0.112f, 0.126f, 0.032f, 0.022f, 0.012f, 0.003f, leatherR, leatherG, leatherB)
        for (lx in listOf(-0.048f, 0.048f, -0.145f, 0.145f)) {
            torso.addBox(lx, 0.112f, 0.116f, 0.013f, 0.044f, 0.016f, leatherR * 0.82f, leatherG * 0.82f, leatherB * 0.82f)
        }
        for (by in listOf(0.43f, 0.478f, 0.522f)) {
            torso.addCylinderZ(0f, by, 0.123f, 0.128f, 0.0052f, 10, 0.26f, 0.28f, 0.32f)
        }
        // Tactical waist belt key-hook & miniature CB/rig keyfob at right hip
        torso.addBox(-0.158f, 0.105f, 0.062f, 0.014f, 0.038f, 0.016f, 0.78f, 0.80f, 0.85f)
        torso.addCylinderY(-0.162f, 0.052f, 0.062f, 0.008f, 0.042f, 8, 0.14f, 0.16f, 0.19f)
        torso.addEllipsoid(-0.162f, 0.048f, 0.062f, 0.010f, 0.018f, 0.008f, 6, 8, 0.94f, 0.44f, 0.08f)

        // Full 360-degree Tailored Canvas Trucker Jacket Shell wrapping back, ribs, shoulders & rolled collar
        // With clean open front chest notch so zero shirt clips through back or sides!
        torso.addOpenFrontJacketShell(
            jacketR = jacketR, jacketG = jacketG, jacketB = jacketB,
            trimR = jacketTrimR, trimG = jacketTrimG, trimB = jacketTrimB,
            suedeR = suedeR, suedeG = suedeG, suedeB = suedeB
        )
        // Sculpted folded suede collar band framing neck + open front suede-lined lapels + saddle-suede back yoke
        torso.addFoldedJacketCollar(suedeR = suedeR, suedeG = suedeG, suedeB = suedeB)
        torso.addBeveledBox(0f, 0.494f, -0.095f, 0.35f, 0.096f, 0.068f, 0.018f, suedeR * 0.88f, suedeG * 0.88f, suedeB * 0.88f)
        for (side in listOf(-1f, 1f)) {
            // Turned-out front suede lapel flap framing chest Henley cleanly
            torso.addSlopedBox(side * 0.072f, 0.492f, 0.106f, 0.042f, 0.022f, 0.058f, 0.024f, 0.074f, -0.008f, suedeR * 0.96f, suedeG * 0.96f, suedeB * 0.96f)
            torso.addBeveledBox(side * 0.114f, 0.388f, 0.121f, 0.060f, 0.064f, 0.012f, 0.005f, jacketTrimR, jacketTrimG, jacketTrimB)
            torso.addBeveledBox(side * 0.114f, 0.417f, 0.123f, 0.064f, 0.019f, 0.014f, 0.004f, jacketTrimR * 1.10f, jacketTrimG * 1.10f, jacketTrimB * 1.10f)
            torso.addCylinderZ(side * 0.114f, 0.416f, 0.128f, 0.132f, 0.0046f, 10, 0.88f, 0.74f, 0.38f)
            // Lower welt hand-warmer pockets + brushed brass zipper tab
            torso.addBeveledBox(side * 0.122f, 0.228f, 0.115f, 0.052f, 0.068f, 0.012f, 0.004f, jacketTrimR * 0.95f, jacketTrimG * 0.95f, jacketTrimB * 0.95f)
            torso.addBox(side * 0.100f, 0.228f, 0.123f, 0.007f, 0.050f, 0.005f, 0.80f, 0.66f, 0.36f)
            torso.addEllipsoid(side * 0.198f, 0.494f, -0.004f, 0.061f, 0.060f, 0.082f, 10, 14, jacketTrimR, jacketTrimG, jacketTrimB)
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
                // Watch seated anatomically on outer dorsal wrist (+X) instead of thumb edge (+Z)
                f.addTaperedCylinderY(0f, -0.242f, 0f, 0.032f, 0.033f, 0.017f, 16, 0.11f, 0.12f, 0.14f)
                f.addCylinderX(0.021f, 0.033f, -0.233f, 0f, 0.0145f, 14, 0.82f, 0.85f, 0.90f)
                f.addCylinderX(0.032f, 0.036f, -0.233f, 0f, 0.0112f, 14, 0.06f, 0.16f, 0.24f)
                f.addCylinderZ(0.0355f, -0.233f, -0.002f, 0.007f, 0.0014f, 6, 0.94f, 0.96f, 0.98f)
                f.addEllipsoid(0.036f, -0.233f, 0f, 0.0015f, 0.0028f, 0.0028f, 4, 6, 0.94f, 0.46f, 0.10f)
            }

            val inwardSign = if (isLeft) -1f else 1f
            // Sculpted anatomical palm & fingerless saddle-leather driving glove with knuckle bolster
            f.addLoftedProfileY(
                cx = 0f,
                sectors = 18,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(-0.300f, 0.002f, 0.014f, 0.029f, leatherR * 1.08f, leatherG * 1.08f, leatherB * 1.08f, roundness = 0.72f),
                    LoftRingY(-0.278f, 0.002f, 0.018f, 0.032f, leatherR, leatherG, leatherB, roundness = 0.76f),
                    LoftRingY(-0.256f, 0.000f, 0.018f, 0.026f, skinR, skinG, skinB, roundness = 0.86f)
                )
            )
            // Padded dorsal leather glove protector, stitched knuckle bumps & brass wrist strap snap
            f.addBeveledBox(-inwardSign * 0.013f, -0.281f, 0.002f, 0.010f, 0.032f, 0.048f, 0.004f, 0.24f, 0.15f, 0.10f)
            for (kz in listOf(0.018f, 0.005f, -0.008f, -0.020f)) {
                f.addEllipsoid(-inwardSign * 0.016f, -0.294f, kz, 0.0035f, 0.0045f, 0.0050f, 4, 6, leatherR * 1.15f, leatherG * 1.15f, leatherB * 1.15f)
            }
            f.addCylinderX(-inwardSign * 0.020f, -inwardSign * 0.014f, -0.264f, 0.002f, 0.0042f, 8, 0.86f, 0.72f, 0.36f)

            if (grippingWheel) {
                // Continuous articulated thumb resting cleanly atop front rim bolster
                f.addCurvedFingerChain(
                    nodes = listOf(
                        FingerNode(inwardSign * 0.011f, -0.268f, 0.024f, 0.0092f, skinWarmR, skinWarmG, skinWarmB),
                        FingerNode(inwardSign * 0.017f, -0.279f, 0.034f, 0.0080f, skinR, skinG, skinB),
                        FingerNode(inwardSign * 0.016f, -0.292f, 0.037f, 0.0066f, skinR, skinG, skinB)
                    ),
                    sectors = 10
                )
                // Four smooth continuous articulated fingers wrapping around steering wheel rim!
                for (fi in 0 until 4) {
                    val fz = 0.021f - fi * 0.0135f
                    val fLen = when (fi) {
                        1 -> 0.034f
                        2 -> 0.032f
                        0 -> 0.029f
                        else -> 0.025f
                    }
                    val baseR = if (fi == 3) 0.0056f else 0.0068f
                    f.addCurvedFingerChain(
                        nodes = listOf(
                            FingerNode(inwardSign * 0.001f, -0.296f, fz, baseR, skinWarmR, skinWarmG, skinWarmB),
                            FingerNode(inwardSign * (fLen * 0.36f), -0.312f, fz, baseR * 0.92f, skinR, skinG, skinB),
                            FingerNode(inwardSign * (fLen * 0.74f), -0.310f, fz, baseR * 0.84f, skinWarmR, skinWarmG, skinWarmB),
                            FingerNode(inwardSign * (fLen * 0.82f), -0.296f, fz, baseR * 0.74f, skinR, skinG, skinB)
                        ),
                        sectors = 10
                    )
                }
            } else {
                // Relaxed natural standing/walking hand with smooth 3-joint curved fingers & thumb
                f.addCurvedFingerChain(
                    nodes = listOf(
                        FingerNode(inwardSign * 0.010f, -0.266f, 0.024f, 0.0090f, skinWarmR, skinWarmG, skinWarmB),
                        FingerNode(inwardSign * 0.015f, -0.281f, 0.034f, 0.0076f, skinR, skinG, skinB),
                        FingerNode(inwardSign * 0.013f, -0.295f, 0.035f, 0.0062f, skinR, skinG, skinB)
                    ),
                    sectors = 10
                )
                for (fi in 0 until 4) {
                    val fz = 0.021f - fi * 0.0135f
                    val fLen = when (fi) {
                        1 -> 0.036f
                        2 -> 0.034f
                        0 -> 0.030f
                        else -> 0.025f
                    }
                    val baseR = if (fi == 3) 0.0054f else 0.0066f
                    val curlScale = 0.0095f + fi * 0.0018f
                    f.addCurvedFingerChain(
                        nodes = listOf(
                            FingerNode(0.000f, -0.296f, fz, baseR, skinWarmR, skinWarmG, skinWarmB),
                            FingerNode(inwardSign * (curlScale * 0.28f), -0.296f - fLen * 0.42f, fz, baseR * 0.90f, skinR, skinG, skinB),
                            FingerNode(inwardSign * (curlScale * 0.85f), -0.296f - fLen * 0.78f, fz, baseR * 0.82f, skinWarmR, skinWarmG, skinWarmB),
                            FingerNode(inwardSign * (curlScale * 1.25f), -0.296f - fLen * 0.96f, fz, baseR * 0.70f, skinR, skinG, skinB)
                        ),
                        sectors = 10
                    )
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
        // Symmetric tailored selvedge side seams & subtle knee whisker creases on both thighs
        for (sx in listOf(-0.064f, 0.064f)) {
            thigh.addBox(sx, -0.210f, 0f, 0.005f, 0.36f, 0.010f, denimR * 0.78f, denimG * 0.78f, denimB * 0.78f)
        }

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
                LoftRingY(-0.340f, 0.000f, 0.054f, 0.056f, leatherR * 0.76f, leatherG * 0.76f, leatherB * 0.76f),
                LoftRingY(-0.334f, 0.000f, 0.059f, 0.061f, 0.24f, 0.32f, 0.48f),
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
        // Contrast Goodyear welt trim + rugged lugged Vibram sole + notched lug blocks
        shin.addLoftedProfileZ(
            cx = 0f,
            sectors = 18,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.066f, -0.456f, 0.054f, 0.014f, 0.16f, 0.16f, 0.19f, roundness = 0.65f),
                LoftRingZ(0.005f, -0.456f, 0.055f, 0.013f, 0.17f, 0.17f, 0.20f, roundness = 0.65f),
                LoftRingZ(0.130f, -0.455f, 0.054f, 0.012f, 0.17f, 0.17f, 0.20f, roundness = 0.72f),
                LoftRingZ(0.172f, -0.453f, 0.041f, 0.010f, 0.17f, 0.17f, 0.20f, roundness = 0.85f)
            )
        )
        // Outsole traction lug blocks (bottom flush at -0.474f) + amber Goodyear welt seam
        for (lugZ in listOf(-0.048f, -0.018f, 0.050f, 0.082f, 0.114f, 0.144f)) {
            shin.addBox(0f, -0.468f, lugZ, 0.104f, 0.012f, 0.018f, 0.12f, 0.12f, 0.15f)
        }
        shin.addBox(0f, -0.444f, 0.052f, 0.112f, 0.005f, 0.234f, 0.76f, 0.56f, 0.28f)
        for (ly in listOf(-0.368f, -0.386f, -0.402f)) {
            shin.addCylinderX(-0.019f, 0.019f, ly, 0.054f, 0.0038f, 8, 0.82f, 0.72f, 0.52f)
            for (sx in listOf(-0.020f, 0.020f)) {
                shin.addEllipsoid(sx, ly, 0.053f, 0.0042f, 0.0042f, 0.0035f, 4, 6, 0.86f, 0.72f, 0.36f)
            }
        }

        return DriverMeshSet(
            headFaceMesh = headFace,
            headAccMesh = headAcc.build(),
            headGlassesFrameMesh = glassesFrame.build(),
            headGlassesLensMesh = glassesLens.build(),
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
            // Tapered lower neck base + chiseled masculine oval cranium (narrower temples, deeper front-to-back profile)
            Pair(LoftRingY(-0.028f, -0.003f, 0.055f, 0.050f, 1f, 1f, 1f, roundness = 0.95f), 0.000f),
            Pair(LoftRingY(-0.004f, -0.003f, 0.056f, 0.051f, 1f, 1f, 1f, roundness = 0.96f), 0.067f),
            Pair(LoftRingY(0.020f, -0.002f, 0.056f, 0.053f, 1f, 1f, 1f, roundness = 0.95f), 0.133f),
            Pair(LoftRingY(0.045f, 0.005f, 0.057f, 0.063f, 1f, 1f, 1f, roundness = 0.88f), 0.200f),
            Pair(LoftRingY(0.064f, 0.011f, 0.058f, 0.074f, 1f, 1f, 1f, roundness = 0.80f), 0.267f), // chin bottom
            Pair(LoftRingY(0.075f, 0.012f, 0.061f, 0.077f, 1f, 1f, 1f, roundness = 0.79f), 0.302f), // chin cleft
            Pair(LoftRingY(0.084f, 0.013f, 0.065f, 0.080f, 1f, 1f, 1f, roundness = 0.78f), 0.333f),
            Pair(LoftRingY(0.096f, 0.013f, 0.068f, 0.084f, 1f, 1f, 1f, roundness = 0.78f), 0.372f),
            Pair(LoftRingY(0.105f, 0.012f, 0.070f, 0.086f, 1f, 1f, 1f, roundness = 0.79f), 0.405f), // lower lip
            Pair(LoftRingY(0.114f, 0.011f, 0.072f, 0.088f, 1f, 1f, 1f, roundness = 0.80f), 0.436f), // upper lip
            Pair(LoftRingY(0.123f, 0.010f, 0.074f, 0.089f, 1f, 1f, 1f, roundness = 0.81f), 0.469f), // philtrum
            Pair(LoftRingY(0.132f, 0.009f, 0.076f, 0.090f, 1f, 1f, 1f, roundness = 0.82f), 0.506f), // nose bulb
            Pair(LoftRingY(0.141f, 0.008f, 0.0765f, 0.091f, 1f, 1f, 1f, roundness = 0.83f), 0.542f), // nose bridge
            Pair(LoftRingY(0.149f, 0.008f, 0.077f, 0.0915f, 1f, 1f, 1f, roundness = 0.84f), 0.573f),
            Pair(LoftRingY(0.156f, 0.007f, 0.077f, 0.092f, 1f, 1f, 1f, roundness = 0.85f), 0.600f), // eyes
            Pair(LoftRingY(0.165f, 0.006f, 0.0775f, 0.092f, 1f, 1f, 1f, roundness = 0.86f), 0.633f), // upper lid
            Pair(LoftRingY(0.174f, 0.005f, 0.0775f, 0.092f, 1f, 1f, 1f, roundness = 0.87f), 0.667f), // brow
            Pair(LoftRingY(0.184f, 0.002f, 0.0765f, 0.091f, 1f, 1f, 1f, roundness = 0.89f), 0.700f),
            Pair(LoftRingY(0.194f, 0.000f, 0.0755f, 0.090f, 1f, 1f, 1f, roundness = 0.91f), 0.733f), // cap rim
            Pair(LoftRingY(0.204f, -0.003f, 0.074f, 0.088f, 1f, 1f, 1f, roundness = 0.92f), 0.766f),
            Pair(LoftRingY(0.214f, -0.005f, 0.072f, 0.086f, 1f, 1f, 1f, roundness = 0.93f), 0.800f),
            Pair(LoftRingY(0.234f, -0.008f, 0.065f, 0.079f, 1f, 1f, 1f, roundness = 0.95f), 0.867f),
            Pair(LoftRingY(0.246f, -0.010f, 0.052f, 0.064f, 1f, 1f, 1f, roundness = 0.98f), 0.933f),
            Pair(LoftRingY(0.254f, -0.012f, 0.034f, 0.044f, 1f, 1f, 1f, roundness = 1.00f), 1.000f)
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

                // Sculpted masculine facial reliefs on the front hemisphere (sin(a) > 0)
                if (sa > 0.20f) {
                    val frontMask = ((sa - 0.20f) / 0.80f).coerceIn(0f, 1f)
                    // Anatomical nose ridge, bulb & lateral alar flare centered at x = 0, y = 0.128..0.172
                    val noseX = exp(-(x * x) / (2f * 0.0096f * 0.0096f))
                    val noseY = exp(-((y - 0.132f) * (y - 0.132f)) / (2f * 0.0155f * 0.0155f))
                    val bridgeY = exp(-((y - 0.156f) * (y - 0.156f)) / (2f * 0.016f * 0.016f)) * 0.64f
                    z += (noseY * 0.0255f + bridgeY * 0.0155f) * noseX * frontMask

                    val alarY = exp(-((y - 0.130f) * (y - 0.130f)) / (2f * 0.0062f * 0.0062f))
                    for (side in listOf(-1f, 1f)) {
                        val dxAlar = x - side * 0.0112f
                        val alarX = exp(-(dxAlar * dxAlar) / (2f * 0.0046f * 0.0046f))
                        z += alarX * alarY * 0.0065f * frontMask
                    }

                    // Deepened orbital sockets, temple hollows, cheekbones & buccal hollow
                    for (side in listOf(-1f, 1f)) {
                        val dxEye = x - side * 0.0272f
                        val dyEye = y - 0.156f
                        val eyeSocket = exp(-(dxEye * dxEye) / (2f * 0.012f * 0.012f) - (dyEye * dyEye) / (2f * 0.0085f * 0.0085f))
                        z -= eyeSocket * 0.0082f * frontMask

                        val upperLid = exp(-(dxEye * dxEye) / (2f * 0.008f * 0.008f) - ((y - 0.163f) * (y - 0.163f)) / (2f * 0.0032f * 0.0032f))
                        z += upperLid * 0.0038f * frontMask

                        val dxCheek = x - side * 0.039f
                        val dyCheek = y - 0.130f
                        val cheek = exp(-(dxCheek * dxCheek) / (2f * 0.0145f * 0.0145f) - (dyCheek * dyCheek) / (2f * 0.0115f * 0.0115f))
                        z += cheek * 0.0074f * frontMask

                        // Subtle chiseled buccal hollow & temple contour
                        val dxHollow = x - side * 0.041f
                        val dyHollow = y - 0.105f
                        val buccal = exp(-(dxHollow * dxHollow) / (2f * 0.0115f * 0.0115f) - (dyHollow * dyHollow) / (2f * 0.010f * 0.010f))
                        z -= buccal * 0.0042f * frontMask

                        val dxTemple = x - side * 0.056f
                        val dyTemple = y - 0.172f
                        val temple = exp(-(dxTemple * dxTemple) / (2f * 0.010f * 0.010f) - (dyTemple * dyTemple) / (2f * 0.011f * 0.011f))
                        z -= temple * 0.0035f * frontMask

                        // Strong masculine mandibular jaw angle
                        val dxJaw = x - side * 0.051f
                        val dyJaw = y - 0.072f
                        val jaw = exp(-(dxJaw * dxJaw) / (2f * 0.0105f * 0.0105f) - (dyJaw * dyJaw) / (2f * 0.012f * 0.012f))
                        z += jaw * 0.0074f * frontMask
                    }

                    // Upper & lower lip protrusion + clean philtrum recess
                    val lipX = exp(-(x * x) / (2f * 0.0135f * 0.0135f))
                    val upperLipY = exp(-((y - 0.114f) * (y - 0.114f)) / (2f * 0.0042f * 0.0042f))
                    val lowerLipY = exp(-((y - 0.104f) * (y - 0.104f)) / (2f * 0.0045f * 0.0045f))
                    z += (upperLipY * 0.0052f + lowerLipY * 0.0045f) * lipX * frontMask

                    val philtrumX = exp(-(x * x) / (2f * 0.0038f * 0.0038f))
                    val philtrumY = exp(-((y - 0.123f) * (y - 0.123f)) / (2f * 0.0042f * 0.0042f))
                    z -= philtrumX * philtrumY * 0.0028f * frontMask

                    // Prominent supraorbital brow ridge, squared masculine chin & subtle Adam's apple
                    val browX = exp(-(x * x) / (2f * 0.039f * 0.039f))
                    val browY = exp(-((y - 0.174f) * (y - 0.174f)) / (2f * 0.0075f * 0.0075f))
                    z += browX * browY * 0.0094f * frontMask

                    val chinX = exp(-(x * x) / (2f * 0.023f * 0.023f))
                    val chinY = exp(-((y - 0.074f) * (y - 0.074f)) / (2f * 0.012f * 0.012f))
                    z += chinX * chinY * 0.0106f * frontMask

                    val larynxX = exp(-(x * x) / (2f * 0.009f * 0.009f))
                    val larynxY = exp(-((y - 0.022f) * (y - 0.022f)) / (2f * 0.008f * 0.008f))
                    z += larynxX * larynxY * 0.0042f * frontMask
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
            mb.addVertex(0f, bot.y, bot.centerZ, 0f, -1f, 0f, 0.25f, 0.02f, 1f, 1f, 1f)
            mb.addVertex(posX[0][sNext], bot.y, posZ[0][sNext], 0f, -1f, 0f, 0.25f, 0.02f, 1f, 1f, 1f)
            mb.addVertex(posX[0][s], bot.y, posZ[0][s], 0f, -1f, 0f, 0.25f, 0.02f, 1f, 1f, 1f)
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
            ShellRing(0.132f, 0.002f, 0.168f, 0.118f, 0.22f, 0.82f, trimR, trimG, trimB),
            ShellRing(0.172f, 0.004f, 0.171f, 0.120f, 0.23f, 0.81f, jacketR, jacketG, jacketB),
            ShellRing(0.285f, 0.008f, 0.179f, 0.125f, 0.27f, 0.79f, jacketR, jacketG, jacketB),
            ShellRing(0.395f, 0.010f, 0.194f, 0.132f, 0.34f, 0.77f, jacketR, jacketG, jacketB),
            ShellRing(0.492f, 0.006f, 0.206f, 0.126f, 0.44f, 0.76f, trimR, trimG, trimB),
            ShellRing(0.556f, 0.000f, 0.126f, 0.092f, 0.54f, 0.86f, suedeR, suedeG, suedeB)
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

        // Solid tailored front placket edges + continuous brass zipper trim along s=0 and s=segs
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
            // Continuous brushed-brass zipper teeth strip along left front opening
            addQuad(
                lx0 + 0.003f, ly0, lz0 + 0.002f,
                lx0 - 0.003f, ly0, lz0 + 0.002f,
                lx1 - 0.003f, ly1, lz1 + 0.002f,
                lx1 + 0.003f, ly1, lz1 + 0.002f,
                0f, 0f, 1f, 0.80f, 0.66f, 0.36f
            )

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
            // Continuous brushed-brass zipper teeth strip along right front opening
            addQuad(
                rx0 + 0.003f, ry0, rz0 + 0.002f,
                rx0 - 0.003f, ry0, rz0 + 0.002f,
                rx1 - 0.003f, ry1, rz1 + 0.002f,
                rx1 + 0.003f, ry1, rz1 + 0.002f,
                0f, 0f, 1f, 0.80f, 0.66f, 0.36f
            )
        }
    }

    private fun DynamicMeshBuilder.addFoldedJacketCollar(
        suedeR: Float, suedeG: Float, suedeB: Float
    ) {
        val segs = 20
        val startA = (PI * 0.5).toFloat() + 0.56f
        val endA = (PI * 2.5).toFloat() - 0.56f
        val inRx = 0.094f; val inRz = 0.078f
        val outRx = 0.146f; val outRz = 0.114f
        val yIn = 0.560f; val yOut = 0.522f
        for (s in 0 until segs) {
            val a0 = startA + (endA - startA) * (s.toFloat() / segs)
            val a1 = startA + (endA - startA) * ((s + 1).toFloat() / segs)
            val ix0 = cos(a0) * inRx; val iz0 = sin(a0) * inRz
            val ix1 = cos(a1) * inRx; val iz1 = sin(a1) * inRz
            val ox0 = cos(a0) * outRx; val oz0 = sin(a0) * outRz
            val ox1 = cos(a1) * outRx; val oz1 = sin(a1) * outRz
            addQuad(
                ix0, yIn, iz0,
                ix1, yIn, iz1,
                ox1, yOut, oz1,
                ox0, yOut, oz0,
                cos((a0 + a1) * 0.5f) * 0.5f, 0.78f, sin((a0 + a1) * 0.5f) * 0.5f,
                suedeR, suedeG, suedeB
            )
            addQuad(
                ox0, yOut - 0.012f, oz0,
                ox1, yOut - 0.012f, oz1,
                ox1, yOut, oz1,
                ox0, yOut, oz0,
                cos((a0 + a1) * 0.5f), -0.15f, sin((a0 + a1) * 0.5f),
                suedeR * 0.86f, suedeG * 0.86f, suedeB * 0.86f
            )
        }
    }

    private data class FingerNode(
        val x: Float,
        val y: Float,
        val z: Float,
        val radius: Float,
        val r: Float,
        val g: Float,
        val b: Float
    )

    private fun DynamicMeshBuilder.addCurvedFingerChain(
        nodes: List<FingerNode>,
        sectors: Int = 10
    ) {
        if (nodes.size < 2) return
        val n = nodes.size
        val ringX = Array(n) { FloatArray(sectors) }
        val ringY = Array(n) { FloatArray(sectors) }
        val ringZ = Array(n) { FloatArray(sectors) }
        val ringNx = Array(n) { FloatArray(sectors) }
        val ringNy = Array(n) { FloatArray(sectors) }
        val ringNz = Array(n) { FloatArray(sectors) }

        for (i in 0 until n) {
            val prev = nodes[(i - 1).coerceAtLeast(0)]
            val next = nodes[(i + 1).coerceAtMost(n - 1)]
            var tx = next.x - prev.x
            var ty = next.y - prev.y
            var tz = next.z - prev.z
            val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(0.0001f)
            tx /= tLen; ty /= tLen; tz /= tLen

            // Stable orthonormal basis around local finger tangent (prefer Z as lateral axis unless parallel)
            val upX = 0f; val upY = 0f; val upZ = if (abs(tz) > 0.9f) 0f else 1f
            val altY = if (abs(tz) > 0.9f) 1f else 0f
            var bx = ty * upZ - tz * altY
            var by = tz * upX - tx * upZ
            var bz = tx * altY - ty * upX
            val bLen = sqrt(bx * bx + by * by + bz * bz).coerceAtLeast(0.0001f)
            bx /= bLen; by /= bLen; bz /= bLen

            val nxAxis = ty * bz - tz * by
            val nyAxis = tz * bx - tx * bz
            val nzAxis = tx * by - ty * bx

            val node = nodes[i]
            for (s in 0 until sectors) {
                val a = (s.toFloat() / sectors) * 2f * PI.toFloat()
                val ca = cos(a); val sa = sin(a)
                val dx = bx * ca + nxAxis * sa
                val dy = by * ca + nyAxis * sa
                val dz = bz * ca + nzAxis * sa
                ringX[i][s] = node.x + dx * node.radius
                ringY[i][s] = node.y + dy * node.radius
                ringZ[i][s] = node.z + dz * node.radius
                ringNx[i][s] = dx; ringNy[i][s] = dy; ringNz[i][s] = dz
            }
        }

        for (i in 0 until n - 1) {
            val n0 = nodes[i]
            val n1 = nodes[i + 1]
            val v0 = i.toFloat() / (n - 1)
            val v1 = (i + 1).toFloat() / (n - 1)
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                val u0 = s.toFloat() / sectors
                val u1 = (s + 1).toFloat() / sectors
                addVertex(ringX[i][s], ringY[i][s], ringZ[i][s], ringNx[i][s], ringNy[i][s], ringNz[i][s], u0, v0, n0.r, n0.g, n0.b)
                addVertex(ringX[i][sNext], ringY[i][sNext], ringZ[i][sNext], ringNx[i][sNext], ringNy[i][sNext], ringNz[i][sNext], u1, v0, n0.r, n0.g, n0.b)
                addVertex(ringX[i + 1][sNext], ringY[i + 1][sNext], ringZ[i + 1][sNext], ringNx[i + 1][sNext], ringNy[i + 1][sNext], ringNz[i + 1][sNext], u1, v1, n1.r, n1.g, n1.b)

                addVertex(ringX[i][s], ringY[i][s], ringZ[i][s], ringNx[i][s], ringNy[i][s], ringNz[i][s], u0, v0, n0.r, n0.g, n0.b)
                addVertex(ringX[i + 1][sNext], ringY[i + 1][sNext], ringZ[i + 1][sNext], ringNx[i + 1][sNext], ringNy[i + 1][sNext], ringNz[i + 1][sNext], u1, v1, n1.r, n1.g, n1.b)
                addVertex(ringX[i + 1][s], ringY[i + 1][s], ringZ[i + 1][s], ringNx[i + 1][s], ringNy[i + 1][s], ringNz[i + 1][s], u0, v1, n1.r, n1.g, n1.b)
            }
        }
        // Smooth rounded fingertip dome cap
        val prevLast = nodes[n - 2]
        val tip = nodes.last()
        var fx = tip.x - prevLast.x; var fy = tip.y - prevLast.y; var fz = tip.z - prevLast.z
        val fLen = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(0.0001f)
        fx /= fLen; fy /= fLen; fz /= fLen
        val tipApexX = tip.x + fx * tip.radius * 0.72f
        val tipApexY = tip.y + fy * tip.radius * 0.72f
        val tipApexZ = tip.z + fz * tip.radius * 0.72f
        val lastIdx = n - 1
        for (s in 0 until sectors) {
            val sNext = (s + 1) % sectors
            addVertex(tipApexX, tipApexY, tipApexZ, fx, fy, fz, 0.5f, 1f, tip.r, tip.g, tip.b)
            addVertex(ringX[lastIdx][s], ringY[lastIdx][s], ringZ[lastIdx][s], ringNx[lastIdx][s], ringNy[lastIdx][s], ringNz[lastIdx][s], 0.5f, 1f, tip.r, tip.g, tip.b)
            addVertex(ringX[lastIdx][sNext], ringY[lastIdx][sNext], ringZ[lastIdx][sNext], ringNx[lastIdx][sNext], ringNy[lastIdx][sNext], ringNz[lastIdx][sNext], 0.5f, 1f, tip.r, tip.g, tip.b)
        }
    }
}
