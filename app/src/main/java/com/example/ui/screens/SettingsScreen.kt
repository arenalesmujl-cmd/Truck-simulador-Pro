package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    profile: CareerProfile,
    currentWeather: WeatherType,
    dynamicWeather: Boolean,
    dynamicTime: Boolean,
    currentTimeHours: Float = 12.5f,
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
    var selectedDynamicTime by remember(dynamicTime) { mutableStateOf(dynamicTime) }
    var selectedTimePreset by remember(currentTimeHours) { mutableStateOf(currentTimeHours) }

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
                            .testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CONFIGURACIÓN",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Gráficos 3D, transmisión, controles y clima",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyCyan
                    )
                }
            }

            val saveInteraction = remember { MutableInteractionSource() }
            val savePressed by saveInteraction.collectIsPressedAsState()
            val saveScale by animateFloatAsState(
                targetValue = if (savePressed) 0.96f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "save_btn_scale"
            )
            Button(
                onClick = { onSaveSettings(driverName, graphics, gearbox, steering, sound) },
                interactionSource = saveInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .graphicsLayer(scaleX = saveScale, scaleY = saveScale)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_settings_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SimBackground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("GUARDAR CAMBIOS", style = MaterialTheme.typography.labelLarge, color = SimBackground, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

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
                border = BorderStroke(1.dp, RacingOrange.copy(alpha = 0.36f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(RacingOrange, SkyCyan)))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = RacingOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PERFIL Y CONTROLES", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { if (it.length <= 40) driverName = it },
                        label = { Text("Nombre de conductor") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_name_input")
                    )

                    Text("CALIDAD GRÁFICA 3D (OPTIMIZACIÓN)", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Text("CAJA DE CAMBIOS", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Text("TIPO DE DIRECCIÓN", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.36f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(SkyCyan, EmeraldGo)))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ENTORNO Y SONIDO", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

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

                    Text("CLIMA Y CICLO DÍA/NOCHE", style = MaterialTheme.typography.labelSmall, color = SkyCyan)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = dynamicWeather,
                            onClick = { onSetWeather(WeatherType.CLEAR, true) },
                            label = { Text("Dinámico") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RacingOrange.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("weather_dynamic")
                        )
                        WeatherType.entries.forEach { w ->
                            FilterChip(
                                selected = !dynamicWeather && currentWeather == w,
                                onClick = { onSetWeather(w, false) },
                                label = { Text(w.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RacingOrange.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("weather_${w.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDynamicTime,
                            onClick = {
                                selectedDynamicTime = true
                                onSetTimeOfDay(15.5f, true)
                            },
                            label = { Text("Ciclo Auto") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyCyan.copy(alpha = 0.25f))
                        )
                        FilterChip(
                            selected = !selectedDynamicTime && selectedTimePreset < 16f,
                            onClick = {
                                selectedDynamicTime = false
                                selectedTimePreset = 12.5f
                                onSetTimeOfDay(12.5f, false)
                            },
                            label = { Text("Mediodía") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyCyan.copy(alpha = 0.25f))
                        )
                        FilterChip(
                            selected = !selectedDynamicTime && selectedTimePreset in 16f..19.5f,
                            onClick = {
                                selectedDynamicTime = false
                                selectedTimePreset = 18.0f
                                onSetTimeOfDay(18.0f, false)
                            },
                            label = { Text("Atardecer") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyCyan.copy(alpha = 0.25f))
                        )
                        FilterChip(
                            selected = !selectedDynamicTime && selectedTimePreset > 19.5f,
                            onClick = {
                                selectedDynamicTime = false
                                selectedTimePreset = 22.5f
                                onSetTimeOfDay(22.5f, false)
                            },
                            label = { Text("Noche") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyCyan.copy(alpha = 0.25f))
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    val signOutInteraction = remember { MutableInteractionSource() }
                    val signOutPressed by signOutInteraction.collectIsPressedAsState()
                    val signOutScale by animateFloatAsState(
                        targetValue = if (signOutPressed) 0.97f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "signout_btn_scale"
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        interactionSource = signOutInteraction,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .graphicsLayer(scaleX = signOutScale, scaleY = signOutScale)
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
