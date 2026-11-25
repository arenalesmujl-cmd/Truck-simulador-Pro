package com.example.sim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class ZoneType {
    CARGO_HUB,
    GAS_STATION,
    WORKSHOP,
    PARKING
}

enum class SignType {
    SPEED_80,
    SPEED_50,
    STOP,
    FUEL_AHEAD,
    CURVE
}

enum class LightPhase {
    NS_GREEN,
    NS_AMBER,
    EW_GREEN,
    EW_AMBER
}

data class WorldCity(
    val id: String,
    val name: String,
    val x: Float,
    val z: Float,
    val radius: Float
)

data class RoadSegment(
    val id: String,
    val x1: Float,
    val z1: Float,
    val x2: Float,
    val z2: Float,
    val width: Float,
    val isHighway: Boolean,
    val routeId: String = "route_valley"
) {
    val length: Float = hypot(x2 - x1, z2 - z1)
    val dirX: Float = if (length > 0.001f) (x2 - x1) / length else 0f
    val dirZ: Float = if (length > 0.001f) (z2 - z1) / length else 1f
    val yawRad: Float = atan2(dirX, dirZ)
}

data class WorldZone(
    val id: String,
    val name: String,
    val type: ZoneType,
    val x: Float,
    val z: Float,
    val radius: Float,
    val yawRad: Float = 0f
)

enum class WorldSpriteKind {
    BANCO,
    HOSPITAL,
    POLICIA,
    BOMBEROS,
    ESCUELA,
    MALL,
    CASA_ROJA,
    CASA_AZUL,
    CASA_AMAR,
    CASA_MOD,
    CABANA,
    CASA_NAV,
    GAS,
    RESTAURANTE,
    PIZZERIA,
    PANADERIA,
    OBRA,
    TALLER,
    ARBOL1,
    ARBOL2,
    PINO,
    PALMA,
    ALTO,
    CIUDAD
}

data class BuildingSpec(
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float,
    val height: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val isSkyscraper: Boolean = false,
    val spriteKind: WorldSpriteKind = WorldSpriteKind.CASA_ROJA
)

data class TreeSpec(
    val x: Float,
    val z: Float,
    val scale: Float,
    val isPine: Boolean
)

data class MountainSpec(
    val x: Float,
    val z: Float,
    val radius: Float,
    val height: Float
)

data class TrafficLightNode(
    val id: String,
    val x: Float,
    val z: Float
)

data class RoadSignSpec(
    val x: Float,
    val z: Float,
    val yawRad: Float,
    val type: SignType
)

data class RoadQueryResult(
    val onRoad: Boolean,
    val distanceToCenter: Float,
    val roadWidth: Float,
    val nearestX: Float,
    val nearestZ: Float
)

object TruckWorld {
    const val WORLD_HALF_SIZE = 1150f

    val cities = listOf(
        WorldCity("city_central", "Ciudad Central", 0f, 0f, 290f),
        WorldCity("city_valle", "Valle Verde", -680f, -520f, 200f),
        WorldCity("city_puerto", "Puerto Bahía", 720f, -480f, 220f),
        WorldCity("city_sierra", "Sierra Nevada", 0f, 760f, 210f)
    )

