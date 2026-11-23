package com.example.sim

import com.example.model.BlinkerState
import com.example.model.CameraMode
import com.example.model.CareerProfile
import com.example.model.GearboxMode
import com.example.model.HeadlightMode
import com.example.model.MissionContract
import com.example.model.PlayerControlMode
import com.example.model.TruckCatalog
import com.example.model.WeatherType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

data class DrivingInputs(
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val steering: Float = 0f, // -1 (left) .. +1 (right)
    val hornActive: Boolean = false,
    val moveX: Float = 0f, // On-foot joystick X (-1..+1)
    val moveY: Float = 0f, // On-foot joystick Y (-1..+1, +1 = forward)
    val sprint: Boolean = false
)

data class TruckTelemetry(
    val speedKmh: Float = 0f,
    val rpm: Float = 680f,
    val gear: Int = 1, // -1 = R, 0 = N, 1..6 = D1..D6
    val fuelPercent: Float = 100f,
    val damagePercent: Float = 0f,
    val truckX: Float = 0f,
    val truckZ: Float = 0f,
    val truckYawRad: Float = 0f,
    val timeOfDayHours: Float = 15.5f,
    val weather: WeatherType = WeatherType.CLEAR,
    val headlightMode: HeadlightMode = HeadlightMode.LOW,
    val blinkerState: BlinkerState = BlinkerState.NONE,
    val wipersOn: Boolean = false,
    val cameraMode: CameraMode = CameraMode.EXTERIOR_CHASE,
    val controlMode: PlayerControlMode = PlayerControlMode.IN_TRUCK,
    val driverX: Float = 0f,
    val driverZ: Float = 0f,
    val driverYawRad: Float = 0f,
    val driverSpeedMs: Float = 0f,
    val isNearTruckDoor: Boolean = true,
    val distanceToTruckM: Float = 0f,
    val canExitTruck: Boolean = true,
    val hasTrailer: Boolean = false,
    val nearbyZone: WorldZone? = null,
    val distanceToTargetM: Float = 0f,
    val sessionDistanceKm: Double = 0.0,
    val engineStalled: Boolean = false
)

class TruckPhysicsEngine {
    // Truck World pose
    var x: Float = -35f
    var z: Float = 0f
    var y: Float = 0f
    var yawRad: Float = PI.toFloat() / 2f // Facing East (+X) along main avenue
    var speedMs: Float = 0f

    // Player Control Mode & Realistic Human Driver State
    var controlMode: PlayerControlMode = PlayerControlMode.IN_TRUCK
    var driverX: Float = x
    var driverY: Float = y
    var driverZ: Float = z
    var driverYawRad: Float = yawRad
    var driverSpeedMs: Float = 0f
    var driverWalkPhase: Float = 0f
    var driverSprinting: Boolean = false

    // Camera Orbit (touch-drag around truck or human driver)
    var cameraOrbitYaw: Float = 0f
    var cameraOrbitPitch: Float = 0.14f

    // Articulated trailer state
    var hasTrailer: Boolean = false
    var trailerColorHex: String = "#E2E8F0"
    var trailerYawRad: Float = yawRad
    var trailerX: Float = x - sin(yawRad) * 9.5f
    var trailerZ: Float = z - cos(yawRad) * 9.5f

    // Engine & Gearbox
    var rpm: Float = 680f
    var gear: Int = 1 // -1=R, 0=N, 1..6
    var wheelSpinAngle: Float = 0f
    var frontSteerRad: Float = 0f

    // Suspension visuals
    var cabPitchRad: Float = 0f
    var cabRollRad: Float = 0f
    var bounceOffset: Float = 0f
    private var bounceVel: Float = 0f

    // Fuel & Damage
    var fuelLiters: Float = 450f
    var maxFuelLiters: Float = 450f
    var damagePercent: Float = 0f

