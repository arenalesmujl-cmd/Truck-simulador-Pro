package com.example.gl

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.TruckStyle
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

    private lateinit var worldShader: WorldShaderProgram
    private lateinit var textures: WorldTextureSet
    private lateinit var unitCube: MeshBuffer
    private lateinit var unitWheel: MeshBuffer
    private lateinit var groundQuad: MeshBuffer
    private lateinit var pedestalMesh: MeshBuffer
    private lateinit var pedestalTrimMesh: MeshBuffer
    private lateinit var truckMeshes: Map<TruckStyle, TruckMeshSet>
    private lateinit var driverMeshes: DriverMeshSet

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        worldShader = WorldShaderProgram()
        textures = TextureFactory.createAll()
        unitCube = WorldMeshFactory.buildUnitCube()
        unitWheel = WorldMeshFactory.buildUnitWheel()
        groundQuad = WorldMeshFactory.buildGroundQuad()

        val ped = DynamicMeshBuilder()
        ped.addTaperedCylinderY(0f, -0.28f, 0f, 5.45f, 5.15f, 0.28f, 48, 0.08f, 0.10f, 0.14f)
        ped.addTaperedCylinderY(0f, -0.02f, 0f, 4.92f, 4.92f, 0.02f, 48, 0.13f, 0.16f, 0.21f)
        pedestalMesh = ped.build()

        val trim = DynamicMeshBuilder()
        trim.addTaperedCylinderY(0f, -0.025f, 0f, 5.18f, 5.18f, 0.03f, 48, 0.98f, 0.46f, 0.08f, capped = false)
        trim.addTaperedCylinderY(0f, -0.01f, 0f, 4.58f, 4.58f, 0.015f, 48, 0.16f, 0.78f, 0.96f, capped = false)
        pedestalTrimMesh = trim.build()

        truckMeshes = TruckStyle.entries.associateWith { TruckMeshFactory.buildTruckMesh(it) }
        driverMeshes = CharacterMeshFactory.buildDriverMeshes()
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
        autoSpinRad += dt * 0.20f

        GLES30.glClearColor(0.035f, 0.052f, 0.088f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 38f, aspect, 0.3f, 65f)

        val camAngle = autoSpinRad + userOrbitOffsetRad
        val camDist = 10.4f
        val camX = sin(camAngle) * camDist
        val camY = 2.60f
        val camZ = cos(camAngle) * camDist
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0.28f, 1.46f, 0.35f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        worldShader.use()
        bindWhite()
        GLES30.glUniform3f(worldShader.uLightDirLoc, 0.60f, 0.85f, 0.58f)
        GLES30.glUniform3f(worldShader.uLightColorLoc, 1.02f, 0.97f, 0.91f)
        GLES30.glUniform3f(worldShader.uAmbientColorLoc, 0.42f, 0.46f, 0.54f)
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

        // Soft studio ambient ground shadows
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.softShadowTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.02f, 0f)
        Matrix.scaleM(modelMatrix, 0, 4.4f, 1f, 9.2f)
        drawMesh(groundQuad, 1f, 1f, 1f, 0.92f)
        if (showDriver) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 2.16f, 0.025f, 1.92f)
            Matrix.scaleM(modelMatrix, 0, 1.05f, 1f, 1.05f)
            drawMesh(groundQuad, 1f, 1f, 1f, 0.88f)
        }
        GLES30.glDepthMask(true)

        // Selected 3D Truck
        bindWhite()
        val style = truckStyle
        val meshSet = truckMeshes[style] ?: return
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

        // Steered hero wheels
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        val axles = when (style) {
            TruckStyle.EURO_CABOVER -> listOf(2.40f, -1.20f, -2.50f)
            TruckStyle.ARCTIC_EXPEDITION -> listOf(2.55f, 1.35f, -1.25f, -2.55f)
            else -> listOf(2.50f, -1.25f, -2.55f)
        }
        axles.forEachIndexed { idx, fz ->
            val st = if (idx == 0 || (style == TruckStyle.ARCTIC_EXPEDITION && idx == 1)) -14f else 0f
            for (sx in listOf(-1.18f, 1.18f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, sx, 0.53f, fz)
                Matrix.rotateM(modelMatrix, 0, st, 0f, 1f, 0f)
                drawMesh(unitWheel, 1f, 1f, 1f, 1f)
            }
        }

        // Studio DRL Headlights & Amber Roof Marker LEDs
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        val noseZ = if (style == TruckStyle.EURO_CABOVER) 3.06f else 3.58f
        drawBox(-0.88f, 0.98f, noseZ, 0.20f, 0.16f, 0.08f, 1f, 0.96f, 0.88f)
        drawBox(0.88f, 0.98f, noseZ, 0.20f, 0.16f, 0.08f, 1f, 0.96f, 0.88f)
        val roofZ = if (style == TruckStyle.EURO_CABOVER) 2.58f else 1.65f
        val roofY = if (style == TruckStyle.EURO_CABOVER) 3.44f else 3.30f
        for (lx in listOf(-0.75f, -0.35f, 0f, 0.35f, 0.75f)) {
            drawBox(lx, roofY, roofZ, 0.12f, 0.07f, 0.08f, 1.0f, 0.62f, 0.06f)
        }

        // Articulated 3D Realistic Human Driver standing in natural relaxed hero stance!
        if (showDriver) {
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)
            val dx = 2.16f
            val dz = 1.92f
            val driverYaw = 28f
            val breathBob = sin(idleTime * 2.0f) * 0.007f
            val pelvisY = 0.89f + breathBob

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, dx, pelvisY, dz)
            Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
            drawMesh(driverMeshes.torsoMesh, 1f, 1f, 1f, 1f)

            Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
            Matrix.rotateM(modelMatrix, 0, sin(idleTime * 1.1f) * 7f, 0f, 1f, 0f)
            drawMesh(driverMeshes.headMesh, 1f, 1f, 1f, 1f)

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, dx, pelvisY, dz)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.24f, 0.49f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 8f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -6f else 4f, 1f, 0f, 0f)
                drawMesh(driverMeshes.upperArmMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) -22f else -14f, 1f, 0f, 0f)
                drawMesh(driverMeshes.forearmHandMesh, 1f, 1f, 1f, 1f)
            }

            for (side in listOf(-1f, 1f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, dx, pelvisY, dz)
                Matrix.rotateM(modelMatrix, 0, driverYaw, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.105f, 0.02f, 0f)
                Matrix.rotateM(modelMatrix, 0, side * 3.0f, 0f, 0f, 1f)
                Matrix.rotateM(modelMatrix, 0, side * 5.0f, 1f, 0f, 0f)
                drawMesh(driverMeshes.thighMesh, 1f, 1f, 1f, 1f)

                Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
                Matrix.rotateM(modelMatrix, 0, if (side > 0) 6f else 0f, 1f, 0f, 0f)
                drawMesh(driverMeshes.shinBootMesh, 1f, 1f, 1f, 1f)
            }
        }
    }

    private fun bindWhite() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.whiteTex)
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
