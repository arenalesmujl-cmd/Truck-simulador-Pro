package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(SimBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MAPA DEL MUNDO Y RUTAS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = EmeraldGo.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldGo.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$$currency USD • Nv.${profile.level}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldGo,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Button(
                    onClick = onPlayFromMap,
                    colors = ButtonDefaults.buttonColors(containerColor = RacingOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("map_drive_button")
                ) {
                    Text("CONDUCIR", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Live Vector World Map
            Card(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurface),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    WorldMapCanvas(
                        telemetry = telemetry,
                        unlockedRoutes = profile.unlockedRouteIds,
                        textMeasurer = textMeasurer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(Modifier.height(6.dp))
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

            // Right: Unlockable Routes
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(RouteCatalog.all, key = { it.id }) { route ->
                    val unlocked = profile.unlockedRouteIds.contains(route.id)
                    val canUnlock = profile.level >= route.requiredLevel && profile.money >= route.unlockCost
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SimSurface),
                        border = BorderStroke(1.dp, if (unlocked) EmeraldGo.copy(alpha = 0.5f) else SimOutline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(route.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                if (unlocked) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Desbloqueada", tint = EmeraldGo, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(route.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            if (!unlocked) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onUnlockRoute(route) },
                                    enabled = canUnlock,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RacingOrange,
                                        disabledContainerColor = SimSurfaceElevated
                                    ),
                                    shape = RoundedCornerShape(8.dp),
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

@Composable
private fun WorldMapCanvas(
    telemetry: TruckTelemetry,
    unlockedRoutes: List<String>,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color(0xFF0A121E), RoundedCornerShape(10.dp))) {
        val w = size.width
        val h = size.height
        val scale = minOf(w, h) / 2100f
        fun toMap(wx: Float, wz: Float): Offset = Offset(
            x = w * 0.5f + wx * scale,
            y = h * 0.5f + wz * scale
        )

        // Roads
        for (road in TruckWorld.roads) {
            val isUnlocked = unlockedRoutes.contains(road.routeId)
            drawLine(
                color = if (isUnlocked) {
                    if (road.isHighway) Color(0xFF475569) else Color(0xFF334155)
                } else {
                    Color(0xFF1E293B)
                },
                start = toMap(road.x1, road.z1),
                end = toMap(road.x2, road.z2),
                strokeWidth = if (road.isHighway) 8f else 5f,
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
            drawCircle(c, radius = 7f, center = toMap(zone.x, zone.z))
        }

        // City Labels
        for (city in TruckWorld.cities) {
            val pos = toMap(city.x, city.z)
            drawText(
                textMeasurer = textMeasurer,
                text = city.name,
                topLeft = Offset(pos.x - 42f, pos.y - 24f),
                style = TextStyle(color = TextPrimary, fontSize = 11.sp)
            )
        }

        // Player Truck Arrow
        val pPos = toMap(telemetry.truckX, telemetry.truckZ)
        rotate(degrees = Math.toDegrees(telemetry.truckYawRad.toDouble()).toFloat(), pivot = pPos) {
            drawCircle(RacingOrange.copy(alpha = 0.28f), radius = 16f, center = pPos)
            drawCircle(RacingOrange, radius = 8f, center = pPos)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
