package com.example.sim

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class TrafficVehicleType {
    CAR,
    BUS,
    TRUCK
}

data class TrafficVehicle(
    val id: Int,
    val type: TrafficVehicleType,
    var roadIndex: Int,
    var progress: Float, // 0..1 along road
    var direction: Int, // +1 or -1
    var speedMs: Float,
    val maxSpeedMs: Float,
    val length: Float,
    val width: Float,
    val height: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    var x: Float = 0f,
    var z: Float = 0f,
    var y: Float = 0f,
    var yawRad: Float = 0f,
    var braking: Boolean = false
)

class TrafficManager {
    val vehicles: MutableList<TrafficVehicle> = mutableListOf()

    private val palette = listOf(
        Triple(0.85f, 0.22f, 0.18f),
        Triple(0.18f, 0.45f, 0.82f),
        Triple(0.90f, 0.92f, 0.95f),
        Triple(0.22f, 0.26f, 0.32f),
        Triple(0.16f, 0.62f, 0.42f),
        Triple(0.88f, 0.66f, 0.14f)
    )

    init {
        spawnInitialTraffic()
    }

    private fun spawnInitialTraffic() {
        vehicles.clear()
        val roadCount = TruckWorld.roads.size
        var idCounter = 1
        for (i in 0 until 24) {
            val roadIdx = (i * 3) % roadCount
            val dir = if (i % 2 == 0) 1 else -1
            val type = when {
                i % 5 == 0 -> TrafficVehicleType.BUS
                i % 4 == 0 -> TrafficVehicleType.TRUCK
                else -> TrafficVehicleType.CAR
            }
            val color = when (type) {
                TrafficVehicleType.BUS -> Triple(0.92f, 0.58f, 0.10f)
                TrafficVehicleType.TRUCK -> Triple(0.85f, 0.87f, 0.90f)
                TrafficVehicleType.CAR -> palette[i % palette.size]
            }
            val maxSpeed = when (type) {
                TrafficVehicleType.CAR -> 16f + (i % 4) * 2f
                TrafficVehicleType.BUS -> 13f
                TrafficVehicleType.TRUCK -> 14f
            }
            val dims = when (type) {
                TrafficVehicleType.CAR -> Triple(4.3f, 1.85f, 1.55f)
                TrafficVehicleType.BUS -> Triple(10.2f, 2.45f, 3.1f)
                TrafficVehicleType.TRUCK -> Triple(8.2f, 2.45f, 3.2f)
            }
            val v = TrafficVehicle(
                id = idCounter++,
                type = type,
                roadIndex = roadIdx,
                progress = ((i * 0.37f) % 0.8f) + 0.1f,
                direction = dir,
                speedMs = maxSpeed * 0.7f,
                maxSpeedMs = maxSpeed,
                length = dims.first,
                width = dims.second,
                height = dims.third,
                colorR = color.first,
                colorG = color.second,
                colorB = color.third
            )
            updateWorldTransform(v)
            vehicles.add(v)
        }
    }

