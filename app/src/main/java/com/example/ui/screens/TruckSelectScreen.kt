package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.TruckCatalog
import com.example.model.TruckModel
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
fun TruckSelectScreen(
    profile: CareerProfile,
    onBuyOrSelect: (TruckModel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val currency = formatter.format(profile.money)
    var focusedTruckId by remember(profile.selectedTruckId) { mutableStateOf(profile.selectedTruckId) }
    val focusedIndex = remember(focusedTruckId) {
        TruckCatalog.all.indexOfFirst { it.id == focusedTruckId }.coerceAtLeast(0)
    }
    val focusedTruck = TruckCatalog.all[focusedIndex]
    val focusedOwned = profile.ownedTruckIds.contains(focusedTruck.id)
    val focusedSelected = profile.selectedTruckId == focusedTruck.id
    val focusedCanAfford = profile.money >= focusedTruck.price

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070D19), SimBackground, Color(0xFF050912))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        // Header bar
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
                            .testTag("truck_select_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TIENDA DE CAMIONES",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paquetes especiales de flota — 20 modelos coleccionables",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SkyCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${profile.ownedTruckIds.size}/${TruckCatalog.all.size} EN FLOTA",
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Pure 2D Illustrated Truck Pack Showcase (no 3D)
            Card(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1324)),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (focusedSelected) RacingOrange.copy(alpha = 0.72f) else SkyCyan.copy(alpha = 0.48f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(RacingOrange, AmberSignal, SkyCyan)))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Crisp 2D truck showcase image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF050A14)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = focusedTruck.drawableRes),
                            contentDescription = focusedTruck.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = Color(0xCC060B16),
                            border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "PAQUETE #${focusedIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = when {
                                focusedSelected -> RacingOrange.copy(alpha = 0.92f)
                                focusedOwned -> EmeraldGo.copy(alpha = 0.90f)
                                else -> Color(0xCC060B16)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    focusedSelected -> RacingOrange
                                    focusedOwned -> EmeraldGo
                                    else -> AmberSignal.copy(alpha = 0.75f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = when {
                                    focusedSelected -> "ACTIVO"
                                    focusedOwned -> "EN FLOTA"
                                    else -> "$${formatter.format(focusedTruck.price)} USD"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (focusedOwned || focusedSelected) Color(0xFF041016) else AmberSignal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = focusedTruck.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = focusedTruck.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Spec bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            fraction = focusedTruck.baseTopSpeedKmh / 145f,
                            color = SkyCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatBarRow(
                            label = "Tanque",
                            value = "${focusedTruck.baseTankLiters.toInt()}L",
                            fraction = focusedTruck.baseTankLiters / 820f,
                            color = EmeraldGo,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { onBuyOrSelect(focusedTruck) },
                        enabled = !focusedSelected && (focusedOwned || focusedCanAfford),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                focusedSelected -> SimSurfaceElevated
                                focusedOwned -> SkyCyan
                                else -> RacingOrange
                            },
                            contentColor = if (focusedOwned && !focusedSelected) SimBackground else TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("truck_showcase_action")
                    ) {
                        Icon(
                            imageVector = if (focusedOwned) Icons.Filled.CheckCircle else Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                focusedSelected -> "CAMIÓN EQUIPADO EN RUTA"
                                focusedOwned -> "EQUIPAR ${focusedTruck.name.uppercase()}"
                                focusedCanAfford -> "COMPRAR POR $${formatter.format(focusedTruck.price)} USD"
                                else -> "FALTAN $${formatter.format(focusedTruck.price - profile.money)} USD"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: 20 Illustrated 2D Truck Packs Store Grid (2 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(TruckCatalog.all, key = { _, truck -> truck.id }) { index, truck ->
                    val isOwned = profile.ownedTruckIds.contains(truck.id)
                    val isSelected = profile.selectedTruckId == truck.id
                    val isFocused = focusedTruckId == truck.id
                    val canAfford = profile.money >= truck.price

                    val cardInteraction = remember { MutableInteractionSource() }
                    val cardPressed by cardInteraction.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue = if (cardPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "truckPackScale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = LocalIndication.current
                            ) { focusedTruckId = truck.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFocused) SimSurfaceElevated else SimSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected || isFocused) 2.dp else 1.dp,
                            color = when {
                                isSelected -> RacingOrange
                                isFocused -> SkyCyan
                                isOwned -> EmeraldGo.copy(alpha = 0.55f)
                                else -> SimOutline
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.20f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF050A14))
                            ) {
                                Image(
                                    painter = painterResource(id = truck.drawableRes),
                                    contentDescription = truck.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color(0xBB060B16),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Activo",
                                        tint = RacingOrange,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = truck.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${truck.basePowerHp} HP • ${truck.baseTopSpeedKmh.toInt()} km/h",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    focusedTruckId = truck.id
                                    onBuyOrSelect(truck)
                                },
                                enabled = !isSelected && (isOwned || canAfford),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSelected -> Color(0xFF172133)
                                        isOwned -> SkyCyan
                                        else -> RacingOrange
                                    },
                                    contentColor = if (isOwned && !isSelected) SimBackground else TextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("truck_action_${truck.id}")
                            ) {
                                val label = when {
                                    isSelected -> "ACTIVO"
                                    isOwned -> "USAR"
                                    truck.price == 0L -> "GRATIS"
                                    else -> "$${formatter.format(truck.price)}"
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
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
        Spacer(Modifier.height(4.dp))
        val clamped = fraction.coerceIn(0.1f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SimSurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.65f), color)))
            )
        }
    }
}
