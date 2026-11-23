package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.TruckCatalog
import com.example.model.TruckModel
import com.example.model.TruckStyle
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
fun TruckSelectScreen(
    profile: CareerProfile,
    onBuyOrSelect: (TruckModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
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
                        .testTag("truck_select_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SELECCIONAR CAMIÓN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
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

        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            items(TruckCatalog.all, key = { it.id }) { truck ->
                val isOwned = profile.ownedTruckIds.contains(truck.id)
                val isSelected = profile.selectedTruckId == truck.id
                val paintHex = if (isSelected) profile.truckPaintHex else truck.defaultColorHex
                val cabColor = Color(android.graphics.Color.parseColor(paintHex))

                Card(
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = SimSurface),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) RacingOrange else SimOutline
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = truck.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Activo",
                                        tint = RacingOrange
                                    )
                                }
                            }
                            Text(
                                text = truck.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1
                            )

                            Spacer(Modifier.height(8.dp))
                            TruckSilhouettePreview(
                                style = truck.style,
                                cabColor = cabColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                            )
                            Spacer(Modifier.height(8.dp))

                            StatBarRow("Potencia", "${truck.basePowerHp} HP", truck.basePowerHp / 900f, RacingOrange)
                            StatBarRow("Vel. Máx", "${truck.baseTopSpeedKmh.toInt()} km/h", truck.baseTopSpeedKmh / 140f, SkyCyan)
                            StatBarRow("Torque", "${(truck.baseTorque * 100).toInt()}%", truck.baseTorque / 1.6f, EmeraldGo)
                        }

                        val canAfford = profile.money >= truck.price
                        Button(
                            onClick = { onBuyOrSelect(truck) },
                            enabled = !isSelected && (isOwned || canAfford),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSelected -> SimSurfaceElevated
                                    isOwned -> SkyCyan
                                    else -> RacingOrange
                                },
                                contentColor = if (isOwned && !isSelected) SimBackground else TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("truck_action_${truck.id}")
                        ) {
                            val label = when {
                                isSelected -> "ACTIVO"
                                isOwned -> "SELECCIONAR"
                                else -> "COMPRAR ($${NumberFormat.getNumberInstance(Locale.US).format(truck.price)})"
                            }
                            Text(text = label, style = MaterialTheme.typography.labelLarge)
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

        // Road floor
        drawLine(
            color = Color(0xFF334155),
            start = Offset(w * 0.05f, baseY + 12f),
            end = Offset(w * 0.95f, baseY + 12f),
            strokeWidth = 3f
        )

        // Chassis
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(w * 0.16f, baseY - 12f),
            size = Size(w * 0.68f, 14f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        when (style) {
            TruckStyle.AMERICAN_LONGNOSE -> {
                // Sleeper cab
                drawRoundRect(cabColor, Offset(w * 0.34f, baseY - 56f), Size(w * 0.28f, 46f), CornerRadius(6f, 6f))
                // Long nose hood
                drawRoundRect(cabColor, Offset(w * 0.60f, baseY - 38f), Size(w * 0.22f, 28f), CornerRadius(5f, 5f))
                // Exhaust stack
                drawRect(Color(0xFFCBD5E1), Offset(w * 0.32f, baseY - 66f), Size(5f, 48f))
            }
            TruckStyle.EURO_CABOVER -> {
                drawRoundRect(cabColor, Offset(w * 0.48f, baseY - 60f), Size(w * 0.32f, 50f), CornerRadius(7f, 7f))
                drawRect(Color(0xFF0F172A), Offset(w * 0.74f, baseY - 50f), Size(w * 0.05f, 20f))
            }
            TruckStyle.HEAVY_TWIN_EXHAUST -> {
                drawRoundRect(cabColor, Offset(w * 0.36f, baseY - 58f), Size(w * 0.28f, 48f), CornerRadius(6f, 6f))
                drawRoundRect(cabColor, Offset(w * 0.62f, baseY - 40f), Size(w * 0.20f, 30f), CornerRadius(4f, 4f))
                drawRect(Color(0xFFE2E8F0), Offset(w * 0.31f, baseY - 68f), Size(6f, 52f))
            }
            TruckStyle.ARCTIC_EXPEDITION -> {
                drawRoundRect(cabColor, Offset(w * 0.36f, baseY - 60f), Size(w * 0.30f, 50f), CornerRadius(6f, 6f))
                drawRoundRect(cabColor, Offset(w * 0.64f, baseY - 40f), Size(w * 0.19f, 30f), CornerRadius(4f, 4f))
                drawRect(Color(0xFFF59E0B), Offset(w * 0.44f, baseY - 66f), Size(w * 0.18f, 6f))
            }
        }

        // Wheels
        val wheelXs = listOf(w * 0.24f, w * 0.35f, w * 0.74f)
        for (wx in wheelXs) {
            drawCircle(Color(0xFF0F172A), radius = 13f, center = Offset(wx, baseY + 2f))
            drawCircle(Color(0xFF94A3B8), radius = 6.5f, center = Offset(wx, baseY + 2f))
        }
    }
}

@Composable
private fun StatBarRow(label: String, value: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(2.dp))
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