    // Environment & Truck systems
    var timeOfDayHours: Float = 15.5f // Starts in scenic golden afternoon
    var dynamicTimeEnabled: Boolean = true
    var weather: WeatherType = WeatherType.CLEAR
    var dynamicWeatherEnabled: Boolean = true
    private var weatherTimer: Float = 0f
    var simTimeSeconds: Float = 0f

    var headlightMode: HeadlightMode = HeadlightMode.LOW
    var blinkerState: BlinkerState = BlinkerState.NONE
    var wipersOn: Boolean = false
    var wiperPhase: Float = 0f
    var cameraMode: CameraMode = CameraMode.EXTERIOR_CHASE
    var sessionDistanceKm: Double = 0.0

    private var profile: CareerProfile = CareerProfile()

    fun applyCareerProfile(newProfile: CareerProfile) {
        val oldMax = maxFuelLiters
        profile = newProfile
        val model = TruckCatalog.byId(newProfile.selectedTruckId)
        maxFuelLiters = model.baseTankLiters * (1f + (newProfile.tankLevel - 1) * 0.14f)
        if (oldMax > 0f) {
            fuelLiters = (fuelLiters / oldMax) * maxFuelLiters
        } else {
            fuelLiters = maxFuelLiters
        }
        fuelLiters = fuelLiters.coerceIn(0f, maxFuelLiters)
    }

    fun resetForNewRun(startX: Float = -45f, startZ: Float = 0f, startYaw: Float = PI.toFloat() / 2f) {
        controlMode = PlayerControlMode.IN_TRUCK
        x = startX
        z = startZ
        y = TruckWorld.terrainHeightAt(x, z)
        yawRad = startYaw
        speedMs = 0f
        gear = 1
        rpm = 680f
        driverX = x
        driverY = y
        driverZ = z
        driverYawRad = yawRad
        driverSpeedMs = 0f
        driverWalkPhase = 0f
        cameraOrbitYaw = 0f
        cameraOrbitPitch = 0.14f
        trailerYawRad = startYaw
        trailerX = x - sin(startYaw) * 9.5f
        trailerZ = z - cos(startYaw) * 9.5f
        cabPitchRad = 0f
        cabRollRad = 0f
        bounceOffset = 0f
        bounceVel = 0f
        sessionDistanceKm = 0.0
        if (fuelLiters < maxFuelLiters * 0.25f) {
            fuelLiters = maxFuelLiters
        }
    }

    fun addCameraOrbit(deltaYawRad: Float, deltaPitchRad: Float) {
        cameraOrbitYaw = normalizeAngle(cameraOrbitYaw + deltaYawRad)
        cameraOrbitPitch = (cameraOrbitPitch + deltaPitchRad).coerceIn(-0.18f, 0.72f)
    }

    fun resetCameraOrbit() {
        cameraOrbitYaw = 0f
        cameraOrbitPitch = 0.14f
    }

