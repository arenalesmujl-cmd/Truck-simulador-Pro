package com.example.gl

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.TruckStyle
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class ShowroomGLRenderer : GLSurfaceView.Renderer {

    @Volatile var truckStyle: TruckStyle = TruckStyle.AMERICAN_LONGNOSE
    @Volatile var paintHex: String = "#FF6D00"
    @Volatile var userOrbitOffsetRad: Float = 0f
    @Volatile var showDriver: Boolean = true

    private var surfaceWidth = 800
    private var surfaceHeight = 480
    private var lastFrameNs = System.nanoTime()
    private var autoSpinRad = 0.56f
    private var idleTime = 0f

    private lateinit var shadowShader: ShadowDepthShaderProgram
    private lateinit var worldShader: WorldShaderProgram
    private lateinit var textures: WorldTextureSet
    private lateinit var unitCube: MeshBuffer
    private lateinit var unitWheelSet: WheelMeshSet
    private lateinit var groundQuad: MeshBuffer
    private lateinit var pedestalMesh: MeshBuffer
    private lateinit var pedestalTrimMesh: MeshBuffer
    private lateinit var studioBayMesh: MeshBuffer
    private lateinit var studioLightsMesh: MeshBuffer
    private lateinit var truckMeshes: Map<TruckStyle, TruckMeshSet>
    private lateinit var driverMeshes: DriverMeshSet

    // Real-time directional shadow map FBO (1024x1024 depth texture)
    private var shadowFbo = 0
    private var shadowDepthTex = 0
    private val shadowSize = 1024

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val lightProjMatrix = FloatArray(16)
    private val lightViewMatrix = FloatArray(16)
    private val lightVpMatrix = FloatArray(16)
    private val lightMvpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        shadowShader = ShadowDepthShaderProgram()
        worldShader = WorldShaderProgram()
        textures = TextureFactory.createAll()
        unitCube = WorldMeshFactory.buildUnitCube()
        unitWheelSet = WorldMeshFactory.buildUnitWheelSet()
        groundQuad = WorldMeshFactory.buildGroundQuad()

        val ped = DynamicMeshBuilder()
        // Expansive polished dark slate garage floor + stepped turntable pedestal
        ped.addTaperedCylinderY(0f, -0.34f, 0f, 30.0f, 30.0f, 0.06f, 56, 0.055f, 0.070f, 0.098f)
        ped.addTaperedCylinderY(0f, -0.28f, 0f, 5.65f, 5.25f, 0.28f, 56, 0.085f, 0.105f, 0.145f)
        ped.addTaperedCylinderY(0f, -0.02f, 0f, 4.96f, 4.96f, 0.02f, 56, 0.14f, 0.17f, 0.22f)
        pedestalMesh = ped.build()

        val trim = DynamicMeshBuilder()
        // Crisp turntable perimeter amber & cyan light rings (grounded on floor, zero floating artifacts)
        trim.addTaperedCylinderY(0f, -0.025f, 0f, 5.22f, 5.22f, 0.03f, 56, 0.98f, 0.46f, 0.08f, capped = false)
        trim.addTaperedCylinderY(0f, -0.01f, 0f, 4.58f, 4.58f, 0.015f, 56, 0.16f, 0.78f, 0.96f, capped = false)
        trim.addTaperedCylinderY(0f, -0.275f, 0f, 8.60f, 8.60f, 0.015f, 56, 0.12f, 0.40f, 0.62f, capped = false)
        pedestalTrimMesh = trim.build()

        // 3D Architectural Studio Garage Bay: dark cyclorama walls, steel columns & overhead truss ring
        val bay = DynamicMeshBuilder()
        val wallSegs = 40
        val wallR = 26.5f
        for (i in 0 until wallSegs) {
            val a0 = (i.toFloat() / wallSegs) * 2f * Math.PI.toFloat()
            val a1 = ((i + 1).toFloat() / wallSegs) * 2f * Math.PI.toFloat()
            val x0 = cos(a0) * wallR; val z0 = sin(a0) * wallR
            val x1 = cos(a1) * wallR; val z1 = sin(a1) * wallR
            val nx = -cos((a0 + a1) * 0.5f); val nz = -sin((a0 + a1) * 0.5f)
            bay.addQuad(x1, -0.35f, z1, x0, -0.35f, z0, x0, 12.5f, z0, x1, 12.5f, z1, nx, 0.15f, nz, 0.055f, 0.072f, 0.105f)
            // Architectural steel wainscot dado band along lower studio wall
            bay.addQuad(x1, -0.32f, z1, x0, -0.32f, z0, x0, 1.55f, z0, x1, 1.55f, z1, nx, 0.1f, nz, 0.082f, 0.102f, 0.138f)
        }
        for (k in 0 until 8) {
            val a = (k.toFloat() / 8f) * 2f * Math.PI.toFloat() + 0.35f
            val px = cos(a) * 19.5f
            val pz = sin(a) * 19.5f
            bay.addBox(px, 4.4f, pz, 0.58f, 9.6f, 0.58f, 0.11f, 0.14f, 0.19f)
            bay.addBox(px, 0.25f, pz, 0.92f, 1.15f, 0.92f, 0.16f, 0.19f, 0.25f)
        }
        // Overhead octagonal steel gantry truss ring + aluminum softbox light housings
        for (k in 0 until 12) {
            val a0 = (k.toFloat() / 12f) * 2f * Math.PI.toFloat()
            val a1 = ((k + 1).toFloat() / 12f) * 2f * Math.PI.toFloat()
            val mx = cos((a0 + a1) * 0.5f) * 11.2f
            val mz = sin((a0 + a1) * 0.5f) * 11.2f
            bay.addBox(mx, 7.65f, mz, 0.42f, 0.36f, 6.1f, 0.12f, 0.15f, 0.20f)
        }
        for (lx in listOf(-4.2f, 0f, 4.2f)) {
            bay.addBox(lx, 7.52f, 0f, 1.32f, 0.14f, 7.42f, 0.09f, 0.11f, 0.15f)
            bay.addCylinderY(lx, 7.58f, -2.6f, 0.06f, 1.2f, 6, 0.14f, 0.16f, 0.20f)
            bay.addCylinderY(lx, 7.58f, 2.6f, 0.06f, 1.2f, 6, 0.14f, 0.16f, 0.20f)
        }
        studioBayMesh = bay.build()

        // Overhead studio softbox LED light panels + crisp wall accent light strips
        val lights = DynamicMeshBuilder()
        for (lx in listOf(-4.2f, 0f, 4.2f)) {
            lights.addBox(lx, 7.42f, 0f, 1.12f, 0.05f, 7.16f, 0.94f, 0.97f, 1.0f)
        }
        for (k in 0 until 8) {
            val a = (k.toFloat() / 8f) * 2f * Math.PI.toFloat() + 0.35f
            val px = cos(a) * 19.12f
            val pz = sin(a) * 19.12f
            val isAmber = k % 2 == 0
            lights.addBox(
                px, 3.4f, pz,
                0.14f, 4.2f, 0.14f,
                if (isAmber) 0.96f else 0.20f,
                if (isAmber) 0.48f else 0.78f,
                if (isAmber) 0.10f else 0.98f
            )
        }
        studioLightsMesh = lights.build()

        truckMeshes = TruckStyle.entries.associateWith { TruckMeshFactory.buildTruckMesh(it) }
        driverMeshes = CharacterMeshFactory.buildDriverMeshes()
        initShadowFbo()
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs
        idleTime += dt
        autoSpinRad += dt * 0.18f

        val style = truckStyle
        val meshSet = truckMeshes[style] ?: return
        val driverX = 1.88f
        val driverZ = 2.62f
        val driverYaw = 20f
        val breathBob = sin(idleTime * 2.0f) * 0.0055f
        val pelvisY = 0.89f + breathBob
        val axles = when (style) {
            TruckStyle.EURO_CABOVER -> listOf(2.40f, -1.20f, -2.50f)
            TruckStyle.ARCTIC_EXPEDITION -> listOf(2.55f, 1.35f, -1.25f, -2.55f)
            else -> listOf(2.50f, -1.25f, -2.55f)
        }

        // 1. Directional Shadow Depth Pass
        renderShadowPass(meshSet, style, axles, driverX, pelvisY, driverZ, driverYaw)

        // 2. Main Beauty Pass
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES30.glClearColor(0.036f, 0.054f, 0.092f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 36f, aspect, 0.3f, 75f)

        val camAngle = autoSpinRad + userOrbitOffsetRad
        val camDist = 8.45f
        val camX = sin(camAngle) * camDist
        val camY = 2.02f
        val camZ = cos(camAngle) * camDist
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0.26f, 1.42f, 0.45f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        worldShader.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glUniform1i(worldShader.uShadowMapLoc, 1)
        GLES30.glUniformMatrix4fv(worldShader.uLightVpLoc, 1, false, lightVpMatrix, 0)
        GLES30.glUniform1f(worldShader.uShadowEnabledLoc, 1f)
        GLES30.glUniform1f(worldShader.uStarIntensityLoc, 0f)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uTimeLoc, idleTime)
        GLES30.glUniform3f(worldShader.uLightDirLoc, 0.58f, 0.86f, 0.62f)
        GLES30.glUniform3f(worldShader.uLightColorLoc, 1.08f, 1.01f, 0.94f)
        GLES30.glUniform3f(worldShader.uAmbientColorLoc, 0.44f, 0.49f, 0.57f)
        GLES30.glUniform3f(worldShader.uCameraPosLoc, camX, camY, camZ)
        GLES30.glUniform3f(worldShader.uFogColorLoc, 0.042f, 0.062f, 0.105f)
        GLES30.glUniform1f(worldShader.uFogDensityLoc, 0.014f)
        GLES30.glUniform1f(worldShader.uWetnessLoc, 0.26f)
        GLES30.glUniform1f(worldShader.uHeadlightIntensityLoc, 0f)

        // Studio Hangar Cyclorama Bay & Overhead Softbox Lighting
        Matrix.setIdentityM(modelMatrix, 0)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMesh(studioBayMesh, 1f, 1f, 1f, 1f)

        // Studio turntable pedestal
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMesh(pedestalMesh, 1f, 1f, 1f, 1f)

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMesh(pedestalTrimMesh, 1f, 1f, 1f, 1f)
        drawMesh(studioLightsMesh, 1f, 1f, 1f, 1f)

        // Soft studio ambient ground shadows beneath tires and boots
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.softShadowTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.02f, 0f)
        Matrix.scaleM(modelMatrix, 0, 4.4f, 1f, 9.2f)
        drawMesh(groundQuad, 1f, 1f, 1f, 0.86f)
        if (showDriver) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, driverX, 0.025f, driverZ)
            Matrix.scaleM(modelMatrix, 0, 0.96f, 1f, 0.96f)
            drawMesh(groundQuad, 1f, 1f, 1f, 0.84f)
        }
        GLES30.glDepthMask(true)

        // Selected 3D Truck
        bindTexture(textures.whiteTex)
        val (pr, pg, pb) = parseHexRgb(paintHex)

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMesh(meshSet.chassisMesh, 1f, 1f, 1f, 1f)

        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
        drawMesh(meshSet.cabPaintMesh, pr, pg, pb, 1f)

        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMesh(meshSet.chromeMesh, 1f, 1f, 1f, 1f)

        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_GLASS)
        GLES30.glDepthMask(false)
        drawMesh(meshSet.glassMesh, 1f, 1f, 1f, 1f)
        GLES30.glDepthMask(true)

        // Steered hero wheels: matte vulcanized rubber tires + mirror forged-aluminum Alcoa rims
        axles.forEachIndexed { idx, fz ->
            val st = if (idx == 0 || (style == TruckStyle.ARCTIC_EXPEDITION && idx == 1)) -14f else 0f
            for (sx in listOf(-1.18f, 1.18f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, sx, 0.53f, fz)
                Matrix.rotateM(modelMatrix, 0, st, 0f, 1f, 0f)
                GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
                drawMesh(unitWheelSet.tireMesh, 1f, 1f, 1f, 1f)
                GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
                drawMesh(unitWheelSet.rimMesh, 1f, 1f, 1f, 1f)
            }
        }

        // Crisp Recessed Dual-Optic Projector Headlights, LED DRL Lightbars & Amber Roof Marker LEDs
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        val noseZ = when (style) {
            TruckStyle.EURO_CABOVER -> 3.05f
            TruckStyle.AMERICAN_LONGNOSE -> 3.55f
            TruckStyle.HEAVY_TWIN_EXHAUST -> 3.50f
            TruckStyle.ARCTIC_EXPEDITION -> 3.58f
        }
        for (sx in listOf(-1f, 1f)) {
            drawBox(sx * 0.88f, 0.99f, noseZ + 0.036f, 0.14f, 0.075f, 0.025f, 1f, 0.97f, 0.90f)
            drawBox(sx * 0.88f, 0.915f, noseZ + 0.040f, 0.21f, 0.024f, 0.020f, 0.90f, 0.96f, 1.0f)
            drawBox(sx * 0.94f, 0.66f, noseZ + 0.222f, 0.18f, 0.048f, 0.022f, 0.96f, 0.94f, 0.86f)
        }
        val roofZ = if (style == TruckStyle.EURO_CABOVER) 2.58f else 1.65f
        val roofY = if (style == TruckStyle.EURO_CABOVER) 3.44f else 3.30f
        for (lx in listOf(-0.75f, -0.35f, 0f, 0.35f, 0.75f)) {
            drawBox(lx, roofY, roofZ, 0.12f, 0.065f, 0.07f, 1.0f, 0.62f, 0.06f)
        }

        // Articulated UV-Textured 3D Realistic Human Driver standing in natural contrapposto hero stance
        if (showDriver) {
            val headLook = sin(idleTime * 1.1f) * 6.5f
            val torsoRoll = sin(idleTime * 1.4f) * 0.8f + 1.8f

            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)

            bindTexture(textures.driverClothTex)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
            Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, torsoRoll, 0f, 0f, 1f)
            drawMesh(driverMeshes.torsoMesh, 1f, 1f, 1f, 1f)

            // UV-textured sculpted head + ears/hair/cap + sport-aviator sunglasses
            Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
            Matrix.rotateM(modelMatrix, 0, headLook, 0f, 1f, 0f)
            bindTexture(textures.driverFaceTex)
            drawMesh(driverMeshes.headFaceMesh, 1f, 1f, 1f, 1f)
            bindTexture(textures.whiteTex)
            drawMesh(driverMeshes.headAccMesh, 1f, 1f, 1f, 1f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
            drawMesh(driverMeshes.headGlassesFrameMesh, 1f, 1f, 1f, 1f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_GLASS)
            GLES30.glDepthMask(false)
            drawMesh(driverMeshes.headGlassesLensMesh, 1f, 1f, 1f, 1f)
            GLES30.glDepthMask(true)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)

            bindTexture(textures.driverClothTex)
            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.rotateM(modelMatrix, 0, torsoRoll, 0f, 0f, 1f)
                Matrix.translateM(modelMatrix, 0, side * 0.214f, 0.49f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 9.0f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -8f else 5f, 1f, 0f, 0f)
                drawMesh(driverMeshes.upperArmMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -28f else -14f, 1f, 0f, 0f)
                val forearmMesh = if (side > 0) driverMeshes.leftForearmHandMesh else driverMeshes.rightForearmHandMesh
                drawMesh(forearmMesh, 1f, 1f, 1f, 1f)
            }

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.102f, 0.02f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 4.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -4.5f else 3.0f, 1f, 0f, 0f)
                drawMesh(driverMeshes.thighMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) 7.5f else 2.5f, 1f, 0f, 0f)
                drawMesh(driverMeshes.shinBootMesh, 1f, 1f, 1f, 1f)
            }
        }
    }

    private fun renderShadowPass(
        meshSet: TruckMeshSet,
        style: TruckStyle,
        axles: List<Float>,
        driverX: Float,
        pelvisY: Float,
        driverZ: Float,
        driverYaw: Float
    ) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glViewport(0, 0, shadowSize, shadowSize)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glPolygonOffset(1.8f, 3.0f)

        Matrix.orthoM(lightProjMatrix, 0, -9.5f, 9.5f, -9.5f, 9.5f, 1f, 36f)
        Matrix.setLookAtM(lightViewMatrix, 0, 9.6f, 14.6f, 9.8f, 0f, 1.0f, 0.5f, 0f, 1f, 0f)
        Matrix.multiplyMM(lightVpMatrix, 0, lightProjMatrix, 0, lightViewMatrix, 0)

        shadowShader.use()

        Matrix.setIdentityM(modelMatrix, 0)
        drawShadowMesh(meshSet.chassisMesh)
        drawShadowMesh(meshSet.cabPaintMesh)
        drawShadowMesh(meshSet.chromeMesh)

        axles.forEachIndexed { idx, fz ->
            val st = if (idx == 0 || (style == TruckStyle.ARCTIC_EXPEDITION && idx == 1)) -14f else 0f
            for (sx in listOf(-1.18f, 1.18f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, sx, 0.53f, fz)
                Matrix.rotateM(modelMatrix, 0, st, 0f, 1f, 0f)
                drawShadowMesh(unitWheelSet.tireMesh)
            }
        }

        if (showDriver) {
            val headLook = sin(idleTime * 1.1f) * 6.5f
            val torsoRoll = sin(idleTime * 1.4f) * 0.8f + 1.8f

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
            Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, torsoRoll, 0f, 0f, 1f)
            drawShadowMesh(driverMeshes.torsoMesh)

            Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
            Matrix.rotateM(modelMatrix, 0, headLook, 0f, 1f, 0f)
            drawShadowMesh(driverMeshes.headFaceMesh)
            drawShadowMesh(driverMeshes.headAccMesh)
            drawShadowMesh(driverMeshes.headGlassesFrameMesh)

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.rotateM(modelMatrix, 0, torsoRoll, 0f, 0f, 1f)
                Matrix.translateM(modelMatrix, 0, side * 0.214f, 0.49f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 9.0f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -8f else 5f, 1f, 0f, 0f)
                drawShadowMesh(driverMeshes.upperArmMesh)

                Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -28f else -14f, 1f, 0f, 0f)
                val forearmMesh = if (side > 0) driverMeshes.leftForearmHandMesh else driverMeshes.rightForearmHandMesh
                drawShadowMesh(forearmMesh)
            }

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.102f, 0.02f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 4.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -4.5f else 3.0f, 1f, 0f, 0f)
                drawShadowMesh(driverMeshes.thighMesh)

                Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) 7.5f else 2.5f, 1f, 0f, 0f)
                drawShadowMesh(driverMeshes.shinBootMesh)
            }
        }

        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
    }

    private fun drawShadowMesh(mesh: MeshBuffer) {
        Matrix.multiplyMM(lightMvpMatrix, 0, lightVpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(shadowShader.uLightMvpLoc, 1, false, lightMvpMatrix, 0)
        mesh.draw()
    }

    private fun initShadowFbo() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)

        shadowFbo = fbos[0]
        shadowDepthTex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            shadowSize, shadowSize, 0,
            GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null as ByteBuffer?
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, shadowDepthTex, 0)
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun bindTexture(texId: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1i(worldShader.uTextureLoc, 0)
    }

    private fun drawMesh(mesh: MeshBuffer, r: Float, g: Float, b: Float, a: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(worldShader.uColorTintLoc, r, g, b, a)
        mesh.draw()
    }

    private fun drawBox(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMesh(unitCube, r, g, b, 1f)
    }

    private fun parseHexRgb(hex: String): Triple<Float, Float, Float> {
        return try {
            val c = Color.parseColor(hex)
            Triple(Color.red(c) / 255f, Color.green(c) / 255f, Color.blue(c) / 255f)
        } catch (_: Exception) {
            Triple(1f, 0.43f, 0f)
        }
    }
}
