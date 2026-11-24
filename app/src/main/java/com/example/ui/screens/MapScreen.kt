package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.RouteCatalog
import com.example.model.RouteInfo
import com.example.sim.TruckTelemetry
import com.example.sim.TruckWorld
import com.example.sim.ZoneType
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurface
import com.example.ui.theme.SimSurfaceElevated
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MapScreen(
    profile: CareerProfile,
    telemetry: TruckTelemetry,
    onUnlockRoute: (RouteInfo) -> Unit,
    onPlayFromMap: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val textMeasurer = rememberTextMeasurer()
    val currency = NumberFormat.getNumberInstance(Locale.US).format(profile.money)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070B12), SimBackground, Color(0xFF0E1625))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = SimSurfaceGlass,
                    border = BorderStroke(1.dp, SimOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MAPA DEL MUNDO Y RUTAS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Explora autopistas, gasolineras, talleres y puertos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyCyan
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = EmeraldGo.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldGo.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$$currency USD • Nv.${profile.level}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldGo,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                Button(
                    onClick = onPlayFromMap,
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("map_drive_button")
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CONDUCIR", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Live Tactical GPS Vector World Map
            Card(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurfaceGlass),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Brush.horizontalGradient(listOf(SkyCyan, RacingOrange)))
                    )
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Map, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SATÉLITE EN VIVO", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
                            }
                            Text(
                                text = "Pos: (${telemetry.truckX.toInt()}m, ${telemetry.truckZ.toInt()}m)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        WorldMapCanvas(
                            telemetry = telemetry,
                            unlockedRoutes = profile.unlockedRouteIds,
                            textMeasurer = textMeasurer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendDot(EmeraldGo, "Carga")
                            LegendDot(AmberSignal, "Gasolinera")
                            LegendDot(SkyCyan, "Taller")
                            LegendDot(Color(0xFF94A3B8), "Parking")
                            LegendDot(RacingOrange, "Tu Camión")
                        }
                    }
                }
            }

            // Right: Unlockable Routes
            LazyColumn(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(RouteCatalog.all, key = { it.id }) { route ->
                    val unlocked = profile.unlockedRouteIds.contains(route.id)
                    val canUnlock = profile.level >= route.requiredLevel && profile.money >= route.unlockCost
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SimSurface),
                        border = BorderStroke(
                            width = if (unlocked) 1.5.dp else 1.dp,
                            color = if (unlocked) EmeraldGo.copy(alpha = 0.55f) else SimOutline
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(
                                        if (unlocked) {
                                            Brush.horizontalGradient(listOf(EmeraldGo, SkyCyan))
                                        } else {
                                            Brush.horizontalGradient(listOf(RacingOrange, AmberSignal))
                                        }
                                    )
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(route.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    if (unlocked) {
                                        Surface(
                                            color = EmeraldGo.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = "Desbloqueada", tint = EmeraldGo, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("ACTIVA", style = MaterialTheme.typography.labelSmall, color = EmeraldGo)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(route.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                if (!unlocked) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { onUnlockRoute(route) },
                                        enabled = canUnlock,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RacingOrange,
                                            disabledContainerColor = SimSurfaceElevated
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("unlock_route_${route.id}")
                                    ) {
                                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "DESBLOQUEAR $${route.unlockCost} (Nv.${route.requiredLevel})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldMapCanvas(
    telemetry: TruckTelemetry,
    unlockedRoutes: List<String>,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    val cityLabelStyle = MaterialTheme.typography.labelSmall.copy(
        color = TextPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Canvas(modifier = modifier.background(Color(0xFF07101C), RoundedCornerShape(12.dp))) {
        val w = size.width
        val h = size.height
        val scale = minOf(w, h) / 2150f
        fun toMap(wx: Float, wz: Float): Offset = Offset(
            x = w * 0.5f + wx * scale,
            y = h * 0.5f + wz * scale
        )

        // Coastal Bay & Alpine Sierra geographic regions with glowing contour boundaries
        val oceanTopLeft = toMap(840f, -1080f)
        val oceanBot = toMap(840f, -150f).y
        drawRect(
            color = Color(0xFF0A2A48),
            topLeft = oceanTopLeft,
            size = Size((w - oceanTopLeft.x).coerceAtLeast(0f), (oceanBot - oceanTopLeft.y).coerceAtLeast(0f))
        )
        drawLine(
            color = SkyCyan.copy(alpha = 0.28f),
            start = Offset(oceanTopLeft.x, 0f),
            end = Offset(oceanTopLeft.x, oceanBot),
            strokeWidth = 2f
        )
        val sierraTop = toMap(-1050f, 480f)
        drawRect(
            color = Color(0xFF112329),
            topLeft = Offset(0f, sierraTop.y),
            size = Size(w, (h - sierraTop.y).coerceAtLeast(0f))
        )
        drawLine(
            color = EmeraldGo.copy(alpha = 0.22f),
            start = Offset(0f, sierraTop.y),
            end = Offset(w, sierraTop.y),
            strokeWidth = 1.5f
        )

        // Subtle tactical grid lines
        for (g in -800..800 step 400) {
            val gx = toMap(g.toFloat(), 0f).x
            val gy = toMap(0f, g.toFloat()).y
            drawLine(Color(0xFF162436), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
            drawLine(Color(0xFF162436), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        }

        // Roads with outer glow casing + inner surface
        for (road in TruckWorld.roads) {
            val isUnlocked = unlockedRoutes.contains(road.routeId)
            val p1 = toMap(road.x1, road.z1)
            val p2 = toMap(road.x2, road.z2)
            drawLine(
                color = if (isUnlocked) Color(0xFF0F172A) else Color(0xFF151E2E),
                start = p1,
                end = p2,
                strokeWidth = if (road.isHighway) 12f else 7.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = if (isUnlocked) {
                    if (road.isHighway) Color(0xFF64748B) else Color(0xFF475569)
                } else {
                    Color(0xFF1E293B)
                },
                start = p1,
                end = p2,
                strokeWidth = if (road.isHighway) 7.5f else 4.5f,
                cap = StrokeCap.Round
            )
        }

        // Zones
        for (zone in TruckWorld.zones) {
            val c = when (zone.type) {
                ZoneType.CARGO_HUB -> EmeraldGo
                ZoneType.GAS_STATION -> AmberSignal
                ZoneType.WORKSHOP -> SkyCyan
                ZoneType.PARKING -> Color(0xFF94A3B8)
            }
            val pos = toMap(zone.x, zone.z)
            drawCircle(c.copy(alpha = 0.24f), radius = 11f, center = pos)
            drawCircle(c, radius = 6.5f, center = pos)
        }

        // City Labels + zone ring
        for (city in TruckWorld.cities) {
            val pos = toMap(city.x, city.z)
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.22f), radius = city.radius * scale, center = pos, style = Stroke(width = 1.6f))
            drawCircle(SkyCyan.copy(alpha = 0.10f), radius = city.radius * scale * 0.65f, center = pos)
            drawText(
                textMeasurer = textMeasurer,
                text = city.name,
                topLeft = Offset(pos.x - 44f, pos.y - 26f),
                style = cityLabelStyle
            )
        }

        // Player Truck Directional Chevron
        val pPos = toMap(telemetry.truckX, telemetry.truckZ)
        rotate(degrees = Math.toDegrees(telemetry.truckYawRad.toDouble()).toFloat(), pivot = pPos) {
            drawCircle(RacingOrange.copy(alpha = 0.25f), radius = 19f, center = pPos)
            drawCircle(RacingOrange.copy(alpha = 0.45f), radius = 19f, center = pPos, style = Stroke(width = 1.4f))
            val path = Path().apply {
                moveTo(pPos.x, pPos.y + 11f)
                lineTo(pPos.x - 8f, pPos.y - 8f)
                lineTo(pPos.x, pPos.y - 4f)
                lineTo(pPos.x + 8f, pPos.y - 8f)
                close()
            }
            drawPath(path, color = RacingOrange)
            drawPath(path, color = TextPrimary.copy(alpha = 0.85f), style = Stroke(width = 1.6f))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
