package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TruckAudioEngine
import com.example.data.CareerRepository
import com.example.gl.TruckGLRenderer
import com.example.model.AppScreen
import com.example.model.BlinkerState
import com.example.model.CareerProfile
import com.example.model.DeliveryRecord
import com.example.model.GearboxMode
import com.example.model.GraphicsQuality
import com.example.model.MissionContract
import com.example.model.RouteInfo
import com.example.model.SteeringMode
import com.example.model.TruckModel
import com.example.model.UiState
import com.example.model.WeatherType
import com.example.sim.TrafficManager
import com.example.sim.TruckPhysicsEngine
import com.example.sim.TruckTelemetry
import com.example.sim.TruckWorld
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

enum class UpgradeType {
    ENGINE,
    TRANSMISSION,
    SUSPENSION,
    TANK
}

class GameViewModel(
    private val repository: CareerRepository,
    private val currentUserId: String,
    private val driverDisplayName: String?
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeMission = MutableStateFlow<MissionContract?>(null)
    val activeMission: StateFlow<MissionContract?> = _activeMission.asStateFlow()

    private val _missionLoaded = MutableStateFlow(false)
    val missionLoaded: StateFlow<Boolean> = _missionLoaded.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _telemetry = MutableStateFlow(TruckTelemetry())
    val telemetry: StateFlow<TruckTelemetry> = _telemetry.asStateFlow()

    val physicsEngine = TruckPhysicsEngine()
    val trafficManager = TrafficManager()
    val audioEngine = TruckAudioEngine()

    val renderer: TruckGLRenderer = TruckGLRenderer(
        physics = physicsEngine,
        trafficManager = trafficManager,
        audioEngine = audioEngine,
        onTelemetry = { t -> _telemetry.value = t }
    )

    val profileState: StateFlow<UiState<CareerProfile>> = repository.observeCareerProfile(currentUserId)
        .map<CareerProfile?, UiState<CareerProfile>> { profile ->
            if (profile != null) {
                physicsEngine.applyCareerProfile(profile)
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing career profile", rootError)
            emit(UiState.Error(rootError.message ?: "Error al cargar el perfil de carrera"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val deliveriesState: StateFlow<UiState<List<DeliveryRecord>>> = repository.observeDeliveries(currentUserId)
        .map<List<DeliveryRecord>, UiState<List<DeliveryRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing deliveries", rootError)
            emit(UiState.Error(rootError.message ?: "Error al cargar historial de entregas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val result = repository.ensureInitialProfile(currentUserId, driverDisplayName)
            result.onFailure { err ->
                _statusBanner.value = "Error inicializando perfil: ${err.localizedMessage ?: "desconocido"}"
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun startDriving(resetPosition: Boolean = false) {
        if (resetPosition) {
            physicsEngine.resetForNewRun()
        }
        _currentScreen.value = AppScreen.DRIVE
    }

    fun acceptMissionAndDrive(mission: MissionContract) {
        _activeMission.value = mission
        _missionLoaded.value = false
        physicsEngine.detachTrailer()
        // Position truck near road ready to head toward origin hub
        val originHub = TruckWorld.hubById(mission.originHubId)
        if (originHub != null) {
            // Place player ~95m along nearest road approaching the origin hub for immediate action
            val q = TruckWorld.queryRoad(originHub.x - 70f, originHub.z)
            physicsEngine.resetForNewRun(q.nearestX, q.nearestZ, kotlin.math.PI.toFloat() / 2f)
        } else {
            physicsEngine.resetForNewRun()
        }
        _statusBanner.value = "Misión aceptada: recoge '${mission.cargoName}' en ${mission.originName}"
        _currentScreen.value = AppScreen.DRIVE
    }

    fun cancelActiveMission() {
        _activeMission.value = null
        _missionLoaded.value = false
        physicsEngine.detachTrailer()
        _statusBanner.value = "Misión cancelada. Modo libre activo."
    }

    fun loadCargoAtOrigin() {
        val mission = _activeMission.value ?: return
        _missionLoaded.value = true
        physicsEngine.attachTrailer(mission.trailerColorHex)
        _statusBanner.value = "Carga enganchada (${mission.weightTons}t). Destino: ${mission.destinationName}"
    }

    fun deliverCargoAtDestination(profile: CareerProfile) {
        val mission = _activeMission.value ?: return
        val dmg = physicsEngine.damagePercent.roundToLong().coerceIn(0L, 100L)
        val penaltyFactor = (1.0 - dmg * 0.005).coerceIn(0.4, 1.0)
        val payout = (mission.rewardMoney * penaltyFactor).roundToLong()
        val newMoney = (profile.money + payout).coerceAtMost(100_000_000L)
        val newXp = profile.xp + mission.rewardXp
        val newLevel = (1L + newXp / 500L).coerceAtMost(999L)
        val newDist = profile.totalDistanceKm + physicsEngine.sessionDistanceKm
        val newCount = profile.completedMissionsCount + 1L

        physicsEngine.sessionDistanceKm = 0.0
        physicsEngine.detachTrailer()
        _activeMission.value = null
        _missionLoaded.value = false

        viewModelScope.launch {
            val recResult = repository.addDeliveryRecord(
                userId = currentUserId,
                missionId = mission.id,
                cargoName = mission.cargoName,
                originName = mission.originName,
                destinationName = mission.destinationName,
                truckId = profile.selectedTruckId,
                distanceKm = String.format(java.util.Locale.US, "%.2f", newDist).toDouble(),
                rewardMoney = payout,
                damagePercent = dmg
            )
            val profResult = repository.updateCareerProgress(
                userId = currentUserId,
                money = newMoney,
                xp = newXp,
                level = newLevel,
                totalDistanceKm = newDist,
                completedMissionsCount = newCount,
                unlockedRouteIds = profile.unlockedRouteIds
            )
            if (recResult.isSuccess && profResult.isSuccess) {
                _statusBanner.value = "¡Entrega completada! +$$payout USD y +${mission.rewardXp} XP"
            } else {
                _statusBanner.value = "Entrega registrada localmente; error al sincronizar nube."
            }
        }
    }

    fun refuelAtStation(profile: CareerProfile) {
        val neededFrac = (1f - physicsEngine.fuelLiters / physicsEngine.maxFuelLiters).coerceIn(0f, 1f)
        val cost = (neededFrac * 420f).roundToLong().coerceAtLeast(25L)
        if (profile.money < cost) {
            _statusBanner.value = "Fondos insuficientes para repostar ($$cost USD)"
            return
        }
        physicsEngine.refuelFull()
        viewModelScope.launch {
            repository.updateCareerProgress(
                userId = currentUserId,
                money = profile.money - cost,
                xp = profile.xp,
                level = profile.level,
                totalDistanceKm = profile.totalDistanceKm,
                completedMissionsCount = profile.completedMissionsCount,
                unlockedRouteIds = profile.unlockedRouteIds
            )
        }
        _statusBanner.value = "Depósito lleno (-$$cost USD)"
    }

    fun repairAtWorkshop(profile: CareerProfile) {
        val cost = (physicsEngine.damagePercent * 35f).roundToLong().coerceAtLeast(50L)
        if (profile.money < cost) {
            _statusBanner.value = "Fondos insuficientes para reparar ($$cost USD)"
            return
        }
        physicsEngine.repairFull()
        viewModelScope.launch {
            repository.updateCareerProgress(
                userId = currentUserId,
                money = profile.money - cost,
                xp = profile.xp,
                level = profile.level,
                totalDistanceKm = profile.totalDistanceKm,
                completedMissionsCount = profile.completedMissionsCount,
                unlockedRouteIds = profile.unlockedRouteIds
            )
        }
        _statusBanner.value = "Camión reparado al 100% (-$$cost USD)"
    }

    fun buyOrSelectTruck(profile: CareerProfile, truck: TruckModel) {
        val alreadyOwned = profile.ownedTruckIds.contains(truck.id)
        if (alreadyOwned) {
            viewModelScope.launch {
                repository.updateGarageAndTruck(
                    userId = currentUserId,
                    money = profile.money,
                    selectedTruckId = truck.id,
                    ownedTruckIds = profile.ownedTruckIds,
                    truckPaintHex = profile.truckPaintHex,
                    engineLevel = profile.engineLevel,
                    transmissionLevel = profile.transmissionLevel,
                    suspensionLevel = profile.suspensionLevel,
                    tankLevel = profile.tankLevel
                ).onSuccess {
                    _statusBanner.value = "Camión activo: ${truck.name}"
                }.onFailure {
                    _statusBanner.value = "Error al seleccionar camión: ${it.localizedMessage}"
                }
            }
        } else {
            if (profile.money < truck.price) {
                _statusBanner.value = "Necesitas $${truck.price - profile.money} USD más para comprar ${truck.name}"
                return
            }
            val newOwned = (profile.ownedTruckIds + truck.id).distinct()
            viewModelScope.launch {
                repository.updateGarageAndTruck(
                    userId = currentUserId,
                    money = profile.money - truck.price,
                    selectedTruckId = truck.id,
                    ownedTruckIds = newOwned,
                    truckPaintHex = truck.defaultColorHex,
                    engineLevel = profile.engineLevel,
                    transmissionLevel = profile.transmissionLevel,
                    suspensionLevel = profile.suspensionLevel,
                    tankLevel = profile.tankLevel
                ).onSuccess {
                    _statusBanner.value = "¡Compraste y equipaste ${truck.name}!"
                }.onFailure {
                    _statusBanner.value = "Error en la compra: ${it.localizedMessage}"
                }
            }
        }
    }

    fun changeTruckPaint(profile: CareerProfile, hex: String) {
        val paintCost = 500L
        if (profile.truckPaintHex == hex) return
        if (profile.money < paintCost) {
            _statusBanner.value = "Necesitas $$paintCost USD para pintar la cabina"
            return
        }
        viewModelScope.launch {
            repository.updateGarageAndTruck(
                userId = currentUserId,
                money = profile.money - paintCost,
                selectedTruckId = profile.selectedTruckId,
                ownedTruckIds = profile.ownedTruckIds,
                truckPaintHex = hex,
                engineLevel = profile.engineLevel,
                transmissionLevel = profile.transmissionLevel,
                suspensionLevel = profile.suspensionLevel,
                tankLevel = profile.tankLevel
            ).onSuccess {
                _statusBanner.value = "Pintura aplicada (-$$paintCost USD)"
            }
        }
    }

    fun upgradeComponent(profile: CareerProfile, type: UpgradeType) {
        val currentLvl = when (type) {
            UpgradeType.ENGINE -> profile.engineLevel
            UpgradeType.TRANSMISSION -> profile.transmissionLevel
            UpgradeType.SUSPENSION -> profile.suspensionLevel
            UpgradeType.TANK -> profile.tankLevel
        }
        if (currentLvl >= 5L) {
            _statusBanner.value = "Componente al nivel máximo (Nv. 5)"
            return
        }
        val cost = currentLvl * 3500L
        if (profile.money < cost) {
            _statusBanner.value = "Necesitas $$cost USD para esta mejora"
            return
        }
        val newEng = if (type == UpgradeType.ENGINE) currentLvl + 1 else profile.engineLevel
        val newTrn = if (type == UpgradeType.TRANSMISSION) currentLvl + 1 else profile.transmissionLevel
        val newSus = if (type == UpgradeType.SUSPENSION) currentLvl + 1 else profile.suspensionLevel
        val newTnk = if (type == UpgradeType.TANK) currentLvl + 1 else profile.tankLevel

        viewModelScope.launch {
            repository.updateGarageAndTruck(
                userId = currentUserId,
                money = profile.money - cost,
                selectedTruckId = profile.selectedTruckId,
                ownedTruckIds = profile.ownedTruckIds,
                truckPaintHex = profile.truckPaintHex,
                engineLevel = newEng,
                transmissionLevel = newTrn,
                suspensionLevel = newSus,
                tankLevel = newTnk
            ).onSuccess {
                _statusBanner.value = "Mejora instalada (Nv. ${currentLvl + 1})"
            }
        }
    }

    fun unlockRoute(profile: CareerProfile, route: RouteInfo) {
        if (profile.unlockedRouteIds.contains(route.id)) return
        if (profile.level < route.requiredLevel) {
            _statusBanner.value = "Requiere Nivel ${route.requiredLevel} para desbloquear ${route.name}"
            return
        }
        if (profile.money < route.unlockCost) {
            _statusBanner.value = "Necesitas $${route.unlockCost} USD para desbloquear esta ruta"
            return
        }
        val newRoutes = (profile.unlockedRouteIds + route.id).distinct()
        viewModelScope.launch {
            repository.updateCareerProgress(
                userId = currentUserId,
                money = profile.money - route.unlockCost,
                xp = profile.xp,
                level = profile.level,
                totalDistanceKm = profile.totalDistanceKm,
                completedMissionsCount = profile.completedMissionsCount,
                unlockedRouteIds = newRoutes
            ).onSuccess {
                _statusBanner.value = "¡Ruta desbloqueada: ${route.name}!"
            }
        }
    }

    fun saveSettings(
        driverName: String,
        graphicsQuality: GraphicsQuality,
        gearboxMode: GearboxMode,
        steeringMode: SteeringMode,
        soundEnabled: Boolean
    ) {
        audioEngine.setEnabled(soundEnabled)
        viewModelScope.launch {
            repository.updateSettings(
                userId = currentUserId,
                driverName = driverName,
                graphicsQuality = graphicsQuality,
                gearboxMode = gearboxMode,
                steeringMode = steeringMode,
                soundEnabled = soundEnabled
            ).onSuccess {
                _statusBanner.value = "Configuración guardada"
            }
        }
    }

    // Driving controls delegated to physicsEngine
    fun cycleCamera() {
        physicsEngine.cameraMode = physicsEngine.cameraMode.next()
    }

    fun cycleHeadlights() {
        physicsEngine.headlightMode = physicsEngine.headlightMode.next()
    }

    fun toggleBlinkerLeft() {
        physicsEngine.blinkerState = if (physicsEngine.blinkerState == BlinkerState.LEFT) BlinkerState.NONE else BlinkerState.LEFT
    }

    fun toggleBlinkerRight() {
        physicsEngine.blinkerState = if (physicsEngine.blinkerState == BlinkerState.RIGHT) BlinkerState.NONE else BlinkerState.RIGHT
    }

    fun toggleHazard() {
        physicsEngine.blinkerState = if (physicsEngine.blinkerState == BlinkerState.HAZARD) BlinkerState.NONE else BlinkerState.HAZARD
    }

    fun toggleWipers() {
        physicsEngine.wipersOn = !physicsEngine.wipersOn
    }

    fun setWeather(weather: WeatherType, dynamic: Boolean) {
        physicsEngine.weather = weather
        physicsEngine.dynamicWeatherEnabled = dynamic
    }

    fun setTimeOfDay(hour: Float, dynamic: Boolean) {
        physicsEngine.timeOfDayHours = hour
        physicsEngine.dynamicTimeEnabled = dynamic
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }

    private companion object {
        const val TAG = "GameViewModel"
    }
}