    val roads = listOf(
        // Ciudad Central Avenue Grid
        RoadSegment("r_c_ew_main", -260f, 0f, 260f, 0f, 20f, isHighway = true),
        RoadSegment("r_c_ns_main", 0f, -260f, 0f, 260f, 20f, isHighway = true),
        RoadSegment("r_c_n_ring", -240f, -240f, 240f, -240f, 16f, isHighway = false),
        RoadSegment("r_c_s_ring", -240f, 240f, 240f, 240f, 16f, isHighway = false),
        RoadSegment("r_c_w_ring", -240f, -240f, -240f, 240f, 16f, isHighway = false),
        RoadSegment("r_c_e_ring", 240f, -240f, 240f, 240f, 16f, isHighway = false),

        // Highway Central -> Valle Verde
        RoadSegment("r_valle_1", -240f, 0f, -460f, -260f, 18f, isHighway = true),
        RoadSegment("r_valle_2", -460f, -260f, -680f, -520f, 18f, isHighway = true),
        RoadSegment("r_valle_town", -790f, -520f, -580f, -520f, 14f, isHighway = false),

        // Highway Central -> Puerto Bahia
        RoadSegment("r_puerto_1", 240f, 0f, 480f, -240f, 18f, isHighway = true, routeId = "route_coast"),
        RoadSegment("r_puerto_2", 480f, -240f, 720f, -480f, 18f, isHighway = true, routeId = "route_coast"),
        RoadSegment("r_puerto_dock", 640f, -480f, 840f, -480f, 16f, isHighway = false, routeId = "route_coast"),

        // Northern Secondary Country Road Valle Verde <-> Puerto Bahia
        RoadSegment("r_north_1", -680f, -520f, -220f, -640f, 13f, isHighway = false),
        RoadSegment("r_north_2", -220f, -640f, 260f, -620f, 13f, isHighway = false),
        RoadSegment("r_north_3", 260f, -620f, 720f, -480f, 13f, isHighway = false),

        // Mountain Highway Central -> Sierra Nevada
        RoadSegment("r_sierra_1", 0f, 240f, -120f, 490f, 18f, isHighway = true, routeId = "route_sierra"),
        RoadSegment("r_sierra_2", -120f, 490f, 0f, 760f, 18f, isHighway = true, routeId = "route_sierra"),
        RoadSegment("r_sierra_loop1", 0f, 760f, 220f, 520f, 15f, isHighway = false, routeId = "route_express"),
        RoadSegment("r_sierra_loop2", 220f, 520f, 240f, 0f, 15f, isHighway = false, routeId = "route_express"),
        RoadSegment("r_sierra_quarry", -90f, 760f, 140f, 760f, 15f, isHighway = false, routeId = "route_sierra")
    )

    val zones = listOf(
        // Cargo Loading / Unloading Hubs
        WorldZone("hub_central", "Centro Logístico Central", ZoneType.CARGO_HUB, 115f, 45f, 22f, 0f),
        WorldZone("hub_valle", "Cooperativa Valle Verde", ZoneType.CARGO_HUB, -740f, -475f, 22f, 0f),
        WorldZone("hub_puerto", "Muelles Puerto Bahía", ZoneType.CARGO_HUB, 795f, -435f, 22f, 0f),
        WorldZone("hub_sierra", "Cantera Sierra Nevada", ZoneType.CARGO_HUB, 85f, 805f, 22f, 0f),

        // Gas Stations
        WorldZone("gas_central", "Gasolinera Central", ZoneType.GAS_STATION, -110f, -42f, 16f, 0f),
        WorldZone("gas_valle", "Estación Valle Verde", ZoneType.GAS_STATION, -630f, -560f, 16f, 0f),
        WorldZone("gas_puerto", "Depósito Diésel Puerto", ZoneType.GAS_STATION, 665f, -522f, 16f, 0f),
        WorldZone("gas_sierra", "Surtidor Ruta Alpina", ZoneType.GAS_STATION, -150f, 490f, 16f, 0.3f),

        // Mechanic Workshops
        WorldZone("shop_central", "Taller Mecánico Central", ZoneType.WORKSHOP, 120f, -198f, 16f, 0f),
        WorldZone("shop_puerto", "Taller Naval y Diésel", ZoneType.WORKSHOP, 725f, -525f, 16f, 0f),
        WorldZone("shop_sierra", "Taller Montaña", ZoneType.WORKSHOP, -45f, 718f, 16f, 0f),

        // Parking / Rest Areas
        WorldZone("park_central", "Estacionamiento Norte", ZoneType.PARKING, -135f, 200f, 18f, 0f),
        WorldZone("park_sierra", "Mirador Sierra Nevada", ZoneType.PARKING, 195f, 545f, 18f, 0.4f)
    )

