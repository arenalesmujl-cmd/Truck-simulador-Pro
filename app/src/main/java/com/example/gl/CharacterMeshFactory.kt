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
     * Builds an articulated realistic 3D human truck driver (~1.82m tall).
     * Each limb segment has its rotation pivot at (0, 0, 0) extending along -Y so
     * skeletal joint rotations (shoulder, elbow, hip, knee) articulate naturally.
     */
    fun buildDriverMeshes(): DriverMeshSet {
        // 1. Head + Neck + Jaw + Nose + Ears + Hair + Trucker Cap + Aviator Sunglasses
        val head = DynamicMeshBuilder()
        // Neck
        head.addTaperedCylinderY(0f, -0.02f, 0f, 0.058f, 0.052f, 0.09f, 10, 0.84f, 0.66f, 0.54f)
        // Cranium & face ellipsoid
        head.addEllipsoid(0f, 0.155f, 0.005f, 0.095f, 0.118f, 0.105f, 8, 12, 0.88f, 0.70f, 0.58f)
        // Jaw & chin definition
        head.addEllipsoid(0f, 0.088f, 0.026f, 0.072f, 0.056f, 0.078f, 6, 10, 0.85f, 0.67f, 0.55f)
        // Nose bridge
        head.addSlopedBox(0f, 0.145f, 0.102f, 0.028f, 0.032f, 0.018f, 0.018f, 0.048f, -0.006f, 0.84f, 0.66f, 0.54f)
        // Ears
        head.addEllipsoid(-0.094f, 0.152f, 0f, 0.016f, 0.030f, 0.022f, 4, 6, 0.82f, 0.64f, 0.52f)
        head.addEllipsoid(0.094f, 0.152f, 0f, 0.016f, 0.030f, 0.022f, 4, 6, 0.82f, 0.64f, 0.52f)
        // Short dark hair back/sides
        head.addEllipsoid(0f, 0.175f, -0.018f, 0.098f, 0.096f, 0.096f, 6, 10, 0.16f, 0.13f, 0.11f)
        // Trucker Cap dome + curved front visor + front orange patch
        head.addEllipsoid(0f, 0.215f, -0.006f, 0.104f, 0.076f, 0.112f, 6, 12, 0.14f, 0.18f, 0.25f)
        head.addSlopedBox(0f, 0.205f, 0.125f, 0.165f, 0.105f, 0.140f, 0.090f, 0.016f, 0.008f, 0.95f, 0.42f, 0.06f)
        head.addBox(0f, 0.232f, 0.095f, 0.058f, 0.036f, 0.016f, 0.95f, 0.42f, 0.06f)
        // Aviator sunglasses
        head.addBox(-0.042f, 0.165f, 0.096f, 0.048f, 0.032f, 0.020f, 0.06f, 0.07f, 0.09f)
        head.addBox(0.042f, 0.165f, 0.096f, 0.048f, 0.032f, 0.020f, 0.06f, 0.07f, 0.09f)
        head.addBox(0f, 0.174f, 0.098f, 0.095f, 0.008f, 0.014f, 0.78f, 0.80f, 0.84f)

        // 2. Torso (Pelvis/Waist origin at 0, extends up +0.58m to shoulder/neck line)
        val torso = DynamicMeshBuilder()
        // Pelvis / Belt area
        torso.addTaperedCylinderY(0f, 0.0f, 0f, 0.155f, 0.148f, 0.13f, 12, 0.14f, 0.20f, 0.30f, scaleX = 1.18f, scaleZ = 0.78f)
        // Leather belt & steel buckle
        torso.addTaperedCylinderY(0f, 0.09f, 0f, 0.154f, 0.154f, 0.045f, 12, 0.18f, 0.12f, 0.08f, scaleX = 1.20f, scaleZ = 0.80f)
        torso.addBox(0f, 0.112f, 0.125f, 0.065f, 0.046f, 0.020f, 0.82f, 0.84f, 0.88f)
        // Tapered muscular trucker jacket torso (waist -> broad chest/shoulders)
        torso.addTaperedCylinderY(0f, 0.12f, 0f, 0.152f, 0.195f, 0.42f, 14, 0.14f, 0.17f, 0.22f, scaleX = 1.18f, scaleZ = 0.76f)
        // Rounded shoulder yoke
        torso.addEllipsoid(0f, 0.50f, 0f, 0.235f, 0.078f, 0.135f, 6, 12, 0.14f, 0.17f, 0.22f)
        // Inner collar & shirt V-panel
        torso.addSlopedBox(0f, 0.47f, 0.122f, 0.075f, 0.03f, 0.105f, 0.03f, 0.16f, 0f, 0.86f, 0.88f, 0.90f)
        torso.addTaperedCylinderY(0f, 0.52f, 0f, 0.095f, 0.085f, 0.06f, 10, 0.18f, 0.21f, 0.27f)
        // High-visibility safety reflective stripes on jacket (orange/amber + silver reflector)
        torso.addTaperedCylinderY(0f, 0.20f, 0f, 0.162f, 0.168f, 0.055f, 14, 0.96f, 0.44f, 0.05f, capped = false, scaleX = 1.20f, scaleZ = 0.78f)
        torso.addTaperedCylinderY(0f, 0.215f, 0f, 0.165f, 0.167f, 0.022f, 14, 0.88f, 0.90f, 0.94f, capped = false, scaleX = 1.21f, scaleZ = 0.79f)
        torso.addBox(-0.10f, 0.38f, 0.138f, 0.038f, 0.28f, 0.018f, 0.96f, 0.44f, 0.05f)
        torso.addBox(0.10f, 0.38f, 0.138f, 0.038f, 0.28f, 0.018f, 0.96f, 0.44f, 0.05f)

        // 3. Upper Arm (pivot at shoulder (0,0,0), extends down to -0.29m)
        val upperArm = DynamicMeshBuilder()
        upperArm.addEllipsoid(0f, -0.02f, 0f, 0.062f, 0.065f, 0.062f, 6, 10, 0.15f, 0.18f, 0.23f)
        upperArm.addTaperedCylinderY(0f, -0.29f, 0f, 0.048f, 0.058f, 0.27f, 10, 0.14f, 0.17f, 0.22f)
        upperArm.addEllipsoid(0f, -0.29f, 0f, 0.047f, 0.047f, 0.047f, 5, 8, 0.15f, 0.18f, 0.23f)

        // 4. Forearm + Hand (pivot at elbow (0,0,0), extends down to -0.32m)
        val forearm = DynamicMeshBuilder()
        // Jacket sleeve cuff + bare skin wrist/forearm
        forearm.addTaperedCylinderY(0f, -0.18f, 0f, 0.040f, 0.046f, 0.18f, 10, 0.15f, 0.18f, 0.23f)
        forearm.addTaperedCylinderY(0f, -0.24f, 0f, 0.035f, 0.039f, 0.06f, 10, 0.85f, 0.67f, 0.55f)
        // Driving glove / sculpted hand
        forearm.addEllipsoid(0f, -0.285f, 0.008f, 0.036f, 0.052f, 0.024f, 5, 8, 0.22f, 0.16f, 0.12f)
        forearm.addBox(0f, -0.30f, 0.026f, 0.022f, 0.036f, 0.018f, 0.22f, 0.16f, 0.12f)

        // 5. Thigh (pivot at hip (0,0,0), extends down to -0.42m)
        val thigh = DynamicMeshBuilder()
        thigh.addEllipsoid(0f, -0.02f, 0f, 0.078f, 0.072f, 0.078f, 5, 10, 0.13f, 0.20f, 0.32f)
        thigh.addTaperedCylinderY(0f, -0.42f, 0f, 0.060f, 0.076f, 0.40f, 10, 0.14f, 0.21f, 0.34f)
        thigh.addEllipsoid(0f, -0.42f, 0f, 0.059f, 0.056f, 0.059f, 5, 8, 0.13f, 0.20f, 0.32f)

        // 6. Shin + Work Boot (pivot at knee (0,0,0), extends down to -0.47m)
        val shin = DynamicMeshBuilder()
        shin.addTaperedCylinderY(0f, -0.33f, 0f, 0.050f, 0.058f, 0.33f, 10, 0.14f, 0.21f, 0.34f)
        // Leather boot shaft & ankle
        shin.addTaperedCylinderY(0f, -0.42f, 0f, 0.056f, 0.054f, 0.10f, 10, 0.26f, 0.16f, 0.10f)
        // Boot foot & rounded steel-toe cap + lugged rubber sole
        shin.addSlopedBox(0f, -0.425f, 0.048f, 0.105f, 0.22f, 0.095f, 0.18f, 0.065f, -0.01f, 0.28f, 0.17f, 0.11f)
        shin.addBox(0f, -0.462f, 0.048f, 0.110f, 0.024f, 0.23f, 0.10f, 0.10f, 0.11f)

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
