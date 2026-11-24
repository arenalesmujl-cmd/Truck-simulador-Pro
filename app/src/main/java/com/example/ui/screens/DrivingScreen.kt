package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gl.TruckGLRenderer
import com.example.gl.TruckGLSurfaceView
import com.example.model.BlinkerState
import com.example.model.CareerProfile
import com.example.model.GearboxMode
import com.example.model.HeadlightMode
import com.example.model.MissionContract
import com.example.model.PlayerControlMode
import com.example.model.SteeringMode
import com.example.sim.DrivingInputs
import com.example.sim.TruckTelemetry
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun DrivingScreen(
    renderer: TruckGLRenderer,
    profile: CareerProfile,
    telemetry: TruckTelemetry,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    onCycleCamera: () -> Unit,
    onToggleDriverMode: () -> Unit,
    onOrbitCamera: (Float, Float) -> Unit,
    onCycleHeadlights: () -> Unit,
    onToggleLeftBlinker: () -> Unit,
    onToggleRightBlinker: () -> Unit,
    onToggleHazard: () -> Unit,
    onToggleWipers: () -> Unit,
    onToggleReverseAuto: () -> Unit,
    onShiftManual: (Int) -> Unit,
    onLoadCargo: () -> Unit,
    onDeliverCargo: () -> Unit,
    onRefuel: () -> Unit,
    onRepair: () -> Unit,
    onTowToRoad: () -> Unit,
    onPauseMenu: () -> Unit
) {
    BackHandler { onPauseMenu() }

    var gasHeld by remember { mutableStateOf(false) }
    var brakeHeld by remember { mutableStateOf(false) }
    var hornHeld by remember { mutableStateOf(false) }
    var leftButtonHeld by remember { mutableStateOf(false) }
    var rightButtonHeld by remember { mutableStateOf(false) }
    var wheelSteer by remember { mutableFloatStateOf(0f) }
    var wheelDragging by remember { mutableStateOf(false) }

    // On-foot joystick & sprint state
    var joyMoveX by remember { mutableFloatStateOf(0f) }
    var joyMoveY by remember { mutableFloatStateOf(0f) }
    var sprintHeld by remember { mutableStateOf(false) }

    val steeringMode = SteeringMode.fromRaw(profile.steeringMode)
    val gearboxMode = GearboxMode.fromRaw(profile.gearboxMode)
    val isOnFoot = telemetry.controlMode == PlayerControlMode.ON_FOOT

    LaunchedEffect(wheelDragging, leftButtonHeld, rightButtonHeld) {
        while (true) {
            if (steeringMode == SteeringMode.BUTTONS) {
                val target = when {
                    leftButtonHeld && !rightButtonHeld -> -1f
                    rightButtonHeld && !leftButtonHeld -> 1f
                    else -> 0f
                }
                wheelSteer += (target - wheelSteer) * 0.25f
            } else if (!wheelDragging) {
                wheelSteer *= 0.82f
                if (kotlin.math.abs(wheelSteer) < 0.01f) wheelSteer = 0f
            }
            delay(16L)
        }
    }

    // Clear pedal states when stepping out of cab
    LaunchedEffect(isOnFoot) {
        if (isOnFoot) {
            gasHeld = false
            brakeHeld = false
            hornHeld = false
        } else {
            joyMoveX = 0f
            joyMoveY = 0f
            sprintHeld = false
        }
    }

    val currentInputs = DrivingInputs(
        throttle = if (gasHeld && !isOnFoot) 1f else 0f,
        brake = if (brakeHeld && !isOnFoot) 1f else 0f,
        steering = if (!isOnFoot) wheelSteer.coerceIn(-1f, 1f) else 0f,
        hornActive = hornHeld && !isOnFoot,
        moveX = if (isOnFoot) joyMoveX else 0f,
        moveY = if (isOnFoot) joyMoveY else 0f,
        sprint = isOnFoot && sprintHeld
    )

    Box(modifier = Modifier.fillMaxSize().background(SimBackground)) {
        // Real-time 3D OpenGL ES 3.0 Viewport with Touch-Drag 360 Camera Orbit
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onOrbitCamera(-dragAmount.x * 0.0055f, dragAmount.y * 0.0042f)
                    }
                }
        ) {
            TruckGLSurfaceView(
                renderer = renderer,
                inputs = currentInputs,
                profile = profile,
                activeMission = activeMission,
                missionLoaded = missionLoaded,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tactile Landscape HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD ROW: Mini-Map Radar + Mission Banner + Driver Mode / Camera / Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = SimSurfaceGlass,
                    border = BorderStroke(1.dp, SimOutline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(104.dp)
                ) {
                    MiniMapRadar(
                        telemetry = telemetry,
                        activeMission = activeMission,
                        missionLoaded = missionLoaded,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }

                val fuelWarn = !isOnFoot && telemetry.fuelPercent < 18f
                val rpmWarn = !isOnFoot && telemetry.rpm > 2100f
                val bannerBorderColor = when {
                    fuelWarn -> CrimsonAlert.copy(alpha = 0.78f)
                    rpmWarn -> AmberSignal.copy(alpha = 0.78f)
                    isOnFoot && telemetry.isNearTruckDoor -> EmeraldGo.copy(alpha = 0.72f)
                    activeMission != null -> AmberSignal.copy(alpha = 0.45f)
                    else -> SkyCyan.copy(alpha = 0.42f)
                }
                Surface(
                    color = SimSurfaceGlass,
                    border = BorderStroke(1.2.dp, bannerBorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val hr = telemetry.timeOfDayHours.toInt()
                        val min = ((telemetry.timeOfDayHours - hr) * 60f).toInt().coerceIn(0, 59)
                        val timeText = String.format(java.util.Locale.US, "%02d:%02d", hr, min)
                        val modeText = if (isOnFoot) "Conductor A Pie" else telemetry.cameraMode.labelEs
                        Text(
                            text = "$timeText • ${telemetry.weather.labelEs} • $modeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCyan
                        )
                        if (fuelWarn) {
                            Text(
                                text = "COMBUSTIBLE BAJO (${telemetry.fuelPercent.toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (activeMission != null) {
                            val targetLabel = if (!missionLoaded) {
                                "Recoger en: ${activeMission.originName}"
                            } else {
                                "Entregar en: ${activeMission.destinationName}"
                            }
                            Text(
                                text = "$targetLabel (${telemetry.distanceToTargetM.toInt()} m)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberSignal,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isOnFoot) {
                            Text(
                                text = if (telemetry.isNearTruckDoor) {
                                    "Junto a la cabina • Desliza la pantalla para rotar cámara"
                                } else {
                                    "Distancia al camión: ${telemetry.distanceToTruckM.toInt()} m"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (telemetry.isNearTruckDoor) EmeraldGo else TextPrimary
                            )
                        } else {
                            Text(
                                text = "Modo Libre • ${String.format(java.util.Locale.US, "%.1f", telemetry.sessionDistanceKm)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Top-Right: Exit/Enter Driver + Camera Switch + Pause Menu
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (isOnFoot && telemetry.isNearTruckDoor) EmeraldGo.copy(alpha = 0.25f) else SimSurfaceGlass,
                        border = BorderStroke(
                            1.dp,
                            when {
                                isOnFoot && telemetry.isNearTruckDoor -> EmeraldGo
                                !isOnFoot && telemetry.canExitTruck -> SkyCyan
                                else -> SimOutline
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("driver_mode_button")
                            .clickable(onClick = onToggleDriverMode)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOnFoot) Icons.Filled.LocalShipping else Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = if (isOnFoot) "Subir al camión" else "Bajar del camión",
                                tint = if (isOnFoot) EmeraldGo else SkyCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOnFoot) "SUBIR" else "BAJAR",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    if (!isOnFoot) {
                        Surface(
                            color = SimSurfaceGlass,
                            border = BorderStroke(1.dp, RacingOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("camera_switch_button")
                                .clickable(onClick = onCycleCamera)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = "Cambiar cámara", tint = RacingOrange, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("CAM", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            }
                        }
                    }

                    Surface(
                        color = SimSurfaceGlass,
                        border = BorderStroke(1.dp, SimOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pause_menu_button")
                            .clickable(onClick = onPauseMenu)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Menú", tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // CONTEXT ZONE ACTION BANNER (Cargo Load / Deliver / Refuel / Repair / Enter Cab)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOnFoot && telemetry.isNearTruckDoor) {
                    Button(
                        onClick = onToggleDriverMode,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("enter_truck_button")
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = SimBackground)
                        Spacer(Modifier.width(8.dp))
                        Text("SUBIR AL CAMIÓN", color = SimBackground, style = MaterialTheme.typography.labelLarge)
                    }
                }
                ZoneActionBanner(
                    telemetry = telemetry,
                    activeMission = activeMission,
                    missionLoaded = missionLoaded,
                    onLoadCargo = onLoadCargo,
                    onDeliverCargo = onDeliverCargo,
                    onRefuel = onRefuel,
                    onRepair = onRepair,
                    onTowToRoad = onTowToRoad
                )
            }

            // BOTTOM CONTROLS ROW: Switch seamlessly between IN_TRUCK and ON_FOOT controls!
            if (isOnFoot) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left: 360 Virtual Analog Joystick for Realistic Human Driver
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MOVER CONDUCTOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCyan,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OnFootAnalogJoystick(
                            onMoveChange = { mx, my ->
                                joyMoveX = mx
                                joyMoveY = my
                            },
                            modifier = Modifier
                                .size(142.dp)
                                .testTag("driver_joystick")
                        )
                    }

                    // Right: Driver Status + Sprint Pad
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = SimSurfaceGlass,
                            border = BorderStroke(
                                1.2.dp,
                                if (telemetry.isNearTruckDoor) EmeraldGo.copy(alpha = 0.72f) else SkyCyan.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = profile.driverName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", telemetry.driverSpeedMs * 3.6f)} km/h • A pie",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyCyan
                                )
                                Text(
                                    text = "Camión a ${telemetry.distanceToTruckM.toInt()} m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (telemetry.isNearTruckDoor) EmeraldGo else AmberSignal
                                )
                            }
                        }

                        HoldButtonPad(
                            label = "CORRER",
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            held = sprintHeld,
                            accent = SkyCyan,
                            testTag = "sprint_button",
                            onHeldChange = { sprintHeld = it },
                            modifier = Modifier.size(width = 96.dp, height = 96.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // LEFT: Utility Switches + Steering Wheel / Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HudToggleChip(
                                label = "◀",
                                active = telemetry.blinkerState == BlinkerState.LEFT || telemetry.blinkerState == BlinkerState.HAZARD,
                                activeColor = AmberSignal,
                                testTag = "blinker_left_button",
                                onClick = onToggleLeftBlinker
                            )
                            HudToggleChip(
                                icon = Icons.Filled.Warning,
                                active = telemetry.blinkerState == BlinkerState.HAZARD,
                                activeColor = CrimsonAlert,
                                testTag = "hazard_button",
                                onClick = onToggleHazard
                            )
                            HudToggleChip(
                                label = "▶",
                                active = telemetry.blinkerState == BlinkerState.RIGHT || telemetry.blinkerState == BlinkerState.HAZARD,
                                activeColor = AmberSignal,
                                testTag = "blinker_right_button",
                                onClick = onToggleRightBlinker
                            )
                            HudToggleChip(
                                icon = Icons.Filled.Highlight,
                                active = telemetry.headlightMode != HeadlightMode.OFF,
                                activeColor = SkyCyan,
                                testTag = "headlights_button",
                                onClick = onCycleHeadlights
                            )
                            HudToggleChip(
                                icon = Icons.Filled.WaterDrop,
                                active = telemetry.wipersOn,
                                activeColor = EmeraldGo,
                                testTag = "wipers_button",
                                onClick = onToggleWipers
                            )
                            Surface(
                                color = if (hornHeld) RacingOrange else SimSurfaceGlass,
                                border = BorderStroke(1.dp, if (hornHeld) RacingOrange else SimOutline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("horn_button")
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                hornHeld = event.changes.any { it.pressed }
                                            }
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Campaign, contentDescription = "Bocina", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (steeringMode == SteeringMode.WHEEL) {
                            TactileSteeringWheel(
                                steerValue = wheelSteer,
                                onSteerChange = { angle, dragging ->
                                    wheelDragging = dragging
                                    if (dragging) wheelSteer = angle
                                },
                                modifier = Modifier
                                    .size(138.dp)
                                    .testTag("steering_wheel")
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HoldButtonPad(
                                    label = "IZQ",
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    held = leftButtonHeld,
                                    accent = SkyCyan,
                                    testTag = "steer_left_button",
                                    onHeldChange = { leftButtonHeld = it },
                                    modifier = Modifier.size(width = 76.dp, height = 68.dp)
                                )
                                HoldButtonPad(
                                    label = "DER",
                                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    held = rightButtonHeld,
                                    accent = SkyCyan,
                                    testTag = "steer_right_button",
                                    onHeldChange = { rightButtonHeld = it },
                                    modifier = Modifier.size(width = 76.dp, height = 68.dp)
                                )
                            }
                        }
                    }

                    // RIGHT: Analog+Digital Telemetry Cluster + Gearbox + Brake & Throttle Pedals
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedometerArcCluster(telemetry = telemetry)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (gearboxMode == GearboxMode.MANUAL) {
                                Surface(
                                    color = SimSurfaceGlass,
                                    border = BorderStroke(1.dp, SimOutline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .size(width = 50.dp, height = 46.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("gear_up_button")
                                        .clickable { onShiftManual(1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+", style = MaterialTheme.typography.titleLarge, color = EmeraldGo)
                                    }
                                }
                                Surface(
                                    color = SimSurfaceGlass,
                                    border = BorderStroke(1.dp, SimOutline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .size(width = 50.dp, height = 46.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("gear_down_button")
                                        .clickable { onShiftManual(-1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("-", style = MaterialTheme.typography.titleLarge, color = AmberSignal)
                                    }
                                }
                            } else {
                                Surface(
                                    color = if (telemetry.gear == -1) CrimsonAlert.copy(alpha = 0.3f) else SimSurfaceGlass,
                                    border = BorderStroke(1.dp, if (telemetry.gear == -1) CrimsonAlert else SimOutline),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .size(width = 52.dp, height = 64.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("reverse_toggle_button")
                                        .clickable(onClick = onToggleReverseAuto)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(if (telemetry.gear == -1) "R" else "D", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                        Text("REV", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        HoldButtonPad(
                            label = "FRENO",
                            icon = null,
                            held = brakeHeld,
                            accent = CrimsonAlert,
                            testTag = "brake_pedal",
                            onHeldChange = { brakeHeld = it },
                            modifier = Modifier.size(width = 74.dp, height = 104.dp)
                        )
                        HoldButtonPad(
                            label = "GAS",
                            icon = null,
                            held = gasHeld,
                            accent = EmeraldGo,
                            testTag = "throttle_pedal",
                            onHeldChange = { gasHeld = it },
                            modifier = Modifier.size(width = 80.dp, height = 122.dp)
                        )
                    }
                }
            }
        }
    }
}
