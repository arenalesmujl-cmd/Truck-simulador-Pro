package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
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
    val defaultColorHex: String,
    @DrawableRes val drawableRes: Int
)

object TruckCatalog {
    val all: List<TruckModel> = listOf(
        TruckModel(
            id = "titan_680",
            name = "Santa Express",
            subtitle = "Cabina roja clásica con gorro navideño y mural de Santa",
            price = 0L,
            basePowerHp = 480,
            baseTopSpeedKmh = 105f,
            baseTorque = 1.00f,
            baseTankLiters = 450f,
            suspensionStiffness = 1.00f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_01
        ),
        TruckModel(
            id = "pack_02",
            name = "Rodolfo Turbo",
            subtitle = "Edición verde esmeralda con astas de reno y nariz roja",
            price = 6500L,
            basePowerHp = 510,
            baseTopSpeedKmh = 108f,
            baseTorque = 1.04f,
            baseTankLiters = 470f,
            suspensionStiffness = 1.05f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_02
        ),
        TruckModel(
            id = "pack_03",
            name = "Muñeco Polar",
            subtitle = "Blanco invernal con detalles rojos y muñeco de nieve",
            price = 8500L,
            basePowerHp = 525,
            baseTopSpeedKmh = 110f,
            baseTorque = 1.06f,
            baseTankLiters = 490f,
            suspensionStiffness = 1.08f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#E2E8F0",
            drawableRes = R.drawable.truck_pack_03
        ),
        TruckModel(
            id = "pack_04",
            name = "Árbol Dorado",
            subtitle = "Rojo brillante con pino navideño iluminado en furgón",
            price = 11000L,
            basePowerHp = 545,
            baseTopSpeedKmh = 112f,
            baseTorque = 1.10f,
            baseTankLiters = 510f,
            suspensionStiffness = 1.10f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_04
        ),
        TruckModel(
            id = "pack_05",
            name = "Regalo Real",
            subtitle = "Cabina verde con caja de regalo gigante y lazo dorado",
            price = 13500L,
            basePowerHp = 560,
            baseTopSpeedKmh = 114f,
            baseTorque = 1.14f,
            baseTankLiters = 530f,
            suspensionStiffness = 1.12f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_05
        ),
        TruckModel(
            id = "euro_fh16",
            name = "Pingüino Azul",
            subtitle = "Azul eléctrico con copos de nieve y pingüino ártico",
            price = 16000L,
            basePowerHp = 580,
            baseTopSpeedKmh = 116f,
            baseTorque = 1.18f,
            baseTankLiters = 550f,
            suspensionStiffness = 1.15f,
            style = TruckStyle.EURO_CABOVER,
            defaultColorHex = "#1E88E5",
            drawableRes = R.drawable.truck_pack_06
        ),
        TruckModel(
            id = "pack_07",
            name = "Bastón Dulce",
            subtitle = "Pintura caramelo rayada con bastón gigante en techo",
            price = 19000L,
            basePowerHp = 600,
            baseTopSpeedKmh = 118f,
            baseTorque = 1.22f,
            baseTankLiters = 565f,
            suspensionStiffness = 1.18f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_07
        ),
        TruckModel(
            id = "pack_08",
            name = "Feliz Navidad",
            subtitle = "Cabina verde de duende con furgón rojo Feliz Navidad",
            price = 22000L,
            basePowerHp = 615,
            baseTopSpeedKmh = 119f,
            baseTorque = 1.25f,
            baseTankLiters = 580f,
            suspensionStiffness = 1.20f,
            style = TruckStyle.EURO_CABOVER,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_08
        ),
        TruckModel(
            id = "pack_09",
            name = "Ciervo Ártico",
            subtitle = "Blanco glaciar con astas y paisaje invernal nevado",
            price = 26000L,
            basePowerHp = 635,
            baseTopSpeedKmh = 121f,
            baseTorque = 1.28f,
            baseTankLiters = 600f,
            suspensionStiffness = 1.22f,
            style = TruckStyle.ARCTIC_EXPEDITION,
            defaultColorHex = "#E2E8F0",
            drawableRes = R.drawable.truck_pack_09
        ),
        TruckModel(
            id = "pack_10",
            name = "Papá Noel Pro",
            subtitle = "Rojo fuego con parachoques dulce y luces navideñas",
            price = 30000L,
            basePowerHp = 650,
            baseTopSpeedKmh = 122f,
            baseTorque = 1.30f,
            baseTankLiters = 615f,
            suspensionStiffness = 1.24f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_10
        ),
        TruckModel(
            id = "pack_11",
            name = "Elfo Express",
            subtitle = "Verde festivo con gorro de elfo y carga veloz",
            price = 34000L,
            basePowerHp = 670,
            baseTopSpeedKmh = 124f,
            baseTorque = 1.33f,
            baseTankLiters = 630f,
            suspensionStiffness = 1.26f,
            style = TruckStyle.EURO_CABOVER,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_11
        ),
        TruckModel(
            id = "pack_12",
            name = "Caja Festiva",
            subtitle = "Rojo metálico con remolque de regalo y moño dorado",
            price = 38000L,
            basePowerHp = 690,
            baseTopSpeedKmh = 125f,
            baseTorque = 1.36f,
            baseTankLiters = 645f,
            suspensionStiffness = 1.28f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_12
        ),
        TruckModel(
            id = "pack_13",
            name = "Copo Blanco",
            subtitle = "Edición nívea con copos azules y muñeco clásico",
            price = 42000L,
            basePowerHp = 710,
            baseTopSpeedKmh = 126f,
            baseTorque = 1.39f,
            baseTankLiters = 660f,
            suspensionStiffness = 1.30f,
            style = TruckStyle.AMERICAN_LONGNOSE,
            defaultColorHex = "#E2E8F0",
            drawableRes = R.drawable.truck_pack_13
        ),
        TruckModel(
            id = "pack_14",
            name = "Convoy Regalos",
            subtitle = "Techo cargado de regalos y árbol iluminado en ruta",
            price = 46000L,
            basePowerHp = 730,
            baseTopSpeedKmh = 127f,
            baseTorque = 1.42f,
            baseTankLiters = 680f,
            suspensionStiffness = 1.32f,
            style = TruckStyle.ARCTIC_EXPEDITION,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_14
        ),
        TruckModel(
            id = "pack_15",
            name = "Jengibre V8",
            subtitle = "Rayas de menta con galleta de jengibre en furgón",
            price = 52000L,
            basePowerHp = 750,
            baseTopSpeedKmh = 128f,
            baseTorque = 1.45f,
            baseTankLiters = 700f,
            suspensionStiffness = 1.34f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_15
        ),
        TruckModel(
            id = "volcano_v8",
            name = "Oso Polar V8",
            subtitle = "Blanco ártico con oso polar y bufanda de invierno",
            price = 58000L,
            basePowerHp = 770,
            baseTopSpeedKmh = 129f,
            baseTorque = 1.48f,
            baseTankLiters = 720f,
            suspensionStiffness = 1.36f,
            style = TruckStyle.ARCTIC_EXPEDITION,
            defaultColorHex = "#E2E8F0",
            drawableRes = R.drawable.truck_pack_16
        ),
        TruckModel(
            id = "pack_17",
            name = "Saco Mágico",
            subtitle = "Cabina verde con el gran saco rojo de juguetes",
            price = 65000L,
            basePowerHp = 790,
            baseTopSpeedKmh = 130f,
            baseTorque = 1.50f,
            baseTankLiters = 735f,
            suspensionStiffness = 1.38f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#10B981",
            drawableRes = R.drawable.truck_pack_17
        ),
        TruckModel(
            id = "pack_18",
            name = "Árbol Neón",
            subtitle = "Carmesí nocturno con pino neón LED de alta potencia",
            price = 72000L,
            basePowerHp = 805,
            baseTopSpeedKmh = 131f,
            baseTorque = 1.52f,
            baseTankLiters = 750f,
            suspensionStiffness = 1.40f,
            style = TruckStyle.HEAVY_TWIN_EXHAUST,
            defaultColorHex = "#D32F2F",
            drawableRes = R.drawable.truck_pack_18
        ),
        TruckModel(
            id = "pack_19",
            name = "Regalo Zafiro",
            subtitle = "Azul real con caja de lujo roja y cinta dorada",
            price = 82000L,
            basePowerHp = 820,
            baseTopSpeedKmh = 132f,
            baseTorque = 1.54f,
            baseTankLiters = 765f,
            suspensionStiffness = 1.42f,
            style = TruckStyle.EURO_CABOVER,
            defaultColorHex = "#1E88E5",
            drawableRes = R.drawable.truck_pack_19
        ),
        TruckModel(
            id = "arctic_hauler",
            name = "Santa Supremo",
            subtitle = "Edición legendaria blanco y rojo de potencia máxima",
            price = 95000L,
            basePowerHp = 860,
            baseTopSpeedKmh = 135f,
            baseTorque = 1.60f,
            baseTankLiters = 800f,
            suspensionStiffness = 1.45f,
            style = TruckStyle.ARCTIC_EXPEDITION,
            defaultColorHex = "#E2E8F0",
            drawableRes = R.drawable.truck_pack_20
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
