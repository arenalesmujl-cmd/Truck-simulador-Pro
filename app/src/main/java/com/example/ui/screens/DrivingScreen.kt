package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gl.TruckGLRenderer
import com.example.gl.TruckGLSurfaceView
import com.example.model.BlinkerState
import com.example.model.CareerProfile
import com.example.model.GearboxMode
import com.example.model.HeadlightMode
import com.example.model.MissionContract
import com.example.model.SteeringMode
import com.example.sim.DrivingInputs
import com.example.sim.TruckTelemetry
import com.example.sim.TruckWorld
import com.example.sim.ZoneType
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurfaceElevated
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DrivingScreen(
    renderer: TruckGLRenderer,
    profile: CareerProfile,
    telemetry: TruckTelemetry,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    onCycleCamera: () -> Unit,
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

    val steeringMode = SteeringMode.fromRaw(profile.steeringMode)
    val gearboxMode = GearboxMode.fromRaw(profile.gearboxMode)

    // Smooth auto-centering for steering wheel & button steering ramp
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

    val currentInputs = DrivingInputs(
        throttle = if (gasHeld) 1f else 0f,
        brake = if (brakeHeld) 1f else 0f,
        steering = wheelSteer.coerceIn(-1f, 1f),
        hornActive = hornHeld
    )

    Box(modifier = Modifier.fillMaxSize().background(SimBackground)) {
        // Real-time 3D OpenGL ES 3.0 Viewport
        TruckGLSurfaceView(
            renderer = renderer,
            inputs = currentInputs,
            profile = profile,
            activeMission = activeMission,
            missionLoaded = missionLoaded,
            modifier = Modifier.fillMaxSize()
        )

        // Tactile Landscape HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD ROW: Mini-Map Radar + Mission Banner + Camera / Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left: Mini-Map Radar
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

                // Top-Center: Mission Objective & Environment Info
                Surface(
                    color = SimSurfaceGlass,
                    border = BorderStroke(1.dp, SimOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val hr = telemetry.timeOfDayHours.toInt()
                        val min = ((telemetry.timeOfDayHours - hr) * 60f).toInt().coerceIn(0, 59)
                        val timeText = String.format(java.util.Locale.US, "%02d:%02d", hr, min)
                        Text(
                            text = "$timeText • ${telemetry.weather.labelEs} • ${telemetry.cameraMode.labelEs}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyCyan
                        )
                        if (activeMission != null) {
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
                        } else {
                            Text(
                                text = "Modo Libre • ${String.format(java.util.Locale.US, "%.1f", telemetry.sessionDistanceKm)} km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Top-Right: Camera Switch + Pause Menu
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // CONTEXT ZONE ACTION BANNER (Cargo Load / Deliver / Refuel / Repair / Tow)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
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

            // BOTTOM CONTROLS ROW: Left Steering & Lights/Horn | Right Telemetry & Pedals
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
                        // Holdable Air Horn
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

                // RIGHT: Telemetry Cluster + Gearbox + Brake & Throttle Pedals
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dashboard Telemetry Box
                    Surface(
                        color = SimSurfaceGlass,
                        border = BorderStroke(1.dp, SimOutline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${telemetry.speedKmh.toInt()}",
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                                    color = TextPrimary
                                )
                                Text("km/h", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                val gearLabel = when (telemetry.gear) {
                                    -1 -> "R"
                                    0 -> "N"
                                    else -> "D${telemetry.gear}"
                                }
                                Surface(
                                    color = if (telemetry.gear == -1) CrimsonAlert.copy(alpha = 0.25f) else RacingOrange.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = gearLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (telemetry.gear == -1) CrimsonAlert else RacingOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            // RPM Bar
                            LinearProgressIndicator(
                                progress = { (telemetry.rpm / 2450f).coerceIn(0f, 1f) },
                                color = if (telemetry.rpm > 2050f) CrimsonAlert else SkyCyan,
                                trackColor = SimSurfaceElevated,
                                modifier = Modifier.width(128.dp).height(5.dp)
                            )
                            Spacer(Modifier.height(5.dp))
                            Row(
                                modifier = Modifier.width(128.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⛽ ${telemetry.fuelPercent.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (telemetry.fuelPercent < 20f) CrimsonAlert else EmeraldGo
                                )
                                Text(
                                    text = "🔧 ${telemetry.damagePercent.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (telemetry.damagePercent > 40f) AmberSignal else TextSecondary
                                )
                            }
                        }
                    }

                    // Gearbox Shift Column
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

                    // Brake & Throttle Pedals (multi-touch holdable)
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

@Composable
private fun ZoneActionBanner(
    telemetry: TruckTelemetry,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    onLoadCargo: () -> Unit,
    onDeliverCargo: () -> Unit,
    onRefuel: () -> Unit,
    onRepair: () -> Unit,
    onTowToRoad: () -> Unit
) {
    if (telemetry.engineStalled) {
        Button(
            onClick = onTowToRoad,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("emergency_tow_button")
        ) {
            Text("SIN COMBUSTIBLE • ASISTENCIA EN RUTA", style = MaterialTheme.typography.labelLarge)
        }
        return
    }

    val zone = telemetry.nearbyZone ?: return
    when (zone.type) {
        ZoneType.CARGO_HUB -> {
            if (activeMission != null && !missionLoaded && zone.id == activeMission.originHubId) {
                Button(
                    onClick = onLoadCargo,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSignal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("load_cargo_button")
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = SimBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("CARGAR: ${activeMission.cargoName}", color = SimBackground, style = MaterialTheme.typography.labelLarge)
                }
            } else if (activeMission != null && missionLoaded && zone.id == activeMission.destinationHubId) {
                Button(
                    onClick = onDeliverCargo,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("deliver_cargo_button")
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = SimBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("ENTREGAR CARGA (+$${activeMission.rewardMoney})", color = SimBackground, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        ZoneType.GAS_STATION -> {
            if (telemetry.fuelPercent < 98f) {
                Button(
                    onClick = onRefuel,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSignal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("refuel_button")
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = SimBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("REPOSTAR DIÉSEL (${zone.name})", color = SimBackground, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        ZoneType.WORKSHOP -> {
            if (telemetry.damagePercent > 1f) {
                Button(
                    onClick = onRepair,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("repair_button")
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = SimBackground)
                    Spacer(Modifier.width(8.dp))
                    Text("REPARAR CAMIÓN (${telemetry.damagePercent.toInt()}% daño)", color = SimBackground, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        ZoneType.PARKING -> {
            Surface(
                color = SimSurfaceGlass,
                border = BorderStroke(1.dp, SkyCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Zona de descanso: ${zone.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TactileSteeringWheel(
    steerValue: Float,
    onSteerChange: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SimSurfaceGlass)
            .border(1.dp, SimOutline, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.firstOrNull { it.pressed }
                        if (pressed != null) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = pressed.position.x - cx
                            val dy = pressed.position.y - cy
                            val angle = atan2(dx, -dy)
                            val normalized = (angle / (PI.toFloat() * 0.45f)).coerceIn(-1f, 1f)
                            onSteerChange(normalized, true)
                        } else {
                            onSteerChange(0f, false)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            rotate(degrees = steerValue * 110f, pivot = c) {
                drawCircle(color = Color(0xFF94A3B8), radius = r, center = c, style = Stroke(width = 16f))
                drawCircle(color = RacingOrange, radius = 18f, center = c)
                // Spokes
                drawLine(Color(0xFFCBD5E1), Offset(c.x - r + 6f, c.y), Offset(c.x + r - 6f, c.y), strokeWidth = 10f, cap = StrokeCap.Round)
                drawLine(Color(0xFFCBD5E1), Offset(c.x, c.y), Offset(c.x, c.y + r - 6f), strokeWidth = 10f, cap = StrokeCap.Round)
                // Top center marker
                drawCircle(RacingOrange, radius = 6f, center = Offset(c.x, c.y - r))
            }
        }
    }
}

@Composable
private fun HoldButtonPad(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    held: Boolean,
    accent: Color,
    testTag: String,
    onHeldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (held) accent.copy(alpha = 0.85f) else SimSurfaceGlass,
        border = BorderStroke(2.dp, if (held) accent else accent.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        onHeldChange(event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(28.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (held) SimBackground else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HudToggleChip(
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    active: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) activeColor.copy(alpha = 0.28f) else SimSurfaceGlass,
        border = BorderStroke(1.dp, if (active) activeColor else SimOutline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(42.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = testTag, tint = if (active) activeColor else TextSecondary, modifier = Modifier.size(18.dp))
            } else if (label != null) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) activeColor else TextSecondary)
            }
        }
    }
}

@Composable
private fun MiniMapRadar(
    telemetry: TruckTelemetry,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = w / 460f // Covers +-230m around truck

        fun toRadar(wx: Float, wz: Float): Offset = Offset(
            x = cx + (wx - telemetry.truckX) * scale,
            y = cy + (wz - telemetry.truckZ) * scale
        )

        // Roads in radar
        for (road in TruckWorld.roads) {
            drawLine(
                color = Color(0xFF475569),
                start = toRadar(road.x1, road.z1),
                end = toRadar(road.x2, road.z2),
                strokeWidth = 4f
            )
        }

        // Nearby facility dots
        for (z in TruckWorld.zones) {
            val pos = toRadar(z.x, z.z)
            if (pos.x in 0f..w && pos.y in 0f..h) {
                val c = when (z.type) {
                    ZoneType.CARGO_HUB -> EmeraldGo
                    ZoneType.GAS_STATION -> AmberSignal
                    ZoneType.WORKSHOP -> SkyCyan
                    ZoneType.PARKING -> Color(0xFF94A3B8)
                }
                drawCircle(c, radius = 4.5f, center = pos)
            }
        }

        // Active mission direction indicator on radar edge
        if (activeMission != null) {
            val hubId = if (!missionLoaded) activeMission.originHubId else activeMission.destinationHubId
            val hub = TruckWorld.hubById(hubId)
            if (hub != null) {
                val raw = toRadar(hub.x, hub.z)
                val clamped = Offset(raw.x.coerceIn(6f, w - 6f), raw.y.coerceIn(6f, h - 6f))
                drawCircle(if (missionLoaded) EmeraldGo else AmberSignal, radius = 6f, center = clamped)
            }
        }

        // Player heading arrow at center
        rotate(degrees = Math.toDegrees(telemetry.truckYawRad.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
            drawCircle(RacingOrange, radius = 5.5f, center = Offset(cx, cy))
            drawLine(RacingOrange, Offset(cx, cy), Offset(cx, cy - 11f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}
