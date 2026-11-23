package com.example.sim

import com.example.model.BlinkerState
import com.example.model.CameraMode
import com.example.model.CareerProfile
import com.example.model.GearboxMode
import com.example.model.HeadlightMode
import com.example.model.MissionContract
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
    val hornActive: Boolean = false
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
    val hasTrailer: Boolean = false,
    val nearbyZone: WorldZone? = null,
    val distanceToTargetM: Float = 0f,
    val sessionDistanceKm: Double = 0.0,
    val engineStalled: Boolean = false
)

class TruckPhysicsEngine {
    // World pose
    var x: Float = -35f
    var z: Float = 0f
    var y: Float = 0f
    var yawRad: Float = PI.toFloat() / 2f // Facing East (+X) along main avenue
    var speedMs: Float = 0f

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
        x = startX
        z = startZ
        y = TruckWorld.terrainHeightAt(x, z)
        yawRad = startYaw
        speedMs = 0f
        gear = 1
        rpm = 680f
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
            } else if (gear == -1 && speedMs > 1.5f && inputs.throttle > 0.2f) {
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
            inputs.throttle * 6.8f * model.baseTorque * engineMult * transMult * gearFactor * trailerMassFactor * damagePenalty
        } else {
            0f
        }

        // Rolling resistance + aerodynamic drag + offroad mud/grass drag
        val rolling = (if (isOffRoad) 2.2f else 0.42f) * speedMs
        val aeroDrag = 0.0055f * speedMs * abs(speedMs)

        // Slope gravity component
        val aheadX = x + sin(yawRad) * 4f
        val aheadZ = z + cos(yawRad) * 4f
        val slopeSin = ((TruckWorld.terrainHeightAt(aheadX, aheadZ) - y) / 4f).coerceIn(-0.25f, 0.25f)
        val gravityForce = -slopeSin * 9.81f * 0.55f

        // Brake force
        val brakeDecel = inputs.brake * (if (isWet) 12.5f else 15.5f)
        var netAccel = throttleForce - rolling - aeroDrag + gravityForce

        if (inputs.brake > 0.05f) {
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

        // Prevent micro creep when parked
        if (inputs.throttle < 0.02f && abs(speedMs) < 0.15f && abs(slopeSin) < 0.03f) {
            speedMs = 0f
        }

        // Steering & Ackermann kinematics
        val targetFrontAngle = inputs.steering * 0.58f
        frontSteerRad += (targetFrontAngle - frontSteerRad) * (safeDt * 9f).coerceAtMost(1f)
        val wheelbase = 5.2f
        val yawRate = if (abs(speedMs) > 0.05f) {
            (speedMs / wheelbase) * tan(frontSteerRad) * (if (isWet) 0.92f else 1.0f)
        } else {
            0f
        }
        yawRad += yawRate * safeDt

        // Advance world position
        val prevX = x
        val prevZ = z
        x += sin(yawRad) * speedMs * safeDt
        z += cos(yawRad) * speedMs * safeDt

        // Clamp within world bounds
        x = x.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)
        z = z.coerceIn(-TruckWorld.WORLD_HALF_SIZE, TruckWorld.WORLD_HALF_SIZE)

        // Collision checks with buildings & traffic
        resolveCollisions(prevX, prevZ, trafficManager)

        y = TruckWorld.terrainHeightAt(x, z)
        val movedMeters = hypot(x - prevX, z - prevZ)
        sessionDistanceKm += movedMeters / 1000.0
        wheelSpinAngle += (speedMs / 0.52f) * safeDt

        // Articulated trailer follow kinematics
        if (hasTrailer) {
            val hitchX = x - sin(yawRad) * 2.4f
            val hitchZ = z - cos(yawRad) * 2.4f
            val dxToTrailer = hitchX - trailerX
            val dzToTrailer = hitchZ - trailerZ
            val desiredTrailerYaw = atan2(dxToTrailer, dzToTrailer)
            var angleDiff = normalizeAngle(desiredTrailerYaw - trailerYawRad)
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

        // Functional suspension (pitch on accel/brake, roll on cornering, bounce off-road)
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
        } else if (gear == 0) {
            680f + inputs.throttle * 1350f
        } else {
            (680f + speedRatio * 1450f + inputs.throttle * 260f).coerceIn(650f, 2450f)
        }
        rpm += (targetRpm - rpm) * (safeDt * 7f).coerceAtMost(1f)

        // Fuel consumption (liters/sec)
        if (hasFuel) {
            val burnRate = 0.015f + (rpm / 2400f) * 0.055f + inputs.throttle * 0.07f
            fuelLiters = (fuelLiters - burnRate * safeDt).coerceAtLeast(0f)
        }

        // Update traffic
        trafficManager.update(safeDt, simTimeSeconds, x, z)

        // Mission target distance
        val targetHub = if (activeMission != null) {
            val hubId = if (!missionLoaded) activeMission.originHubId else activeMission.destinationHubId
            TruckWorld.hubById(hubId)
        } else null
        val distToTarget = if (targetHub != null) hypot(x - targetHub.x, z - targetHub.z) else 0f

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
            hasTrailer = hasTrailer,
            nearbyZone = TruckWorld.findNearbyZone(x, z),
            distanceToTargetM = distToTarget,
            sessionDistanceKm = sessionDistanceKm,
            engineStalled = !hasFuel
        )
    }

    private fun resolveCollisions(prevX: Float, prevZ: Float, trafficManager: TrafficManager) {
        val truckRadius = 2.1f
        // Building collisions
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
        // Traffic vehicle collisions
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