    fun exitTruck(): Boolean {
        if (controlMode == PlayerControlMode.ON_FOOT) return true
        if (abs(speedMs) > 3.2f) return false
        speedMs = 0f
        val fwdX = sin(yawRad)
        val fwdZ = cos(yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        // Step out just outside left driver cab door, facing forward along the truck
        driverX = x - rightX * 2.05f + fwdX * 1.15f
        driverZ = z - rightZ * 2.05f + fwdZ * 1.15f
        driverY = TruckWorld.terrainHeightAt(driverX, driverZ)
        driverYawRad = yawRad
        driverSpeedMs = 0f
        driverWalkPhase = 0f
        controlMode = PlayerControlMode.ON_FOOT
        cameraOrbitYaw = 0.22f
        cameraOrbitPitch = 0.16f
        return true
    }

    fun enterTruck(): Boolean {
        if (controlMode == PlayerControlMode.IN_TRUCK) return true
        if (!isDriverNearCabDoor()) return false
        controlMode = PlayerControlMode.IN_TRUCK
        driverSpeedMs = 0f
        cameraOrbitYaw = 0f
        cameraOrbitPitch = 0.14f
        return true
    }

    fun isDriverNearCabDoor(): Boolean {
        val fwdX = sin(yawRad)
        val fwdZ = cos(yawRad)
        val cabCenterX = x + fwdX * 1.15f
        val cabCenterZ = z + fwdZ * 1.15f
        return hypot(driverX - cabCenterX, driverZ - cabCenterZ) <= 4.8f
    }

    fun attachTrailer(colorHex: String) {
        hasTrailer = true
        trailerColorHex = colorHex
        trailerYawRad = yawRad
        trailerX = x - sin(yawRad) * 9.8f
        trailerZ = z - cos(yawRad) * 9.8f
    }

    fun detachTrailer() {
        hasTrailer = false
    }

    fun refuelFull() {
        fuelLiters = maxFuelLiters
    }

    fun repairFull() {
        damagePercent = 0f
    }

    fun emergencyTowToRoad() {
        val q = TruckWorld.queryRoad(x, z)
        x = q.nearestX
        z = q.nearestZ
        y = TruckWorld.terrainHeightAt(x, z)
        speedMs = 0f
        if (fuelLiters < maxFuelLiters * 0.2f) {
            fuelLiters = maxFuelLiters * 0.35f
        }
        if (controlMode == PlayerControlMode.ON_FOOT) {
            driverX = x - (-cos(yawRad)) * 2.05f + sin(yawRad) * 1.15f
            driverZ = z - (sin(yawRad)) * 2.05f + cos(yawRad) * 1.15f
            driverY = TruckWorld.terrainHeightAt(driverX, driverZ)
        }
    }

    fun shiftManual(delta: Int) {
        gear = (gear + delta).coerceIn(-1, 6)
    }

    fun toggleReverseAuto() {
        gear = if (gear == -1) 1 else -1
    }

    fun update(
        dt: Float,
        inputs: DrivingInputs,
        trafficManager: TrafficManager,
        activeMission: MissionContract?,
        missionLoaded: Boolean
    ): TruckTelemetry {
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        simTimeSeconds += safeDt

        // Day/night progression (~1 game hour every 25 real seconds)
        if (dynamicTimeEnabled) {
            timeOfDayHours = (timeOfDayHours + safeDt * 0.04f) % 24f
        }

        // Dynamic weather transitions every ~45 seconds
        if (dynamicWeatherEnabled) {
            weatherTimer += safeDt
            if (weatherTimer > 45f) {
                weatherTimer = 0f
                weather = when (weather) {
                    WeatherType.CLEAR -> WeatherType.CLOUDY
                    WeatherType.CLOUDY -> WeatherType.RAIN
                    WeatherType.RAIN -> WeatherType.CLEAR
                }
            }
        }

        if (wipersOn) {
            wiperPhase += safeDt * 3.8f
        }

        val truckInputs = if (controlMode == PlayerControlMode.IN_TRUCK) inputs else DrivingInputs()

        val model = TruckCatalog.byId(profile.selectedTruckId)
        val engineMult = 1f + (profile.engineLevel - 1) * 0.12f
        val transMult = 1f + (profile.transmissionLevel - 1) * 0.08f
        val suspStiffness = model.suspensionStiffness * (1f + (profile.suspensionLevel - 1) * 0.1f)
        val gearboxMode = GearboxMode.fromRaw(profile.gearboxMode)

        val maxForwardMs = (model.baseTopSpeedKmh / 3.6f) * engineMult * (if (hasTrailer) 0.90f else 1f)
        val maxReverseMs = 9.5f

        val roadQuery = TruckWorld.queryRoad(x, z)
        val isOffRoad = !roadQuery.onRoad
        val isWet = weather == WeatherType.RAIN
        val hasFuel = fuelLiters > 0.05f
        val damagePenalty = 1f - (damagePercent / 250f).coerceIn(0f, 0.35f)

        // Automatic transmission shifting
        if (gearboxMode == GearboxMode.AUTO) {
            if (gear != -1 && gear != 0) {
                val absSpeed = abs(speedMs)
                gear = when {
                    absSpeed < 4.5f -> 1
                    absSpeed < 9.5f -> 2
                    absSpeed < 15.5f -> 3
                    absSpeed < 22.0f -> 4
                    absSpeed < 28.5f -> 5
                    else -> 6
                }
            } else if (gear == -1 && speedMs > 1.5f && truckInputs.throttle > 0.2f) {
                gear = 1
            }
        }

        val prevSpeed = speedMs
        val throttleForce = if (hasFuel && gear != 0) {
            val gearFactor = when (gear) {
                -1 -> -0.85f
                1 -> 1.55f
                2 -> 1.25f
                3 -> 1.00f
                4 -> 0.80f
                5 -> 0.65f
                else -> 0.52f
            }
            val trailerMassFactor = if (hasTrailer) 0.78f else 1.0f
            truckInputs.throttle * 6.8f * model.baseTorque * engineMult * transMult * gearFactor * trailerMassFactor * damagePenalty
        } else {
            0f
        }

        val rolling = (if (isOffRoad) 2.2f else 0.42f) * speedMs
        val aeroDrag = 0.0055f * speedMs * abs(speedMs)

        val aheadX = x + sin(yawRad) * 4f
        val aheadZ = z + cos(yawRad) * 4f
        val slopeSin = ((TruckWorld.terrainHeightAt(aheadX, aheadZ) - y) / 4f).coerceIn(-0.25f, 0.25f)
        val gravityForce = -slopeSin * 9.81f * 0.55f

        val brakeApply = if (controlMode == PlayerControlMode.ON_FOOT) 0.85f else truckInputs.brake
        val brakeDecel = brakeApply * (if (isWet) 12.5f else 15.5f)
        var netAccel = throttleForce - rolling - aeroDrag + gravityForce

        if (brakeApply > 0.05f) {
            if (abs(speedMs) > 0.35f) {
                netAccel -= kotlin.math.sign(speedMs) * brakeDecel
            } else {
                speedMs = 0f
                netAccel = 0f
            }
        }

        speedMs += netAccel * safeDt
        if (gear == -1) {
            speedMs = speedMs.coerceIn(-maxReverseMs, 1.5f)
        } else if (gear == 0) {
            speedMs *= (1f - 0.5f * safeDt)
        } else {
            speedMs = speedMs.coerceIn(-2.0f, maxForwardMs)
        }

        if (truckInputs.throttle < 0.02f && abs(speedMs) < 0.15f && abs(slopeSin) < 0.03f) {
            speedMs = 0f
        }

        // Steering & Ackermann kinematics
        val targetFrontAngle = truckInputs.steering * 0.58f
        frontSteerRad += (targetFrontAngle - frontSteerRad) * (safeDt * 9f).coerceAtMost(1f)
        val wheelbase = 5.2f
        val yawRate = if (abs(speedMs) > 0.05f) {
            (speedMs / wheelbase) * tan(frontSteerRad) * (if (isWet) 0.92f else 1.0f)
        } else {
            0f
        }
        yawRad += yawRate * safeDt

        // Advance truck position
        val prevX = x
        val prevZ = z
        x += sin(yawRad) * speedMs * safeDt
        z += cos(yawRad) * speedMs * safeDt

        x = x.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)
        z = z.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)

