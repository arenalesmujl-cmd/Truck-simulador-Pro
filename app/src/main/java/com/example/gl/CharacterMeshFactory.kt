package com.example.gl

data class DriverMeshSet(
    val headMesh: MeshBuffer,
    val torsoMesh: MeshBuffer,
    val upperArmMesh: MeshBuffer,
    val forearmHandMesh: MeshBuffer,
    val thighMesh: MeshBuffer,
    val shinBootMesh: MeshBuffer
)

object CharacterMeshFactory {

    /**
     * Sculpted, continuous, anatomically proportioned 3D human driver (~1.84m tall).
     * Built with continuous asymmetric multi-ring lofting (zero boxy blocks or stacked egg artifacts)
     * and seamless color transitions for beard/stubble, jacket lapels, belt, gloves, and boots.
     * Joint pivots sit cleanly at (0, 0, 0) along -Y for natural stride and steering wheel grip.
     */
    fun buildDriverMeshes(): DriverMeshSet {
        val skinR = 0.89f; val skinG = 0.68f; val skinB = 0.55f
        val skinWarmR = 0.84f; val skinWarmG = 0.62f; val skinWarmB = 0.49f
        val skinShadowR = 0.76f; val skinShadowG = 0.54f; val skinShadowB = 0.42f

        val hairR = 0.13f; val hairG = 0.10f; val hairB = 0.08f
        val stubbleR = 0.36f; val stubbleG = 0.27f; val stubbleB = 0.22f

        val jacketR = 0.15f; val jacketG = 0.18f; val jacketB = 0.23f
        val jacketCollarR = 0.21f; val jacketCollarG = 0.25f; val jacketCollarB = 0.32f
        val henleyR = 0.88f; val henleyG = 0.90f; val henleyB = 0.93f
        val denimR = 0.15f; val denimG = 0.23f; val denimB = 0.38f
        val denimFadeR = 0.21f; val denimFadeG = 0.30f; val denimFadeB = 0.47f
        val leatherR = 0.28f; val leatherG = 0.16f; val leatherB = 0.09f

        // =========================================================================
        // 1. CONTINUOUS SCULPTED HUMAN HEAD, SQUARE JAW, FACE, HAIR & TRUCKER CAP
        // =========================================================================
        val head = DynamicMeshBuilder()

        // Continuous Neck -> Chiseled Jawline -> Cheekbones -> Brow -> Cranium
        // Beard stubble is seamlessly blended on chin/jawline rings instead of floating ellipsoids!
        head.addLoftedProfileY(
            cx = 0f,
            sectors = 24,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.028f, -0.004f, 0.064f, 0.058f, skinShadowR, skinShadowG, skinShadowB, roundness = 0.95f),
                LoftRingY(0.018f, -0.004f, 0.057f, 0.054f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.96f),
                LoftRingY(0.056f, 0.012f, 0.060f, 0.068f, stubbleR, stubbleG, stubbleB, roundness = 0.88f),
                LoftRingY(0.082f, 0.022f, 0.072f, 0.084f, stubbleR, stubbleG, stubbleB, roundness = 0.78f),
                LoftRingY(0.110f, 0.018f, 0.080f, 0.092f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.82f),
                LoftRingY(0.138f, 0.014f, 0.084f, 0.096f, skinR, skinG, skinB, roundness = 0.86f),
                LoftRingY(0.168f, 0.010f, 0.086f, 0.098f, skinR, skinG, skinB, roundness = 0.88f),
                LoftRingY(0.198f, 0.002f, 0.084f, 0.095f, skinR, skinG, skinB, roundness = 0.92f),
                LoftRingY(0.228f, -0.006f, 0.078f, 0.088f, hairR, hairG, hairB, roundness = 0.96f),
                LoftRingY(0.256f, -0.010f, 0.052f, 0.058f, hairR, hairG, hairB, roundness = 1.0f)
            )
        )

        // Subtle cheekbones & brow prominence
        head.addEllipsoid(-0.044f, 0.136f, 0.074f, 0.028f, 0.020f, 0.026f, 6, 12, skinR, skinG, skinB)
        head.addEllipsoid(0.044f, 0.136f, 0.074f, 0.028f, 0.020f, 0.026f, 6, 12, skinR, skinG, skinB)
        head.addEllipsoid(0f, 0.172f, 0.084f, 0.058f, 0.014f, 0.022f, 6, 12, skinR, skinG, skinB)

        // Sculpted continuous Nose (nasal bridge tapering seamlessly down to nasal tip + nostrils)
        head.addLoftedProfileY(
            cx = 0f,
            sectors = 14,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.123f, 0.112f, 0.015f, 0.012f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.85f),
                LoftRingY(0.130f, 0.115f, 0.013f, 0.013f, skinR, skinG, skinB, roundness = 0.90f),
                LoftRingY(0.146f, 0.106f, 0.010f, 0.011f, skinR, skinG, skinB, roundness = 0.85f),
                LoftRingY(0.164f, 0.098f, 0.009f, 0.009f, skinWarmR, skinWarmG, skinWarmB, roundness = 0.85f)
            )
        )

        // Natural trimmed mustache & defined upper/lower lips flush with face contour
        head.addEllipsoid(0f, 0.112f, 0.105f, 0.026f, 0.0055f, 0.008f, 5, 10, stubbleR, stubbleG, stubbleB)
        head.addEllipsoid(0f, 0.103f, 0.106f, 0.021f, 0.0050f, 0.007f, 5, 10, 0.72f, 0.45f, 0.40f)
        head.addEllipsoid(0f, 0.096f, 0.103f, 0.018f, 0.0058f, 0.007f, 5, 10, 0.76f, 0.49f, 0.44f)

        // Realistic recessed Eyes: subtle eyelid fold + sclera + blue-hazel iris + black pupil + catchlight + curved brows
        for (side in listOf(-1f, 1f)) {
            val ex = side * 0.032f
            head.addEllipsoid(ex, 0.158f, 0.094f, 0.016f, 0.011f, 0.009f, 5, 10, skinShadowR, skinShadowG, skinShadowB)
            head.addEllipsoid(ex, 0.163f, 0.099f, 0.014f, 0.0045f, 0.008f, 4, 8, skinWarmR, skinWarmG, skinWarmB)
            // Almond sclera + iris + pupil + glint
            head.addEllipsoid(ex, 0.155f, 0.099f, 0.0125f, 0.0080f, 0.0085f, 6, 10, 0.96f, 0.96f, 0.96f)
            head.addEllipsoid(ex, 0.155f, 0.1045f, 0.0064f, 0.0064f, 0.0045f, 5, 8, 0.20f, 0.38f, 0.50f)
            head.addEllipsoid(ex, 0.155f, 0.1072f, 0.0033f, 0.0033f, 0.0028f, 4, 6, 0.04f, 0.05f, 0.07f)
            head.addEllipsoid(ex + 0.002f, 0.157f, 0.1088f, 0.0014f, 0.0014f, 0.0012f, 3, 4, 0.98f, 0.98f, 1.0f)
            // Natural arched two-segment eyebrow
            head.addEllipsoid(ex - side * 0.005f, 0.171f, 0.098f, 0.012f, 0.0038f, 0.006f, 4, 8, hairR, hairG, hairB)
            head.addEllipsoid(ex + side * 0.009f, 0.170f, 0.095f, 0.011f, 0.0034f, 0.006f, 4, 8, hairR, hairG, hairB)
        }

        // Sculpted anatomical ears & clean temple sideburns
        for (side in listOf(-1f, 1f)) {
            head.addEllipsoid(side * 0.086f, 0.148f, -0.004f, 0.013f, 0.028f, 0.018f, 6, 10, skinWarmR, skinWarmG, skinWarmB)
            head.addEllipsoid(side * 0.088f, 0.148f, 0.000f, 0.008f, 0.017f, 0.012f, 4, 8, skinShadowR, skinShadowG, skinShadowB)
            head.addEllipsoid(side * 0.079f, 0.145f, 0.022f, 0.009f, 0.032f, 0.016f, 5, 8, hairR, hairG, hairB)
        }

        // Layered natural hair on back & nape
        head.addEllipsoid(0f, 0.168f, -0.026f, 0.090f, 0.086f, 0.082f, 10, 16, hairR, hairG, hairB)

        // Tailored 5-Panel Trucker Cap (contoured lofted crown + curved visor + aviator shades)
        head.addLoftedProfileY(
            cx = 0f,
            sectors = 22,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.194f, -0.006f, 0.092f, 0.102f, 0.12f, 0.15f, 0.21f, roundness = 0.92f),
                LoftRingY(0.220f, -0.006f, 0.094f, 0.104f, 0.15f, 0.18f, 0.25f, roundness = 0.94f),
                LoftRingY(0.254f, -0.008f, 0.082f, 0.090f, 0.15f, 0.18f, 0.25f, roundness = 0.96f),
                LoftRingY(0.278f, -0.010f, 0.048f, 0.052f, 0.13f, 0.16f, 0.22f, roundness = 1.0f)
            )
        )
        head.addEllipsoid(0f, 0.281f, -0.010f, 0.011f, 0.005f, 0.011f, 4, 8, 0.96f, 0.44f, 0.06f)
        head.addEllipsoid(0f, 0.232f, 0.098f, 0.026f, 0.015f, 0.008f, 6, 10, 0.96f, 0.44f, 0.06f)
        head.addCurvedVisor(
            cy = 0.201f,
            cz = 0.092f,
            width = 0.168f,
            length = 0.092f,
            thickness = 0.008f,
            archHeight = 0.022f,
            segs = 14,
            r = 0.95f, g = 0.43f, b = 0.06f
        )
        // Sleek metal-frame Aviator Sunglasses perched cleanly above cap visor
        head.addEllipsoid(-0.036f, 0.216f, 0.106f, 0.023f, 0.012f, 0.006f, 5, 10, 0.06f, 0.08f, 0.12f)
        head.addEllipsoid(0.036f, 0.216f, 0.106f, 0.023f, 0.012f, 0.006f, 5, 10, 0.06f, 0.08f, 0.12f)
        head.addCylinderX(-0.046f, 0.046f, 0.222f, 0.107f, 0.0028f, 6, 0.84f, 0.86f, 0.90f)

        // =========================================================================
        // 2. SEAMLESS ATHLETIC TORSO, TAILORED JACKET, BELT & HIGH-VIS STRIPE
        // =========================================================================
        val torso = DynamicMeshBuilder()

        // Inner athletic human torso core (denim pelvis -> belt -> fitted henley chest -> collar)
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 24,
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
                LoftRingY(0.580f, 0.000f, 0.074f, 0.070f, henleyR, henleyG, henleyB, roundness = 0.94f)
            )
        )
        // Brushed steel belt buckle
        torso.addEllipsoid(0f, 0.112f, 0.122f, 0.030f, 0.020f, 0.009f, 6, 12, 0.82f, 0.84f, 0.88f)
        torso.addEllipsoid(0f, 0.112f, 0.127f, 0.016f, 0.010f, 0.006f, 4, 8, leatherR, leatherG, leatherB)

        // Tailored Open Jacket Shell wrapping cleanly around back & sides (never intersects center V-chest)
        torso.addLoftedProfileY(
            cx = 0f,
            sectors = 24,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(0.134f, -0.024f, 0.174f, 0.100f, jacketR, jacketG, jacketB, roundness = 0.78f),
                LoftRingY(0.175f, -0.024f, 0.177f, 0.102f, 0.96f, 0.44f, 0.06f, roundness = 0.78f),
                LoftRingY(0.204f, -0.024f, 0.178f, 0.103f, 0.92f, 0.94f, 0.97f, roundness = 0.78f),
                LoftRingY(0.226f, -0.022f, 0.177f, 0.104f, jacketR, jacketG, jacketB, roundness = 0.78f),
                LoftRingY(0.365f, -0.020f, 0.198f, 0.110f, jacketR, jacketG, jacketB, roundness = 0.76f),
                LoftRingY(0.495f, -0.014f, 0.218f, 0.112f, jacketR, jacketG, jacketB, roundness = 0.74f),
                LoftRingY(0.550f, -0.008f, 0.136f, 0.088f, jacketCollarR, jacketCollarG, jacketCollarB, roundness = 0.86f)
            )
        )
        // Left & right open front jacket wings separated cleanly from center henley V-opening
        for (side in listOf(-1f, 1f)) {
            torso.addLoftedProfileY(
                cx = side * 0.116f,
                sectors = 16,
                cappedBottom = true,
                cappedTop = true,
                rings = listOf(
                    LoftRingY(0.136f, 0.036f, 0.062f, 0.076f, jacketR, jacketG, jacketB, roundness = 0.78f),
                    LoftRingY(0.175f, 0.038f, 0.064f, 0.078f, 0.96f, 0.44f, 0.06f, roundness = 0.78f),
                    LoftRingY(0.204f, 0.040f, 0.065f, 0.079f, 0.92f, 0.94f, 0.97f, roundness = 0.78f),
                    LoftRingY(0.226f, 0.042f, 0.066f, 0.080f, jacketR, jacketG, jacketB, roundness = 0.78f),
                    LoftRingY(0.365f, 0.052f, 0.074f, 0.078f, jacketR, jacketG, jacketB, roundness = 0.76f),
                    LoftRingY(0.492f, 0.042f, 0.078f, 0.068f, jacketCollarR, jacketCollarG, jacketCollarB, roundness = 0.76f)
                )
            )
            // Soft chest pocket + chrome snap stud + rounded shoulder deltoid cap
            torso.addEllipsoid(side * 0.114f, 0.402f, 0.128f, 0.032f, 0.036f, 0.010f, 6, 10, jacketCollarR, jacketCollarG, jacketCollarB)
            torso.addEllipsoid(side * 0.114f, 0.420f, 0.135f, 0.006f, 0.006f, 0.005f, 4, 6, 0.84f, 0.86f, 0.90f)
            torso.addEllipsoid(side * 0.194f, 0.495f, 0f, 0.056f, 0.060f, 0.092f, 8, 12, jacketR, jacketG, jacketB)
        }

        // =========================================================================
        // 3. UPPER ARM (shoulder pivot (0,0,0), extends down to elbow -0.29m)
        // =========================================================================
        val upperArm = DynamicMeshBuilder()
        upperArm.addLoftedProfileY(
            cx = 0f,
            sectors = 18,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.304f, 0f, 0.047f, 0.047f, jacketR, jacketG, jacketB),
                LoftRingY(-0.270f, 0f, 0.050f, 0.049f, jacketR, jacketG, jacketB),
                LoftRingY(-0.180f, 0.004f, 0.056f, 0.055f, jacketR, jacketG, jacketB),
                LoftRingY(-0.105f, 0.006f, 0.062f, 0.060f, 0.96f, 0.44f, 0.06f),
                LoftRingY(-0.080f, 0.005f, 0.063f, 0.061f, jacketR, jacketG, jacketB),
                LoftRingY(-0.015f, 0f, 0.065f, 0.065f, jacketCollarR, jacketCollarG, jacketCollarB),
                LoftRingY(0.036f, 0f, 0.053f, 0.053f, jacketCollarR, jacketCollarG, jacketCollarB)
            )
        )

        // =========================================================================
        // 4. FOREARM, CHRONOGRAPH WATCH & SCULPTED LEATHER-GLOVED HAND
        // =========================================================================
        val forearm = DynamicMeshBuilder()
        forearm.addLoftedProfileY(
            cx = 0f,
            sectors = 18,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.258f, 0f, 0.031f, 0.024f, skinR, skinG, skinB),
                LoftRingY(-0.215f, 0f, 0.038f, 0.033f, skinR, skinG, skinB),
                LoftRingY(-0.192f, 0f, 0.043f, 0.041f, jacketCollarR, jacketCollarG, jacketCollarB),
                LoftRingY(-0.172f, 0f, 0.045f, 0.044f, jacketCollarR, jacketCollarG, jacketCollarB),
                LoftRingY(-0.168f, 0f, 0.046f, 0.045f, jacketR, jacketG, jacketB),
                LoftRingY(-0.075f, 0f, 0.050f, 0.049f, jacketR, jacketG, jacketB),
                LoftRingY(-0.005f, 0f, 0.048f, 0.048f, jacketR, jacketG, jacketB),
                LoftRingY(0.030f, 0f, 0.039f, 0.039f, jacketR, jacketG, jacketB)
            )
        )
        forearm.addTaperedCylinderY(0f, -0.242f, 0f, 0.035f, 0.035f, 0.017f, 16, 0.11f, 0.12f, 0.14f)
        forearm.addCylinderZ(0f, -0.233f, 0.023f, 0.038f, 0.015f, 14, 0.80f, 0.83f, 0.88f)
        forearm.addCylinderZ(0f, -0.233f, 0.036f, 0.040f, 0.011f, 14, 0.06f, 0.14f, 0.20f)

        // Fingerless leather driving glove palm & knuckle bridge + sculpted fingers
        forearm.addEllipsoid(0f, -0.282f, 0.004f, 0.032f, 0.037f, 0.019f, 8, 12, leatherR, leatherG, leatherB)
        forearm.addEllipsoid(0f, -0.296f, 0.009f, 0.031f, 0.014f, 0.015f, 5, 10, leatherR * 1.15f, leatherG * 1.15f, leatherB * 1.15f)
        forearm.addEllipsoid(0.030f, -0.280f, 0.018f, 0.009f, 0.020f, 0.009f, 6, 8, skinR, skinG, skinB)
        for (fi in 0 until 4) {
            val fx = -0.020f + fi * 0.0135f
            val fLen = when (fi) {
                1 -> 0.038f
                2 -> 0.036f
                0 -> 0.032f
                else -> 0.029f
            }
            forearm.addTaperedCylinderY(fx, -0.298f - fLen, 0.009f, 0.0058f, 0.0074f, fLen, 8, skinWarmR, skinWarmG, skinWarmB)
            forearm.addEllipsoid(fx, -0.298f - fLen, 0.009f, 0.0060f, 0.0060f, 0.0060f, 4, 6, skinR, skinG, skinB)
        }

        // =========================================================================
        // 5. THIGH (hip pivot (0,0,0), extends down to knee -0.42m)
        // =========================================================================
        val thigh = DynamicMeshBuilder()
        thigh.addLoftedProfileY(
            cx = 0f,
            sectors = 18,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.434f, 0f, 0.055f, 0.057f, denimR, denimG, denimB),
                LoftRingY(-0.400f, 0.004f, 0.059f, 0.061f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.270f, 0.006f, 0.067f, 0.069f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.140f, 0.005f, 0.075f, 0.077f, denimR, denimG, denimB),
                LoftRingY(-0.020f, 0f, 0.080f, 0.080f, denimR, denimG, denimB),
                LoftRingY(0.038f, 0f, 0.068f, 0.068f, denimR, denimG, denimB)
            )
        )
        thigh.addEllipsoid(0.064f, -0.195f, 0f, 0.011f, 0.050f, 0.042f, 6, 8, denimR * 0.9f, denimG * 0.9f, denimB * 0.9f)

        // =========================================================================
        // 6. SHIN + SCULPTED LEATHER WORK BOOT (knee pivot (0,0,0), sole at -0.475m)
        // =========================================================================
        val shin = DynamicMeshBuilder()
        shin.addLoftedProfileY(
            cx = 0f,
            sectors = 18,
            cappedBottom = true,
            cappedTop = true,
            rings = listOf(
                LoftRingY(-0.424f, 0.002f, 0.052f, 0.055f, leatherR, leatherG, leatherB),
                LoftRingY(-0.352f, 0.000f, 0.051f, 0.053f, leatherR, leatherG, leatherB),
                LoftRingY(-0.334f, 0.000f, 0.056f, 0.058f, 0.16f, 0.11f, 0.09f),
                LoftRingY(-0.324f, 0.000f, 0.055f, 0.057f, denimFadeR, denimFadeG, denimFadeB),
                LoftRingY(-0.240f, -0.002f, 0.054f, 0.056f, denimR, denimG, denimB),
                LoftRingY(-0.125f, -0.004f, 0.060f, 0.061f, denimR, denimG, denimB),
                LoftRingY(-0.015f, 0.002f, 0.057f, 0.059f, denimR, denimG, denimB),
                LoftRingY(0.032f, 0f, 0.047f, 0.047f, denimR, denimG, denimB)
            )
        )
        shin.addLoftedProfileZ(
            cx = 0f,
            sectors = 18,
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
            sectors = 16,
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
            shin.addCylinderX(-0.019f, 0.019f, ly, 0.054f, 0.0038f, 6, 0.82f, 0.72f, 0.52f)
        }

        return DriverMeshSet(
            headMesh = head.build(),
            torsoMesh = torso.build(),
            upperArmMesh = upperArm.build(),
            forearmHandMesh = forearm.build(),
            thighMesh = thigh.build(),
            shinBootMesh = shin.build()
        )
    }
}
