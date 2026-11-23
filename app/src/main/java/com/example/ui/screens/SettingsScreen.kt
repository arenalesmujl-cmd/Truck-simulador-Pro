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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CareerProfile
import com.example.model.GearboxMode
import com.example.model.GraphicsQuality
import com.example.model.SteeringMode
import com.example.model.WeatherType
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.RacingOrange
import com.example.ui.theme.SimBackground
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    profile: CareerProfile,
    currentWeather: WeatherType,
    dynamicWeather: Boolean,
    dynamicTime: Boolean,
    onSetWeather: (WeatherType, Boolean) -> Unit,
    onSetTimeOfDay: (Float, Boolean) -> Unit,
    onSaveSettings: (String, GraphicsQuality, GearboxMode, SteeringMode, Boolean) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var driverName by remember(profile.driverName) { mutableStateOf(profile.driverName) }
    var graphics by remember(profile.graphicsQuality) { mutableStateOf(GraphicsQuality.fromRaw(profile.graphicsQuality)) }
    var gearbox by remember(profile.gearboxMode) { mutableStateOf(GearboxMode.fromRaw(profile.gearboxMode)) }
    var steering by remember(profile.steeringMode) { mutableStateOf(SteeringMode.fromRaw(profile.steeringMode)) }
    var sound by remember(profile.soundEnabled) { mutableStateOf(profile.soundEnabled) }

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
                        .testTag("settings_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("CONFIGURACIÓN", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }

            Button(
                onClick = { onSaveSettings(driverName, graphics, gearbox, steering, sound) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_settings_button")
            ) {
                Text("GUARDAR CAMBIOS", style = MaterialTheme.typography.labelLarge, color = SimBackground)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Graphics, Controls & Profile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurface),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { if (it.length <= 40) driverName = it },
                        label = { Text("Nombre de conductor") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_name_input")
                    )

                    Text("CALIDAD GRÁFICA 3D (OPTIMIZACIÓN)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsQuality.entries.forEach { q ->
                            FilterChip(
                                selected = graphics == q,
                                onClick = { graphics = q },
                                label = { Text(q.labelEs, style = MaterialTheme.typography.bodyMedium) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RacingOrange.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("graphics_${q.name}")
                            )
                        }
                    }

                    Text("CAJA DE CAMBIOS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GearboxMode.entries.forEach { g ->
                            FilterChip(
                                selected = gearbox == g,
                                onClick = { gearbox = g },
                                label = { Text(g.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RacingOrange.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("gearbox_${g.name}")
                            )
                        }
                    }

                    Text("TIPO DE DIRECCIÓN", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SteeringMode.entries.forEach { s ->
                            FilterChip(
                                selected = steering == s,
                                onClick = { steering = s },
                                label = { Text(s.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RacingOrange.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("steering_${s.name}")
                            )
                        }
                    }
                }
            }

            // Right: Audio, Environment Presets & Account
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = SimSurface),
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sonido de Motor y Bocina", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Audio diésel sintetizado en tiempo real", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Switch(
                            checked = sound,
                            onCheckedChange = { sound = it },
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }

                    Text("CLIMA Y CICLO DÍA/NOCHE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dynamicWeather && currentWeather == WeatherType.CLEAR,
                            onClick = { onSetWeather(WeatherType.CLEAR, true) },
                            label = { Text("Dinámico") },
                            modifier = Modifier.testTag("weather_dynamic")
                        )
                        WeatherType.entries.forEach { w ->
                            FilterChip(
                                selected = !dynamicWeather && currentWeather == w,
                                onClick = { onSetWeather(w, false) },
                                label = { Text(w.labelEs) },
                                modifier = Modifier.testTag("weather_${w.name}")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dynamicTime,
                            onClick = { onSetTimeOfDay(15.5f, true) },
                            label = { Text("Ciclo Auto") }
                        )
                        FilterChip(
                            selected = !dynamicTime,
                            onClick = { onSetTimeOfDay(12.5f, false) },
                            label = { Text("Mediodía") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onSetTimeOfDay(18.0f, false) },
                            label = { Text("Atardecer") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { onSetTimeOfDay(22.5f, false) },
                            label = { Text("Noche") }
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("settings_sign_out_button")
                    ) {
                        Text("CERRAR SESIÓN DE GOOGLE", color = CrimsonAlert, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