        resolveTruckCollisions(prevX, prevZ, trafficManager)

        y = TruckWorld.terrainHeightAt(x, z)
        val movedMeters = hypot(x - prevX, z - prevZ)
        sessionDistanceKm += movedMeters / 1000.0
        wheelSpinAngle += (speedMs / 0.53f) * safeDt

        // Articulated trailer follow kinematics
        if (hasTrailer) {
            val hitchX = x - sin(yawRad) * 2.4f
            val hitchZ = z - cos(yawRad) * 2.4f
            val dxToTrailer = hitchX - trailerX
            val dzToTrailer = hitchZ - trailerZ
            val desiredTrailerYaw = atan2(dxToTrailer, dzToTrailer)
            val angleDiff = normalizeAngle(desiredTrailerYaw - trailerYawRad)
            val maxJackknife = 1.15f
            val relToCab = normalizeAngle(desiredTrailerYaw - yawRad)
            if (abs(relToCab) > maxJackknife) {
                trailerYawRad = normalizeAngle(yawRad + kotlin.math.sign(relToCab) * maxJackknife)
            } else {
                trailerYawRad = normalizeAngle(trailerYawRad + angleDiff * (safeDt * 10f).coerceAtMost(1f))
            }
            val kingpinToCenter = 7.2f
            trailerX = hitchX - sin(trailerYawRad) * kingpinToCenter
            trailerZ = hitchZ - cos(trailerYawRad) * kingpinToCenter
        }

