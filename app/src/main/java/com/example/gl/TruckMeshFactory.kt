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

        // Common heavy-duty ladder chassis frame + rear fenders + fifth-wheel hitch
        chassis.addBox(-0.46f, 0.72f, -0.35f, 0.22f, 0.30f, 6.9f, 0.14f, 0.16f, 0.19f)
        chassis.addBox(0.46f, 0.72f, -0.35f, 0.22f, 0.30f, 6.9f, 0.14f, 0.16f, 0.19f)
        // Crossmembers
        for (cz in listOf(-3.3f, -2.0f, -0.5f, 1.0f, 2.5f)) {
            chassis.addBox(0f, 0.72f, cz, 1.05f, 0.22f, 0.20f, 0.12f, 0.14f, 0.16f)
        }
        // Fifth wheel turntable plate & jaws over rear tandem axles
        chassis.addTaperedCylinderY(0f, 0.88f, -1.85f, 0.68f, 0.62f, 0.14f, 14, 0.18f, 0.20f, 0.23f)
        chassis.addBox(0f, 0.86f, -1.95f, 0.96f, 0.12f, 0.85f, 0.13f, 0.15f, 0.18f)

        // Rear drive-axle quarter fenders & mudflaps
        for (side in listOf(-1f, 1f)) {
            val fx = side * 1.15f
            chassis.addSlopedBox(fx, 1.14f, -1.90f, 0.56f, 2.75f, 0.54f, 2.45f, 0.14f, 0f, 0.13f, 0.14f, 0.17f)
            chassis.addBox(fx, 0.68f, -3.32f, 0.52f, 0.72f, 0.06f, 0.10f, 0.11f, 0.13f)
        }

        // Twin cylindrical aluminum fuel tanks with steel mounting straps
        for (side in listOf(-1f, 1f)) {
            val tx = side * 1.04f
            chrome.addCylinderZ(tx, 0.76f, -0.95f, 0.75f, 0.30f, 14, 0.84f, 0.87f, 0.92f)
            chassis.addBox(tx, 0.76f, -0.55f, 0.64f, 0.64f, 0.08f, 0.16f, 0.18f, 0.20f)
            chassis.addBox(tx, 0.76f, 0.35f, 0.64f, 0.64f, 0.08f, 0.16f, 0.18f, 0.20f)
            // Boarding step plates below doors
            chrome.addBox(side * 1.24f, 0.62f, 1.35f, 0.20f, 0.06f, 0.75f, 0.82f, 0.85f, 0.90f)
            chrome.addBox(side * 1.22f, 0.92f, 1.35f, 0.16f, 0.05f, 0.72f, 0.82f, 0.85f, 0.90f)
        }

        // Front bumper & under-chassis dark valence
        val noseZ = when (style) {
            TruckStyle.EURO_CABOVER -> 3.05f
            TruckStyle.AMERICAN_LONGNOSE -> 3.55f
            TruckStyle.HEAVY_TWIN_EXHAUST -> 3.50f
            TruckStyle.ARCTIC_EXPEDITION -> 3.58f
        }
        chrome.addSlopedBox(0f, 0.76f, noseZ + 0.06f, 2.48f, 0.32f, 2.38f, 0.24f, 0.44f, -0.04f, 0.88f, 0.90f, 0.94f)
        chassis.addBox(0f, 0.56f, noseZ + 0.04f, 2.34f, 0.16f, 0.26f, 0.14f, 0.16f, 0.18f)

        when (style) {
            TruckStyle.AMERICAN_LONGNOSE -> buildAmericanLongnose(paint, chrome, glass, chassis)
            TruckStyle.EURO_CABOVER -> buildEuroCabover(paint, chrome, glass, chassis)
            TruckStyle.HEAVY_TWIN_EXHAUST -> buildVolcanoHeavy(paint, chrome, glass, chassis)
            TruckStyle.ARCTIC_EXPEDITION -> buildArcticExpedition(paint, chrome, glass, chassis)
        }

        // Side mirrors on chrome bracket arms for every truck
        val mirrorZ = if (style == TruckStyle.EURO_CABOVER) 2.55f else 1.80f
        for (side in listOf(-1f, 1f)) {
            val mx = side * 1.38f
            chrome.addBox(mx - side * 0.10f, 2.42f, mirrorZ, 0.28f, 0.04f, 0.04f, 0.82f, 0.85f, 0.90f)
            chrome.addBox(mx - side * 0.10f, 2.02f, mirrorZ, 0.28f, 0.04f, 0.04f, 0.82f, 0.85f, 0.90f)
            chassis.addBox(mx, 2.22f, mirrorZ - 0.02f, 0.10f, 0.54f, 0.18f, 0.14f, 0.16f, 0.19f)
            chrome.addBox(mx + side * 0.02f, 2.22f, mirrorZ + 0.05f, 0.06f, 0.50f, 0.14f, 0.86f, 0.89f, 0.93f)
        }

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
        // Main sleeper & daycab lower shell (white so paint tint applies cleanly)
        paint.addSlopedBox(0f, 1.55f, 0.65f, 2.44f, 2.70f, 2.42f, 2.65f, 1.32f, 0f, 1f, 1f, 1f)
        // Upper cab & sleeper cabin roof with aerodynamic taper
        paint.addSlopedBox(0f, 2.75f, 0.48f, 2.42f, 2.40f, 2.22f, 2.08f, 1.10f, -0.14f, 1f, 1f, 1f)
        // Sleeper aero roof fairing
        paint.addSlopedBox(0f, 3.45f, 0.18f, 2.18f, 1.45f, 1.96f, 0.85f, 0.42f, -0.25f, 0.95f, 0.95f, 0.95f)

        // Sculpted tapered long engine hood
        paint.addSlopedBox(0f, 1.55f, 2.75f, 1.94f, 1.65f, 1.72f, 1.55f, 1.08f, -0.05f, 1f, 1f, 1f)
        // Flared front wheel fenders
        for (side in listOf(-1f, 1f)) {
            paint.addSlopedBox(side * 1.10f, 1.24f, 2.55f, 0.48f, 1.45f, 0.42f, 1.15f, 0.52f, 0f, 0.96f, 0.96f, 0.96f)
        }

        // Chrome hood trim strip + heavy vertical-bar radiator grille
        chrome.addBox(0f, 2.10f, 2.75f, 0.08f, 0.04f, 1.50f, 0.92f, 0.94f, 0.97f)
        chrome.addBox(0f, 1.52f, 3.56f, 1.38f, 0.96f, 0.12f, 0.88f, 0.90f, 0.94f)
        for (bx in listOf(-0.48f, -0.24f, 0f, 0.24f, 0.48f)) {
            chassis.addBox(bx, 1.52f, 3.63f, 0.05f, 0.82f, 0.03f, 0.25f, 0.28f, 0.32f)
        }

        // Twin vertical chrome exhaust stacks behind cab with perforated heat shields
        for (side in listOf(-1f, 1f)) {
            chrome.addCylinderY(side * 1.28f, 0.95f, -0.58f, 0.09f, 2.95f, 12, 0.88f, 0.91f, 0.95f)
            chrome.addCylinderY(side * 1.28f, 1.45f, -0.58f, 0.125f, 1.55f, 12, 0.78f, 0.80f, 0.85f)
            // Dual roof chrome air horns
            chrome.addCylinderZ(side * 0.46f, 3.38f, 1.25f, 1.78f, 0.055f, 10, 0.90f, 0.92f, 0.96f)
            // Door handle & trim
            chrome.addBox(side * 1.23f, 1.72f, 1.15f, 0.04f, 0.08f, 0.22f, 0.88f, 0.90f, 0.94f)
        }

        // Sun visor over windshield
        chrome.addSlopedBox(0f, 3.02f, 1.88f, 2.28f, 0.30f, 2.24f, 0.22f, 0.16f, -0.05f, 0.86f, 0.88f, 0.92f)

        // Angled tinted windshield & cab side windows
        glass.addSlopedBox(0f, 2.55f, 1.88f, 2.12f, 0.18f, 2.04f, 0.14f, 0.76f, -0.14f, 0.14f, 0.24f, 0.34f, 0.42f)
        glass.addBox(0f, 2.52f, 1.25f, 2.46f, 0.64f, 0.92f, 0.14f, 0.24f, 0.34f, 0.42f)
        glass.addBox(0f, 2.52f, 0.05f, 2.46f, 0.56f, 0.68f, 0.12f, 0.20f, 0.28f, 0.55f)
    }

    private fun buildEuroCabover(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        // Sculpted European high-roof cabover
        paint.addSlopedBox(0f, 1.62f, 1.68f, 2.50f, 2.65f, 2.46f, 2.52f, 1.38f, -0.04f, 1f, 1f, 1f)
        paint.addSlopedBox(0f, 2.86f, 1.56f, 2.46f, 2.48f, 2.26f, 2.12f, 1.18f, -0.16f, 1f, 1f, 1f)
        // Aero roof spoiler & corner air deflectors
        paint.addSlopedBox(0f, 3.68f, 1.15f, 2.26f, 1.75f, 2.08f, 1.10f, 0.52f, -0.25f, 0.95f, 0.95f, 0.95f)
        // Side aero chassis skirts between front and rear axles
        for (side in listOf(-1f, 1f)) {
            paint.addBox(side * 1.18f, 0.78f, 0.10f, 0.10f, 0.62f, 2.40f, 0.92f, 0.92f, 0.92f)
        }

        // Modern dark slatted V-grille with chrome emblem badge
        chassis.addSlopedBox(0f, 1.54f, 2.98f, 1.98f, 0.14f, 1.90f, 0.12f, 1.02f, -0.04f, 0.14f, 0.16f, 0.20f)
        for (gy in listOf(1.22f, 1.44f, 1.66f, 1.88f)) {
            chrome.addBox(0f, gy, 3.05f, 1.72f, 0.045f, 0.04f, 0.85f, 0.88f, 0.92f)
        }
        chrome.addBox(0f, 1.96f, 3.06f, 0.44f, 0.10f, 0.05f, 0.92f, 0.94f, 0.98f)

        // Panoramic curved-look windshield + deep side windows
        glass.addSlopedBox(0f, 2.66f, 2.84f, 2.26f, 0.16f, 2.14f, 0.14f, 0.96f, -0.14f, 0.12f, 0.22f, 0.32f, 0.40f)
        glass.addBox(0f, 2.66f, 1.95f, 2.50f, 0.76f, 1.10f, 0.12f, 0.22f, 0.32f, 0.40f)
        // Sun visor
        chassis.addSlopedBox(0f, 3.24f, 2.82f, 2.34f, 0.26f, 2.28f, 0.20f, 0.18f, -0.04f, 0.12f, 0.13f, 0.16f)
    }

    private fun buildVolcanoHeavy(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        // Muscular wide-body sleeper cab
        paint.addSlopedBox(0f, 1.60f, 0.82f, 2.54f, 2.85f, 2.50f, 2.75f, 1.38f, 0f, 1f, 1f, 1f)
        paint.addSlopedBox(0f, 2.84f, 0.65f, 2.50f, 2.55f, 2.28f, 2.18f, 1.16f, -0.16f, 1f, 1f, 1f)
        // Sculpted muscular hood with twin power bulges
        paint.addSlopedBox(0f, 1.62f, 2.76f, 2.12f, 1.50f, 1.88f, 1.42f, 1.12f, -0.05f, 1f, 1f, 1f)
        paint.addBox(-0.42f, 2.22f, 2.72f, 0.36f, 0.14f, 1.05f, 0.92f, 0.92f, 0.92f)
        paint.addBox(0.42f, 2.22f, 2.72f, 0.36f, 0.14f, 1.05f, 0.92f, 0.92f, 0.92f)
        for (side in listOf(-1f, 1f)) {
            paint.addSlopedBox(side * 1.14f, 1.26f, 2.58f, 0.50f, 1.45f, 0.44f, 1.18f, 0.54f, 0f, 0.96f, 0.96f, 0.96f)
        }

        // Aggressive chrome grille & heavy twin chrome exhaust stacks
        chrome.addBox(0f, 1.56f, 3.51f, 1.62f, 1.02f, 0.14f, 0.88f, 0.90f, 0.95f)
        for (side in listOf(-1f, 1f)) {
            chrome.addCylinderY(side * 1.32f, 0.95f, -0.45f, 0.11f, 3.15f, 12, 0.90f, 0.92f, 0.96f)
            chrome.addCylinderY(side * 1.32f, 1.45f, -0.45f, 0.15f, 1.65f, 12, 0.78f, 0.80f, 0.85f)
            chrome.addCylinderY(side * 1.18f, 1.85f, 1.82f, 0.14f, 0.65f, 10, 0.85f, 0.88f, 0.92f)
        }
        glass.addSlopedBox(0f, 2.62f, 2.02f, 2.20f, 0.18f, 2.10f, 0.14f, 0.82f, -0.14f, 0.14f, 0.22f, 0.32f, 0.42f)
        glass.addBox(0f, 2.60f, 1.35f, 2.54f, 0.68f, 0.96f, 0.14f, 0.22f, 0.32f, 0.42f)
    }

    private fun buildArcticExpedition(
        paint: DynamicMeshBuilder,
        chrome: DynamicMeshBuilder,
        glass: DynamicMeshBuilder,
        chassis: DynamicMeshBuilder
    ) {
        // Heavy expedition armored cab
        paint.addSlopedBox(0f, 1.66f, 0.95f, 2.58f, 2.95f, 2.54f, 2.85f, 1.40f, 0f, 1f, 1f, 1f)
        paint.addSlopedBox(0f, 2.92f, 0.78f, 2.54f, 2.65f, 2.34f, 2.28f, 1.18f, -0.14f, 1f, 1f, 1f)
        paint.addSlopedBox(0f, 1.66f, 2.82f, 2.18f, 1.52f, 1.94f, 1.42f, 1.14f, -0.05f, 1f, 1f, 1f)
        for (side in listOf(-1f, 1f)) {
            chassis.addSlopedBox(side * 1.16f, 1.28f, 2.62f, 0.52f, 1.48f, 0.46f, 1.22f, 0.56f, 0f, 0.15f, 0.17f, 0.20f)
        }

        // Heavy steel bull-bar brush guard
        chassis.addBox(0f, 1.22f, 3.74f, 2.15f, 0.12f, 0.12f, 0.16f, 0.18f, 0.22f)
        chassis.addBox(0f, 1.68f, 3.74f, 2.05f, 0.10f, 0.10f, 0.16f, 0.18f, 0.22f)
        for (bx in listOf(-0.98f, -0.45f, 0.45f, 0.98f)) {
            chassis.addBox(bx, 1.32f, 3.72f, 0.10f, 0.95f, 0.12f, 0.16f, 0.18f, 0.22f)
        }

        // Roof expedition rack + 4 off-road auxiliary light housings + snorkel
        chassis.addBox(0f, 3.60f, 1.25f, 2.25f, 0.18f, 1.65f, 0.16f, 0.18f, 0.20f)
        for (lx in listOf(-0.75f, -0.25f, 0.25f, 0.75f)) {
            chrome.addCylinderZ(lx, 3.75f, 1.95f, 2.12f, 0.12f, 10, 0.84f, 0.87f, 0.92f)
        }
        chassis.addCylinderY(1.32f, 1.25f, 2.05f, 0.11f, 2.45f, 10, 0.14f, 0.16f, 0.18f)

        glass.addSlopedBox(0f, 2.68f, 2.12f, 2.22f, 0.18f, 2.12f, 0.14f, 0.80f, -0.14f, 0.14f, 0.24f, 0.34f, 0.42f)
        glass.addBox(0f, 2.66f, 1.45f, 2.58f, 0.68f, 0.98f, 0.14f, 0.24f, 0.34f, 0.42f)
    }

    fun buildTrailerMesh(): TrailerMeshSet {
        val body = DynamicMeshBuilder()
        val chassis = DynamicMeshBuilder()
        val chrome = DynamicMeshBuilder()

        // Steel chassis & side underrun safety bars
        chassis.addBox(-0.55f, 1.02f, 0f, 0.22f, 0.26f, 11.1f, 0.14f, 0.16f, 0.19f)
        chassis.addBox(0.55f, 1.02f, 0f, 0.22f, 0.26f, 11.1f, 0.14f, 0.16f, 0.19f)
        for (side in listOf(-1f, 1f)) {
            // Side underrun bars
            chrome.addBox(side * 1.24f, 0.82f, 0.2f, 0.06f, 0.10f, 4.6f, 0.78f, 0.80f, 0.84f)
            chrome.addBox(side * 1.24f, 0.58f, 0.2f, 0.06f, 0.10f, 4.6f, 0.78f, 0.80f, 0.84f)
            // Landing gear legs near front
            chassis.addBox(side * 0.82f, 0.70f, 3.2f, 0.16f, 0.85f, 0.16f, 0.15f, 0.16f, 0.19f)
            chassis.addBox(side * 0.82f, 0.28f, 3.2f, 0.32f, 0.08f, 0.32f, 0.15f, 0.16f, 0.19f)
            // Mudflaps behind rear tandem axles
            chassis.addBox(side * 1.15f, 0.66f, -5.45f, 0.54f, 0.70f, 0.06f, 0.10f, 0.11f, 0.13f)
        }
        // Rear DOT crash bumper
        chrome.addBox(0f, 0.76f, -5.60f, 2.46f, 0.18f, 0.16f, 0.82f, 0.85f, 0.90f)

        // Main ribbed cargo container box (white base tinted by trailerColorHex)
        body.addSlopedBox(0f, 2.52f, 0f, 2.54f, 11.2f, 2.48f, 11.16f, 2.74f, 0f, 1f, 1f, 1f)
        // Corrugated vertical ribs along both sides for authentic metal trailer look
        for (rz in -50..50 step 8) {
            val zPos = rz / 10f
            body.addBox(-1.28f, 2.52f, zPos, 0.04f, 2.56f, 0.18f, 0.92f, 0.92f, 0.92f)
            body.addBox(1.28f, 2.52f, zPos, 0.04f, 2.56f, 0.18f, 0.92f, 0.92f, 0.92f)
        }
        // Roof rail trim & rear door frame
        chrome.addBox(0f, 3.88f, 0f, 2.54f, 0.10f, 11.22f, 0.82f, 0.84f, 0.88f)
        // Rear roll-up door vertical chrome lock bars
        for (lx in listOf(-0.62f, -0.22f, 0.22f, 0.62f)) {
            chrome.addBox(lx, 2.50f, -5.61f, 0.05f, 2.45f, 0.05f, 0.88f, 0.90f, 0.94f)
        }

        return TrailerMeshSet(
            bodyPaintMesh = body.build(),
            chassisMesh = chassis.build(),
            chromeMesh = chrome.build()
        )
    }

    fun buildTrafficMeshes(): TrafficMeshSet {
        // 1. Sculpted Sedan
        val sedanBody = DynamicMeshBuilder()
        val sedanGlass = DynamicMeshBuilder()
        sedanBody.addSlopedBox(0f, 0.62f, 0f, 1.84f, 4.25f, 1.78f, 3.95f, 0.56f, -0.05f, 1f, 1f, 1f)
        sedanBody.addSlopedBox(0f, 1.14f, -0.18f, 1.62f, 2.15f, 1.38f, 1.40f, 0.52f, -0.06f, 1f, 1f, 1f)
        sedanGlass.addSlopedBox(0f, 1.15f, -0.16f, 1.66f, 1.85f, 1.42f, 1.30f, 0.42f, -0.04f, 0.12f, 0.20f, 0.28f, 0.78f)

        // 2. Sculpted City Coach Bus
        val busBody = DynamicMeshBuilder()
        val busGlass = DynamicMeshBuilder()
        busBody.addSlopedBox(0f, 1.65f, 0f, 2.46f, 10.1f, 2.38f, 9.85f, 2.45f, -0.05f, 1f, 1f, 1f)
        // Roof AC units
        busBody.addBox(0f, 2.98f, 1.2f, 1.45f, 0.28f, 2.2f, 0.88f, 0.90f, 0.92f)
        busBody.addBox(0f, 2.98f, -2.0f, 1.45f, 0.28f, 2.2f, 0.88f, 0.90f, 0.92f)
        // Panoramic windows
        busGlass.addBox(0f, 2.02f, 0.2f, 2.48f, 0.92f, 8.8f, 0.12f, 0.20f, 0.30f, 0.82f)
        busGlass.addSlopedBox(0f, 1.98f, 4.95f, 2.26f, 0.16f, 2.20f, 0.12f, 1.18f, -0.05f, 0.12f, 0.20f, 0.30f, 0.82f)

        // 3. Delivery Box Truck
        val cargoCab = DynamicMeshBuilder()
        val cargoBox = DynamicMeshBuilder()
        cargoCab.addSlopedBox(0f, 1.42f, 2.45f, 2.36f, 2.25f, 2.28f, 2.05f, 2.05f, -0.08f, 0.86f, 0.24f, 0.18f)
        cargoCab.addSlopedBox(0f, 1.95f, 3.45f, 2.12f, 0.14f, 2.04f, 0.12f, 0.78f, -0.08f, 0.12f, 0.20f, 0.28f)
        cargoBox.addBox(0f, 1.78f, -1.25f, 2.44f, 2.55f, 5.25f, 1f, 1f, 1f)

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
        // Sculpted curved dashboard binnacle & center console
        dash.addSlopedBox(0.24f, -0.46f, 0.82f, 1.95f, 0.48f, 1.88f, 0.38f, 0.36f, 0.04f, 0.12f, 0.14f, 0.17f)
        // Gauge hood cowl in front of driver
        dash.addSlopedBox(0f, -0.24f, 0.72f, 0.56f, 0.24f, 0.50f, 0.18f, 0.20f, 0.02f, 0.09f, 0.10f, 0.13f)
        // Left & Right A-pillars + Roof header + sun visor
        dash.addBox(-0.56f, 0.06f, 0.92f, 0.09f, 1.25f, 0.10f, 0.10f, 0.12f, 0.15f)
        dash.addBox(1.14f, 0.06f, 0.92f, 0.09f, 1.25f, 0.10f, 0.10f, 0.12f, 0.15f)
        dash.addBox(0.29f, 0.58f, 0.90f, 1.92f, 0.16f, 0.22f, 0.09f, 0.10f, 0.13f)
        // Steering column shaft
        dash.addCylinderZ(0f, -0.34f, 0.52f, 0.75f, 0.055f, 10, 0.14f, 0.16f, 0.19f)

        // Real 3D Torus Steering Wheel with 3 ergonomic spokes & center airbag hub
        val wheel = DynamicMeshBuilder()
        wheel.addTorusZ(0f, 0f, 0f, 0.20f, 0.022f, 28, 10, 0.13f, 0.14f, 0.16f)
        // Racing top-center stripe on wheel rim
        wheel.addBox(0f, 0.20f, 0f, 0.032f, 0.046f, 0.046f, 0.96f, 0.44f, 0.06f)
        // Left, Right & Bottom metallic spokes
        wheel.addCylinderX(-0.19f, 0.19f, 0f, 0f, 0.022f, 8, 0.58f, 0.62f, 0.68f)
        wheel.addCylinderY(0f, -0.19f, 0f, 0.022f, 0.19f, 8, 0.58f, 0.62f, 0.68f)
        // Center hub & badge
        wheel.addCylinderZ(0f, 0f, -0.02f, 0.03f, 0.068f, 14, 0.16f, 0.18f, 0.22f)
        wheel.addCylinderZ(0f, 0f, 0.025f, 0.036f, 0.026f, 12, 0.94f, 0.44f, 0.06f)

        return CockpitMeshSet(
            dashboardMesh = dash.build(),
            steeringWheelMesh = wheel.build()
        )
    }
}
