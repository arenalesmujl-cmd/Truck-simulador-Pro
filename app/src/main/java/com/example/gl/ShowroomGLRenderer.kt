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
    private lateinit var unitWheel: MeshBuffer
    private lateinit var groundQuad: MeshBuffer
    private lateinit var pedestalMesh: MeshBuffer
    private lateinit var pedestalTrimMesh: MeshBuffer
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
        unitWheel = WorldMeshFactory.buildUnitWheel()
        groundQuad = WorldMeshFactory.buildGroundQuad()

        val ped = DynamicMeshBuilder()
        ped.addTaperedCylinderY(0f, -0.34f, 0f, 18.0f, 18.0f, 0.06f, 48, 0.045f, 0.058f, 0.082f)
        ped.addTaperedCylinderY(0f, -0.28f, 0f, 5.45f, 5.15f, 0.28f, 48, 0.08f, 0.10f, 0.14f)
        ped.addTaperedCylinderY(0f, -0.02f, 0f, 4.92f, 4.92f, 0.02f, 48, 0.13f, 0.16f, 0.21f)
        pedestalMesh = ped.build()

        val trim = DynamicMeshBuilder()
        trim.addTaperedCylinderY(0f, -0.025f, 0f, 5.18f, 5.18f, 0.03f, 48, 0.98f, 0.46f, 0.08f, capped = false)
        trim.addTaperedCylinderY(0f, -0.01f, 0f, 4.58f, 4.58f, 0.015f, 48, 0.16f, 0.78f, 0.96f, capped = false)
        trim.addTaperedCylinderY(0f, -0.275f, 0f, 8.20f, 8.20f, 0.015f, 48, 0.10f, 0.36f, 0.56f, capped = false)
        trim.addTaperedCylinderY(0f, 5.15f, 0f, 4.45f, 4.45f, 0.055f, 48, 0.85f, 0.91f, 1.0f, capped = false)
        pedestalTrimMesh = trim.build()

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
        val driverX = 2.04f
        val driverZ = 2.52f
        val driverYaw = 30f
        val breathBob = sin(idleTime * 2.0f) * 0.006f
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
        GLES30.glClearColor(0.035f, 0.052f, 0.088f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 38f, aspect, 0.3f, 65f)

        val camAngle = autoSpinRad + userOrbitOffsetRad
        val camDist = 10.4f
        val camX = sin(camAngle) * camDist
        val camY = 2.55f
        val camZ = cos(camAngle) * camDist
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0.32f, 1.44f, 0.42f, 0f, 1f, 0f)
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
        GLES30.glUniform3f(worldShader.uLightDirLoc, 0.60f, 0.85f, 0.58f)
        GLES30.glUniform3f(worldShader.uLightColorLoc, 1.02f, 0.97f, 0.91f)
        GLES30.glUniform3f(worldShader.uAmbientColorLoc, 0.44f, 0.48f, 0.56f)
        GLES30.glUniform3f(worldShader.uCameraPosLoc, camX, camY, camZ)
        GLES30.glUniform3f(worldShader.uFogColorLoc, 0.04f, 0.06f, 0.10f)
        GLES30.glUniform1f(worldShader.uFogDensityLoc, 0.011f)
        GLES30.glUniform1f(worldShader.uWetnessLoc, 0.20f)
        GLES30.glUniform1f(worldShader.uHeadlightIntensityLoc, 0f)

        // Studio turntable pedestal
        Matrix.setIdentityM(modelMatrix, 0)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMesh(pedestalMesh, 1f, 1f, 1f, 1f)

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMesh(pedestalTrimMesh, 1f, 1f, 1f, 1f)

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
        drawMesh(meshSet.glassMesh, 1f, 1f, 1f, 1f)

        // Steered hero forged-aluminum wheels
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        axles.forEachIndexed { idx, fz ->
            val st = if (idx == 0 || (style == TruckStyle.ARCTIC_EXPEDITION && idx == 1)) -14f else 0f
            for (sx in listOf(-1.18f, 1.18f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, sx, 0.53f, fz)
                Matrix.rotateM(modelMatrix, 0, st, 0f, 1f, 0f)
                drawMesh(unitWheel, 1f, 1f, 1f, 1f)
            }
        }

        // Studio DRL Headlights, Fog Lamps & Amber Roof Marker LEDs
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        val noseZ = when (style) {
            TruckStyle.EURO_CABOVER -> 3.05f
            TruckStyle.AMERICAN_LONGNOSE -> 3.55f
            TruckStyle.HEAVY_TWIN_EXHAUST -> 3.50f
            TruckStyle.ARCTIC_EXPEDITION -> 3.58f
        }
        drawBox(-0.88f, 0.98f, noseZ + 0.03f, 0.18f, 0.16f, 0.05f, 1f, 0.96f, 0.88f)
        drawBox(0.88f, 0.98f, noseZ + 0.03f, 0.18f, 0.16f, 0.05f, 1f, 0.96f, 0.88f)
        drawBox(-0.94f, 0.66f, noseZ + 0.22f, 0.19f, 0.075f, 0.03f, 0.95f, 0.92f, 0.82f)
        drawBox(0.94f, 0.66f, noseZ + 0.22f, 0.19f, 0.075f, 0.03f, 0.95f, 0.92f, 0.82f)
        val roofZ = if (style == TruckStyle.EURO_CABOVER) 2.58f else 1.65f
        val roofY = if (style == TruckStyle.EURO_CABOVER) 3.44f else 3.30f
        for (lx in listOf(-0.75f, -0.35f, 0f, 0.35f, 0.75f)) {
            drawBox(lx, roofY, roofZ, 0.12f, 0.07f, 0.08f, 1.0f, 0.62f, 0.06f)
        }

        // Articulated UV-Textured 3D Realistic Human Driver standing in grounded hero stance
        if (showDriver) {
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)

            bindTexture(textures.driverClothTex)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
            Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
            drawMesh(driverMeshes.torsoMesh, 1f, 1f, 1f, 1f)

            // UV-textured sculpted head + ears/hair/cap accessories
            Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
            Matrix.rotateM(modelMatrix, 0, sin(idleTime * 1.1f) * 5.5f, 0f, 1f, 0f)
            bindTexture(textures.driverFaceTex)
            drawMesh(driverMeshes.headFaceMesh, 1f, 1f, 1f, 1f)
            bindTexture(textures.whiteTex)
            drawMesh(driverMeshes.headAccMesh, 1f, 1f, 1f, 1f)

            bindTexture(textures.driverClothTex)
            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.235f, 0.49f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 8.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -6f else 5f, 1f, 0f, 0f)
                drawMesh(driverMeshes.upperArmMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -24f else -15f, 1f, 0f, 0f)
                val forearmMesh = if (side > 0) driverMeshes.leftForearmHandMesh else driverMeshes.rightForearmHandMesh
                drawMesh(forearmMesh, 1f, 1f, 1f, 1f)
            }

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.112f, 0.02f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 4.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, side * 3.5f, 1f, 0f, 0f)
                drawMesh(driverMeshes.thighMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) 4f else -2f, 1f, 0f, 0f)
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
                drawShadowMesh(unitWheel)
            }
        }

        if (showDriver) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
            Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
            drawShadowMesh(driverMeshes.torsoMesh)

            Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
            Matrix.rotateM(modelMatrix, 0, sin(idleTime * 1.1f) * 5.5f, 0f, 1f, 0f)
            drawShadowMesh(driverMeshes.headFaceMesh)
            drawShadowMesh(driverMeshes.headAccMesh)

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.235f, 0.49f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 8.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -6f else 5f, 1f, 0f, 0f)
                drawShadowMesh(driverMeshes.upperArmMesh)

                Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -24f else -15f, 1f, 0f, 0f)
                val forearmMesh = if (side > 0) driverMeshes.leftForearmHandMesh else driverMeshes.rightForearmHandMesh
                drawShadowMesh(forearmMesh)
            }

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, driverX, pelvisY, driverZ)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.112f, 0.02f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 4.5f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, side * 3.5f, 1f, 0f, 0f)
                drawShadowMesh(driverMeshes.thighMesh)

                Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) 4f else -2f, 1f, 0f, 0f)
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
