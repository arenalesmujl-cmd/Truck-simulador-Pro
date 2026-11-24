package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.DeliveryRecord
import com.example.model.MissionCatalog
import com.example.model.MissionContract
import com.example.model.UiState
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

@Composable
fun MissionsScreen(
    profile: CareerProfile,
    deliveriesState: UiState<List<DeliveryRecord>>,
    activeMission: MissionContract?,
    onAcceptMission: (MissionContract) -> Unit,
    onCancelMission: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

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
                            .testTag("missions_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CONTRATOS Y MISIONES",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${profile.completedMissionsCount} entregas completadas • Nivel ${profile.level}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyCyan
                    )
                }
            }
            if (activeMission != null) {
                val cancelInteraction = remember { MutableInteractionSource() }
                val cancelPressed by cancelInteraction.collectIsPressedAsState()
                val cancelScale by animateFloatAsState(
                    targetValue = if (cancelPressed) 0.96f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "cancel_btn_scale"
                )
                Button(
                    onClick = onCancelMission,
                    interactionSource = cancelInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = SimSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .graphicsLayer(scaleX = cancelScale, scaleY = cancelScale)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_mission_button")
                ) {
                    Text("CANCELAR MISIÓN ACTUAL", style = MaterialTheme.typography.labelSmall, color = AmberSignal)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Available Freight Missions
            LazyColumn(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(MissionCatalog.all, key = { it.id }) { mission ->
                    val routeUnlocked = profile.unlockedRouteIds.contains(mission.routeId)
                    val isCurrent = activeMission?.id == mission.id
                    val btnInteraction = remember(mission.id) { MutableInteractionSource() }
                    val btnPressed by btnInteraction.collectIsPressedAsState()
                    val cardScale by animateFloatAsState(
                        targetValue = if (btnPressed) 0.985f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "mission_card_scale"
                    )
                    Card(
                        modifier = Modifier.graphicsLayer(scaleX = cardScale, scaleY = cardScale),
                        colors = CardDefaults.cardColors(containerColor = SimSurface),
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) RacingOrange else SimOutline
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        when {
                                            isCurrent -> listOf(EmeraldGo, RacingOrange)
                                            routeUnlocked -> listOf(RacingOrange, SkyCyan.copy(alpha = 0.6f))
                                            else -> listOf(SimOutline, SimSurfaceElevated)
                                        }
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mission.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = SkyCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${mission.weightTons}t",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCyan,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(mission.cargoName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = "${mission.originName} → ${mission.destinationName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberSignal,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = EmeraldGo.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Pago: +$${mission.rewardMoney} USD • +${mission.rewardXp} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGo,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { onAcceptMission(mission) },
                                enabled = routeUnlocked,
                                interactionSource = btnInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) EmeraldGo else RacingOrange,
                                    disabledContainerColor = SimSurfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("accept_mission_${mission.id}")
                            ) {
                                if (!routeUnlocked) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Bloqueado", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("RUTA BLOQ.", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Text(
                                        text = if (isCurrent) "EN CURSO" else "CONDUCIR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right: Completed Deliveries Log (from Firestore)
            Card(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurfaceGlass),
                border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(SkyCyan, EmeraldGo)))
                )
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldGo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("HISTORIAL DE ENTREGAS", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))

                    when (deliveriesState) {
                        is UiState.Loading -> Text("Sincronizando entregas...", color = TextSecondary)
                        is UiState.Error -> Text(deliveriesState.message, color = AmberSignal)
                        is UiState.Success -> {
                            val list = deliveriesState.data
                            if (list.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Sin entregas registradas aún.\nAcepta una carga y llega al destino.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(list, key = { it.id }) { rec ->
                                        Surface(
                                            color = SimSurfaceElevated,
                                            border = BorderStroke(1.dp, SimOutline.copy(alpha = 0.7f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(11.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(rec.cargoName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                                    Text("+$${rec.rewardMoney}", style = MaterialTheme.typography.labelSmall, color = EmeraldGo)
                                                }
                                                Text("${rec.originName} → ${rec.destinationName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                                Text("Daño: ${rec.damagePercent}%", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
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
    }
}