    fun update(dt: Float, simTimeSeconds: Float, playerX: Float, playerZ: Float) {
        val phase = TruckWorld.lightPhaseAt(simTimeSeconds)
        for (v in vehicles) {
            val road = TruckWorld.roads[v.roadIndex]
            var desiredSpeed = v.maxSpeedMs
            var shouldBrake = false

            // 1. Check traffic lights ahead
            val forwardX = road.dirX * v.direction
            val forwardZ = road.dirZ * v.direction
            for (tl in TruckWorld.trafficLights) {
                val dx = tl.x - v.x
                val dz = tl.z - v.z
                val along = dx * forwardX + dz * forwardZ
                val lateral = abs(-dx * forwardZ + dz * forwardX)
                if (along in 5f..36f && lateral < 8f) {
                    val movingNS = abs(forwardZ) >= abs(forwardX)
                    val redForMe = if (movingNS) {
                        phase == LightPhase.EW_GREEN || phase == LightPhase.EW_AMBER || phase == LightPhase.NS_AMBER
                    } else {
                        phase == LightPhase.NS_GREEN || phase == LightPhase.NS_AMBER || phase == LightPhase.EW_AMBER
                    }
                    if (redForMe) {
                        desiredSpeed = 0f
                        shouldBrake = true
                    }
                }
            }

            // 2. Check player truck ahead
            val pdx = playerX - v.x
            val pdz = playerZ - v.z
            val pAlong = pdx * forwardX + pdz * forwardZ
            val pLat = abs(-pdx * forwardZ + pdz * forwardX)
            if (pAlong in 3f..26f && pLat < 4.5f) {
                desiredSpeed = 0f
                shouldBrake = true
            }

            // 3. Check other traffic vehicles ahead
            for (other in vehicles) {
                if (other.id == v.id) continue
                val odx = other.x - v.x
                val odz = other.z - v.z
                val oAlong = odx * forwardX + odz * forwardZ
                val oLat = abs(-odx * forwardZ + odz * forwardX)
                if (oAlong in 2f..(v.length + other.length + 10f) && oLat < 3.2f) {
                    desiredSpeed = minOf(desiredSpeed, other.speedMs * 0.7f)
                    if (oAlong < v.length + 7f) {
                        desiredSpeed = 0f
                        shouldBrake = true
                    }
                }
            }

            // Smooth acceleration / braking
            val accel = if (desiredSpeed > v.speedMs) 6.5f else 14f
            if (v.speedMs < desiredSpeed) {
                v.speedMs = (v.speedMs + accel * dt).coerceAtMost(desiredSpeed)
            } else {
                v.speedMs = (v.speedMs - accel * dt).coerceAtLeast(desiredSpeed)
            }
            v.braking = shouldBrake || v.speedMs < 0.8f

            val distStep = v.speedMs * dt
            v.progress += (distStep / road.length.coerceAtLeast(1f)) * v.direction

            if (v.progress >= 1f || v.progress <= 0f) {
                switchToConnectedRoad(v)
            }
            updateWorldTransform(v)
        }
    }

    private fun switchToConnectedRoad(v: TrafficVehicle) {
        val currentRoad = TruckWorld.roads[v.roadIndex]
        val endX = if (v.progress >= 1f) currentRoad.x2 else currentRoad.x1
        val endZ = if (v.progress >= 1f) currentRoad.z2 else currentRoad.z1

        val candidates = mutableListOf<Pair<Int, Int>>()
        TruckWorld.roads.forEachIndexed { idx, candidate ->
            if (idx != v.roadIndex) {
                if (hypot(candidate.x1 - endX, candidate.z1 - endZ) < 12f) {
                    candidates.add(idx to 1)
                } else if (hypot(candidate.x2 - endX, candidate.z2 - endZ) < 12f) {
                    candidates.add(idx to -1)
                }
            }
        }
        if (candidates.isNotEmpty()) {
            val pick = candidates[(v.id + v.roadIndex) % candidates.size]
            v.roadIndex = pick.first
            v.direction = pick.second
            v.progress = if (v.direction > 0) 0.02f else 0.98f
        } else {
            // Turn around on dead-end spur
            v.direction = -v.direction
            v.progress = v.progress.coerceIn(0.02f, 0.98f)
        }
    }

    private fun updateWorldTransform(v: TrafficVehicle) {
        val road = TruckWorld.roads[v.roadIndex]
        val laneOffset = road.width * 0.24f
        val cx = road.x1 + (road.x2 - road.x1) * v.progress
        val cz = road.z1 + (road.z2 - road.z1) * v.progress
        val forwardX = road.dirX * v.direction
        val forwardZ = road.dirZ * v.direction
        // Right-hand lane offset
        val rightX = -forwardZ
        val rightZ = forwardX
        v.x = cx + rightX * laneOffset
        v.z = cz + rightZ * laneOffset
        v.y = TruckWorld.terrainHeightAt(v.x, v.z)
        v.yawRad = kotlin.math.atan2(forwardX, forwardZ)
    }
}
