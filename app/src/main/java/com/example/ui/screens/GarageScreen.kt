package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.PaintCatalog
import com.example.model.TruckCatalog
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
import com.example.viewmodel.UpgradeType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GarageScreen(
    profile: CareerProfile,
    onSelectPaint: (String) -> Unit,
    onUpgrade: (UpgradeType) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val truck = TruckCatalog.byId(profile.selectedTruckId)
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
                        .testTag("garage_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TALLER Y PERSONALIZACIÓN 3D",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pintura automotriz metalizada y potenciación de alto rendimiento",
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
            // Left: Live 3D Garage Bay + Paint Swatches
            Card(
                modifier = Modifier
                    .weight(0.95f)
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
                            truckStyle = truck.style,
                            paintHex = profile.truckPaintHex,
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
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(truck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val totalHp = truck.basePowerHp + (profile.engineLevel - 1) * 45
                                val topSpeed = (truck.baseTopSpeedKmh * (1f + (profile.engineLevel - 1) * 0.12f)).toInt()
                                val tankL = (truck.baseTankLiters * (1f + (profile.tankLevel - 1) * 0.14f)).toInt()
                                Text(
                                    text = "$totalHp HP • $topSpeed km/h • ${tankL}L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyCyan
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "PINTURA METALIZADA DE CABINA ($500 USD)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PaintCatalog.options.forEach { paint ->
                                val selected = profile.truckPaintHex.equals(paint.hex, ignoreCase = true)
                                val color = Color(android.graphics.Color.parseColor(paint.hex))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) TextPrimary else SimOutline,
                                            shape = CircleShape
                                        )
                                        .minimumInteractiveComponentSize()
                                        .testTag("paint_${paint.hex}")
                                        .clickable { onSelectPaint(paint.hex) }
                                )
                            }
                        }
                    }
                }
            }

            // Right: 4 Component Upgrades with Level Segment Pips
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UpgradeCard(
                    title = "Motor Diésel Turbo",
                    desc = "Potencia y velocidad máxima (+12%/nv)",
                    level = profile.engineLevel,
                    icon = Icons.Filled.Speed,
                    canAfford = profile.money >= profile.engineLevel * 3500L,
                    testTag = "upgrade_engine_button",
                    onUpgrade = { onUpgrade(UpgradeType.ENGINE) }
                )
                UpgradeCard(
                    title = "Caja de Cambios",
                    desc = "Aceleración y par en subidas (+8%/nv)",
                    level = profile.transmissionLevel,
                    icon = Icons.Filled.SettingsInputComponent,
                    canAfford = profile.money >= profile.transmissionLevel * 3500L,
                    testTag = "upgrade_transmission_button",
                    onUpgrade = { onUpgrade(UpgradeType.TRANSMISSION) }
                )
                UpgradeCard(
                    title = "Suspensión Neumática",
                    desc = "Estabilidad y confort en cabina (+10%/nv)",
                    level = profile.suspensionLevel,
                    icon = Icons.Filled.Build,
                    canAfford = profile.money >= profile.suspensionLevel * 3500L,
                    testTag = "upgrade_suspension_button",
                    onUpgrade = { onUpgrade(UpgradeType.SUSPENSION) }
                )
                UpgradeCard(
                    title = "Tanque de Combustible",
                    desc = "Mayor autonomía en rutas largas (+14%/nv)",
                    level = profile.tankLevel,
                    icon = Icons.Filled.LocalGasStation,
                    canAfford = profile.money >= profile.tankLevel * 3500L,
                    testTag = "upgrade_tank_button",
                    onUpgrade = { onUpgrade(UpgradeType.TANK) }
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    desc: String,
    level: Long,
    icon: ImageVector,
    canAfford: Boolean,
    testTag: String,
    onUpgrade: () -> Unit
) {
    val isMax = level >= 5L
    val cost = level * 3500L
    Card(
        colors = CardDefaults.cardColors(containerColor = SimSurface),
        border = BorderStroke(1.dp, SimOutline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = RacingOrange.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = RacingOrange)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("$title • Nv. $level/5", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (pip in 1L..5L) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (pip <= level) RacingOrange else SimSurfaceElevated)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onUpgrade,
                enabled = !isMax && canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMax) SimSurfaceElevated else RacingOrange,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Text(
                    text = if (isMax) "MÁX" else "MEJORAR $$cost",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
