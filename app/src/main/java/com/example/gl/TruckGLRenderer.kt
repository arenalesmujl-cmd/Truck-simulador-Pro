package com.example.gl

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.audio.TruckAudioEngine
import com.example.model.BlinkerState
import com.example.model.CameraMode
import com.example.model.CareerProfile
import com.example.model.GraphicsQuality
import com.example.model.HeadlightMode
import com.example.model.MissionContract
import com.example.model.PlayerControlMode
import com.example.model.TruckCatalog
import com.example.model.TruckStyle
import com.example.model.WeatherType
import com.example.sim.DrivingInputs
import com.example.sim.LightPhase
import com.example.sim.TrafficManager
import com.example.sim.TrafficVehicleType
import com.example.sim.TruckPhysicsEngine
import com.example.sim.TruckTelemetry
import com.example.sim.TruckWorld
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class TruckGLRenderer(
    val physics: TruckPhysicsEngine,
    val trafficManager: TrafficManager,
    val audioEngine: TruckAudioEngine,
    private val onTelemetry: (TruckTelemetry) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile var currentInputs = DrivingInputs()
    @Volatile var currentProfile = CareerProfile()
    @Volatile var activeMission: MissionContract? = null
    @Volatile var missionLoaded: Boolean = false

    private var surfaceWidth = 1280
    private var surfaceHeight = 720
    private var lastFrameNs = System.nanoTime()
    private var lastTelemetryMs = 0L

    private lateinit var worldShader: WorldShaderProgram
    private lateinit var mirrorShader: MirrorQuadShaderProgram
    private lateinit var textures: WorldTextureSet

    private lateinit var unitCube: MeshBuffer
    private lateinit var unitWheel: MeshBuffer
    private lateinit var groundQuad: MeshBuffer
    private lateinit var billboardQuad: MeshBuffer
    private lateinit var skyDomeMesh: MeshBuffer
    private lateinit var starFieldMesh: MeshBuffer
    private lateinit var terrainMesh: MeshBuffer
    private lateinit var roadsMesh: MeshBuffer
    private lateinit var buildingsMesh: MeshBuffer
    private lateinit var staticEnvMesh: MeshBuffer
    private lateinit var nightLightsMesh: MeshBuffer
    private lateinit var cloudsMesh: MeshBuffer
    private lateinit var screenQuad: ScreenQuadBuffer

    private lateinit var truckMeshes: Map<TruckStyle, TruckMeshSet>
    private lateinit var trailerMesh: TrailerMeshSet
    private lateinit var trafficMeshes: TrafficMeshSet
    private lateinit var cockpitMeshes: CockpitMeshSet
    private lateinit var driverMeshes: DriverMeshSet

    // Off-screen FBO for live rear-view side mirrors in Interior Camera
    private var mirrorFbo = 0
    private var mirrorTex = 0
    private var mirrorDepthRb = 0
    private val mirrorWidth = 256
    private val mirrorHeight = 160

    // Pre-allocated matrices (zero per-frame GC allocations)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Rain streak positions
    private val rainDrops = FloatArray(650 * 3)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        worldShader = WorldShaderProgram()
        mirrorShader = MirrorQuadShaderProgram()
        textures = TextureFactory.createAll()

        unitCube = WorldMeshFactory.buildUnitCube()
        unitWheel = WorldMeshFactory.buildUnitWheel()
        groundQuad = WorldMeshFactory.buildGroundQuad()
        billboardQuad = WorldMeshFactory.buildBillboardQuad()
        skyDomeMesh = WorldMeshFactory.buildSkyDomeMesh()
        starFieldMesh = WorldMeshFactory.buildStarFieldMesh()
        terrainMesh = WorldMeshFactory.buildTerrainMesh()
        roadsMesh = WorldMeshFactory.buildRoadsAndMarkingsMesh()
        buildingsMesh = WorldMeshFactory.buildBuildingsMesh()
        staticEnvMesh = WorldMeshFactory.buildStaticEnvironmentMesh()
        nightLightsMesh = WorldMeshFactory.buildNightWindowsAndLightsMesh()
        cloudsMesh = WorldMeshFactory.buildCloudsMesh()
        screenQuad = WorldMeshFactory.buildScreenQuad()

        truckMeshes = TruckStyle.entries.associateWith { TruckMeshFactory.buildTruckMesh(it) }
        trailerMesh = TruckMeshFactory.buildTrailerMesh()
        trafficMeshes = TruckMeshFactory.buildTrafficMeshes()
        cockpitMeshes = TruckMeshFactory.buildCockpitMeshes()
        driverMeshes = CharacterMeshFactory.buildDriverMeshes()

        initRainDrops()
        initMirrorFbo()
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

        val inputs = currentInputs
        val telemetry = physics.update(
            dt = dt,
            inputs = inputs,
            trafficManager = trafficManager,
            activeMission = activeMission,
            missionLoaded = missionLoaded
        )

        audioEngine.setEnabled(currentProfile.soundEnabled)
        val drivingHorn = physics.controlMode == PlayerControlMode.IN_TRUCK && inputs.hornActive
        val drivingThrottle = if (physics.controlMode == PlayerControlMode.IN_TRUCK) inputs.throttle else 0f
        audioEngine.updateState(
            currentRpm = telemetry.rpm,
            currentThrottle = drivingThrottle,
            isHorn = drivingHorn,
            isReverse = telemetry.gear == -1,
            blinkers = telemetry.blinkerState,
            hasFuel = !telemetry.engineStalled
        )

        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTelemetryMs > 45L) {
            lastTelemetryMs = nowMs
            onTelemetry(telemetry)
        }

        if (physics.weather == WeatherType.RAIN) {
            updateRain(dt)
        }

        if (physics.controlMode == PlayerControlMode.IN_TRUCK && physics.cameraMode == CameraMode.INTERIOR_CAB) {
            renderMirrorFbo()
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val quality = GraphicsQuality.fromRaw(currentProfile.graphicsQuality)
        val fov = if (physics.controlMode == PlayerControlMode.IN_TRUCK && physics.cameraMode == CameraMode.INTERIOR_CAB) 64f else 55f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.20f, quality.drawDistance)

        val camPos = setupMainCameraView()
        val showExterior = physics.controlMode == PlayerControlMode.ON_FOOT || physics.cameraMode != CameraMode.INTERIOR_CAB
        renderScene(camPos, quality, includePlayerExterior = showExterior)

        if (physics.controlMode == PlayerControlMode.IN_TRUCK && physics.cameraMode == CameraMode.INTERIOR_CAB) {
            renderInteriorCockpit(camPos, inputs)
        }
    }

    private fun renderMirrorFbo() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mirrorFbo)
        GLES30.glViewport(0, 0, mirrorWidth, mirrorHeight)
        Matrix.perspectiveM(projMatrix, 0, 50f, mirrorWidth.toFloat() / mirrorHeight.toFloat(), 0.4f, 220f)

        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val camX = physics.x - fwdX * 1.1f
        val camY = physics.y + 2.6f
        val camZ = physics.z - fwdZ * 1.1f
        val lookX = camX - fwdX * 30f
        val lookY = camY - 1.2f
        val lookZ = camZ - fwdZ * 30f
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        renderScene(floatArrayOf(camX, camY, camZ), GraphicsQuality.LOW, includePlayerExterior = true)
    }

    private fun setupMainCameraView(): FloatArray {
        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        val baseH = physics.y + physics.bounceOffset

        val camX: Float
        val camY: Float
        val camZ: Float
        val lookX: Float
        val lookY: Float
        val lookZ: Float

        if (physics.controlMode == PlayerControlMode.ON_FOOT) {
            val orbitYaw = physics.yawRad + physics.cameraOrbitYaw
            val pitch = physics.cameraOrbitPitch.coerceIn(-0.08f, 0.65f)
            val dist = 3.85f
            val horizDist = dist * cos(pitch)
            val vertOffset = dist * sin(pitch) + 1.55f

            val targetX = physics.driverX
            val targetY = physics.driverY + 1.38f
            val targetZ = physics.driverZ

            camX = targetX - sin(orbitYaw) * horizDist
            camY = (physics.driverY + vertOffset).coerceAtLeast(physics.driverY + 0.55f)
            camZ = targetZ - cos(orbitYaw) * horizDist
            lookX = targetX + sin(orbitYaw) * 1.5f
            lookY = targetY
            lookZ = targetZ + cos(orbitYaw) * 1.5f
        } else {
            when (physics.cameraMode) {
                CameraMode.EXTERIOR_CHASE -> {
                    val dist = if (physics.hasTrailer) 18.8f else 13.5f
                    val orbitYaw = physics.yawRad + physics.cameraOrbitYaw
                    val pitch = physics.cameraOrbitPitch.coerceIn(0.02f, 0.65f)
                    val horizDist = dist * cos(pitch)
                    val height = dist * sin(pitch) + 3.4f

                    camX = physics.x - sin(orbitYaw) * horizDist
                    camY = baseH + height
                    camZ = physics.z - cos(orbitYaw) * horizDist
                    lookX = physics.x + fwdX * 4.5f
                    lookY = baseH + 2.1f
                    lookZ = physics.z + fwdZ * 4.5f
                }
                CameraMode.INTERIOR_CAB -> {
                    camX = physics.x + fwdX * 1.32f - rightX * 0.58f
                    camY = baseH + 2.58f
                    camZ = physics.z + fwdZ * 1.32f - rightZ * 0.58f
                    lookX = camX + fwdX * 35f
                    lookY = camY - 0.55f + physics.cabPitchRad * 10f
                    lookZ = camZ + fwdZ * 35f
                }
                CameraMode.SIDE_VIEW -> {
                    val orbitYaw = physics.yawRad + PI.toFloat() * 0.42f + physics.cameraOrbitYaw
                    val dist = 11.8f
                    camX = physics.x - sin(orbitYaw) * dist
                    camY = baseH + 3.4f
                    camZ = physics.z - cos(orbitYaw) * dist
                    lookX = physics.x - fwdX * 1.5f
                    lookY = baseH + 1.85f
                    lookZ = physics.z - fwdZ * 1.5f
                }
                CameraMode.FRONT_BUMPER -> {
                    camX = physics.x + fwdX * 3.85f
                    camY = baseH + 1.38f
                    camZ = physics.z + fwdZ * 3.85f
                    lookX = camX + fwdX * 36f
                    lookY = camY - 0.32f
                    lookZ = camZ + fwdZ * 36f
                }
            }
        }

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        return floatArrayOf(camX, camY, camZ)
    }

    private fun renderScene(camPos: FloatArray, quality: GraphicsQuality, includePlayerExterior: Boolean) {
        val env = computeLightingForTime(physics.timeOfDayHours, physics.weather)

        GLES30.glClearColor(env.skyR, env.skyG, env.skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        worldShader.use()
        bindTexture(textures.whiteTex)
        GLES30.glUniform3f(worldShader.uLightDirLoc, env.lightDirX, env.lightDirY, env.lightDirZ)
        GLES30.glUniform3f(worldShader.uLightColorLoc, env.lightR, env.lightG, env.lightB)
        GLES30.glUniform3f(worldShader.uAmbientColorLoc, env.ambR, env.ambG, env.ambB)
        GLES30.glUniform3f(worldShader.uCameraPosLoc, camPos[0], camPos[1], camPos[2])
        GLES30.glUniform3f(worldShader.uFogColorLoc, env.skyR, env.skyG, env.skyB)
        val fogDensity = when (physics.weather) {
            WeatherType.RAIN -> 0.0044f
            WeatherType.CLOUDY -> 0.0024f
            WeatherType.CLEAR -> 0.0014f
        }
        GLES30.glUniform1f(worldShader.uFogDensityLoc, fogDensity)
        GLES30.glUniform1f(worldShader.uWetnessLoc, if (physics.weather == WeatherType.RAIN) 1.0f else 0.0f)

        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val hlIntensity = when (physics.headlightMode) {
            HeadlightMode.OFF -> 0f
            HeadlightMode.LOW -> if (env.isNight) 1.15f else 0.35f
            HeadlightMode.HIGH -> if (env.isNight) 1.95f else 0.60f
        }
        GLES30.glUniform3f(worldShader.uHeadlightPosLoc, physics.x + fwdX * 3.1f, physics.y + 1.15f, physics.z + fwdZ * 3.1f)
        GLES30.glUniform3f(worldShader.uHeadlightDirLoc, fwdX, -0.09f, fwdZ)
        GLES30.glUniform1f(worldShader.uHeadlightIntensityLoc, hlIntensity)

        // 0. Atmospheric Sky Dome & Night Starfield
        GLES30.glDepthMask(false)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 2f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, camPos[0], camPos[1] - 25f, camPos[2])
        drawMeshWithModelMatrix(skyDomeMesh, 1f, 1f, 1f, 1f)

        if (env.isNight && physics.weather != WeatherType.RAIN) {
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, camPos[0], camPos[1] - 10f, camPos[2])
            drawMeshWithModelMatrix(starFieldMesh, 1f, 1f, 1f, 0.9f)
        }

        // 1. Radiant Sun / Moon Corona Billboard Disc
        val sunDist = 330f
        val sunX = camPos[0] + env.lightDirX * sunDist
        val sunY = camPos[1] + env.lightDirY * sunDist
        val sunZ = camPos[2] + env.lightDirZ * sunDist
        bindTexture(textures.glowDiscTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        drawBillboard(
            sunX, sunY, sunZ, camPos,
            size = if (env.isNight) 46f else 82f,
            r = if (env.isNight) 0.85f else 1.0f,
            g = if (env.isNight) 0.92f else 0.94f,
            b = if (env.isNight) 1.0f else 0.78f,
            a = if (physics.weather == WeatherType.RAIN) 0.35f else 0.92f
        )
        GLES30.glDepthMask(true)

        // Clouds
        bindTexture(textures.whiteTex)
        if (quality != GraphicsQuality.LOW) {
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            setIdentityModel()
            val cloudShade = if (env.isNight) 0.22f else if (physics.weather == WeatherType.RAIN) 0.52f else 1.0f
            GLES30.glUniform4f(worldShader.uColorTintLoc, cloudShade, cloudShade, cloudShade + 0.03f, 0.90f)
            cloudsMesh.draw()
        }

        // 2. Textured Terrain, Asphalt Highway, Architectural Curtain-Wall Buildings & Props
        setIdentityModel()
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        GLES30.glUniform4f(worldShader.uColorTintLoc, 1f, 1f, 1f, 1f)
        bindTexture(textures.terrainTex)
        terrainMesh.draw()

        bindTexture(textures.asphaltTex)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_ROAD)
        roadsMesh.draw()

        bindTexture(textures.buildingTex)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        buildingsMesh.draw()

        bindTexture(textures.whiteTex)
        staticEnvMesh.draw()

        // 3. Nighttime windows & streetlights
        if (env.isNight) {
            setIdentityModel()
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            GLES30.glUniform4f(worldShader.uColorTintLoc, 1f, 1f, 1f, 1f)
            nightLightsMesh.draw()
        }

        // 4. Traffic lights & Mission 3D Beacon
        renderTrafficLights()
        renderMissionBeacon()

        // 5. Realistic AI Traffic Vehicles
        renderTrafficVehicles(env.isNight)

        // 6. Realistic Player Truck & Articulated Semi-Trailer
        if (includePlayerExterior) {
            renderPlayerTruck(camPos, env.isNight)
        }
        if (physics.hasTrailer) {
            renderPlayerTrailer(env.isNight)
        }

        // 7. Realistic Articulated Human Driver (walking/standing on foot, OR seated in cab!)
        if (physics.controlMode == PlayerControlMode.ON_FOOT) {
            renderHumanDriverOnFoot()
        } else if (includePlayerExterior) {
            renderSeatedDriverInCab()
        }

        // 8. Rain particles
        if (physics.weather == WeatherType.RAIN) {
            renderRainParticles(camPos, quality.rainCount)
        }
    }

    private fun renderTrafficLights() {
        val phase = TruckWorld.lightPhaseAt(physics.simTimeSeconds)
        bindTexture(textures.whiteTex)
        for (tl in TruckWorld.trafficLights) {
            val ty = TruckWorld.terrainHeightAt(tl.x, tl.z)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
            drawBox(tl.x + 11f, ty + 3.2f, tl.z + 11f, 0f, 0.35f, 6.4f, 0.35f, 0.25f, 0.27f, 0.30f)
            drawBox(tl.x + 8.5f, ty + 6.0f, tl.z + 11f, 0f, 5.0f, 0.3f, 0.3f, 0.25f, 0.27f, 0.30f)
            drawBox(tl.x + 7.2f, ty + 5.6f, tl.z + 11f, 0f, 0.7f, 1.5f, 0.6f, 0.14f, 0.14f, 0.16f)

            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            val nsColor = when (phase) {
                LightPhase.NS_GREEN -> Triple(0.1f, 0.95f, 0.35f)
                LightPhase.NS_AMBER -> Triple(1.0f, 0.75f, 0.05f)
                else -> Triple(0.95f, 0.12f, 0.12f)
            }
            drawBox(tl.x + 7.2f, ty + 5.6f, tl.z + 11f, 0f, 0.4f, 0.4f, 0.7f, nsColor.first, nsColor.second, nsColor.third)
        }
    }

    private fun renderMissionBeacon() {
        val mission = activeMission ?: return
        val targetId = if (!missionLoaded) mission.originHubId else mission.destinationHubId
        val hub = TruckWorld.hubById(targetId) ?: return
        val hy = TruckWorld.terrainHeightAt(hub.x, hub.z)
        val pulse = 0.75f + 0.25f * sin(physics.simTimeSeconds * 4f)
        val (r, g, b) = if (!missionLoaded) Triple(1.0f, 0.65f, 0.08f) else Triple(0.10f, 0.92f, 0.48f)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        drawBox(hub.x, hy + 18f, hub.z, 0f, 2.2f * pulse, 36f, 2.2f * pulse, r, g, b, 0.32f)
        bindTexture(textures.glowDiscTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        drawGroundShadow(hub.x, hy + 0.15f, hub.z, 0f, 18f * pulse, 18f * pulse, r, g, b, 0.52f)
        GLES30.glDepthMask(true)
    }

    private fun renderTrafficVehicles(isNight: Boolean) {
        val wheelSpin = physics.simTimeSeconds * 22f
        for (v in trafficManager.vehicles) {
            val yawDeg = Math.toDegrees(v.yawRad.toDouble()).toFloat()
            val fwdX = sin(v.yawRad)
            val fwdZ = cos(v.yawRad)
            val rightX = -fwdZ
            val rightZ = fwdX

            // Smooth radial ambient shadow
            bindTexture(textures.softShadowTex)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
            GLES30.glDepthMask(false)
            drawGroundShadow(v.x, v.y + 0.08f, v.z, v.yawRad, v.width * 1.45f, v.length * 1.25f, 1f, 1f, 1f, 0.78f)
            GLES30.glDepthMask(true)

            bindTexture(textures.whiteTex)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, v.x, v.y, v.z)
            Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)

            when (v.type) {
                TrafficVehicleType.CAR -> {
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
                    drawMeshWithModelMatrix(trafficMeshes.sedanBodyMesh, v.colorR, v.colorG, v.colorB, 1f)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_GLASS)
                    drawMeshWithModelMatrix(trafficMeshes.sedanGlassMesh, 1f, 1f, 1f, 0.85f)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
                    for (ax in listOf(1.32f, -1.28f)) {
                        drawWheel(v.x + fwdX * ax + rightX * 0.86f, v.y + 0.36f, v.z + fwdZ * ax + rightZ * 0.86f, v.yawRad, 0f, wheelSpin, scale = 0.68f)
                        drawWheel(v.x + fwdX * ax - rightX * 0.86f, v.y + 0.36f, v.z + fwdZ * ax - rightZ * 0.86f, v.yawRad, 0f, wheelSpin, scale = 0.68f)
                    }
                }
                TrafficVehicleType.BUS -> {
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
                    drawMeshWithModelMatrix(trafficMeshes.busBodyMesh, v.colorR, v.colorG, v.colorB, 1f)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_GLASS)
                    drawMeshWithModelMatrix(trafficMeshes.busGlassMesh, 1f, 1f, 1f, 0.88f)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
                    for (ax in listOf(3.15f, -3.10f)) {
                        drawWheel(v.x + fwdX * ax + rightX * 1.14f, v.y + 0.48f, v.z + fwdZ * ax + rightZ * 1.14f, v.yawRad, 0f, wheelSpin * 0.8f, scale = 0.90f)
                        drawWheel(v.x + fwdX * ax - rightX * 1.14f, v.y + 0.48f, v.z + fwdZ * ax - rightZ * 1.14f, v.yawRad, 0f, wheelSpin * 0.8f, scale = 0.90f)
                    }
                }
                TrafficVehicleType.TRUCK -> {
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
                    drawMeshWithModelMatrix(trafficMeshes.cargoCabMesh, 1f, 1f, 1f, 1f)
                    bindTexture(textures.corrugatedTex)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
                    drawMeshWithModelMatrix(trafficMeshes.cargoBoxMesh, v.colorR, v.colorG, v.colorB, 1f)
                    bindTexture(textures.whiteTex)
                    GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
                    for (ax in listOf(2.45f, -1.55f, -2.75f)) {
                        drawWheel(v.x + fwdX * ax + rightX * 1.14f, v.y + 0.48f, v.z + fwdZ * ax + rightZ * 1.14f, v.yawRad, 0f, wheelSpin * 0.8f, scale = 0.90f)
                        drawWheel(v.x + fwdX * ax - rightX * 1.14f, v.y + 0.48f, v.z + fwdZ * ax - rightZ * 1.14f, v.yawRad, 0f, wheelSpin * 0.8f, scale = 0.90f)
                    }
                }
            }

            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
            val hlAlpha = if (isNight) 1f else 0.65f
            drawBox(v.x + fwdX * (v.length * 0.49f) + rightX * (v.width * 0.30f), v.y + 0.62f, v.z + fwdZ * (v.length * 0.49f) + rightZ * (v.width * 0.30f), v.yawRad, 0.28f, 0.16f, 0.08f, 1.0f, 0.96f, 0.82f, hlAlpha)
            drawBox(v.x + fwdX * (v.length * 0.49f) - rightX * (v.width * 0.30f), v.y + 0.62f, v.z + fwdZ * (v.length * 0.49f) - rightZ * (v.width * 0.30f), v.yawRad, 0.28f, 0.16f, 0.08f, 1.0f, 0.96f, 0.82f, hlAlpha)
            val brBrightness = if (v.braking) 1.0f else 0.45f
            drawBox(v.x - fwdX * (v.length * 0.49f) + rightX * (v.width * 0.32f), v.y + 0.68f, v.z - fwdZ * (v.length * 0.49f) + rightZ * (v.width * 0.32f), v.yawRad, 0.28f, 0.16f, 0.08f, brBrightness, 0.06f, 0.06f)
            drawBox(v.x - fwdX * (v.length * 0.49f) - rightX * (v.width * 0.32f), v.y + 0.68f, v.z - fwdZ * (v.length * 0.49f) - rightZ * (v.width * 0.32f), v.yawRad, 0.28f, 0.16f, 0.08f, brBrightness, 0.06f, 0.06f)
        }
    }

    private fun renderPlayerTruck(camPos: FloatArray, isNight: Boolean) {
        val model = TruckCatalog.byId(currentProfile.selectedTruckId)
        val meshSet = truckMeshes[model.style] ?: return
        val (paintR, paintG, paintB) = parseHexRgb(currentProfile.truckPaintHex)

        val px = physics.x
        val pz = physics.z
        val py = physics.y + physics.bounceOffset
        val yaw = physics.yawRad
        val yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
        val fwdX = sin(yaw)
        val fwdZ = cos(yaw)
        val rightX = -fwdZ
        val rightZ = fwdX
        val pitchDeg = Math.toDegrees(physics.cabPitchRad.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(physics.cabRollRad.toDouble()).toFloat()

        // Smooth radial contact shadow under tractor
        bindTexture(textures.softShadowTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        drawGroundShadow(px, physics.y + 0.09f, pz, yaw, 4.1f, 9.4f, 1f, 1f, 1f, 0.88f)
        GLES30.glDepthMask(true)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)

        // Chassis
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, py, pz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchDeg * 0.45f, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollDeg * 0.45f, 0f, 0f, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMeshWithModelMatrix(meshSet.chassisMesh, 1f, 1f, 1f, 1f)

        // Cab & Hood (Metallic Clearcoat Paint)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, py, pz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
        drawMeshWithModelMatrix(meshSet.cabPaintMesh, paintR, paintG, paintB, 1f)

        // Mirror Chrome
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMeshWithModelMatrix(meshSet.chromeMesh, 1f, 1f, 1f, 1f)

        // Tinted Automotive Glass
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_GLASS)
        drawMeshWithModelMatrix(meshSet.glassMesh, 1f, 1f, 1f, 1f)

        // Steerable Front & Tandem/Triple Rear Wheels
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        val spinDeg = Math.toDegrees(physics.wheelSpinAngle.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(physics.frontSteerRad.toDouble()).toFloat()
        val axleOffsets = when (model.style) {
            TruckStyle.EURO_CABOVER -> listOf(2.40f, -1.20f, -2.50f)
            TruckStyle.ARCTIC_EXPEDITION -> listOf(2.55f, 1.35f, -1.25f, -2.55f)
            else -> listOf(2.50f, -1.25f, -2.55f)
        }
        axleOffsets.forEachIndexed { idx, fwdOff ->
            val st = if (idx == 0 || (model.style == TruckStyle.ARCTIC_EXPEDITION && idx == 1)) steerDeg else 0f
            drawWheel(px + fwdX * fwdOff + rightX * 1.18f, physics.y + 0.53f, pz + fwdZ * fwdOff + rightZ * 1.18f, yaw, st, spinDeg)
            drawWheel(px + fwdX * fwdOff - rightX * 1.18f, physics.y + 0.53f, pz + fwdZ * fwdOff - rightZ * 1.18f, yaw, st, spinDeg)
        }

        // Emissive Headlights, DRLs, Roof Clearance Marker LEDs, Tail & Turn Signals
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        val hlOn = physics.headlightMode != HeadlightMode.OFF
        val hlBrightness = when (physics.headlightMode) {
            HeadlightMode.OFF -> 0.25f
            HeadlightMode.LOW -> 0.96f
            HeadlightMode.HIGH -> 1.0f
        }
        val noseZ = when (model.style) {
            TruckStyle.EURO_CABOVER -> 3.05f
            TruckStyle.AMERICAN_LONGNOSE -> 3.55f
            TruckStyle.HEAVY_TWIN_EXHAUST -> 3.50f
            TruckStyle.ARCTIC_EXPEDITION -> 3.58f
        }
        drawBox(px + fwdX * (noseZ + 0.03f) + rightX * 0.88f, py + 0.98f, pz + fwdZ * (noseZ + 0.03f) + rightZ * 0.88f, yaw, 0.18f, 0.16f, 0.05f, hlBrightness, hlBrightness * 0.95f, if (hlOn) 0.84f else 0.28f)
        drawBox(px + fwdX * (noseZ + 0.03f) - rightX * 0.88f, py + 0.98f, pz + fwdZ * (noseZ + 0.03f) - rightZ * 0.88f, yaw, 0.18f, 0.16f, 0.05f, hlBrightness, hlBrightness * 0.95f, if (hlOn) 0.84f else 0.28f)
        // Lower bumper fog lamps
        val fogB = if (hlOn) 0.92f else 0.20f
        drawBox(px + fwdX * (noseZ + 0.22f) + rightX * 0.94f, py + 0.66f, pz + fwdZ * (noseZ + 0.22f) + rightZ * 0.94f, yaw, 0.19f, 0.075f, 0.03f, fogB, fogB * 0.95f, if (hlOn) 0.82f else 0.2f)
        drawBox(px + fwdX * (noseZ + 0.22f) - rightX * 0.94f, py + 0.66f, pz + fwdZ * (noseZ + 0.22f) - rightZ * 0.94f, yaw, 0.19f, 0.075f, 0.03f, fogB, fogB * 0.95f, if (hlOn) 0.82f else 0.2f)

        if (hlOn) {
            bindTexture(textures.glowDiscTex)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
            GLES30.glDepthMask(false)
            val haloSize = if (physics.headlightMode == HeadlightMode.HIGH) 2.2f else 1.45f
            drawBillboard(px + fwdX * (noseZ + 0.14f) + rightX * 0.88f, py + 0.98f, pz + fwdZ * (noseZ + 0.14f) + rightZ * 0.88f, camPos, haloSize, 1f, 0.96f, 0.84f, 0.55f)
            drawBillboard(px + fwdX * (noseZ + 0.14f) - rightX * 0.88f, py + 0.98f, pz + fwdZ * (noseZ + 0.14f) - rightZ * 0.88f, camPos, haloSize, 1f, 0.96f, 0.84f, 0.55f)
            GLES30.glDepthMask(true)
            bindTexture(textures.whiteTex)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        }

        // Roof clearance amber LEDs
        val roofZ = if (model.style == TruckStyle.EURO_CABOVER) 2.58f else 1.65f
        val roofY = if (model.style == TruckStyle.EURO_CABOVER) 3.44f else 3.30f
        for (lx in listOf(-0.75f, -0.35f, 0f, 0.35f, 0.75f)) {
            drawBox(px + fwdX * roofZ + rightX * lx, py + roofY, pz + fwdZ * roofZ + rightZ * lx, yaw, 0.12f, 0.07f, 0.08f, 1.0f, 0.62f, 0.06f)
        }
        if (model.style == TruckStyle.ARCTIC_EXPEDITION && hlOn) {
            for (lx in listOf(-0.75f, -0.25f, 0.25f, 0.75f)) {
                drawBox(px + fwdX * 2.13f + rightX * lx, py + 3.75f, pz + fwdZ * 2.13f + rightZ * lx, yaw, 0.18f, 0.16f, 0.06f, 1f, 0.96f, 0.88f)
            }
        }

        // Tail / Brake / Reverse lights
        val brBright = if (currentInputs.brake > 0.08f && physics.controlMode == PlayerControlMode.IN_TRUCK) 1.0f else if (isNight) 0.58f else 0.40f
        drawBox(px - fwdX * 3.75f + rightX * 0.85f, py + 0.76f, pz - fwdZ * 3.75f + rightZ * 0.85f, yaw, 0.38f, 0.18f, 0.10f, brBright, 0.06f, 0.06f)
        drawBox(px - fwdX * 3.75f - rightX * 0.85f, py + 0.76f, pz - fwdZ * 3.75f - rightZ * 0.85f, yaw, 0.38f, 0.18f, 0.10f, brBright, 0.06f, 0.06f)
        if (physics.gear == -1) {
            drawBox(px - fwdX * 3.75f, py + 0.76f, pz - fwdZ * 3.75f, yaw, 0.32f, 0.16f, 0.10f, 1f, 1f, 1f)
        }

        // Turn signals (Intermitentes)
        val blinkOn = ((physics.simTimeSeconds * 2.6f).toInt() % 2) == 0
        if (blinkOn && physics.blinkerState != BlinkerState.NONE) {
            if (physics.blinkerState == BlinkerState.LEFT || physics.blinkerState == BlinkerState.HAZARD) {
                drawBox(px + fwdX * noseZ - rightX * 1.15f, py + 0.96f, pz + fwdZ * noseZ - rightZ * 1.15f, yaw, 0.22f, 0.20f, 0.14f, 1.0f, 0.62f, 0.05f)
                drawBox(px - fwdX * 3.75f - rightX * 1.15f, py + 0.76f, pz - fwdZ * 3.75f - rightZ * 1.15f, yaw, 0.22f, 0.18f, 0.14f, 1.0f, 0.62f, 0.05f)
            }
            if (physics.blinkerState == BlinkerState.RIGHT || physics.blinkerState == BlinkerState.HAZARD) {
                drawBox(px + fwdX * noseZ + rightX * 1.15f, py + 0.96f, pz + fwdZ * noseZ + rightZ * 1.15f, yaw, 0.22f, 0.20f, 0.14f, 1.0f, 0.62f, 0.05f)
                drawBox(px - fwdX * 3.75f + rightX * 1.15f, py + 0.76f, pz - fwdZ * 3.75f + rightZ * 1.15f, yaw, 0.22f, 0.18f, 0.14f, 1.0f, 0.62f, 0.05f)
            }
        }
    }

    private fun renderPlayerTrailer(isNight: Boolean) {
        val tx = physics.trailerX
        val tz = physics.trailerZ
        val ty = TruckWorld.terrainHeightAt(tx, tz)
        val yaw = physics.trailerYawRad
        val yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
        val fwdX = sin(yaw)
        val fwdZ = cos(yaw)
        val rightX = -fwdZ
        val rightZ = fwdX
        val (tr, tg, tb) = parseHexRgb(physics.trailerColorHex)

        // Smooth radial contact shadow under semi-trailer
        bindTexture(textures.softShadowTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        drawGroundShadow(tx, ty + 0.09f, tz, yaw, 3.8f, 13.4f, 1f, 1f, 1f, 0.85f)
        GLES30.glDepthMask(true)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)

        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        drawMeshWithModelMatrix(trailerMesh.chassisMesh, 1f, 1f, 1f, 1f)

        bindTexture(textures.corrugatedTex)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_METALLIC_PAINT)
        drawMeshWithModelMatrix(trailerMesh.bodyPaintMesh, tr, tg, tb, 1f)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMeshWithModelMatrix(trailerMesh.chromeMesh, 1f, 1f, 1f, 1f)

        val spinDeg = Math.toDegrees(physics.wheelSpinAngle.toDouble()).toFloat()
        for (off in listOf(-3.4f, -4.6f)) {
            drawWheel(tx + fwdX * off + rightX * 1.18f, ty + 0.53f, tz + fwdZ * off + rightZ * 1.18f, yaw, 0f, spinDeg)
            drawWheel(tx + fwdX * off - rightX * 1.18f, ty + 0.53f, tz + fwdZ * off - rightZ * 1.18f, yaw, 0f, spinDeg)
        }

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)
        val brBright = if (currentInputs.brake > 0.08f && physics.controlMode == PlayerControlMode.IN_TRUCK) 1.0f else if (isNight) 0.58f else 0.35f
        drawBox(tx - fwdX * 5.62f + rightX * 0.95f, ty + 1.05f, tz - fwdZ * 5.62f + rightZ * 0.95f, yaw, 0.42f, 0.20f, 0.08f, brBright, 0.06f, 0.06f)
        drawBox(tx - fwdX * 5.62f - rightX * 0.95f, ty + 1.05f, tz - fwdZ * 5.62f - rightZ * 0.95f, yaw, 0.42f, 0.20f, 0.08f, brBright, 0.06f, 0.06f)
        for (sz in listOf(-4.2f, -1.2f, 1.8f, 4.5f)) {
            drawBox(tx + fwdX * sz + rightX * 1.29f, ty + 1.18f, tz + fwdZ * sz + rightZ * 1.29f, yaw, 0.06f, 0.08f, 0.22f, 1.0f, 0.64f, 0.06f)
            drawBox(tx + fwdX * sz - rightX * 1.29f, ty + 1.18f, tz + fwdZ * sz - rightZ * 1.29f, yaw, 0.06f, 0.08f, 0.22f, 1.0f, 0.64f, 0.06f)
        }
    }

    /**
     * Renders the realistic articulated 3D human driver with soft wrap lighting, breathing idle, and natural stride.
     */
    private fun renderHumanDriverOnFoot() {
        val dx = physics.driverX
        val dy = physics.driverY
        val dz = physics.driverZ
        val yawDeg = Math.toDegrees(physics.driverYawRad.toDouble()).toFloat()

        val speedNorm = (physics.driverSpeedMs / 5.4f).coerceIn(0f, 1f)
        val moving = physics.driverSpeedMs > 0.15f
        val phase = physics.driverWalkPhase

        val strideSwing = if (moving) sin(phase) * (25f + 22f * speedNorm) else 3.5f
        val armSwing = if (moving) sin(phase) * (22f + 20f * speedNorm) else -4f
        val leftKneeFlex = if (moving) maxOf(0f, -cos(phase)) * (34f + 22f * speedNorm) else 3f
        val rightKneeFlex = if (moving) maxOf(0f, cos(phase)) * (34f + 22f * speedNorm) else 5f
        val leftElbowFlex = if (moving) 18f + maxOf(0f, sin(phase)) * 24f else 14f
        val rightElbowFlex = if (moving) 18f + maxOf(0f, -sin(phase)) * 24f else 16f
        val hipBob = if (moving) kotlin.math.abs(sin(phase)) * (0.045f + 0.035f * speedNorm) else sin(physics.simTimeSeconds * 2.1f) * 0.008f
        val forwardLean = if (moving) 5f + 12f * speedNorm else 1.5f
        val hipTwist = if (moving) sin(phase) * 5.5f else 2.5f

        // Smooth radial shadow beneath boots
        bindTexture(textures.softShadowTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 3f)
        GLES30.glDepthMask(false)
        drawGroundShadow(dx, dy + 0.04f, dz, physics.driverYawRad, 0.92f, 0.92f, 1f, 1f, 1f, 0.85f)
        GLES30.glDepthMask(true)

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)

        val pelvisY = dy + 0.89f + hipBob

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, dx, pelvisY, dz)
        Matrix.rotateM(modelMatrix, 0, yawDeg + hipTwist, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, forwardLean, 1f, 0f, 0f)
        drawMeshWithModelMatrix(driverMeshes.torsoMesh, 1f, 1f, 1f, 1f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, dx, pelvisY, dz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, forwardLean, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
        Matrix.rotateM(modelMatrix, 0, sin(physics.simTimeSeconds * 1.4f) * 4f, 0f, 1f, 0f)
        drawMeshWithModelMatrix(driverMeshes.headMesh, 1f, 1f, 1f, 1f)

        drawArticulatedArm(dx, pelvisY, dz, yawDeg + hipTwist, forwardLean, side = 1f, upperRotX = -armSwing, elbowFlex = leftElbowFlex)
        drawArticulatedArm(dx, pelvisY, dz, yawDeg + hipTwist, forwardLean, side = -1f, upperRotX = armSwing, elbowFlex = rightElbowFlex)

        drawArticulatedLeg(dx, pelvisY, dz, yawDeg, side = 1f, hipRotX = strideSwing, kneeFlex = leftKneeFlex)
        drawArticulatedLeg(dx, pelvisY, dz, yawDeg, side = -1f, hipRotX = -strideSwing, kneeFlex = rightKneeFlex)
    }

    private fun drawArticulatedArm(
        px: Float, pelvisY: Float, pz: Float,
        yawDeg: Float, forwardLean: Float,
        side: Float, upperRotX: Float, elbowFlex: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, pelvisY, pz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, forwardLean, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, side * 0.24f, 0.49f, 0f)
        Matrix.rotateM(modelMatrix, 0, side * 8f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, upperRotX, 1f, 0f, 0f)
        drawMeshWithModelMatrix(driverMeshes.upperArmMesh, 1f, 1f, 1f, 1f)

        Matrix.translateM(modelMatrix, 0, 0f, -0.29f, 0f)
        Matrix.rotateM(modelMatrix, 0, -elbowFlex, 1f, 0f, 0f)
        drawMeshWithModelMatrix(driverMeshes.forearmHandMesh, 1f, 1f, 1f, 1f)
    }

    private fun drawArticulatedLeg(
        px: Float, pelvisY: Float, pz: Float,
        yawDeg: Float, side: Float, hipRotX: Float, kneeFlex: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, pelvisY, pz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, side * 0.105f, 0.02f, 0f)
        Matrix.rotateM(modelMatrix, 0, hipRotX, 1f, 0f, 0f)
        drawMeshWithModelMatrix(driverMeshes.thighMesh, 1f, 1f, 1f, 1f)

        Matrix.translateM(modelMatrix, 0, 0f, -0.42f, 0f)
        Matrix.rotateM(modelMatrix, 0, kneeFlex, 1f, 0f, 0f)
        drawMeshWithModelMatrix(driverMeshes.shinBootMesh, 1f, 1f, 1f, 1f)
    }

    private fun renderSeatedDriverInCab() {
        val model = TruckCatalog.byId(currentProfile.selectedTruckId)
        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        val seatFwd = if (model.style == TruckStyle.EURO_CABOVER) 1.98f else 1.25f
        val seatX = physics.x + fwdX * seatFwd - rightX * 0.56f
        val seatY = physics.y + physics.bounceOffset + 1.82f
        val seatZ = physics.z + fwdZ * seatFwd - rightZ * 0.56f
        val yawDeg = Math.toDegrees(physics.yawRad.toDouble()).toFloat()

        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, seatX, seatY, seatZ)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        drawMeshWithModelMatrix(driverMeshes.torsoMesh, 1f, 1f, 1f, 1f)

        Matrix.translateM(modelMatrix, 0, 0f, 0.58f, 0f)
        Matrix.rotateM(modelMatrix, 0, -currentInputs.steering * 12f, 0f, 1f, 0f)
        drawMeshWithModelMatrix(driverMeshes.headMesh, 1f, 1f, 1f, 1f)

        // Seated forward arms reaching toward steering wheel
        drawArticulatedArm(seatX, seatY, seatZ, yawDeg, 0f, side = 1f, upperRotX = -42f, elbowFlex = 46f)
        drawArticulatedArm(seatX, seatY, seatZ, yawDeg, 0f, side = -1f, upperRotX = -42f, elbowFlex = 46f)
    }

    private fun renderInteriorCockpit(camPos: FloatArray, inputs: DrivingInputs) {
        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        val yaw = physics.yawRad
        val yawDeg = Math.toDegrees(yaw.toDouble()).toFloat()
        val cx = camPos[0]
        val cy = camPos[1]
        val cz = camPos[2]

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        worldShader.use()
        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_STANDARD)

        // Sculpted Dashboard, Gauge Binnacle Rings, Shifter & A-Pillars
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        drawMeshWithModelMatrix(cockpitMeshes.dashboardMesh, 1f, 1f, 1f, 1f)

        // Compact Digital Trip Computer Strip between Round Chrome Analog Gauges
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        drawBox(cx + fwdX * 0.66f, cy - 0.28f, cz + fwdZ * 0.66f, yaw, 0.075f, 0.105f, 0.02f, 0.06f, 0.16f, 0.26f)
        val speedFrac = (abs(physics.speedMs) * 3.6f / 130f).coerceIn(0f, 1f)
        val rpmFrac = (physics.rpm / 2450f).coerceIn(0f, 1f)

        // Left analog speedometer dial + orange needle + hub pin
        val speedNeedleRoll = 120f - speedFrac * 240f
        drawTiltedBox(cx + fwdX * 0.648f - rightX * 0.12f, cy - 0.28f, cz + fwdZ * 0.648f - rightZ * 0.12f, yaw, 0f, speedNeedleRoll, 0.009f, 0.056f, 0.010f, 1.0f, 0.45f, 0.05f)
        drawBox(cx + fwdX * 0.644f - rightX * 0.12f, cy - 0.28f, cz + fwdZ * 0.644f - rightZ * 0.12f, yaw, 0.022f, 0.022f, 0.012f, 0.85f, 0.88f, 0.94f)
        // Right analog tachometer dial + cyan needle + hub pin
        val rpmNeedleRoll = 120f - rpmFrac * 240f
        drawTiltedBox(cx + fwdX * 0.648f + rightX * 0.12f, cy - 0.28f, cz + fwdZ * 0.648f + rightZ * 0.12f, yaw, 0f, rpmNeedleRoll, 0.009f, 0.056f, 0.010f, 0.18f, 0.86f, 1.0f)
        drawBox(cx + fwdX * 0.644f + rightX * 0.12f, cy - 0.28f, cz + fwdZ * 0.644f + rightZ * 0.12f, yaw, 0.022f, 0.022f, 0.012f, 0.85f, 0.88f, 0.94f)

        // Center GPS Nav Screen glow (on center console to driver's right)
        drawBox(cx + fwdX * 0.69f + rightX * 0.46f, cy - 0.34f, cz + fwdZ * 0.69f + rightZ * 0.46f, yaw, 0.24f, 0.14f, 0.02f, 0.07f, 0.24f, 0.22f)
        drawBox(cx + fwdX * 0.68f + rightX * 0.46f, cy - 0.34f, cz + fwdZ * 0.68f + rightZ * 0.46f, yaw, 0.02f, 0.10f, 0.02f, 0.20f, 0.90f, 0.55f)

        // Real 3D Torus Steering Wheel + Driver Forearms/Hands gripping rim at 9-and-3!
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        val steerRollDeg = -inputs.steering * 155f
        val wheelX = cx + fwdX * 0.50f
        val wheelY = cy - 0.32f
        val wheelZ = cz + fwdZ * 0.50f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, wheelX, wheelY, wheelZ)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, -28f, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, steerRollDeg, 0f, 0f, 1f)
        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_CHROME)
        drawMeshWithModelMatrix(cockpitMeshes.steeringWheelMesh, 1f, 1f, 1f, 1f)

        GLES30.glUniform1f(worldShader.uMaterialModeLoc, WorldShaderProgram.MAT_SKIN_FABRIC)
        for (side in listOf(-1f, 1f)) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, wheelX, wheelY, wheelZ)
            Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, -28f, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, steerRollDeg, 0f, 0f, 1f)
            Matrix.translateM(modelMatrix, 0, side * 0.20f, 0.02f, -0.02f)
            Matrix.rotateM(modelMatrix, 0, -72f, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, side * -12f, 0f, 0f, 1f)
            Matrix.translateM(modelMatrix, 0, 0f, 0.28f, 0f)
            drawMeshWithModelMatrix(driverMeshes.forearmHandMesh, 1f, 1f, 1f, 1f)
        }

        // Animated Windshield Wipers
        if (physics.wipersOn) {
            val wiperAngle = sin(physics.wiperPhase) * 42f
            drawTiltedBox(cx + fwdX * 0.96f - rightX * 0.15f, cy - 0.18f, cz + fwdZ * 0.96f - rightZ * 0.15f, yaw, 0f, wiperAngle, 0.035f, 0.65f, 0.03f, 0.08f, 0.08f, 0.10f)
            drawTiltedBox(cx + fwdX * 0.96f + rightX * 0.55f, cy - 0.18f, cz + fwdZ * 0.96f + rightZ * 0.55f, yaw, 0f, wiperAngle, 0.035f, 0.65f, 0.03f, 0.08f, 0.08f, 0.10f)
        }

        // Live Rear-View Left & Right Side Mirrors rendered from FBO texture
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        mirrorShader.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mirrorTex)
        GLES30.glUniform1i(mirrorShader.uTextureLoc, 0)

        GLES30.glUniform4f(mirrorShader.uRectLoc, -0.96f, 0.04f, 0.20f, 0.34f)
        screenQuad.draw()

        GLES30.glUniform4f(mirrorShader.uRectLoc, 0.76f, 0.04f, 0.20f, 0.34f)
        screenQuad.draw()

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun initRainDrops() {
        var seed = 999
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 10000) / 10000f
        }
        for (i in 0 until 650) {
            rainDrops[i * 3] = (rand() * 2f - 1f) * 45f
            rainDrops[i * 3 + 1] = rand() * 28f
            rainDrops[i * 3 + 2] = (rand() * 2f - 1f) * 45f
        }
    }

    private fun updateRain(dt: Float) {
        for (i in 0 until 650) {
            rainDrops[i * 3 + 1] -= 28f * dt
            if (rainDrops[i * 3 + 1] < 0f) {
                rainDrops[i * 3 + 1] += 28f
            }
        }
    }

    private fun renderRainParticles(camPos: FloatArray, count: Int) {
        bindTexture(textures.whiteTex)
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        val n = count.coerceAtMost(650)
        for (i in 0 until n) {
            val rx = camPos[0] + rainDrops[i * 3]
            val ry = (camPos[1] - 8f) + rainDrops[i * 3 + 1]
            val rz = camPos[2] + rainDrops[i * 3 + 2]
            drawBox(rx, ry, rz, 0f, 0.04f, 0.85f, 0.04f, 0.72f, 0.84f, 0.98f, 0.45f)
        }
    }

    private fun drawWheel(
        x: Float, y: Float, z: Float,
        yawRad: Float, steerDeg: Float, spinDeg: Float,
        scale: Float = 1f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat() + steerDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, spinDeg, 1f, 0f, 0f)
        if (scale != 1f) {
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        }
        drawMeshWithModelMatrix(unitWheel, 1f, 1f, 1f, 1f)
    }

    private fun drawGroundShadow(
        x: Float, y: Float, z: Float,
        yawRad: Float, sx: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, 1f, sz)
        drawMeshWithModelMatrix(groundQuad, r, g, b, a)
    }

    private fun drawBillboard(
        x: Float, y: Float, z: Float,
        camPos: FloatArray,
        size: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val dx = camPos[0] - x
        val dy = camPos[1] - y
        val dz = camPos[2] - z
        val yawDeg = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
        val horiz = hypot(dx, dz).coerceAtLeast(0.001f)
        val pitchDeg = -Math.toDegrees(atan2(dy.toDouble(), horiz.toDouble())).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, size, size, 1f)
        drawMeshWithModelMatrix(billboardQuad, r, g, b, a)
    }

    private fun drawMeshWithModelMatrix(mesh: MeshBuffer, r: Float, g: Float, b: Float, a: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(worldShader.uColorTintLoc, r, g, b, a)
        mesh.draw()
    }

    private fun drawBox(
        x: Float, y: Float, z: Float,
        yawRad: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMeshWithModelMatrix(unitCube, r, g, b, a)
    }

    private fun drawTiltedBox(
        x: Float, y: Float, z: Float,
        yawRad: Float, pitchDeg: Float, rollDeg: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMeshWithModelMatrix(unitCube, r, g, b, a)
    }

    private fun bindTexture(texId: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glUniform1i(worldShader.uTextureLoc, 0)
    }

    private fun setIdentityModel() {
        Matrix.setIdentityM(modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, vpMatrix, 0)
    }

    private fun initMirrorFbo() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        val rbs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)
        GLES30.glGenRenderbuffers(1, rbs, 0)

        mirrorFbo = fbos[0]
        mirrorTex = texs[0]
        mirrorDepthRb = rbs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mirrorTex)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, mirrorWidth, mirrorHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, mirrorDepthRb)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, mirrorWidth, mirrorHeight)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mirrorFbo)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mirrorTex, 0)
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, mirrorDepthRb)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private data class EnvLighting(
        val skyR: Float, val skyG: Float, val skyB: Float,
        val lightDirX: Float, val lightDirY: Float, val lightDirZ: Float,
        val lightR: Float, val lightG: Float, val lightB: Float,
        val ambR: Float, val ambG: Float, val ambB: Float,
        val isNight: Boolean
    )

    private fun computeLightingForTime(hour: Float, weather: WeatherType): EnvLighting {
        val dayFrac = ((hour - 6f) / 13.5f).coerceIn(0f, 1f)
        val isDay = hour in 6.0f..19.5f
        val sunElev = if (isDay) sin(dayFrac * PI.toFloat()) else 0.35f
        val sunHoriz = if (isDay) cos(dayFrac * PI.toFloat()) else -0.5f

        val isTwilight = isDay && (hour < 7.5f || hour > 17.5f)
        val cloudDim = when (weather) {
            WeatherType.CLEAR -> 1.0f
            WeatherType.CLOUDY -> 0.72f
            WeatherType.RAIN -> 0.48f
        }

        val (sr, sg, sb) = when {
            !isDay -> Triple(0.04f, 0.06f, 0.13f)
            isTwilight -> Triple(0.76f * cloudDim, 0.48f * cloudDim, 0.42f * cloudDim)
            weather == WeatherType.RAIN -> Triple(0.32f, 0.38f, 0.46f)
            weather == WeatherType.CLOUDY -> Triple(0.55f, 0.64f, 0.74f)
            else -> Triple(0.46f, 0.72f, 0.96f)
        }

        val (lr, lg, lb) = when {
            !isDay -> Triple(0.32f, 0.38f, 0.56f)
            isTwilight -> Triple(1.0f * cloudDim, 0.68f * cloudDim, 0.42f * cloudDim)
            else -> Triple(0.98f * cloudDim, 0.95f * cloudDim, 0.88f * cloudDim)
        }

        val (ar, ag, ab) = when {
            !isDay -> Triple(0.22f, 0.26f, 0.36f)
            isTwilight -> Triple(0.42f, 0.34f, 0.36f)
            else -> Triple(0.42f * cloudDim + 0.1f, 0.45f * cloudDim + 0.1f, 0.50f * cloudDim + 0.1f)
        }

        return EnvLighting(
            skyR = sr, skyG = sg, skyB = sb,
            lightDirX = sunHoriz, lightDirY = sunElev.coerceAtLeast(0.18f), lightDirZ = 0.35f,
            lightR = lr, lightG = lg, lightB = lb,
            ambR = ar, ambG = ag, ambB = ab,
            isNight = !isDay || hour > 18.2f || hour < 6.6f
        )
    }

    private fun parseHexRgb(hex: String): Triple<Float, Float, Float> {
        return try {
            val c = Color.parseColor(hex)
            Triple(Color.red(c) / 255f, Color.green(c) / 255f, Color.blue(c) / 255f)
        } catch (_: Exception) {
            Triple(1f, 0.43f, 0f)
        }
    }

    private fun abs(v: Float): Float = if (v < 0f) -v else v
}
