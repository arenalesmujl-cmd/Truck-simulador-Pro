package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MissionContract
import com.example.model.PlayerControlMode
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SpeedometerArcCluster(
    telemetry: TruckTelemetry,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SimSurfaceGlass,
        border = BorderStroke(1.dp, SimOutline),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerR = size.minDimension * 0.45f

                    // Subtle radial instrument backdrop & outer brushed bezel ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0F172A).copy(alpha = 0.72f), Color(0xFF020617).copy(alpha = 0.25f)),
                            center = center,
                            radius = outerR
                        ),
                        radius = outerR,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF475569).copy(alpha = 0.55f),
                        radius = outerR + 3.5f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Precision speedometer dial ticks
                    for (i in 0..14) {
                        val deg = 135f + (i / 14f) * 270f
                        val rad = Math.toRadians(deg.toDouble()).toFloat()
                        val inner = if (i % 2 == 0) outerR * 0.80f else outerR * 0.87f
                        drawLine(
                            color = if (i >= 11) CrimsonAlert.copy(alpha = 0.7f) else Color(0xFF475569),
                            start = Offset(center.x + cos(rad) * inner, center.y + sin(rad) * inner),
                            end = Offset(center.x + cos(rad) * outerR, center.y + sin(rad) * outerR),
                            strokeWidth = if (i % 2 == 0) 2.5f else 1.5f
                        )
                    }

                    // Speedometer outer track
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    val speedFrac = (telemetry.speedKmh / 140f).coerceIn(0f, 1f)
                    val speedSweep = speedFrac * 270f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(SkyCyan, RacingOrange, CrimsonAlert)),
                        startAngle = 135f,
                        sweepAngle = speedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Glowing Analog Speed Needle
                    val needleRad = Math.toRadians((135f + speedSweep).toDouble()).toFloat()
                    drawLine(
                        color = RacingOrange,
                        start = Offset(center.x + cos(needleRad) * (outerR * 0.36f), center.y + sin(needleRad) * (outerR * 0.36f)),
                        end = Offset(center.x + cos(needleRad) * (outerR * 0.94f), center.y + sin(needleRad) * (outerR * 0.94f)),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )

                    // Inner RPM arc
                    val innerInset = 20f
                    val innerSize = Size(size.width - innerInset * 2f, size.height - innerInset * 2f)
                    val innerTopLeft = Offset(innerInset, innerInset)
                    drawArc(
                        color = Color(0xFF0F172A),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerSize,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                    val rpmSweep = ((telemetry.rpm / 2450f).coerceIn(0f, 1f)) * 270f
                    drawArc(
                        color = if (telemetry.rpm > 2050f) CrimsonAlert else EmeraldGo,
                        startAngle = 135f,
                        sweepAngle = rpmSweep,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerSize,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${telemetry.speedKmh.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text("KM/H", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.End
            ) {
                val gearLabel = when (telemetry.gear) {
                    -1 -> "R"
                    0 -> "N"
                    else -> "D${telemetry.gear}"
                }
                Surface(
                    color = if (telemetry.gear == -1) CrimsonAlert.copy(alpha = 0.25f) else RacingOrange.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, if (telemetry.gear == -1) CrimsonAlert else RacingOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = gearLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (telemetry.gear == -1) CrimsonAlert else RacingOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }

                val fuelColor = if (telemetry.fuelPercent < 20f) CrimsonAlert else EmeraldGo
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = "Combustible", tint = fuelColor, modifier = Modifier.size(13.dp))
                        Text("${telemetry.fuelPercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = fuelColor)
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SimSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .width((46f * (telemetry.fuelPercent / 100f).coerceIn(0f, 1f)).dp)
                                .height(3.dp)
                                .background(fuelColor)
                        )
                    }
                }
                val dmgColor = if (telemetry.damagePercent > 40f) AmberSignal else TextSecondary
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Build, contentDescription = "Daño", tint = dmgColor, modifier = Modifier.size(13.dp))
                        Text("${telemetry.damagePercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = dmgColor)
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SimSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .width((46f * ((100f - telemetry.damagePercent) / 100f).coerceIn(0f, 1f)).dp)
                                .height(3.dp)
                                .background(if (telemetry.damagePercent > 40f) AmberSignal else SkyCyan)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnFootAnalogJoystick(
    onMoveChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SimSurfaceGlass)
            .border(1.5.dp, SkyCyan.copy(alpha = 0.65f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMoveChange(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMoveChange(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val maxRadius = (size.width.coerceAtMost(size.height) * 0.34f).coerceAtLeast(1f)
                        val candidate = knobOffset + dragAmount
                        val dist = hypot(candidate.x, candidate.y)
                        val clamped = if (dist > maxRadius) {
                            Offset(candidate.x / dist * maxRadius, candidate.y / dist * maxRadius)
                        } else {
                            candidate
                        }
                        knobOffset = clamped
                        onMoveChange(
                            (clamped.x / maxRadius).coerceIn(-1f, 1f),
                            (-clamped.y / maxRadius).coerceIn(-1f, 1f)
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.40f
            drawCircle(color = Color(0xFF334155), radius = outerR, center = c, style = Stroke(width = 2.5f))
            drawLine(Color(0xFF1E293B), Offset(c.x - outerR * 0.6f, c.y), Offset(c.x + outerR * 0.6f, c.y), strokeWidth = 1.5f)
            drawLine(Color(0xFF1E293B), Offset(c.x, c.y - outerR * 0.6f), Offset(c.x, c.y + outerR * 0.6f), strokeWidth = 1.5f)
            drawCircle(color = SkyCyan.copy(alpha = 0.22f), radius = 30f, center = c + knobOffset)
            drawCircle(color = SkyCyan, radius = 22f, center = c + knobOffset)
            drawCircle(color = TextPrimary, radius = 8f, center = c + knobOffset)
        }
    }
}

@Composable
fun ZoneActionBanner(
    telemetry: TruckTelemetry,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    onLoadCargo: () -> Unit,
    onDeliverCargo: () -> Unit,
    onRefuel: () -> Unit,
    onRepair: () -> Unit,
    onTowToRoad: () -> Unit
) {
    if (telemetry.engineStalled && telemetry.controlMode == PlayerControlMode.IN_TRUCK) {
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
fun TactileSteeringWheel(
    steerValue: Float,
    onSteerChange: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(SimSurfaceGlass)
            .border(1.5.dp, SimOutline, CircleShape)
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
                // Dark stitched leather rim & subtle metallic outer highlight
                drawCircle(color = Color(0xFF0F172A), radius = r, center = c, style = Stroke(width = 22f))
                drawCircle(color = Color(0xFF1E293B), radius = r, center = c, style = Stroke(width = 16f))
                drawCircle(color = Color(0xFF64748B), radius = r + 5f, center = c, style = Stroke(width = 2.5f))
                // Thumb grip bolsters at 9 & 3 o'clock
                drawCircle(color = Color(0xFF334155), radius = 11f, center = Offset(c.x - r + 2f, c.y))
                drawCircle(color = Color(0xFF334155), radius = 11f, center = Offset(c.x + r - 2f, c.y))
                // Brushed aluminum spokes
                drawLine(Color(0xFF94A3B8), Offset(c.x - r + 8f, c.y), Offset(c.x + r - 8f, c.y), strokeWidth = 12f, cap = StrokeCap.Round)
                drawLine(Color(0xFFE2E8F0), Offset(c.x - r + 10f, c.y - 1.5f), Offset(c.x + r - 10f, c.y - 1.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(Color(0xFF94A3B8), Offset(c.x, c.y), Offset(c.x, c.y + r - 8f), strokeWidth = 12f, cap = StrokeCap.Round)
                // Center horn boss with metallic trim, orange racing badge & center gloss pin
                drawCircle(Color(0xFF0F172A), radius = 24f, center = c)
                drawCircle(Color(0xFF94A3B8), radius = 24f, center = c, style = Stroke(width = 2.5f))
                drawCircle(RacingOrange, radius = 13f, center = c)
                drawCircle(Color(0xFFFFD54F), radius = 5f, center = c)
                // Top dead center stripe
                drawLine(
                    color = RacingOrange,
                    start = Offset(c.x, c.y - r - 8f),
                    end = Offset(c.x, c.y - r + 8f),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun HoldButtonPad(
    label: String,
    icon: ImageVector?,
    held: Boolean,
    accent: Color,
    testTag: String,
    onHeldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        border = BorderStroke(2.dp, if (held) accent else accent.copy(alpha = 0.68f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .background(
                Brush.verticalGradient(
                    colors = if (held) {
                        listOf(accent.copy(alpha = 0.96f), accent.copy(alpha = 0.76f))
                    } else {
                        listOf(Color(0xFF1E293B).copy(alpha = 0.88f), Color(0xFF0B1220).copy(alpha = 0.92f))
                    }
                ),
                RoundedCornerShape(14.dp)
            )
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle ribbed pedal traction bars behind content
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                val barCount = 5
                val step = size.height / (barCount + 1)
                val barColor = if (held) Color.Black.copy(alpha = 0.24f) else accent.copy(alpha = 0.16f)
                for (i in 1..barCount) {
                    val y = step * i
                    drawLine(
                        color = barColor,
                        start = Offset(4f, y),
                        end = Offset(size.width - 4f, y),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = label, tint = if (held) SimBackground else TextPrimary, modifier = Modifier.size(26.dp))
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
}

@Composable
fun HudToggleChip(
    label: String? = null,
    icon: ImageVector? = null,
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
fun MiniMapRadar(
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
        val scale = w / 460f

        // Dark radial GPS backdrop, concentric radar range rings & cardinal ticks
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0F172A).copy(alpha = 0.78f), Color(0xFF020617).copy(alpha = 0.45f)),
                center = Offset(cx, cy),
                radius = w * 0.48f
            ),
            radius = w * 0.46f,
            center = Offset(cx, cy)
        )
        drawCircle(Color(0xFF334155).copy(alpha = 0.72f), radius = w * 0.45f, center = Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(Color(0xFF1E293B), radius = w * 0.24f, center = Offset(cx, cy), style = Stroke(1f))
        drawLine(Color(0xFF334155), Offset(cx, 4f), Offset(cx, 12f), strokeWidth = 2f)
        drawLine(Color(0xFF334155), Offset(cx, h - 12f), Offset(cx, h - 4f), strokeWidth = 2f)
        drawLine(Color(0xFF334155), Offset(4f, cy), Offset(12f, cy), strokeWidth = 2f)
        drawLine(Color(0xFF334155), Offset(w - 12f, cy), Offset(w - 4f, cy), strokeWidth = 2f)

        val refX = if (telemetry.controlMode == PlayerControlMode.ON_FOOT) telemetry.driverX else telemetry.truckX
        val refZ = if (telemetry.controlMode == PlayerControlMode.ON_FOOT) telemetry.driverZ else telemetry.truckZ

        fun toRadar(wx: Float, wz: Float): Offset = Offset(
            x = cx + (wx - refX) * scale,
            y = cy + (wz - refZ) * scale
        )

        for (road in TruckWorld.roads) {
            val s = toRadar(road.x1, road.z1)
            val e = toRadar(road.x2, road.z2)
            drawLine(color = Color(0xFF334155), start = s, end = e, strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFF64748B), start = s, end = e, strokeWidth = 2.5f, cap = StrokeCap.Round)
        }

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

        if (activeMission != null) {
            val hubId = if (!missionLoaded) activeMission.originHubId else activeMission.destinationHubId
            val hub = TruckWorld.hubById(hubId)
            if (hub != null) {
                val raw = toRadar(hub.x, hub.z)
                val clamped = Offset(raw.x.coerceIn(6f, w - 6f), raw.y.coerceIn(6f, h - 6f))
                val targetCol = if (missionLoaded) EmeraldGo else AmberSignal
                drawLine(targetCol.copy(alpha = 0.55f), Offset(cx, cy), clamped, strokeWidth = 2.5f)
                drawCircle(targetCol.copy(alpha = 0.25f), radius = 10f, center = clamped)
                drawCircle(targetCol, radius = 5.5f, center = clamped)
            }
        }

        if (telemetry.controlMode == PlayerControlMode.ON_FOOT) {
            val truckPos = toRadar(telemetry.truckX, telemetry.truckZ)
            drawCircle(RacingOrange, radius = 6f, center = Offset(truckPos.x.coerceIn(6f, w - 6f), truckPos.y.coerceIn(6f, h - 6f)))
            rotate(degrees = Math.toDegrees(telemetry.driverYawRad.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
                drawArc(SkyCyan.copy(alpha = 0.22f), startAngle = -115f, sweepAngle = 50f, useCenter = true, topLeft = Offset(cx - 22f, cy - 22f), size = Size(44f, 44f))
                drawCircle(SkyCyan, radius = 4.5f, center = Offset(cx, cy))
                drawLine(SkyCyan, Offset(cx, cy), Offset(cx, cy - 10f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        } else {
            rotate(degrees = Math.toDegrees(telemetry.truckYawRad.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
                drawArc(RacingOrange.copy(alpha = 0.22f), startAngle = -115f, sweepAngle = 50f, useCenter = true, topLeft = Offset(cx - 24f, cy - 24f), size = Size(48f, 48f))
                drawCircle(RacingOrange, radius = 5.5f, center = Offset(cx, cy))
                drawLine(RacingOrange, Offset(cx, cy), Offset(cx, cy - 11f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
    }
}