    val trafficLights = listOf(
        TrafficLightNode("tl_origin", 0f, 0f),
        TrafficLightNode("tl_north", 0f, -240f),
        TrafficLightNode("tl_south", 0f, 240f),
        TrafficLightNode("tl_east", 240f, 0f),
        TrafficLightNode("tl_west", -240f, 0f)
    )

    val roadSigns = listOf(
        RoadSignSpec(16f, -90f, 0f, SignType.SPEED_50),
        RoadSignSpec(-16f, 90f, PI.toFloat(), SignType.SPEED_50),
        RoadSignSpec(-260f, -18f, -PI.toFloat() / 2f, SignType.SPEED_80),
        RoadSignSpec(260f, 18f, PI.toFloat() / 2f, SignType.SPEED_80),
        RoadSignSpec(-95f, -20f, -PI.toFloat() / 2f, SignType.FUEL_AHEAD),
        RoadSignSpec(-85f, 360f, 0.25f, SignType.CURVE),
        RoadSignSpec(18f, 215f, 0f, SignType.STOP),
        RoadSignSpec(-18f, -215f, PI.toFloat(), SignType.STOP)
    )

    val mountains = listOf(
        MountainSpec(-360f, 780f, 220f, 95f),
        MountainSpec(380f, 840f, 240f, 110f),
        MountainSpec(0f, 1080f, 290f, 130f),
        MountainSpec(-820f, 450f, 210f, 85f),
        MountainSpec(860f, 420f, 230f, 90f),
        MountainSpec(-980f, -860f, 240f, 88f),
        MountainSpec(1020f, -820f, 210f, 76f),
        MountainSpec(0f, -980f, 260f, 82f)
    )

    val buildings: List<BuildingSpec> by lazy { generateBuildings() }
    val trees: List<TreeSpec> by lazy { generateTrees() }

    fun terrainHeightAt(x: Float, z: Float): Float {
        // Gentle climb into Sierra Nevada south highway (z > 280)
        val sierraBlend = ((z - 280f) / 480f).coerceIn(0f, 1f)
        val smoothHill = sierraBlend * sierraBlend * (3f - 2f * sierraBlend) * 14f
        return smoothHill
    }

    fun lightPhaseAt(simTimeSeconds: Float): LightPhase {
        val cycle = simTimeSeconds % 16f
        return when {
            cycle < 6.5f -> LightPhase.NS_GREEN
            cycle < 8.0f -> LightPhase.NS_AMBER
            cycle < 14.5f -> LightPhase.EW_GREEN
            else -> LightPhase.EW_AMBER
        }
    }

    fun queryRoad(x: Float, z: Float): RoadQueryResult {
        var bestDist = Float.MAX_VALUE
        var bestWidth = 16f
        var bestX = x
        var bestZ = z
        for (r in roads) {
            val dx = r.x2 - r.x1
            val dz = r.z2 - r.z1
            val lenSq = dx * dx + dz * dz
            if (lenSq < 0.001f) continue
            val t = (((x - r.x1) * dx + (z - r.z1) * dz) / lenSq).coerceIn(0f, 1f)
            val px = r.x1 + t * dx
            val pz = r.z1 + t * dz
            val d = hypot(x - px, z - pz)
            if (d < bestDist) {
                bestDist = d
                bestWidth = r.width
                bestX = px
                bestZ = pz
            }
        }
        // Also count zone pads as drivable tarmac
        for (zZone in zones) {
            val d = hypot(x - zZone.x, z - zZone.z)
            if (d <= zZone.radius) {
                return RoadQueryResult(true, 0f, zZone.radius * 2f, x, z)
            }
        }
        return RoadQueryResult(
            onRoad = bestDist <= (bestWidth * 0.56f),
            distanceToCenter = bestDist,
            roadWidth = bestWidth,
            nearestX = bestX,
            nearestZ = bestZ
        )
    }

    fun findNearbyZone(x: Float, z: Float): WorldZone? {
        return zones.firstOrNull { hypot(x - it.x, z - it.z) <= it.radius }
    }

    fun hubById(id: String): WorldZone? = zones.firstOrNull { it.id == id }

