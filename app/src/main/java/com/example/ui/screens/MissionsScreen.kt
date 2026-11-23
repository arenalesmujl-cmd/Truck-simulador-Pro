package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
                        .testTag("missions_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("CONTRATOS Y MISIONES", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            if (activeMission != null) {
                Button(
                    onClick = onCancelMission,
                    colors = ButtonDefaults.buttonColors(containerColor = SimSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_mission_button")
                ) {
                    Text("CANCELAR MISIÓN ACTUAL", style = MaterialTheme.typography.labelSmall, color = AmberSignal)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SimSurface),
                        border = BorderStroke(1.dp, if (isCurrent) RacingOrange else SimOutline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mission.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = SkyCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${mission.weightTons}t",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("${mission.cargoName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = "${mission.originName} → ${mission.destinationName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberSignal
                                )
                                Text(
                                    text = "Pago: +$${mission.rewardMoney} USD • +${mission.rewardXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGo
                                )
                            }

                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { onAcceptMission(mission) },
                                enabled = routeUnlocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) EmeraldGo else RacingOrange,
                                    disabledContainerColor = SimSurfaceElevated
                                ),
                                shape = RoundedCornerShape(10.dp),
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
                                        style = MaterialTheme.typography.labelSmall
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
                colors = CardDefaults.cardColors(containerColor = SimSurface),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Text("HISTORIAL DE ENTREGAS", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))

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
                                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(6.dp))
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
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(rec.cargoName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