        // Functional suspension
        val longitudinalAccel = (speedMs - prevSpeed) / safeDt
        val targetPitch = (-longitudinalAccel * 0.0085f / suspStiffness).coerceIn(-0.07f, 0.07f)
        val centripetal = speedMs * yawRate
        val targetRoll = (centripetal * 0.011f / suspStiffness).coerceIn(-0.08f, 0.08f)
        cabPitchRad += (targetPitch - cabPitchRad) * (safeDt * 8f).coerceAtMost(1f)
        cabRollRad += (targetRoll - cabRollRad) * (safeDt * 8f).coerceAtMost(1f)

        val bumpExcitation = if (isOffRoad && abs(speedMs) > 2f) {
            sin(simTimeSeconds * 22f) * 0.08f * (abs(speedMs) / 15f).coerceAtMost(1f)
        } else {
            0f
        }
        val springForce = (-bounceOffset * 45f * suspStiffness) - (bounceVel * 7.5f) + bumpExcitation * 45f
        bounceVel += springForce * safeDt
        bounceOffset = (bounceOffset + bounceVel * safeDt).coerceIn(-0.18f, 0.18f)

        // Engine RPM calculation
        val speedRatio = when (gear) {
            -1 -> (abs(speedMs) / maxReverseMs).coerceIn(0f, 1f)
            0 -> 0f
            else -> {
                val gearTop = maxForwardMs * (gear / 6f)
                val gearFloor = maxForwardMs * ((gear - 1) / 6.5f)
                ((abs(speedMs) - gearFloor) / (gearTop - gearFloor).coerceAtLeast(1f)).coerceIn(0f, 1.15f)
            }
        }
        val targetRpm = if (!hasFuel) {
            0f
        } else if (gear == 0 || controlMode == PlayerControlMode.ON_FOOT) {
            680f + truckInputs.throttle * 1350f
        } else {
            (680f + speedRatio * 1450f + truckInputs.throttle * 260f).coerceIn(650f, 2450f)
        }
        rpm += (targetRpm - rpm) * (safeDt * 7f).coerceAtMost(1f)

        if (hasFuel) {
            val burnRate = 0.015f + (rpm / 2400f) * 0.055f + truckInputs.throttle * 0.07f
            fuelLiters = (fuelLiters - burnRate * safeDt).coerceAtLeast(0f)
        }

        // Update On-Foot Realistic Human Driver Physics
        updateDriverCharacter(safeDt, inputs, trafficManager)

        // Update AI Traffic
        val avoidX = if (controlMode == PlayerControlMode.ON_FOOT) driverX else x
        val avoidZ = if (controlMode == PlayerControlMode.ON_FOOT) driverZ else z
        trafficManager.update(safeDt, simTimeSeconds, avoidX, avoidZ)

