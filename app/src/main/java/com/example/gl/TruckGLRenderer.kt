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
import kotlin.math.cos
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

    private lateinit var unitCube: MeshBuffer
    private lateinit var unitWheel: MeshBuffer
    private lateinit var terrainMesh: MeshBuffer
    private lateinit var roadsMesh: MeshBuffer
    private lateinit var staticEnvMesh: MeshBuffer
    private lateinit var nightLightsMesh: MeshBuffer
    private lateinit var cloudsMesh: MeshBuffer
    private lateinit var screenQuad: ScreenQuadBuffer

    // Off-screen FBO for rear-view side mirrors in Interior Camera
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

        unitCube = WorldMeshFactory.buildUnitCube()
        unitWheel = WorldMeshFactory.buildUnitWheel()
        terrainMesh = WorldMeshFactory.buildTerrainMesh()
        roadsMesh = WorldMeshFactory.buildRoadsAndMarkingsMesh()
        staticEnvMesh = WorldMeshFactory.buildStaticEnvironmentMesh()
        nightLightsMesh = WorldMeshFactory.buildNightWindowsAndLightsMesh()
        cloudsMesh = WorldMeshFactory.buildCloudsMesh()
        screenQuad = WorldMeshFactory.buildScreenQuad()

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
        audioEngine.updateState(
            currentRpm = telemetry.rpm,
            currentThrottle = inputs.throttle,
            isHorn = inputs.hornActive,
            isReverse = telemetry.gear == -1,
            blinkers = telemetry.blinkerState,
            hasFuel = !telemetry.engineStalled
        )

        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTelemetryMs > 45L) {
            lastTelemetryMs = nowMs
            onTelemetry(telemetry)
        }

        // Update rain drops
        if (physics.weather == WeatherType.RAIN) {
            updateRain(dt)
        }

        // If interior cab camera is active, render rear-facing mirror view into FBO first
        if (physics.cameraMode == CameraMode.INTERIOR_CAB) {
            renderMirrorFbo()
        }

        // Main viewport render
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val quality = GraphicsQuality.fromRaw(currentProfile.graphicsQuality)
        val fov = if (physics.cameraMode == CameraMode.INTERIOR_CAB) 64f else 56f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.25f, quality.drawDistance)

        val camPos = setupMainCameraView()
        renderScene(camPos, quality, includePlayerExterior = physics.cameraMode != CameraMode.INTERIOR_CAB)

        // Render 3D Cab Interior + Animated Wheel + Wipers + Live Side Mirrors
        if (physics.cameraMode == CameraMode.INTERIOR_CAB) {
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

        when (physics.cameraMode) {
            CameraMode.EXTERIOR_CHASE -> {
                val dist = if (physics.hasTrailer) 18.5f else 13.5f
                val height = if (physics.hasTrailer) 6.4f else 5.2f
                camX = physics.x - fwdX * dist
                camY = baseH + height
                camZ = physics.z - fwdZ * dist
                lookX = physics.x + fwdX * 8f
                lookY = baseH + 2.2f
                lookZ = physics.z + fwdZ * 8f
            }
            CameraMode.INTERIOR_CAB -> {
                // Driver left-seat eye POV
                camX = physics.x + fwdX * 1.35f - rightX * 0.58f
                camY = baseH + 2.55f
                camZ = physics.z + fwdZ * 1.35f - rightZ * 0.58f
                lookX = camX + fwdX * 35f
                lookY = camY - 0.6f + physics.cabPitchRad * 10f
                lookZ = camZ + fwdZ * 35f
            }
            CameraMode.SIDE_VIEW -> {
                camX = physics.x + rightX * 11.5f - fwdX * 2.5f
                camY = baseH + 3.6f
                camZ = physics.z + rightZ * 11.5f - fwdZ * 2.5f
                lookX = physics.x - fwdX * 2.0f
                lookY = baseH + 1.8f
                lookZ = physics.z - fwdZ * 2.0f
            }
            CameraMode.FRONT_BUMPER -> {
                camX = physics.x + fwdX * 3.8f
                camY = baseH + 1.35f
                camZ = physics.z + fwdZ * 3.8f
                lookX = camX + fwdX * 36f
                lookY = camY - 0.35f
                lookZ = camZ + fwdZ * 36f
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
        GLES30.glUniform3f(worldShader.uLightDirLoc, env.lightDirX, env.lightDirY, env.lightDirZ)
        GLES30.glUniform3f(worldShader.uLightColorLoc, env.lightR, env.lightG, env.lightB)
        GLES30.glUniform3f(worldShader.uAmbientColorLoc, env.ambR, env.ambG, env.ambB)
        GLES30.glUniform3f(worldShader.uCameraPosLoc, camPos[0], camPos[1], camPos[2])
        GLES30.glUniform3f(worldShader.uFogColorLoc, env.skyR, env.skyG, env.skyB)
        val fogDensity = when (physics.weather) {
            WeatherType.RAIN -> 0.0048f
            WeatherType.CLOUDY -> 0.0028f
            WeatherType.CLEAR -> 0.0018f
        }
        GLES30.glUniform1f(worldShader.uFogDensityLoc, fogDensity)
        GLES30.glUniform1f(worldShader.uWetnessLoc, if (physics.weather == WeatherType.RAIN) 1.0f else 0.0f)

        // Headlight spotlight uniform
        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val hlIntensity = when (physics.headlightMode) {
            HeadlightMode.OFF -> 0f
            HeadlightMode.LOW -> if (env.isNight) 1.0f else 0.3f
            HeadlightMode.HIGH -> if (env.isNight) 1.75f else 0.5f
        }
        GLES30.glUniform3f(worldShader.uHeadlightPosLoc, physics.x + fwdX * 2.8f, physics.y + 1.2f, physics.z + fwdZ * 2.8f)
        GLES30.glUniform3f(worldShader.uHeadlightDirLoc, fwdX, -0.10f, fwdZ)
        GLES30.glUniform1f(worldShader.uHeadlightIntensityLoc, hlIntensity)

        // 1. Static world terrain, roads, buildings, trees
        setIdentityModel()
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        GLES30.glUniform4f(worldShader.uColorTintLoc, 1f, 1f, 1f, 1f)
        terrainMesh.draw()
        roadsMesh.draw()
        staticEnvMesh.draw()

        // 2. Sun / Moon disc & Clouds
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        val sunDist = 360f
        drawBox(
            camPos[0] + env.lightDirX * sunDist,
            camPos[1] + env.lightDirY * sunDist,
            camPos[2] + env.lightDirZ * sunDist,
            0f, 18f, 18f, 18f,
            if (env.isNight) 0.85f else 1.0f,
            if (env.isNight) 0.90f else 0.92f,
            if (env.isNight) 1.0f else 0.70f,
            1f
        )
        if (quality != GraphicsQuality.LOW) {
            setIdentityModel()
            val cloudShade = if (env.isNight) 0.25f else if (physics.weather == WeatherType.RAIN) 0.55f else 1.0f
            GLES30.glUniform4f(worldShader.uColorTintLoc, cloudShade, cloudShade, cloudShade + 0.03f, 0.92f)
            cloudsMesh.draw()
        }

        // 3. Nighttime windows & streetlights
        if (env.isNight) {
            setIdentityModel()
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            GLES30.glUniform4f(worldShader.uColorTintLoc, 1f, 1f, 1f, 1f)
            nightLightsMesh.draw()
        }

        // 4. Traffic lights
        renderTrafficLights()

        // 5. Mission Objective 3D Beacon
        renderMissionBeacon()

        // 6. AI Traffic Vehicles
        renderTrafficVehicles(env.isNight)

        // 7. Player Truck & Articulated Trailer
        if (includePlayerExterior) {
            renderPlayerTruck(env.isNight)
        }
        if (physics.hasTrailer) {
            renderPlayerTrailer(env.isNight)
        }

        // 8. Rain particles
        if (physics.weather == WeatherType.RAIN) {
            renderRainParticles(camPos, quality.rainCount)
        }
    }

    private fun renderTrafficLights() {
        val phase = TruckWorld.lightPhaseAt(physics.simTimeSeconds)
        for (tl in TruckWorld.trafficLights) {
            val ty = TruckWorld.terrainHeightAt(tl.x, tl.z)
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
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
        val pulse = 0.7f + 0.3f * sin(physics.simTimeSeconds * 4f)
        val (r, g, b) = if (!missionLoaded) Triple(1.0f, 0.65f, 0.08f) else Triple(0.10f, 0.92f, 0.48f)

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        drawBox(hub.x, hy + 18f, hub.z, 0f, 2.2f * pulse, 36f, 2.2f * pulse, r, g, b, 0.35f)
        drawBox(hub.x, hy + 0.2f, hub.z, 0f, 14f, 0.12f, 14f, r, g, b, 0.28f)
    }

    private fun renderTrafficVehicles(isNight: Boolean) {
        for (v in trafficManager.vehicles) {
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
            // Contact shadow
            drawBox(v.x, v.y + 0.08f, v.z, v.yawRad, v.width * 1.05f, 0.04f, v.length * 1.05f, 0.05f, 0.05f, 0.06f, 0.45f)

            when (v.type) {
                TrafficVehicleType.CAR -> {
                    drawBox(v.x, v.y + 0.72f, v.z, v.yawRad, v.width, 0.75f, v.length, v.colorR, v.colorG, v.colorB)
                    drawBox(v.x - sin(v.yawRad) * 0.2f, v.y + 1.30f, v.z - cos(v.yawRad) * 0.2f, v.yawRad, v.width * 0.85f, 0.55f, v.length * 0.52f, v.colorR * 0.85f, v.colorG * 0.85f, v.colorB * 0.85f)
                }
                TrafficVehicleType.BUS -> {
                    drawBox(v.x, v.y + 1.65f, v.z, v.yawRad, v.width, 2.5f, v.length, v.colorR, v.colorG, v.colorB)
                    drawBox(v.x, v.y + 2.0f, v.z, v.yawRad, v.width + 0.06f, 0.85f, v.length * 0.85f, 0.14f, 0.22f, 0.30f)
                }
                TrafficVehicleType.TRUCK -> {
                    drawBox(v.x + sin(v.yawRad) * 2.4f, v.y + 1.45f, v.z + cos(v.yawRad) * 2.4f, v.yawRad, v.width, 2.2f, 2.6f, 0.82f, 0.22f, 0.16f)
                    drawBox(v.x - sin(v.yawRad) * 1.3f, v.y + 1.70f, v.z - cos(v.yawRad) * 1.3f, v.yawRad, v.width, 2.6f, 5.2f, v.colorR, v.colorG, v.colorB)
                }
            }

            // Headlights & Brake lights
            GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
            val fwdX = sin(v.yawRad)
            val fwdZ = cos(v.yawRad)
            val hlAlpha = if (isNight) 1f else 0.6f
            drawBox(v.x + fwdX * (v.length * 0.5f), v.y + 0.72f, v.z + fwdZ * (v.length * 0.5f), v.yawRad, v.width * 0.8f, 0.22f, 0.1f, 1.0f, 0.96f, 0.82f, hlAlpha)
            val brBrightness = if (v.braking) 1.0f else 0.45f
            drawBox(v.x - fwdX * (v.length * 0.5f), v.y + 0.75f, v.z - fwdZ * (v.length * 0.5f), v.yawRad, v.width * 0.8f, 0.22f, 0.1f, brBrightness, 0.08f, 0.08f)
        }
    }

    private fun renderPlayerTruck(isNight: Boolean) {
        val model = TruckCatalog.byId(currentProfile.selectedTruckId)
        val (paintR, paintG, paintB) = parseHexRgb(currentProfile.truckPaintHex)
        val px = physics.x
        val pz = physics.z
        val py = physics.y + physics.bounceOffset
        val yaw = physics.yawRad
        val fwdX = sin(yaw)
        val fwdZ = cos(yaw)
        val rightX = -fwdZ
        val rightZ = fwdX

        // Soft grounded contact shadow
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        drawBox(px, physics.y + 0.09f, pz, yaw, 2.9f, 0.04f, 7.6f, 0.03f, 0.03f, 0.04f, 0.52f)

        // Chassis frame
        drawBox(px, py + 0.78f, pz, yaw, 2.25f, 0.42f, 6.8f, 0.14f, 0.16f, 0.19f)
        // Fuel tanks on sides
        drawBox(px + rightX * 1.12f - fwdX * 0.4f, py + 0.72f, pz + rightZ * 1.12f - fwdZ * 0.4f, yaw, 0.45f, 0.52f, 1.9f, 0.76f, 0.78f, 0.82f)
        drawBox(px - rightX * 1.12f - fwdX * 0.4f, py + 0.72f, pz - rightZ * 1.12f - fwdZ * 0.4f, yaw, 0.45f, 0.52f, 1.9f, 0.76f, 0.78f, 0.82f)

        // Cab with suspension pitch & roll
        val pitchDeg = Math.toDegrees(physics.cabPitchRad.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(physics.cabRollRad.toDouble()).toFloat()
        when (model.style) {
            TruckStyle.AMERICAN_LONGNOSE -> {
                // Sleeper & cab
                drawTiltedBox(px + fwdX * 0.6f, py + 2.15f, pz + fwdZ * 0.6f, yaw, pitchDeg, rollDeg, 2.48f, 2.35f, 2.9f, paintR, paintG, paintB)
                // Long engine hood
                drawTiltedBox(px + fwdX * 2.55f, py + 1.62f, pz + fwdZ * 2.55f, yaw, pitchDeg, rollDeg, 2.15f, 1.25f, 1.8f, paintR, paintG, paintB)
                // Chrome grille
                drawTiltedBox(px + fwdX * 3.48f, py + 1.55f, pz + fwdZ * 3.48f, yaw, pitchDeg, rollDeg, 1.45f, 1.08f, 0.12f, 0.84f, 0.86f, 0.90f)
                // Twin chrome exhaust stacks
                drawTiltedBox(px + rightX * 1.26f - fwdX * 0.5f, py + 2.6f, pz + rightZ * 1.26f - fwdZ * 0.5f, yaw, pitchDeg, rollDeg, 0.18f, 2.8f, 0.18f, 0.82f, 0.84f, 0.88f)
                drawTiltedBox(px - rightX * 1.26f - fwdX * 0.5f, py + 2.6f, pz - rightZ * 1.26f - fwdZ * 0.5f, yaw, pitchDeg, rollDeg, 0.18f, 2.8f, 0.18f, 0.82f, 0.84f, 0.88f)
            }
            TruckStyle.EURO_CABOVER -> {
                drawTiltedBox(px + fwdX * 1.6f, py + 2.28f, pz + fwdZ * 1.6f, yaw, pitchDeg, rollDeg, 2.52f, 2.65f, 2.65f, paintR, paintG, paintB)
                // Aero roof deflector
                drawTiltedBox(px + fwdX * 1.0f, py + 3.72f, pz + fwdZ * 1.0f, yaw, pitchDeg, rollDeg, 2.35f, 0.45f, 1.9f, paintR * 0.9f, paintG * 0.9f, paintB * 0.9f)
                // Dark front grille
                drawTiltedBox(px + fwdX * 2.95f, py + 1.55f, pz + fwdZ * 2.95f, yaw, pitchDeg, rollDeg, 1.9f, 1.0f, 0.1f, 0.16f, 0.18f, 0.22f)
            }
            TruckStyle.HEAVY_TWIN_EXHAUST -> {
                drawTiltedBox(px + fwdX * 0.9f, py + 2.25f, pz + fwdZ * 0.9f, yaw, pitchDeg, rollDeg, 2.54f, 2.5f, 3.0f, paintR, paintG, paintB)
                drawTiltedBox(px + fwdX * 2.65f, py + 1.70f, pz + fwdZ * 2.65f, yaw, pitchDeg, rollDeg, 2.25f, 1.38f, 1.55f, paintR, paintG, paintB)
                drawTiltedBox(px + fwdX * 3.45f, py + 1.58f, pz + fwdZ * 3.45f, yaw, pitchDeg, rollDeg, 1.7f, 1.1f, 0.14f, 0.78f, 0.80f, 0.85f)
            }
            TruckStyle.ARCTIC_EXPEDITION -> {
                drawTiltedBox(px + fwdX * 1.1f, py + 2.35f, pz + fwdZ * 1.1f, yaw, pitchDeg, rollDeg, 2.60f, 2.65f, 3.1f, paintR, paintG, paintB)
                drawTiltedBox(px + fwdX * 2.7f, py + 1.75f, pz + fwdZ * 2.7f, yaw, pitchDeg, rollDeg, 2.28f, 1.42f, 1.6f, paintR, paintG, paintB)
                // Roof expedition rack & LED bar
                drawTiltedBox(px + fwdX * 1.5f, py + 3.82f, pz + fwdZ * 1.5f, yaw, pitchDeg, rollDeg, 2.2f, 0.22f, 1.4f, 0.15f, 0.16f, 0.18f)
            }
        }

        // Windshield
        val winFwd = if (model.style == TruckStyle.EURO_CABOVER) 2.94f else 1.85f
        drawTiltedBox(px + fwdX * winFwd, py + 2.58f, pz + fwdZ * winFwd, yaw, pitchDeg, rollDeg, 2.22f, 0.92f, 0.12f, 0.12f, 0.20f, 0.28f, 0.85f)

        // Wheels (Front steerable + 2 Rear drive axles)
        val spinDeg = Math.toDegrees(physics.wheelSpinAngle.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(physics.frontSteerRad.toDouble()).toFloat()
        val axleOffsets = listOf(2.45f, -1.25f, -2.55f)
        axleOffsets.forEachIndexed { idx, fwdOff ->
            val st = if (idx == 0) steerDeg else 0f
            drawWheel(px + fwdX * fwdOff + rightX * 1.18f, physics.y + 0.52f, pz + fwdZ * fwdOff + rightZ * 1.18f, yaw, st, spinDeg)
            drawWheel(px + fwdX * fwdOff - rightX * 1.18f, physics.y + 0.52f, pz + fwdZ * fwdOff - rightZ * 1.18f, yaw, st, spinDeg)
        }

        // Emissive lights: Headlights, Brake lights, Blinkers
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        val hlOn = physics.headlightMode != HeadlightMode.OFF
        val hlBrightness = when (physics.headlightMode) {
            HeadlightMode.OFF -> 0.25f
            HeadlightMode.LOW -> 0.95f
            HeadlightMode.HIGH -> 1.0f
        }
        val noseZ = if (model.style == TruckStyle.EURO_CABOVER) 2.96f else 3.48f
        drawBox(px + fwdX * noseZ + rightX * 0.88f, py + 0.98f, pz + fwdZ * noseZ + rightZ * 0.88f, yaw, 0.36f, 0.22f, 0.12f, hlBrightness, hlBrightness * 0.95f, if (hlOn) 0.78f else 0.25f)
        drawBox(px + fwdX * noseZ - rightX * 0.88f, py + 0.98f, pz + fwdZ * noseZ - rightZ * 0.88f, yaw, 0.36f, 0.22f, 0.12f, hlBrightness, hlBrightness * 0.95f, if (hlOn) 0.78f else 0.25f)

        // Tail / Brake / Reverse lights
        val brBright = if (currentInputs.brake > 0.08f) 1.0f else 0.42f
        drawBox(px - fwdX * 3.42f + rightX * 0.85f, py + 0.78f, pz - fwdZ * 3.42f + rightZ * 0.85f, yaw, 0.38f, 0.20f, 0.12f, brBright, 0.06f, 0.06f)
        drawBox(px - fwdX * 3.42f - rightX * 0.85f, py + 0.78f, pz - fwdZ * 3.42f - rightZ * 0.85f, yaw, 0.38f, 0.20f, 0.12f, brBright, 0.06f, 0.06f)
        if (physics.gear == -1) {
            drawBox(px - fwdX * 3.42f, py + 0.78f, pz - fwdZ * 3.42f, yaw, 0.34f, 0.18f, 0.12f, 1f, 1f, 1f)
        }

        // Turn signals (Intermitentes)
        val blinkOn = ((physics.simTimeSeconds * 2.6f).toInt() % 2) == 0
        if (blinkOn && physics.blinkerState != BlinkerState.NONE) {
            if (physics.blinkerState == BlinkerState.LEFT || physics.blinkerState == BlinkerState.HAZARD) {
                drawBox(px + fwdX * noseZ - rightX * 1.16f, py + 0.98f, pz + fwdZ * noseZ - rightZ * 1.16f, yaw, 0.22f, 0.22f, 0.14f, 1.0f, 0.62f, 0.05f)
                drawBox(px - fwdX * 3.42f - rightX * 1.15f, py + 0.78f, pz - fwdZ * 3.42f - rightZ * 1.15f, yaw, 0.22f, 0.20f, 0.14f, 1.0f, 0.62f, 0.05f)
            }
            if (physics.blinkerState == BlinkerState.RIGHT || physics.blinkerState == BlinkerState.HAZARD) {
                drawBox(px + fwdX * noseZ + rightX * 1.16f, py + 0.98f, pz + fwdZ * noseZ + rightZ * 1.16f, yaw, 0.22f, 0.22f, 0.14f, 1.0f, 0.62f, 0.05f)
                drawBox(px - fwdX * 3.42f + rightX * 1.15f, py + 0.78f, pz - fwdZ * 3.42f + rightZ * 1.15f, yaw, 0.22f, 0.20f, 0.14f, 1.0f, 0.62f, 0.05f)
            }
        }
    }

    private fun renderPlayerTrailer(isNight: Boolean) {
        val tx = physics.trailerX
        val tz = physics.trailerZ
        val ty = TruckWorld.terrainHeightAt(tx, tz)
        val yaw = physics.trailerYawRad
        val fwdX = sin(yaw)
        val fwdZ = cos(yaw)
        val rightX = -fwdZ
        val rightZ = fwdX
        val (tr, tg, tb) = parseHexRgb(physics.trailerColorHex)

        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        // Shadow
        drawBox(tx, ty + 0.09f, tz, yaw, 2.8f, 0.04f, 11.4f, 0.03f, 0.03f, 0.04f, 0.50f)
        // Trailer chassis
        drawBox(tx, ty + 0.95f, tz, yaw, 2.3f, 0.35f, 11.0f, 0.16f, 0.18f, 0.20f)
        // Cargo container box
        drawBox(tx, ty + 2.45f, tz, yaw, 2.55f, 2.75f, 11.2f, tr, tg, tb)
        // Racing accent stripe
        drawBox(tx, ty + 2.35f, tz, yaw, 2.58f, 0.30f, 11.22f, 0.95f, 0.45f, 0.05f)

        // Tandem rear wheels
        val spinDeg = Math.toDegrees(physics.wheelSpinAngle.toDouble()).toFloat()
        for (off in listOf(-3.4f, -4.6f)) {
            drawWheel(tx + fwdX * off + rightX * 1.18f, ty + 0.52f, tz + fwdZ * off + rightZ * 1.18f, yaw, 0f, spinDeg)
            drawWheel(tx + fwdX * off - rightX * 1.18f, ty + 0.52f, tz + fwdZ * off - rightZ * 1.18f, yaw, 0f, spinDeg)
        }

        // Rear trailer brake/marker lights
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        val brBright = if (currentInputs.brake > 0.08f) 1.0f else if (isNight) 0.55f else 0.35f
        drawBox(tx - fwdX * 5.62f + rightX * 0.95f, ty + 1.15f, tz - fwdZ * 5.62f + rightZ * 0.95f, yaw, 0.42f, 0.22f, 0.10f, brBright, 0.06f, 0.06f)
        drawBox(tx - fwdX * 5.62f - rightX * 0.95f, ty + 1.15f, tz - fwdZ * 5.62f - rightZ * 0.95f, yaw, 0.42f, 0.22f, 0.10f, brBright, 0.06f, 0.06f)
    }

    private fun renderInteriorCockpit(camPos: FloatArray, inputs: DrivingInputs) {
        // Draw cab interior components relative to driver camera
        val fwdX = sin(physics.yawRad)
        val fwdZ = cos(physics.yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        val yaw = physics.yawRad
        val cx = camPos[0]
        val cy = camPos[1]
        val cz = camPos[2]

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
        worldShader.use()
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)

        // Dashboard console ahead of driver
        drawBox(cx + fwdX * 0.85f + rightX * 0.25f, cy - 0.55f, cz + fwdZ * 0.85f + rightZ * 0.25f, yaw, 1.9f, 0.45f, 0.42f, 0.12f, 0.14f, 0.18f)
        // Left & Right A-Pillars + Sun visor
        drawBox(cx + fwdX * 0.92f - rightX * 0.55f, cy + 0.05f, cz + fwdZ * 0.92f - rightZ * 0.55f, yaw, 0.10f, 1.2f, 0.10f, 0.10f, 0.12f, 0.15f)
        drawBox(cx + fwdX * 0.92f + rightX * 1.12f, cy + 0.05f, cz + fwdZ * 0.92f + rightZ * 1.12f, yaw, 0.10f, 1.2f, 0.10f, 0.10f, 0.12f, 0.15f)
        drawBox(cx + fwdX * 0.90f + rightX * 0.28f, cy + 0.58f, cz + fwdZ * 0.90f + rightZ * 0.28f, yaw, 1.9f, 0.16f, 0.20f, 0.09f, 0.10f, 0.13f)

        // Illuminated digital/analog gauge cluster
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        drawBox(cx + fwdX * 0.65f, cy - 0.30f, cz + fwdZ * 0.65f, yaw, 0.48f, 0.16f, 0.04f, 0.08f, 0.22f, 0.34f)
        // Speed needle bar
        val speedFrac = (abs(physics.speedMs) * 3.6f / 130f).coerceIn(0.05f, 1f)
        drawBox(cx + fwdX * 0.63f - rightX * 0.10f, cy - 0.30f, cz + fwdZ * 0.63f - rightZ * 0.10f, yaw, 0.18f * speedFrac, 0.04f, 0.02f, 1.0f, 0.45f, 0.05f)

        // Rotating 3D Steering Wheel
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 0f)
        val steerRollDeg = -inputs.steering * 155f
        val wheelX = cx + fwdX * 0.52f
        val wheelY = cy - 0.32f
        val wheelZ = cz + fwdZ * 0.52f
        // Wheel hub & spokes
        drawTiltedBox(wheelX, wheelY, wheelZ, yaw, -28f, steerRollDeg, 0.38f, 0.05f, 0.05f, 0.22f, 0.24f, 0.28f)
        drawTiltedBox(wheelX, wheelY, wheelZ, yaw, -28f, steerRollDeg, 0.05f, 0.38f, 0.05f, 0.22f, 0.24f, 0.28f)
        drawTiltedBox(wheelX, wheelY, wheelZ, yaw, -28f, steerRollDeg, 0.14f, 0.14f, 0.07f, 0.95f, 0.42f, 0.05f)

        // Animated Windshield Wipers
        if (physics.wipersOn) {
            val wiperAngle = sin(physics.wiperPhase) * 42f
            drawTiltedBox(cx + fwdX * 0.96f - rightX * 0.15f, cy - 0.18f, cz + fwdZ * 0.96f - rightZ * 0.15f, yaw, 0f, wiperAngle, 0.04f, 0.65f, 0.03f, 0.08f, 0.08f, 0.10f)
            drawTiltedBox(cx + fwdX * 0.96f + rightX * 0.55f, cy - 0.18f, cz + fwdZ * 0.96f + rightZ * 0.55f, yaw, 0f, wiperAngle, 0.04f, 0.65f, 0.03f, 0.08f, 0.08f, 0.10f)
        }

        // Live Rear-View Left & Right Side Mirrors rendered from FBO texture!
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        mirrorShader.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mirrorTex)
        GLES30.glUniform1i(mirrorShader.uTextureLoc, 0)

        // Left Mirror Quad (NDC: x=-0.95, y=0.05, w=0.20, h=0.34)
        GLES30.glUniform4f(mirrorShader.uRectLoc, -0.96f, 0.04f, 0.20f, 0.34f)
        screenQuad.draw()

        // Right Mirror Quad (NDC: x=0.76, y=0.05, w=0.20, h=0.34)
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
        GLES30.glUniform1f(worldShader.uEmissiveLoc, 1f)
        val n = count.coerceAtMost(650)
        for (i in 0 until n) {
            val rx = camPos[0] + rainDrops[i * 3]
            val ry = (camPos[1] - 8f) + rainDrops[i * 3 + 1]
            val rz = camPos[2] + rainDrops[i * 3 + 2]
            drawBox(rx, ry, rz, 0f, 0.04f, 0.85f, 0.04f, 0.72f, 0.84f, 0.98f, 0.45f)
        }
    }

    private fun drawWheel(x: Float, y: Float, z: Float, yawRad: Float, steerDeg: Float, spinDeg: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat() + steerDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, spinDeg, 1f, 0f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(worldShader.uColorTintLoc, 1f, 1f, 1f, 1f)
        unitWheel.draw()
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
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(worldShader.uColorTintLoc, r, g, b, a)
        unitCube.draw()
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
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(worldShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(worldShader.uColorTintLoc, r, g, b, a)
        unitCube.draw()
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
        // Sun arc across 6:00 -> 19:30
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
            !isDay -> Triple(0.22f, 0.28f, 0.45f)
            isTwilight -> Triple(1.0f * cloudDim, 0.68f * cloudDim, 0.42f * cloudDim)
            else -> Triple(0.98f * cloudDim, 0.95f * cloudDim, 0.88f * cloudDim)
        }

        val (ar, ag, ab) = when {
            !isDay -> Triple(0.14f, 0.17f, 0.26f)
            isTwilight -> Triple(0.38f, 0.30f, 0.32f)
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
