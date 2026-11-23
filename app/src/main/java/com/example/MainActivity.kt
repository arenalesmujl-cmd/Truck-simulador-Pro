package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.signOut
import com.example.data.CareerRepository
import com.example.model.AppScreen
import com.example.model.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DrivingScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TruckSelectScreen
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TruckSimulatorTheme
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TruckSimulatorTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedGameHost(
                currentUserId = uid,
                driverDisplayName = user.displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedGameHost(
    currentUserId: String,
    driverDisplayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GameViewModel(
                    repository = CareerRepository(db),
                    currentUserId = currentUserId,
                    driverDisplayName = driverDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val deliveriesState by viewModel.deliveriesState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeMission by viewModel.activeMission.collectAsStateWithLifecycle()
    val missionLoaded by viewModel.missionLoaded.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3800L)
            viewModel.clearBanner()
        }
    }

    val handleSignOut: () -> Unit = {
        viewModel.audioEngine.stop()
        signOut(credentialManager, scope)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SimBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val pState = profileState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = RacingOrange)
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando flota y perfil de conductor...", color = TextPrimary)
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(pState.message, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = handleSignOut) {
                            Text("Volver al inicio de sesión")
                        }
                    }
                }
                is UiState.Success -> {
                    val profile = pState.data
                    when (currentScreen) {
                        AppScreen.MENU -> MainMenuScreen(
                            profile = profile,
                            activeMission = activeMission,
                            onPlayClicked = { viewModel.startDriving(resetPosition = false) },
                            onNavigate = { viewModel.navigateTo(it) },
                            onSignOutClicked = handleSignOut
                        )
                        AppScreen.TRUCK_SELECT -> TruckSelectScreen(
                            profile = profile,
                            onBuyOrSelect = { viewModel.buyOrSelectTruck(profile, it) },
                            onBack = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                        AppScreen.GARAGE -> GarageScreen(
                            profile = profile,
                            onSelectPaint = { viewModel.changeTruckPaint(profile, it) },
                            onUpgrade = { viewModel.upgradeComponent(profile, it) },
                            onBack = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                        AppScreen.MAP -> MapScreen(
                            profile = profile,
                            telemetry = telemetry,
                            onUnlockRoute = { viewModel.unlockRoute(profile, it) },
                            onPlayFromMap = { viewModel.startDriving(resetPosition = false) },
                            onBack = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                        AppScreen.MISSIONS -> MissionsScreen(
                            profile = profile,
                            deliveriesState = deliveriesState,
                            activeMission = activeMission,
                            onAcceptMission = { viewModel.acceptMissionAndDrive(it) },
                            onCancelMission = { viewModel.cancelActiveMission() },
                            onBack = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            profile = profile,
                            currentWeather = viewModel.physicsEngine.weather,
                            dynamicWeather = viewModel.physicsEngine.dynamicWeatherEnabled,
                            dynamicTime = viewModel.physicsEngine.dynamicTimeEnabled,
                            onSetWeather = { w, dyn -> viewModel.setWeather(w, dyn) },
                            onSetTimeOfDay = { h, dyn -> viewModel.setTimeOfDay(h, dyn) },
                            onSaveSettings = { name, gfx, gear, steer, snd ->
                                viewModel.saveSettings(name, gfx, gear, steer, snd)
                            },
                            onSignOut = handleSignOut,
                            onBack = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                        AppScreen.DRIVE -> DrivingScreen(
                            renderer = viewModel.renderer,
                            profile = profile,
                            telemetry = telemetry,
                            activeMission = activeMission,
                            missionLoaded = missionLoaded,
                            onCycleCamera = { viewModel.cycleCamera() },
                            onCycleHeadlights = { viewModel.cycleHeadlights() },
                            onToggleLeftBlinker = { viewModel.toggleBlinkerLeft() },
                            onToggleRightBlinker = { viewModel.toggleBlinkerRight() },
                            onToggleHazard = { viewModel.toggleHazard() },
                            onToggleWipers = { viewModel.toggleWipers() },
                            onToggleReverseAuto = { viewModel.physicsEngine.toggleReverseAuto() },
                            onShiftManual = { viewModel.physicsEngine.shiftManual(it) },
                            onLoadCargo = { viewModel.loadCargoAtOrigin() },
                            onDeliverCargo = { viewModel.deliverCargoAtDestination(profile) },
                            onRefuel = { viewModel.refuelAtStation(profile) },
                            onRepair = { viewModel.repairAtWorkshop(profile) },
                            onTowToRoad = { viewModel.physicsEngine.emergencyTowToRoad() },
                            onPauseMenu = { viewModel.navigateTo(AppScreen.MENU) }
                        )
                    }
                }
            }

            if (statusBanner != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 12.dp, start = 24.dp, end = 24.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearBanner() }) {
                            Text("OK", color = RacingOrange)
                        }
                    }
                ) {
                    Text(statusBanner ?: "")
                }
            }
        }
    }
}
