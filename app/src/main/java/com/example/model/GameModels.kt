package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppScreen {
    MENU,
    DRIVE,
    TRUCK_SELECT,
    GARAGE,
    MAP,
    MISSIONS,
    SETTINGS
}

enum class GraphicsQuality(val labelEs: String, val drawDistance: Float, val rainCount: Int, val treeDetail: Int) {
    LOW("Baja (Rápido)", 220f, 180, 1),
    MEDIUM("Media (Equilibrado)", 340f, 380, 2),
    HIGH("Alta (Ultra 3D)", 480f, 650, 3);

    companion object {
        fun fromRaw(raw: String): GraphicsQuality =
            entries.firstOrNull { it.name == raw } ?: HIGH
    }
}

enum class GearboxMode(val labelEs: String) {
    AUTO("Automática"),
    MANUAL("Manual");

    companion object {
        fun fromRaw(raw: String): GearboxMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class SteeringMode(val labelEs: String) {
    WHEEL("Volante Táctil"),
    BUTTONS("Botones Izq/Der");

    companion object {
        fun fromRaw(raw: String): SteeringMode =
            entries.firstOrNull { it.name == raw } ?: WHEEL
    }
}

enum class CameraMode(val labelEs: String) {
    EXTERIOR_CHASE("Exterior"),
    INTERIOR_CAB("Cabina Interior"),
    SIDE_VIEW("Lateral"),
    FRONT_BUMPER("Frontal");

    fun next(): CameraMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class PlayerControlMode(val labelEs: String) {
    IN_TRUCK("En Cabina"),
    ON_FOOT("A Pie")
}

enum class WeatherType(val labelEs: String) {
    CLEAR("Despejado"),
    CLOUDY("Nublado"),
    RAIN("Lluvia")
}

enum class HeadlightMode(val labelEs: String) {
    OFF("Apagadas"),
    LOW("Cortas"),
    HIGH("Largas");

    fun next(): HeadlightMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

enum class BlinkerState {
    NONE,
    LEFT,
    RIGHT,
    HAZARD
}

enum class TruckStyle {
    AMERICAN_LONGNOSE,
    EURO_CABOVER,
    HEAVY_TWIN_EXHAUST,
    ARCTIC_EXPEDITION
}

data class TruckModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val price: Long,
    val basePowerHp: Int,
    val baseTopSpeedKmh: Float,
    val baseTorque: Float,
    val baseTankLiters: Float,
    val suspensionStiffness: Float,
    val style: TruckStyle,
    val defaultColorHex: String
)

object TruckCatalog {
    val all: List<TruckModel> = listOf(
        TruckModel(
            id = "titan_680",
            name = "Titan 680",
            subtitle = "Tractocamión americano de morro largo",
            price = 0L,
            basePowerHp = 480,
            baseTopSpeedKmh = 105f,
            baseTorque = 1.0f,
            baseTankLiters = 450f,
            suspensionStiffness = 1.0f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#FF6D00"
        ),
        TruckModel(
            id = "euro_fh16",
            name = "Euro FH-16",
            subtitle = "Cabina europea aerodinámica de alta precisión",
            price = 38000L,
            basePowerHp = 580,
            baseTopSpeedKmh = 115f,
            baseTorque = 1.18f,
            baseTankLiters = 540f,
            suspensionStiffness = 1.15f,
            style = TruckStyle.EURO_CABOVER,
            defaultColorHex = "#1E88E5"
        ),
        TruckModel(
            id = "volcano_v8",
            name = "Volcano V8",
            subtitle = "Motor V8 biturbo con doble escape vertical",
            price = 62000L,
            basePowerHp = 690,
            baseTopSpeedKmh = 124f,
            baseTorque = 1.35f,
            baseTankLiters = 620f,
            suspensionStiffness = 1.25f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F"
        ),
        TruckModel(
            id = "arctic_hauler",
            name = "Arctic Hauler 8x4",
            subtitle = "Gigante todoterreno de carga extrema",
            price = 95000L,
            basePowerHp = 820,
            baseTopSpeedKmh = 132f,
            baseTorque = 1.55f,
            baseTankLiters = 760f,
            suspensionStiffness = 1.4f,
            style = TruckStyle.ARCTIC_EXPEDITION,
            defaultColorHex = "#00ACC1"
        )
    )

    fun byId(id: String): TruckModel = all.firstOrNull { it.id == id } ?: all.first()
}

data class PaintOption(val name: String, val hex: String)

object PaintCatalog {
    val options = listOf(
        PaintOption("Naranja Racing", "#FF6D00"),
        PaintOption("Azul Atlántico", "#1E88E5"),
        PaintOption("Rojo Fuego", "#D32F2F"),
        PaintOption("Negro Ónix", "#1E293B"),
        PaintOption("Blanco Ártico", "#E2E8F0"),
        PaintOption("Verde Bosque", "#10B981"),
        PaintOption("Amarillo Carga", "#F59E0B"),
        PaintOption("Cian Eléctrico", "#00ACC1")
    )
}

data class RouteInfo(
    val id: String,
    val name: String,
    val description: String,
    val unlockCost: Long,
    val requiredLevel: Long
)

object RouteCatalog {
    val all = listOf(
        RouteInfo("route_valley", "Autopista Valle Central", "Conecta Ciudad Central con Valle Verde y Puerto Bahía.", 0L, 1L),
        RouteInfo("route_coast", "Corredor Costero Sur", "Autopista rápida hacia la Terminal Marítima y refinerías.", 12000L, 1L),
        RouteInfo("route_sierra", "Paso Sierra Nevada", "Carretera de montaña con curvas, bosques y cantera minera.", 26000L, 2L),
        RouteInfo("route_express", "Circuito Industrial Norte", "Ruta pesada para cargas especiales y convoyes nocturnos.", 45000L, 3L)
    )
}

data class MissionContract(
    val id: String,
    val title: String,
    val cargoName: String,
    val weightTons: Int,
    val originHubId: String,
    val originName: String,
    val destinationHubId: String,
    val destinationName: String,
    val routeId: String,
    val rewardMoney: Long,
    val rewardXp: Long,
    val trailerColorHex: String
)

object MissionCatalog {
    val all = listOf(
        MissionContract(
            id = "m_central_valle",
            title = "Suministros Agrícolas",
            cargoName = "Maquinaria y Fertilizantes",
            weightTons = 16,
            originHubId = "hub_central",
            originName = "Centro Logístico Central",
            destinationHubId = "hub_valle",
            destinationName = "Cooperativa Valle Verde",
            routeId = "route_valley",
            rewardMoney = 4200L,
            rewardXp = 220L,
            trailerColorHex = "#E2E8F0"
        ),
        MissionContract(
            id = "m_valle_puerto",
            title = "Madera de Exportación",
            cargoName = "Troncos y Tablones",
            weightTons = 22,
            originHubId = "hub_valle",
            originName = "Aserradero Valle Verde",
            destinationHubId = "hub_puerto",
            destinationName = "Muelles Puerto Bahía",
            routeId = "route_valley",
            rewardMoney = 5800L,
            rewardXp = 310L,
            trailerColorHex = "#D97706"
        ),
        MissionContract(
            id = "m_puerto_central",
            title = "Contenedor Electrónico",
            cargoName = "Componentes Industriales",
            weightTons = 19,
            originHubId = "hub_puerto",
            originName = "Terminal Puerto Bahía",
            destinationHubId = "hub_central",
            destinationName = "Centro Logístico Central",
            routeId = "route_coast",
            rewardMoney = 7500L,
            rewardXp = 400L,
            trailerColorHex = "#2563EB"
        ),
        MissionContract(
            id = "m_central_sierra",
            title = "Vigas de Acero Pesado",
            cargoName = "Estructuras Metálicas",
            weightTons = 28,
            originHubId = "hub_central",
            originName = "Centro Logístico Central",
            destinationHubId = "hub_sierra",
            destinationName = "Cantera Sierra Nevada",
            routeId = "route_sierra",
            rewardMoney = 11200L,
            rewardXp = 580L,
            trailerColorHex = "#DC2626"
        ),
        MissionContract(
            id = "m_sierra_puerto",
            title = "Minerales Refinados",
            cargoName = "Carga Minera Sellada",
            weightTons = 32,
            originHubId = "hub_sierra",
            originName = "Cantera Sierra Nevada",
            destinationHubId = "hub_puerto",
            destinationName = "Muelles Puerto Bahía",
            routeId = "route_express",
            rewardMoney = 15800L,
            rewardXp = 820L,
            trailerColorHex = "#0F766E"
        )
    )
}

@Keep
@IgnoreExtraProperties
data class CareerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val driverName: String = "Capitan Ruta",
    val money: Long = 25000L,
    val xp: Long = 0L,
    val level: Long = 1L,
    val selectedTruckId: String = "titan_680",
    val ownedTruckIds: List<String> = listOf("titan_680"),
    val truckPaintHex: String = "#FF6D00",
    val engineLevel: Long = 1L,
    val transmissionLevel: Long = 1L,
    val suspensionLevel: Long = 1L,
    val tankLevel: Long = 1L,
    val unlockedRouteIds: List<String> = listOf("route_valley"),
    val totalDistanceKm: Double = 0.0,
    val completedMissionsCount: Long = 0L,
    val graphicsQuality: String = GraphicsQuality.HIGH.name,
    val gearboxMode: String = GearboxMode.AUTO.name,
    val steeringMode: String = SteeringMode.WHEEL.name,
    val soundEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val missionId: String = "",
    val cargoName: String = "",
    val originName: String = "",
    val destinationName: String = "",
    val truckId: String = "titan_680",
    val distanceKm: Double = 0.0,
    val rewardMoney: Long = 0L,
    val damagePercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