    private fun isClearOfRoadsAndZones(x: Float, z: Float, margin: Float): Boolean {
        val q = queryRoad(x, z)
        if (q.distanceToCenter < q.roadWidth * 0.5f + margin) return false
        for (zone in zones) {
            if (hypot(x - zone.x, z - zone.z) < zone.radius + margin) return false
        }
        return true
    }

    private fun generateBuildings(): List<BuildingSpec> {
        val list = mutableListOf<BuildingSpec>()

        fun addSpriteBuilding(bx: Float, bz: Float, kind: WorldSpriteKind, margin: Float = 12f) {
            if (!isClearOfRoadsAndZones(bx, bz, margin)) return
            val h = when (kind) {
                WorldSpriteKind.BANCO,
                WorldSpriteKind.HOSPITAL,
                WorldSpriteKind.MALL -> 16.5f
                WorldSpriteKind.POLICIA,
                WorldSpriteKind.BOMBEROS,
                WorldSpriteKind.ESCUELA,
                WorldSpriteKind.OBRA -> 14.5f
                WorldSpriteKind.RESTAURANTE,
                WorldSpriteKind.PIZZERIA,
                WorldSpriteKind.PANADERIA -> 12.2f
                else -> 11.4f
            }
            val w = (h * 1.15f).coerceIn(11f, 20f)
            list.add(
                BuildingSpec(
                    x = bx,
                    z = bz,
                    width = w,
                    depth = 10f,
                    height = h,
                    colorR = 1f,
                    colorG = 1f,
                    colorB = 1f,
                    isSkyscraper = kind == WorldSpriteKind.BANCO || kind == WorldSpriteKind.HOSPITAL || kind == WorldSpriteKind.MALL,
                    spriteKind = kind
                )
            )
        }

        // Ciudad Central — Avenue & downtown neighborhood blocks (including houses & shops visible directly ahead on spawn)
        val centralBuildings = listOf(
            // Main East-West Avenue North Side (z = -34..-38)
            Triple(-184f, -35f, WorldSpriteKind.CASA_AZUL),
            Triple(-150f, -35f, WorldSpriteKind.CASA_MOD),
            Triple(-40f, -34f, WorldSpriteKind.CASA_AMAR),
            Triple(34f, -34f, WorldSpriteKind.CASA_ROJA),
            Triple(68f, -36f, WorldSpriteKind.BANCO),
            Triple(104f, -34f, WorldSpriteKind.PIZZERIA),
            Triple(140f, -34f, WorldSpriteKind.RESTAURANTE),
            Triple(176f, -38f, WorldSpriteKind.HOSPITAL),
            Triple(206f, -36f, WorldSpriteKind.OBRA),

            // Main East-West Avenue South Side (z = +34..+38)
            Triple(-184f, 36f, WorldSpriteKind.CASA_NAV),
            Triple(-148f, 36f, WorldSpriteKind.ESCUELA),
            Triple(-76f, 36f, WorldSpriteKind.BOMBEROS),
            Triple(-42f, 36f, WorldSpriteKind.POLICIA),
            Triple(34f, 34f, WorldSpriteKind.CASA_AZUL),
            Triple(66f, 34f, WorldSpriteKind.PANADERIA),
            Triple(166f, 35f, WorldSpriteKind.CASA_MOD),
            Triple(202f, 35f, WorldSpriteKind.CASA_AMAR),

            // Main North-South Avenue (x = +/-36)
            Triple(36f, -74f, WorldSpriteKind.MALL),
            Triple(-36f, -74f, WorldSpriteKind.CASA_AZUL),
            Triple(36f, -118f, WorldSpriteKind.CASA_MOD),
            Triple(-36f, -118f, WorldSpriteKind.RESTAURANTE),
            Triple(36f, -164f, WorldSpriteKind.HOSPITAL),
            Triple(-36f, -164f, WorldSpriteKind.ESCUELA),
            Triple(36f, 80f, WorldSpriteKind.BANCO),
            Triple(-36f, 80f, WorldSpriteKind.CASA_ROJA),
            Triple(36f, 128f, WorldSpriteKind.PIZZERIA),
            Triple(-36f, 128f, WorldSpriteKind.PANADERIA),
            Triple(36f, 176f, WorldSpriteKind.POLICIA),
            Triple(-36f, 176f, WorldSpriteKind.BOMBEROS),

            // Inner City Residential & Commercial Blocks
            Triple(-88f, -92f, WorldSpriteKind.MALL),
            Triple(88f, -92f, WorldSpriteKind.OBRA),
            Triple(-88f, 92f, WorldSpriteKind.CASA_MOD),
            Triple(88f, 112f, WorldSpriteKind.CASA_AMAR),
            Triple(-155f, -145f, WorldSpriteKind.BANCO),
            Triple(160f, -145f, WorldSpriteKind.POLICIA),
            Triple(160f, 145f, WorldSpriteKind.ESCUELA),
            Triple(-160f, 145f, WorldSpriteKind.CASA_ROJA),
            Triple(-185f, -82f, WorldSpriteKind.CASA_AZUL),
            Triple(185f, 82f, WorldSpriteKind.RESTAURANTE),
            Triple(55f, -195f, WorldSpriteKind.BOMBEROS),
            Triple(-65f, -195f, WorldSpriteKind.CASA_NAV)
        )
        centralBuildings.forEach { (bx, bz, kind) -> addSpriteBuilding(bx, bz, kind, margin = 11.5f) }

        // Valle Verde town houses & local shops
        val valleBuildings = listOf(
            Triple(-635f, -465f, WorldSpriteKind.CABANA),
            Triple(-735f, -575f, WorldSpriteKind.CASA_ROJA),
            Triple(-795f, -465f, WorldSpriteKind.CASA_AMAR),
            Triple(-585f, -470f, WorldSpriteKind.PANADERIA),
            Triple(-685f, -465f, WorldSpriteKind.ESCUELA),
            Triple(-785f, -570f, WorldSpriteKind.CASA_AZUL)
        )
        valleBuildings.forEach { (bx, bz, kind) -> addSpriteBuilding(bx, bz, kind, margin = 12f) }

        // Puerto Bahia waterfront shops & modern buildings
        val puertoBuildings = listOf(
            Triple(685f, -420f, WorldSpriteKind.RESTAURANTE),
            Triple(775f, -540f, WorldSpriteKind.MALL),
            Triple(835f, -425f, WorldSpriteKind.OBRA),
            Triple(645f, -565f, WorldSpriteKind.PIZZERIA),
            Triple(625f, -430f, WorldSpriteKind.BANCO),
            Triple(730f, -425f, WorldSpriteKind.CASA_MOD)
        )
        puertoBuildings.forEach { (bx, bz, kind) -> addSpriteBuilding(bx, bz, kind, margin = 12f) }

        // Sierra Nevada alpine cabins & winter lodge homes
        val sierraBuildings = listOf(
            Triple(-85f, 815f, WorldSpriteKind.CABANA),
            Triple(135f, 705f, WorldSpriteKind.OBRA),
            Triple(20f, 835f, WorldSpriteKind.CASA_NAV),
            Triple(-55f, 705f, WorldSpriteKind.BOMBEROS)
        )
        sierraBuildings.forEach { (bx, bz, kind) -> addSpriteBuilding(bx, bz, kind, margin = 12f) }

        return list
    }

    private fun generateTrees(): List<TreeSpec> {
        val list = mutableListOf<TreeSpec>()
        var seed = 1337
        fun nextRand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 10000) / 10000f
        }
        for (i in 0 until 260) {
            val tx = (nextRand() * 2f - 1f) * 980f
            val tz = (nextRand() * 2f - 1f) * 980f
            // Keep downtown core free of dense forest
            if (hypot(tx, tz) < 255f) continue
            if (!isClearOfRoadsAndZones(tx, tz, 11f)) continue
            val isPine = tz > 150f || tx < -300f || nextRand() > 0.45f
            list.add(TreeSpec(tx, tz, scale = 0.85f + nextRand() * 0.75f, isPine = isPine))
        }
        return list
    }
}
