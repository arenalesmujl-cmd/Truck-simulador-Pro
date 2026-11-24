package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.TruckCatalog
import com.example.model.TruckModel
import com.example.model.TruckStyle
import com.example.ui.components.ShowroomViewport
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
fun TruckSelectScreen(
    profile: CareerProfile,
    onBuyOrSelect: (TruckModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val currency = NumberFormat.getNumberInstance(Locale.US).format(profile.money)
    var focusedTruckId by remember(profile.selectedTruckId) { mutableStateOf(profile.selectedTruckId) }
    val focusedTruck = TruckCatalog.byId(focusedTruckId)
    val focusedPaintHex = if (focusedTruck.id == profile.selectedTruckId) profile.truckPaintHex else focusedTruck.defaultColorHex

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
                        .testTag("truck_select_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CONCESIONARIO DE FLOTA 3D",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Inspecciona cada tractocamión en tiempo real antes de conducir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Surface(
                color = EmeraldGo.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, EmeraldGo.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "$$currency USD",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldGo,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Live 3D Studio Showroom of Focused Truck
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurface),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ShowroomViewport(
                            truckStyle = focusedTruck.style,
                            paintHex = focusedPaintHex,
                            showDriver = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = SimSurfaceGlass,
                            border = BorderStroke(1.dp, SimOutline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = focusedTruck.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = focusedTruck.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyCyan
                                )
                            }
                        }
                    }

                    // Live Spec Telemetry Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBarRow(
                            label = "Potencia",
                            value = "${focusedTruck.basePowerHp} HP",
                            fraction = focusedTruck.basePowerHp / 900f,
                            color = RacingOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatBarRow(
                            label = "Vel. Máx",
                            value = "${focusedTruck.baseTopSpeedKmh.toInt()} km/h",
                            fraction = focusedTruck.baseTopSpeedKmh / 140f,
                            color = SkyCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatBarRow(
                            label = "Torque",
                            value = "${(focusedTruck.baseTorque * 100).toInt()}%",
                            fraction = focusedTruck.baseTorque / 1.6f,
                            color = EmeraldGo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Right: Fleet Selection & Purchase Cards
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(TruckCatalog.all, key = { it.id }) { truck ->
                    val isOwned = profile.ownedTruckIds.contains(truck.id)
                    val isSelected = profile.selectedTruckId == truck.id
                    val isFocused = focusedTruckId == truck.id
                    val canAfford = profile.money >= truck.price

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusedTruckId = truck.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFocused) SimSurfaceElevated else SimSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected || isFocused) 2.dp else 1.dp,
                            color = when {
                                isSelected -> RacingOrange
                                isFocused -> SkyCyan
                                else -> SimOutline
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalShipping,
                                        contentDescription = null,
                                        tint = if (isSelected) RacingOrange else SkyCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = truck.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Activo",
                                            tint = RacingOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${truck.basePowerHp} HP • ${truck.baseTopSpeedKmh.toInt()} km/h • ${truck.baseTankLiters.toInt()}L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    focusedTruckId = truck.id
                                    onBuyOrSelect(truck)
                                },
                                enabled = !isSelected && (isOwned || canAfford),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSelected -> SimSurface
                                        isOwned -> SkyCyan
                                        else -> RacingOrange
                                    },
                                    contentColor = if (isOwned && !isSelected) SimBackground else TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("truck_action_${truck.id}")
                            ) {
                                val label = when {
                                    isSelected -> "ACTIVO"
                                    isOwned -> "USAR"
                                    else -> "$${NumberFormat.getNumberInstance(Locale.US).format(truck.price)}"
                                }
                                Text(text = label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TruckSilhouettePreview(
    style: TruckStyle,
    cabColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.78f
        drawLine(Color(0xFF334155), Offset(w * 0.04f, baseY + 13f), Offset(w * 0.96f, baseY + 13f), strokeWidth = 3f)
        drawRoundRect(Color(0xFF1E293B), Offset(w * 0.14f, baseY - 12f), Size(w * 0.72f, 13f), CornerRadius(4f, 4f))
        drawRoundRect(cabColor, Offset(w * 0.35f, baseY - 56f), Size(w * 0.28f, 44f), CornerRadius(6f, 6f))
        drawRoundRect(cabColor, Offset(w * 0.60f, baseY - 36f), Size(w * 0.22f, 24f), CornerRadius(4f, 4f))
        for (wx in listOf(w * 0.22f, w * 0.33f, w * 0.75f)) {
            drawCircle(Color(0xFF0F172A), radius = 12f, center = Offset(wx, baseY + 2f))
            drawCircle(Color(0xFFCBD5E1), radius = 6f, center = Offset(wx, baseY + 2f))
        }
    }
}

@Composable
private fun StatBarRow(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.1f, 1f) },
            color = color,
            trackColor = SimSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}
