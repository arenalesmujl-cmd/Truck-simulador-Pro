package com.example.gl

import com.example.model.TruckStyle

data class TruckMeshSet(
    val chassisMesh: MeshBuffer,
    val cabPaintMesh: MeshBuffer,
    val chromeMesh: MeshBuffer,
    val glassMesh: MeshBuffer
)

data class TrailerMeshSet(
    val bodyPaintMesh: MeshBuffer,
    val chassisMesh: MeshBuffer,
    val chromeMesh: MeshBuffer
)

data class TrafficMeshSet(
    val sedanBodyMesh: MeshBuffer,
    val sedanGlassMesh: MeshBuffer,
    val busBodyMesh: MeshBuffer,
    val busGlassMesh: MeshBuffer,
    val cargoCabMesh: MeshBuffer,
    val cargoBoxMesh: MeshBuffer
)

data class CockpitMeshSet(
    val dashboardMesh: MeshBuffer,
    val steeringWheelMesh: MeshBuffer
)

object TruckMeshFactory {

    fun buildTruckMesh(style: TruckStyle): TruckMeshSet {
        val chassis = DynamicMeshBuilder()
        val paint = DynamicMeshBuilder()
        val chrome = DynamicMeshBuilder()
        val glass = DynamicMeshBuilder()

        // Heavy-duty C-channel ladder frame rails & crossmembers
        chassis.addBox(-0.46f, 0.72f, -0.35f, 0.22f, 0.30f, 7.0f, 0.14f, 0.16f, 0.19f)
        chassis.addBox(0.46f, 0.72f, -0.35f, 0.22f, 0.30f, 7.0f, 0.14f, 0.16f, 0.19f)
        for (cz in listOf(-3.4f, -2.0f, -0.5f, 1.0f, 2.5f)) {
            chassis.addBox(0f, 0.72f, cz, 1.06f, 0.22f, 0.20f, 0.12f, 0.14f, 0.16f)
        }

        // Rear axle differential pumpkins & air suspension bellows
        for (axZ in listOf(-1.25f, -2.55f)) {
            chassis.addCylinderX(-1.05f, 1.05f, 0.54f, axZ, 0.12f, 10, 0.14f, 0.15f, 0.18f)
            chassis.addEllipsoid(0f, 0.54f, axZ, 0.24f, 0.22f, 0.24f, 6, 10, 0.16f, 0.18f, 0.21f)
            chassis.addCylinderY(-0.52f, 0.58f, axZ, 0.14f, 0.24f, 10, 0.10f, 0.11f, 0.13f)
            chassis.addCylinderY(0.52f, 0.58f, axZ, 0.14f, 0.24f, 10, 0.10f, 0.11f, 0.13f)
        }

        // Fifth wheel turntable plate, jaws & release handle
        chassis.addTaperedCylinderY(0f, 0.88f, -1.88f, 0.68f, 0.62f, 0.14f, 20, 0.18f, 0.20f, 0.23f)
        chassis.addBox(0f, 0.86f, -1.96f, 0.96f, 0.12f, 0.85f, 0.13f, 0.15f, 0.18f)
        chrome.addCylinderX(0.42f, 0.96f, 0.92f, -1.85f, 0.025f, 8, 0.85f, 0.88f, 0.92f)

        // Curved rear drive-axle wheel arch fenders & mudflaps with polished chrome weights
        for (side in listOf(-1f, 1f)) {
            val fx = side * 1.16f
            chassis.addWheelArch(fx, 0.55f, -1.25f, 0.56f, 0.59f, 0.66f, 12, 0.12f, 0.13f, 0.16f)
            chassis.addWheelArch(fx, 0.55f, -2.55f, 0.56f, 0.59f, 0.66f, 12, 0.12f, 0.13f, 0.16f)
            chassis.addBox(fx, 0.66f, -3.38f, 0.52f, 0.72f, 0.06f, 0.09f, 0.10f, 0.12f)
            chrome.addBox(fx, 0.36f, -3.41f, 0.46f, 0.10f, 0.02f, 0.88f, 0.90f, 0.94f)
        }

        // Coiled Red & Blue Air-Brake Gladhand Hoses & Pogo-Stick support behind cab
        val hoseZ = if (style == TruckStyle.EURO_CABOVER) 0.12f else -0.96f
        chrome.addCylinderY(0f, 0.88f, hoseZ, 0.03f, 1.35f, 10, 0.84f, 0.86f, 0.90f)
        chassis.addCylinderY(-0.22f, 1.15f, hoseZ - 0.12f, 0.045f, 0.92f, 10, 0.86f, 0.14f, 0.12f)
        chassis.addCylinderY(0.22f, 1.15f, hoseZ - 0.12f, 0.045f, 0.92f, 10, 0.14f, 0.36f, 0.86f)
        chrome.addBox(0f, 0.89f, hoseZ - 0.32f, 0.92f, 0.05f, 0.68f, 0.76f, 0.79f, 0.84f)

        // Twin polished cylindrical aluminum fuel tanks with mounting straps & boarding steps
        for (side in listOf(-1f, 1f)) {
            val tx = side * 1.04f
            chrome.addCylinderZ(tx, 0.76f, -0.95f, 0.75f, 0.30f, 20, 0.87f, 0.90f, 0.95f)
            chassis.addBox(tx, 0.76f, -0.55f, 0.64f, 0.64f, 0.08f, 0.15f, 0.17f, 0.20f)
            chassis.addBox(tx, 0.76f, 0.35f, 0.64f, 0.64f, 0.08f, 0.15f, 0.17f, 0.20f)
            chrome.addBox(side * 1.24f, 0.62f, 1.35f, 0.22f, 0.055f, 0.78f, 0.84f, 0.87f, 0.92f)
            chrome.addBox(side * 1.22f, 0.92f, 1.35f, 0.17f, 0.05f, 0.74f, 0.84f, 0.87f, 0.92f)
        }

        // Smooth curved aerodynamic front chrome bumper + twin round projector headlight pods + plate
        val noseZ = when (style) {
            TruckStyle.EURO_CABOVER -> 3.05f
            TruckStyle.AMERICAN_LONGNOSE -> 3.55f
            TruckStyle.HEAVY_TWIN_EXHAUST -> 3.50f
            TruckStyle.ARCTIC_EXPEDITION -> 3.58f
        }
        chrome.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(noseZ - 0.12f, 0.75f, 1.25f, 0.23f, 0.86f, 0.89f, 0.94f, roundness = 0.28f),
                LoftRingZ(noseZ + 0.08f, 0.75f, 1.24f, 0.22f, 0.90f, 0.93f, 0.97f, roundness = 0.34f),
                LoftRingZ(noseZ + 0.22f, 0.74f, 1.18f, 0.19f, 0.88f, 0.91f, 0.96f, roundness = 0.42f)
            )
        )
        chassis.addBox(0f, 0.56f, noseZ + 0.04f, 2.30f, 0.14f, 0.24f, 0.13f, 0.15f, 0.18f)
        chassis.addBox(0f, 0.73f, noseZ + 0.23f, 0.46f, 0.20f, 0.04f, 0.94f, 0.95f, 0.97f)
        chassis.addBox(0f, 0.80f, noseZ + 0.24f, 0.44f, 0.04f, 0.03f, 0.12f, 0.34f, 0.74f)

        // Recessed chrome headlight bezels & fog housings
        for (side in listOf(-1f, 1f)) {
            chrome.addCylinderZ(side * 0.88f, 0.98f, noseZ - 0.10f, noseZ + 0.04f, 0.15f, 16, 0.88f, 0.91f, 0.96f)
            chassis.addCylinderZ(side * 0.88f, 0.98f, noseZ - 0.06f, noseZ + 0.045f, 0.115f, 14, 0.14f, 0.16f, 0.20f)
            chrome.addBox(side * 0.94f, 0.66f, noseZ + 0.20f, 0.23f, 0.11f, 0.04f, 0.82f, 0.85f, 0.90f)
        }

        when (style) {
            TruckStyle.AMERICAN_LONGNOSE -> buildAmericanLongnose(paint, chrome, glass, chassis)
            TruckStyle.EURO_CABOVER -> buildEuroCabover(paint, chrome, glass, chassis)
            TruckStyle.HEAVY_TWIN_EXHAUST -> buildVolcanoHeavy(paint, chrome, glass, chassis)
            TruckStyle.ARCTIC_EXPEDITION -> buildArcticExpedition(paint, chrome, glass, chassis)
        }

        // Sleek dual-arm curved side mirrors + convex spotter mirrors + flush door seams
        val mirrorZ = if (style == TruckStyle.EURO_CABOVER) 2.55f else 1.80f
        for (side in listOf(-1f, 1f)) {
            val mx = side * 1.42f
            chrome.addCylinderX(mx - side * 0.26f, mx, 2.44f, mirrorZ, 0.020f, 8, 0.86f, 0.89f, 0.94f)
            chrome.addCylinderX(mx - side * 0.26f, mx, 2.02f, mirrorZ, 0.020f, 8, 0.86f, 0.89f, 0.94f)
            chassis.addLoftedProfileZ(
                cx = mx,
                sectors = 16,
                cappedFront = true,
                cappedBack = true,
                rings = listOf(
                    LoftRingZ(mirrorZ - 0.10f, 2.25f, 0.055f, 0.25f, 0.14f, 0.16f, 0.19f, roundness = 0.45f),
                    LoftRingZ(mirrorZ + 0.06f, 2.25f, 0.050f, 0.23f, 0.13f, 0.15f, 0.18f, roundness = 0.55f)
                )
            )
            chrome.addBox(mx + side * 0.025f, 2.26f, mirrorZ + 0.04f, 0.04f, 0.42f, 0.13f, 0.88f, 0.91f, 0.95f)
            chassis.addEllipsoid(mx, 1.93f, mirrorZ + 0.02f, 0.05f, 0.07f, 0.08f, 6, 10, 0.13f, 0.15f, 0.18f)
            // Exterior door outline & chrome pull handle clearly proud of flank
            chassis.addBox(side * 1.258f, 1.56f, if (style == TruckStyle.EURO_CABOVER) 1.72f else 1.18f, 0.02f, 0.95f, 0.025f, 0.10f, 0.12f, 0.15f)
            chrome.addBox(side * 1.265f, 1.62f, if (style == TruckStyle.EURO_CABOVER) 1.56f else 1.02f, 0.03f, 0.06f, 0.18f, 0.90f, 0.92f, 0.96f)
        }

        // Exterior resting windshield wipers clearly proud of windshield surface
        val wiperZ = if (style == TruckStyle.EURO_CABOVER) 3.01f else 2.09f
        val wiperY = if (style == TruckStyle.EURO_CABOVER) 2.22f else 2.18f
        chassis.addBox(-0.42f, wiperY, wiperZ, 0.56f, 0.03f, 0.03f, 0.10f, 0.11f, 0.14f)
        chassis.addBox(0.42f, wiperY, wiperZ, 0.56f, 0.03f, 0.03f, 0.10f, 0.11f, 0.14f)

        return TruckMeshSet(
            chassisMesh = chassis.build(),
            cabPaintMesh = paint.build(),
            chromeMesh = chrome.build(),
            glassMesh = glass.build()
        )
    }

    private fun buildAmericanLongnose(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        // Rounded-corner sleeper cab shell
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 24,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.74f, 2.14f, 1.23f, 1.22f, 0.95f, 0.95f, 0.95f, roundness = 0.34f),
                LoftRingZ(-0.20f, 2.22f, 1.25f, 1.32f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(0.42f, 2.24f, 1.25f, 1.34f, 1f, 1f, 1f, roundness = 0.36f),
                LoftRingZ(1.05f, 2.18f, 1.24f, 1.26f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(1.62f, 2.10f, 1.21f, 1.16f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(1.98f, 1.98f, 1.14f, 1.04f, 0.96f, 0.96f, 0.96f, roundness = 0.38f)
            )
        )
        // Sleeper high-roof aero fairing crown
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.56f, 3.24f, 1.06f, 0.34f, 0.94f, 0.94f, 0.94f, roundness = 0.42f),
                LoftRingZ(0.28f, 3.32f, 1.08f, 0.40f, 0.98f, 0.98f, 0.98f, roundness = 0.44f),
                LoftRingZ(1.02f, 3.22f, 1.04f, 0.24f, 0.96f, 0.96f, 0.96f, roundness = 0.48f)
            )
        )

        // Aerodynamic Long Engine Hood + curved sculpted front fenders
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 22,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(1.94f, 1.56f, 1.00f, 0.62f, 1f, 1f, 1f, roundness = 0.38f),
                LoftRingZ(2.65f, 1.54f, 0.96f, 0.59f, 1f, 1f, 1f, roundness = 0.42f),
                LoftRingZ(3.38f, 1.50f, 0.88f, 0.54f, 0.98f, 0.98f, 0.98f, roundness = 0.46f),
                LoftRingZ(3.56f, 1.48f, 0.80f, 0.50f, 0.94f, 0.94f, 0.94f, roundness = 0.50f)
            )
        )
        for (side in listOf(-1f, 1f)) {
            paint.addWheelArch(side * 1.10f, 0.56f, 2.50f, 0.52f, 0.60f, 0.72f, 14, 0.98f, 0.98f, 0.98f)
            chrome.addBox(side * 0.96f, 1.72f, 2.48f, 0.04f, 0.34f, 0.56f, 0.86f, 0.89f, 0.93f)
            chrome.addBox(side * 1.25f, 1.32f, 0.62f, 0.04f, 0.06f, 2.42f, 0.90f, 0.92f, 0.96f)
        }

        // Chrome hood spear + vertical-bar classic radiator grille
        chrome.addCylinderZ(0f, 2.12f, 1.98f, 3.48f, 0.038f, 10, 0.92f, 0.94f, 0.98f)
        chrome.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(3.50f, 1.50f, 0.72f, 0.50f, 0.88f, 0.91f, 0.96f, roundness = 0.34f),
                LoftRingZ(3.62f, 1.50f, 0.69f, 0.47f, 0.90f, 0.93f, 0.97f, roundness = 0.36f)
            )
        )
        chassis.addBox(0f, 1.50f, 3.625f, 1.24f, 0.78f, 0.02f, 0.08f, 0.09f, 0.12f)
        for (bx in listOf(-0.46f, -0.23f, 0f, 0.23f, 0.46f)) {
            chrome.addBox(bx, 1.50f, 3.645f, 0.045f, 0.78f, 0.025f, 0.88f, 0.91f, 0.96f)
        }
        chrome.addBox(0f, 1.94f, 3.65f, 0.36f, 0.11f, 0.04f, 0.95f, 0.45f, 0.07f)

        // Twin vertical chrome exhaust stacks, air cleaners & roof horns
        for (side in listOf(-1f, 1f)) {
            chrome.addCylinderY(side * 1.28f, 0.95f, -0.58f, 0.092f, 3.05f, 16, 0.88f, 0.91f, 0.95f)
            chrome.addCylinderY(side * 1.28f, 1.45f, -0.58f, 0.130f, 1.58f, 16, 0.78f, 0.81f, 0.86f)
            chrome.addCylinderZ(side * 0.46f, 3.34f, 1.18f, 1.74f, 0.058f, 12, 0.90f, 0.92f, 0.96f)
            chrome.addCylinderY(side * 1.14f, 1.56f, 1.80f, 0.16f, 0.62f, 14, 0.86f, 0.89f, 0.93f)
        }

        // Curved chrome drop sun visor
        chrome.addSlopedBox(0f, 3.04f, 1.98f, 2.24f, 0.32f, 2.18f, 0.24f, 0.16f, -0.05f, 0.86f, 0.88f, 0.92f)

        // PROUD, CRISP PANORAMIC WINDSHIELD + RUBBER TRIM + SIDE WINDOWS (outside cab contour!)
        chassis.addSlopedBox(0f, 2.54f, 2.00f, 2.10f, 0.14f, 2.02f, 0.12f, 0.78f, -0.10f, 0.08f, 0.10f, 0.13f)
        glass.addSlopedBox(0f, 2.54f, 2.02f, 2.00f, 0.14f, 1.92f, 0.12f, 0.68f, -0.10f, 0.06f, 0.13f, 0.21f, 0.85f)
        // Center split windshield mullion bar
        chassis.addSlopedBox(0f, 2.54f, 2.07f, 0.06f, 0.04f, 0.06f, 0.04f, 0.72f, -0.10f, 0.12f, 0.14f, 0.18f)
        // Side door & sleeper windows clearly proud of cab width (2.53f > 2 * 1.25f)
        chassis.addBox(0f, 2.52f, 1.24f, 2.52f, 0.68f, 0.98f, 0.08f, 0.10f, 0.13f)
        glass.addBox(0f, 2.52f, 1.24f, 2.54f, 0.58f, 0.88f, 0.06f, 0.13f, 0.21f, 0.86f)
        chassis.addBox(0f, 2.52f, 0.05f, 2.52f, 0.60f, 0.74f, 0.08f, 0.10f, 0.13f)
        glass.addBox(0f, 2.52f, 0.05f, 2.54f, 0.50f, 0.64f, 0.06f, 0.13f, 0.21f, 0.86f)
    }

    private fun buildEuroCabover(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 24,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(0.32f, 2.26f, 1.24f, 1.32f, 0.96f, 0.96f, 0.96f, roundness = 0.34f),
                LoftRingZ(0.95f, 2.34f, 1.26f, 1.40f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(1.90f, 2.34f, 1.26f, 1.40f, 1f, 1f, 1f, roundness = 0.36f),
                LoftRingZ(2.72f, 2.26f, 1.22f, 1.32f, 1f, 1f, 1f, roundness = 0.40f),
                LoftRingZ(2.96f, 2.08f, 1.12f, 1.14f, 0.97f, 0.97f, 0.97f, roundness = 0.45f)
            )
        )
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(0.40f, 3.60f, 1.12f, 0.28f, 0.94f, 0.94f, 0.94f, roundness = 0.46f),
                LoftRingZ(1.30f, 3.66f, 1.14f, 0.32f, 0.98f, 0.98f, 0.98f, roundness = 0.48f),
                LoftRingZ(2.30f, 3.50f, 1.08f, 0.24f, 0.95f, 0.95f, 0.95f, roundness = 0.52f)
            )
        )

        for (side in listOf(-1f, 1f)) {
            paint.addBox(side * 1.18f, 0.78f, 0.10f, 0.10f, 0.62f, 2.40f, 0.92f, 0.92f, 0.92f)
            paint.addWheelArch(side * 1.14f, 0.55f, 2.40f, 0.46f, 0.58f, 0.69f, 14, 0.96f, 0.96f, 0.96f)
            chassis.addBox(side * 1.18f, 1.62f, 2.94f, 0.16f, 0.85f, 0.12f, 0.14f, 0.16f, 0.19f)
            chrome.addBox(side * 1.26f, 1.48f, 1.56f, 0.04f, 0.06f, 2.25f, 0.90f, 0.92f, 0.96f)
        }

        chassis.addSlopedBox(0f, 1.54f, 2.98f, 1.98f, 0.14f, 1.90f, 0.12f, 1.02f, -0.04f, 0.14f, 0.16f, 0.20f)
        for (gy in listOf(1.22f, 1.44f, 1.66f, 1.88f)) {
            chrome.addBox(0f, gy, 3.05f, 1.72f, 0.045f, 0.04f, 0.86f, 0.89f, 0.94f)
        }
        chrome.addBox(0f, 1.96f, 3.06f, 0.46f, 0.11f, 0.05f, 0.92f, 0.94f, 0.98f)

        // Proud wide panoramic windshield & side windows with dark rubber trim
        chassis.addSlopedBox(0f, 2.68f, 2.94f, 2.20f, 0.16f, 2.08f, 0.14f, 0.96f, -0.10f, 0.08f, 0.10f, 0.13f)
        glass.addSlopedBox(0f, 2.68f, 2.96f, 2.10f, 0.15f, 1.98f, 0.13f, 0.86f, -0.10f, 0.06f, 0.13f, 0.21f, 0.85f)
        chassis.addBox(0f, 2.68f, 1.95f, 2.53f, 0.78f, 1.16f, 0.08f, 0.10f, 0.13f)
        glass.addBox(0f, 2.68f, 1.95f, 2.55f, 0.68f, 1.06f, 0.06f, 0.13f, 0.21f, 0.86f)
        chassis.addSlopedBox(0f, 3.24f, 2.92f, 2.34f, 0.26f, 2.28f, 0.20f, 0.18f, -0.04f, 0.12f, 0.13f, 0.16f)
    }

    private fun buildVolcanoHeavy(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 24,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.62f, 2.22f, 1.26f, 1.28f, 0.95f, 0.95f, 0.95f, roundness = 0.32f),
                LoftRingZ(0.25f, 2.28f, 1.28f, 1.35f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(1.15f, 2.24f, 1.26f, 1.30f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(2.08f, 2.04f, 1.18f, 1.08f, 0.96f, 0.96f, 0.96f, roundness = 0.38f)
            )
        )
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 22,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(2.00f, 1.60f, 1.06f, 0.62f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(2.75f, 1.58f, 1.02f, 0.58f, 1f, 1f, 1f, roundness = 0.38f),
                LoftRingZ(3.48f, 1.52f, 0.92f, 0.53f, 0.96f, 0.96f, 0.96f, roundness = 0.44f)
            )
        )
        paint.addBeveledBox(-0.42f, 2.22f, 2.72f, 0.36f, 0.16f, 1.05f, 0.04f, 0.92f, 0.92f, 0.92f)
        paint.addBeveledBox(0.42f, 2.22f, 2.72f, 0.36f, 0.16f, 1.05f, 0.04f, 0.92f, 0.92f, 0.92f)
        for (side in listOf(-1f, 1f)) {
            paint.addWheelArch(side * 1.14f, 0.56f, 2.50f, 0.54f, 0.60f, 0.74f, 14, 0.96f, 0.96f, 0.96f)
            chrome.addBox(side * 1.28f, 1.38f, 0.72f, 0.04f, 0.08f, 2.42f, 0.90f, 0.92f, 0.96f)
        }

        chrome.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(3.45f, 1.54f, 0.82f, 0.52f, 0.88f, 0.90f, 0.95f, roundness = 0.32f),
                LoftRingZ(3.56f, 1.54f, 0.78f, 0.49f, 0.90f, 0.93f, 0.97f, roundness = 0.34f)
            )
        )
        chassis.addBox(0f, 1.54f, 3.565f, 1.38f, 0.82f, 0.02f, 0.08f, 0.09f, 0.12f)
        for (bx in listOf(-0.56f, -0.28f, 0f, 0.28f, 0.56f)) {
            chrome.addBox(bx, 1.54f, 3.585f, 0.055f, 0.82f, 0.025f, 0.88f, 0.91f, 0.96f)
        }
        chrome.addBox(0f, 1.98f, 3.59f, 0.40f, 0.11f, 0.04f, 0.95f, 0.45f, 0.07f)
        for (side in listOf(-1f, 1f)) {
            chrome.addCylinderY(side * 1.32f, 0.95f, -0.45f, 0.115f, 3.22f, 16, 0.90f, 0.92f, 0.96f)
            chrome.addCylinderY(side * 1.32f, 1.45f, -0.45f, 0.152f, 1.68f, 16, 0.78f, 0.80f, 0.85f)
            chrome.addCylinderY(side * 1.18f, 1.85f, 1.82f, 0.15f, 0.68f, 14, 0.85f, 0.88f, 0.92f)
        }
        // Proud windshield + side windows
        chassis.addSlopedBox(0f, 2.60f, 2.10f, 2.16f, 0.14f, 2.06f, 0.12f, 0.80f, -0.10f, 0.08f, 0.10f, 0.13f)
        glass.addSlopedBox(0f, 2.60f, 2.12f, 2.06f, 0.14f, 1.96f, 0.12f, 0.70f, -0.10f, 0.06f, 0.13f, 0.21f, 0.85f)
        chassis.addBox(0f, 2.60f, 1.35f, 2.57f, 0.70f, 1.02f, 0.08f, 0.10f, 0.13f)
        glass.addBox(0f, 2.60f, 1.35f, 2.59f, 0.60f, 0.92f, 0.06f, 0.13f, 0.21f, 0.86f)
    }

    private fun buildArcticExpedition(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 24,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-0.55f, 2.30f, 1.29f, 1.30f, 0.96f, 0.96f, 0.96f, roundness = 0.32f),
                LoftRingZ(0.45f, 2.36f, 1.30f, 1.36f, 1f, 1f, 1f, roundness = 0.32f),
                LoftRingZ(1.45f, 2.32f, 1.28f, 1.32f, 1f, 1f, 1f, roundness = 0.32f),
                LoftRingZ(2.22f, 2.10f, 1.20f, 1.10f, 0.96f, 0.96f, 0.96f, roundness = 0.36f)
            )
        )
        paint.addLoftedProfileZ(
            cx = 0f,
            sectors = 22,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(2.06f, 1.66f, 1.09f, 0.60f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(2.85f, 1.62f, 1.04f, 0.56f, 1f, 1f, 1f, roundness = 0.38f),
                LoftRingZ(3.56f, 1.56f, 0.94f, 0.50f, 0.96f, 0.96f, 0.96f, roundness = 0.44f)
            )
        )

        for (side in listOf(-1f, 1f)) {
            chassis.addWheelArch(side * 1.16f, 0.56f, 2.55f, 0.54f, 0.61f, 0.75f, 14, 0.15f, 0.17f, 0.20f)
            chassis.addWheelArch(side * 1.16f, 0.56f, 1.35f, 0.54f, 0.61f, 0.75f, 14, 0.15f, 0.17f, 0.20f)
        }

        chassis.addCylinderX(-1.08f, 1.08f, 1.22f, 3.74f, 0.055f, 10, 0.16f, 0.18f, 0.22f)
        chassis.addCylinderX(-1.02f, 1.02f, 1.68f, 3.74f, 0.050f, 10, 0.16f, 0.18f, 0.22f)
        for (bx in listOf(-0.98f, -0.45f, 0.45f, 0.98f)) {
            chassis.addCylinderY(bx, 0.85f, 3.72f, 0.052f, 0.92f, 10, 0.16f, 0.18f, 0.22f)
        }
        chrome.addBox(-0.55f, 0.68f, 3.74f, 0.08f, 0.12f, 0.10f, 0.96f, 0.42f, 0.06f)
        chrome.addBox(0.55f, 0.68f, 3.74f, 0.08f, 0.12f, 0.10f, 0.96f, 0.42f, 0.06f)

        chassis.addBox(0f, 3.60f, 1.25f, 2.25f, 0.18f, 1.65f, 0.16f, 0.18f, 0.20f)
        for (lx in listOf(-0.75f, -0.25f, 0.25f, 0.75f)) {
            chrome.addCylinderZ(lx, 3.75f, 1.95f, 2.12f, 0.12f, 14, 0.86f, 0.89f, 0.94f)
        }
        chassis.addCylinderY(1.32f, 1.25f, 2.05f, 0.11f, 2.45f, 12, 0.14f, 0.16f, 0.18f)

        // Proud windshield + side windows
        chassis.addSlopedBox(0f, 2.66f, 2.24f, 2.20f, 0.14f, 2.10f, 0.12f, 0.78f, -0.10f, 0.08f, 0.10f, 0.13f)
        glass.addSlopedBox(0f, 2.66f, 2.26f, 2.10f, 0.14f, 2.00f, 0.12f, 0.68f, -0.10f, 0.06f, 0.13f, 0.21f, 0.85f)
        chassis.addBox(0f, 2.66f, 1.45f, 2.61f, 0.70f, 1.04f, 0.08f, 0.10f, 0.13f)
        glass.addBox(0f, 2.66f, 1.45f, 2.63f, 0.60f, 0.94f, 0.06f, 0.13f, 0.21f, 0.86f)
    }

    fun buildTrailerMesh(): TrailerMeshSet {
        val body = DynamicMeshBuilder()
        val chassis = DynamicMeshBuilder()
        val chrome = DynamicMeshBuilder()

        chassis.addBox(-0.55f, 1.02f, 0f, 0.22f, 0.26f, 11.1f, 0.14f, 0.16f, 0.19f)
        chassis.addBox(0.55f, 1.02f, 0f, 0.22f, 0.26f, 11.1f, 0.14f, 0.16f, 0.19f)
        for (side in listOf(-1f, 1f)) {
            chrome.addBox(side * 1.24f, 0.82f, 0.2f, 0.06f, 0.10f, 4.6f, 0.78f, 0.80f, 0.84f)
            chrome.addBox(side * 1.24f, 0.58f, 0.2f, 0.06f, 0.10f, 4.6f, 0.78f, 0.80f, 0.84f)
            chassis.addBox(side * 0.82f, 0.70f, 3.2f, 0.16f, 0.85f, 0.16f, 0.15f, 0.16f, 0.19f)
            chassis.addBox(side * 0.82f, 0.28f, 3.2f, 0.32f, 0.08f, 0.32f, 0.15f, 0.16f, 0.19f)
            chassis.addBox(side * 1.15f, 0.66f, -5.45f, 0.54f, 0.70f, 0.06f, 0.10f, 0.11f, 0.13f)
            chrome.addBox(side * 1.15f, 0.36f, -5.48f, 0.46f, 0.09f, 0.02f, 0.88f, 0.90f, 0.94f)
        }
        chrome.addBox(0f, 0.76f, -5.60f, 2.46f, 0.18f, 0.16f, 0.82f, 0.85f, 0.90f)
        chassis.addBeveledBox(0f, 3.12f, 5.72f, 1.56f, 1.05f, 0.38f, 0.06f, 0.90f, 0.92f, 0.95f)
        chassis.addBox(0f, 3.12f, 5.92f, 1.08f, 0.62f, 0.04f, 0.14f, 0.16f, 0.19f)

        body.addBoxUv(0f, 2.52f, 0f, 2.52f, 2.74f, 11.18f, uvScaleX = 3.5f, uvScaleY = 1.0f, r = 1f, g = 1f, b = 1f)
        for (rz in -50..50 step 8) {
            val zPos = rz / 10f
            body.addBox(-1.27f, 2.52f, zPos, 0.03f, 2.56f, 0.14f, 0.92f, 0.92f, 0.92f)
            body.addBox(1.27f, 2.52f, zPos, 0.03f, 2.56f, 0.14f, 0.92f, 0.92f, 0.92f)
        }
        chrome.addBox(0f, 3.88f, 0f, 2.55f, 0.11f, 11.24f, 0.86f, 0.88f, 0.92f)
        chrome.addBox(-1.27f, 1.24f, 0f, 0.04f, 0.12f, 11.22f, 0.86f, 0.88f, 0.92f)
        chrome.addBox(1.27f, 1.24f, 0f, 0.04f, 0.12f, 11.22f, 0.86f, 0.88f, 0.92f)
        for (lx in listOf(-0.62f, -0.22f, 0.22f, 0.62f)) {
            chrome.addCylinderY(lx, 1.25f, -5.62f, 0.028f, 2.48f, 8, 0.88f, 0.90f, 0.94f)
        }

        return TrailerMeshSet(
            bodyPaintMesh = body.build(),
            chassisMesh = chassis.build(),
            chromeMesh = chrome.build()
        )
    }

    fun buildTrafficMeshes(): TrafficMeshSet {
        val sedanBody = DynamicMeshBuilder()
        val sedanGlass = DynamicMeshBuilder()
        sedanBody.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-2.12f, 0.62f, 0.88f, 0.27f, 0.94f, 0.94f, 0.94f, roundness = 0.45f),
                LoftRingZ(-1.55f, 0.72f, 0.92f, 0.36f, 1f, 1f, 1f, roundness = 0.40f),
                LoftRingZ(-0.65f, 0.92f, 0.88f, 0.54f, 1f, 1f, 1f, roundness = 0.44f),
                LoftRingZ(0.28f, 0.90f, 0.88f, 0.52f, 1f, 1f, 1f, roundness = 0.44f),
                LoftRingZ(0.95f, 0.68f, 0.92f, 0.30f, 1f, 1f, 1f, roundness = 0.42f),
                LoftRingZ(2.08f, 0.56f, 0.84f, 0.22f, 0.95f, 0.95f, 0.95f, roundness = 0.52f)
            )
        )
        // Sedan cabin glass clearly proud of lower beltline
        sedanGlass.addLoftedProfileZ(
            cx = 0f,
            sectors = 18,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-1.05f, 1.18f, 0.80f, 0.26f, 0.08f, 0.14f, 0.22f, roundness = 0.45f),
                LoftRingZ(-0.20f, 1.24f, 0.82f, 0.28f, 0.10f, 0.16f, 0.25f, roundness = 0.45f),
                LoftRingZ(0.62f, 1.12f, 0.78f, 0.22f, 0.08f, 0.14f, 0.22f, roundness = 0.48f)
            )
        )

        val busBody = DynamicMeshBuilder()
        val busGlass = DynamicMeshBuilder()
        busBody.addLoftedProfileZ(
            cx = 0f,
            sectors = 22,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(-5.05f, 1.65f, 1.22f, 1.22f, 0.96f, 0.96f, 0.96f, roundness = 0.34f),
                LoftRingZ(-2.50f, 1.68f, 1.24f, 1.26f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(2.50f, 1.68f, 1.24f, 1.26f, 1f, 1f, 1f, roundness = 0.34f),
                LoftRingZ(4.85f, 1.64f, 1.20f, 1.20f, 0.98f, 0.98f, 0.98f, roundness = 0.40f),
                LoftRingZ(5.10f, 1.54f, 1.10f, 1.08f, 0.94f, 0.94f, 0.94f, roundness = 0.46f)
            )
        )
        busBody.addBeveledBox(0f, 2.98f, 1.2f, 1.45f, 0.28f, 2.2f, 0.06f, 0.88f, 0.90f, 0.92f)
        busBody.addBeveledBox(0f, 2.98f, -2.0f, 1.45f, 0.28f, 2.2f, 0.06f, 0.88f, 0.90f, 0.92f)
        busGlass.addBox(0f, 2.02f, 0.2f, 2.52f, 0.92f, 8.8f, 0.07f, 0.14f, 0.22f, 0.85f)
        busGlass.addSlopedBox(0f, 1.98f, 5.06f, 2.22f, 0.16f, 2.14f, 0.12f, 1.18f, -0.05f, 0.07f, 0.14f, 0.22f, 0.85f)

        val cargoCab = DynamicMeshBuilder()
        val cargoBox = DynamicMeshBuilder()
        cargoCab.addLoftedProfileZ(
            cx = 0f,
            sectors = 20,
            cappedFront = true,
            cappedBack = true,
            rings = listOf(
                LoftRingZ(1.36f, 1.44f, 1.18f, 1.02f, 0.86f, 0.24f, 0.18f, roundness = 0.36f),
                LoftRingZ(2.85f, 1.44f, 1.18f, 1.02f, 0.88f, 0.26f, 0.19f, roundness = 0.38f),
                LoftRingZ(3.52f, 1.34f, 1.10f, 0.92f, 0.84f, 0.23f, 0.17f, roundness = 0.44f)
            )
        )
        cargoCab.addSlopedBox(0f, 1.95f, 3.54f, 2.08f, 0.14f, 2.00f, 0.12f, 0.78f, -0.06f, 0.08f, 0.14f, 0.22f)
        cargoBox.addBoxUv(0f, 1.78f, -1.25f, 2.44f, 2.55f, 5.25f, 2.0f, 1.0f, 1f, 1f, 1f)

        return TrafficMeshSet(
            sedanBodyMesh = sedanBody.build(),
            sedanGlassMesh = sedanGlass.build(),
            busBodyMesh = busBody.build(),
            busGlassMesh = busGlass.build(),
            cargoCabMesh = cargoCab.build(),
            cargoBoxMesh = cargoBox.build()
        )
    }

    fun buildCockpitMeshes(): CockpitMeshSet {
        val dash = DynamicMeshBuilder()
        dash.addSlopedBox(-0.34f, -0.46f, 0.82f, 2.05f, 0.48f, 1.96f, 0.38f, 0.36f, 0.04f, 0.12f, 0.14f, 0.17f)
        dash.addBox(-0.78f, -0.36f, 0.62f, 0.82f, 0.024f, 0.04f, 0.62f, 0.66f, 0.72f)
        dash.addSlopedBox(0f, -0.22f, 0.72f, 0.56f, 0.24f, 0.50f, 0.18f, 0.20f, 0.02f, 0.09f, 0.10f, 0.13f)
        dash.addTorusZ(-0.12f, -0.28f, 0.65f, 0.072f, 0.010f, 24, 8, 0.72f, 0.76f, 0.82f)
        dash.addTorusZ(0.12f, -0.28f, 0.65f, 0.072f, 0.010f, 24, 8, 0.72f, 0.76f, 0.82f)
        // Dark round gauge faces tucked cleanly inside chrome bezels
        dash.addCylinderZ(-0.12f, -0.28f, 0.655f, 0.665f, 0.068f, 20, 0.04f, 0.06f, 0.10f)
        dash.addCylinderZ(0.12f, -0.28f, 0.655f, 0.665f, 0.068f, 20, 0.04f, 0.06f, 0.10f)

        dash.addBox(-0.46f, -0.34f, 0.71f, 0.28f, 0.18f, 0.04f, 0.08f, 0.09f, 0.12f)
        dash.addTaperedCylinderY(-0.34f, -0.68f, 0.42f, 0.068f, 0.025f, 0.14f, 12, 0.14f, 0.15f, 0.17f)
        dash.addCylinderY(-0.34f, -0.56f, 0.42f, 0.014f, 0.22f, 10, 0.72f, 0.75f, 0.80f)
        dash.addEllipsoid(-0.34f, -0.33f, 0.42f, 0.032f, 0.036f, 0.032f, 8, 12, 0.16f, 0.18f, 0.22f)

        dash.addBox(0.62f, 0.06f, 0.90f, 0.095f, 1.25f, 0.11f, 0.10f, 0.12f, 0.15f)
        dash.addBox(-1.24f, 0.06f, 0.90f, 0.095f, 1.25f, 0.11f, 0.10f, 0.12f, 0.15f)
        dash.addBox(-0.31f, 0.58f, 0.88f, 1.98f, 0.16f, 0.24f, 0.09f, 0.10f, 0.13f)
        dash.addCylinderZ(0f, -0.34f, 0.52f, 0.75f, 0.056f, 14, 0.14f, 0.16f, 0.19f)

        val wheel = DynamicMeshBuilder()
        wheel.addTorusZ(0f, 0f, 0f, 0.20f, 0.022f, 32, 12, 0.13f, 0.14f, 0.16f)
        wheel.addBox(0f, 0.20f, 0f, 0.032f, 0.046f, 0.046f, 0.96f, 0.44f, 0.06f)
        wheel.addCylinderX(-0.19f, 0.19f, 0f, 0f, 0.022f, 10, 0.62f, 0.66f, 0.72f)
        wheel.addCylinderY(0f, -0.19f, 0f, 0.022f, 0.19f, 10, 0.62f, 0.66f, 0.72f)
        wheel.addCylinderZ(0f, 0f, -0.02f, 0.03f, 0.068f, 18, 0.16f, 0.18f, 0.22f)
        wheel.addCylinderZ(0f, 0f, 0.025f, 0.036f, 0.026f, 16, 0.94f, 0.44f, 0.06f)

        return CockpitMeshSet(
            dashboardMesh = dash.build(),
            steeringWheelMesh = wheel.build()
        )
    }
}