        // Mission target distance
        val refX = if (controlMode == PlayerControlMode.ON_FOOT) driverX else x
        val refZ = if (controlMode == PlayerControlMode.ON_FOOT) driverZ else z
        val targetHub = if (activeMission != null) {
            val hubId = if (!missionLoaded) activeMission.originHubId else activeMission.destinationHubId
            TruckWorld.hubById(hubId)
        } else null
        val distToTarget = if (targetHub != null) hypot(refX - targetHub.x, refZ - targetHub.z) else 0f

        val distToTruck = hypot(driverX - x, driverZ - z)

        return TruckTelemetry(
            speedKmh = abs(speedMs) * 3.6f,
            rpm = rpm,
            gear = gear,
            fuelPercent = ((fuelLiters / maxFuelLiters) * 100f).coerceIn(0f, 100f),
            damagePercent = damagePercent.coerceIn(0f, 100f),
            truckX = x,
            truckZ = z,
            truckYawRad = yawRad,
            timeOfDayHours = timeOfDayHours,
            weather = weather,
            headlightMode = headlightMode,
            blinkerState = blinkerState,
            wipersOn = wipersOn,
            cameraMode = cameraMode,
            controlMode = controlMode,
            driverX = driverX,
            driverZ = driverZ,
            driverYawRad = driverYawRad,
            driverSpeedMs = driverSpeedMs,
            isNearTruckDoor = isDriverNearCabDoor(),
            distanceToTruckM = distToTruck,
            canExitTruck = abs(speedMs) <= 3.2f,
            hasTrailer = hasTrailer,
            nearbyZone = TruckWorld.findNearbyZone(refX, refZ),
            distanceToTargetM = distToTarget,
            sessionDistanceKm = sessionDistanceKm,
            engineStalled = !hasFuel
        )
    }

    private fun updateDriverCharacter(dt: Float, inputs: DrivingInputs, trafficManager: TrafficManager) {
        if (controlMode == PlayerControlMode.IN_TRUCK) {
            driverX = x
            driverY = y
            driverZ = z
            driverYawRad = yawRad
            driverSpeedMs = 0f
            driverSprinting = false
            return
        }

        val joyMag = hypot(inputs.moveX, inputs.moveY).coerceIn(0f, 1f)
        if (joyMag > 0.08f) {
            // Camera-relative movement direction
            val camYaw = yawRad + cameraOrbitYaw
            val camFwdX = sin(camYaw)
            val camFwdZ = cos(camYaw)
            val camRightX = -camFwdZ
            val camRightZ = camFwdX

            val worldDirX = camFwdX * inputs.moveY + camRightX * inputs.moveX
            val worldDirZ = camFwdZ * inputs.moveY + camRightZ * inputs.moveX
            val normLen = hypot(worldDirX, worldDirZ).coerceAtLeast(0.001f)
            val dirX = worldDirX / normLen
            val dirZ = worldDirZ / normLen

            val targetYaw = atan2(dirX, dirZ)
            val yawDiff = normalizeAngle(targetYaw - driverYawRad)
            driverYawRad = normalizeAngle(driverYawRad + yawDiff * (dt * 11f).coerceAtMost(1f))

            driverSprinting = inputs.sprint && joyMag > 0.4f
            val maxHumanSpeed = if (driverSprinting) 5.6f else 2.4f
            val desiredSpeed = maxHumanSpeed * joyMag
            driverSpeedMs += (desiredSpeed - driverSpeedMs) * (dt * 10f).coerceAtMost(1f)

            val prevDX = driverX
            val prevDZ = driverZ
            driverX += dirX * driverSpeedMs * dt
            driverZ += dirZ * driverSpeedMs * dt

            driverX = driverX.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)
            driverZ = driverZ.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)

            resolveDriverCollisions(prevDX, prevDZ, trafficManager)
            driverWalkPhase += driverSpeedMs * dt * (if (driverSprinting) 2.5f else 3.1f)
        } else {
            driverSprinting = false
            driverSpeedMs *= (1f - 12f * dt).coerceAtLeast(0f)
            if (driverSpeedMs < 0.05f) driverSpeedMs = 0f
        }
        driverY = TruckWorld.terrainHeightAt(driverX, driverZ)
    }

    private fun resolveDriverCollisions(prevDX: Float, prevDZ: Float, trafficManager: TrafficManager) {
        val humanR = 0.45f
        // Buildings
        for (b in TruckWorld.buildings) {
            val halfW = b.width * 0.5f + humanR
            val halfD = b.depth * 0.5f + humanR
            if (abs(driverX - b.x) < halfW && abs(driverZ - b.z) < halfD) {
                driverX = prevDX
                driverZ = prevDZ
                return
            }
        }
        // Keep driver outside truck chassis footprint (oriented bounding box)
        val fwdX = sin(yawRad)
        val fwdZ = cos(yawRad)
        val rightX = -fwdZ
        val rightZ = fwdX
        val relX = driverX - x
        val relZ = driverZ - z
        val localFwd = relX * fwdX + relZ * fwdZ
        val localRight = relX * rightX + relZ * rightZ
        val halfCabW = 1.45f
        val halfCabL = 3.75f
        if (abs(localRight) < halfCabW && abs(localFwd) < halfCabL) {
            val pushOutX = halfCabW - abs(localRight)
            val pushOutZ = halfCabL - abs(localFwd)
            if (pushOutX < pushOutZ) {
                val sign = if (localRight >= 0f) 1f else -1f
                val correctedRight = sign * halfCabW
                driverX = x + fwdX * localFwd + rightX * correctedRight
                driverZ = z + fwdZ * localFwd + rightZ * correctedRight
            } else {
                val sign = if (localFwd >= 0f) 1f else -1f
                val correctedFwd = sign * halfCabL
                driverX = x + fwdX * correctedFwd + rightX * localRight
                driverZ = z + fwdZ * correctedFwd + rightZ * localRight
            }
        }
        // Traffic avoidance
        for (v in trafficManager.vehicles) {
            val minDist = humanR + (v.width * 0.55f)
            val d = hypot(driverX - v.x, driverZ - v.z)
            if (d < minDist && d > 0.001f) {
                driverX += ((driverX - v.x) / d) * (minDist - d + 0.05f)
                driverZ += ((driverZ - v.z) / d) * (minDist - d + 0.05f)
            }
        }
    }

    private fun resolveTruckCollisions(prevX: Float, prevZ: Float, trafficManager: TrafficManager) {
        val truckRadius = 2.1f
        for (b in TruckWorld.buildings) {
            val halfW = b.width * 0.5f + truckRadius
            val halfD = b.depth * 0.5f + truckRadius
            if (abs(x - b.x) < halfW && abs(z - b.z) < halfD) {
                if (abs(speedMs) > 2.5f) {
                    damagePercent = (damagePercent + abs(speedMs) * 0.7f).coerceAtMost(100f)
                    bounceVel -= 1.2f
                }
                x = prevX
                z = prevZ
                speedMs = -speedMs * 0.28f
                return
            }
        }
        for (v in trafficManager.vehicles) {
            val minDist = truckRadius + (v.width * 0.5f + v.length * 0.25f)
            val d = hypot(x - v.x, z - v.z)
            if (d < minDist && d > 0.001f) {
                val impactSpeed = abs(speedMs - v.speedMs)
                if (impactSpeed > 2.5f) {
                    damagePercent = (damagePercent + impactSpeed * 0.85f).coerceAtMost(100f)
                }
                val pushX = (x - v.x) / d
                val pushZ = (z - v.z) / d
                x += pushX * (minDist - d + 0.15f)
                z += pushZ * (minDist - d + 0.15f)
                speedMs *= -0.25f
                v.speedMs = 0f
                return
            }
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > PI) a -= (2.0 * PI).toFloat()
        while (a < -PI) a += (2.0 * PI).toFloat()
        return a
    }
}
