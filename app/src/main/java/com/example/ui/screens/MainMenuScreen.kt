package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppScreen
import com.example.model.CareerProfile
import com.example.model.MissionContract
import com.example.model.TruckCatalog
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurfaceElevated
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MainMenuScreen(
    profile: CareerProfile,
    activeMission: MissionContract?,
    onPlayClicked: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onSignOutClicked: () -> Unit
) {
    val activeTruck = TruckCatalog.byId(profile.selectedTruckId)
    val currency = NumberFormat.getNumberInstance(Locale.US).format(profile.money)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_menu_backdrop_1795474993705),
            contentDescription = "Fondo Truck Simulator Pro",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SimBackground.copy(alpha = 0.88f),
                            SimBackground.copy(alpha = 0.65f),
                            SimBackground.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${profile.driverName} • Nivel ${profile.level} (${profile.xp} XP)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyCyan
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
                            text = "$$currency USD",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldGo,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        color = SimSurfaceGlass,
                        border = BorderStroke(1.dp, SimOutline),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${profile.completedMissionsCount} Entregas",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }

                    IconButton(
                        onClick = onSignOutClicked,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Center-Bottom Action Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Primary JUGAR + Active Rig Card
                Card(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = SimSurfaceGlass),
                    border = BorderStroke(1.dp, SimOutline),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CAMIÓN ACTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = RacingOrange
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = activeTruck.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${activeTruck.basePowerHp + (profile.engineLevel - 1) * 45} HP • Motor Nv.${profile.engineLevel} • Tanque Nv.${profile.tankLevel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = if (activeMission != null) AmberSignal.copy(alpha = 0.16f) else SimSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (activeMission != null) "MISIÓN ACTIVA: ${activeMission.title}" else "MODO LIBRE DISPONIBLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (activeMission != null) AmberSignal else SkyCyan
                                    )
                                    Text(
                                        text = if (activeMission != null) {
                                            "${activeMission.originName} → ${activeMission.destinationName}"
                                        } else {
                                            "Explora autopistas, ciudades, gasolineras y puertos en 3D"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onPlayClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RacingOrange,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("menu_play_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.menu_play),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right 5 Navigation Cards (2 rows)
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MenuTileCard(
                            title = stringResource(R.string.menu_trucks),
                            subtitle = "${profile.ownedTruckIds.size}/${TruckCatalog.all.size} en flota",
                            icon = Icons.Filled.LocalShipping,
                            accent = RacingOrange,
                            testTag = "menu_trucks_button",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onNavigate(AppScreen.TRUCK_SELECT) }
                        )
                        MenuTileCard(
                            title = stringResource(R.string.menu_garage),
                            subtitle = "Pintura y mejoras",
                            icon = Icons.Filled.Build,
                            accent = SkyCyan,
                            testTag = "menu_garage_button",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onNavigate(AppScreen.GARAGE) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MenuTileCard(
                            title = stringResource(R.string.menu_map),
                            subtitle = "${profile.unlockedRouteIds.size}/4 rutas",
                            icon = Icons.Filled.Map,
                            accent = EmeraldGo,
                            testTag = "menu_map_button",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onNavigate(AppScreen.MAP) }
                        )
                        MenuTileCard(
                            title = stringResource(R.string.menu_missions),
                            subtitle = "Cargas y contratos",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            accent = AmberSignal,
                            testTag = "menu_missions_button",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onNavigate(AppScreen.MISSIONS) }
                        )
                        MenuTileCard(
                            title = stringResource(R.string.menu_settings),
                            subtitle = "Gráficos y mandos",
                            icon = Icons.Filled.Settings,
                            accent = TextSecondary,
                            testTag = "menu_settings_button",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onNavigate(AppScreen.SETTINGS) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SimSurfaceGlass),
        border = BorderStroke(1.dp, SimOutline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
