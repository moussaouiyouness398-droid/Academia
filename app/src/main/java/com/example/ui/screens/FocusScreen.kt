package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: AcademiaViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    
    val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val timerTotalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val timerSubjectId by viewModel.timerSubjectId.collectAsState()

    var showSubjectPicker by remember { mutableStateOf(false) }

    // Breathing circle pulsing scale animation when timer is running
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val pulseScale by if (isTimerRunning) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    // Compute progress fraction
    val progressFraction = if (timerTotalSeconds > 0) {
        timerSecondsLeft.toFloat() / timerTotalSeconds.toFloat()
    } else 1f

    // Format remaining time
    val minutes = timerSecondsLeft / 60
    val seconds = timerSecondsLeft % 60
    val countdownText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("focus_screen_layout"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Screen Header uniforme a Menú
        item {
            Text(
                text = "Modo Enfoque Pomodoro",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Text(
                text = "Entrena tu mente, bloquea el ruido y progresa en tus sesiones de estudio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        // Active study subject configuration
        item {
            val linkedSubject = subjects.find { it.id == timerSubjectId }
            val labelText = linkedSubject?.let { "Estudiando: ${it.name}" } ?: "Asignar materia de estudio"
            val indicatorHex = linkedSubject?.colorHex ?: "#7D5260"
            val pillColor = Color(android.graphics.Color.parseColor(indicatorHex))

            Box(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SuggestionChip(
                    onClick = { showSubjectPicker = true },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pillColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(labelText, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("subject_picker_chip")
                )

                DropdownMenu(
                    expanded = showSubjectPicker,
                    onDismissRequest = { showSubjectPicker = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("General / Sin Materia") },
                        onClick = {
                            viewModel.setTimerSubject(null)
                            showSubjectPicker = false
                        }
                    )
                    subjects.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub.name) },
                            onClick = {
                                viewModel.setTimerSubject(sub.id)
                                showSubjectPicker = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Concentric Breathing Countdown Dial
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .scale(pulseScale)
                    .testTag("countdown_dial")
            ) {
                // Background radial glass effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Master circular tracker
                CircularProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.size(246.dp),
                    strokeWidth = 10.dp,
                    color = if (timerMode == "ESTUDIO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // Time description text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current Mode indication label
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (timerMode == "ESTUDIO") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = timerMode,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (timerMode == "ESTUDIO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isTimerRunning) "Enfocado..." else "En Pausa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Countdown Controllers Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause master controller
                FilledIconButton(
                    onClick = {
                        if (isTimerRunning) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .scale(pulseScale)
                        .testTag("play_pause_fab"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (timerMode == "ESTUDIO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Arrancar",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Fast Skip to Break or Study button
                IconButton(
                    onClick = {
                        val nextMode = if (timerMode == "ESTUDIO") "DESCANSO" else "ESTUDIO"
                        viewModel.setTimerDuration(if (nextMode == "ESTUDIO") 25 else 5)
                        viewModel.addNotification("Temporizador saltado a modo $nextMode")
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Omitir bloque", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Rapid Preset Duration options
        item {
            Text(
                text = "ELEGIR BLOQUES DE TIEMPO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    val isActiveSelection = timerTotalSeconds == mins * 60
                    
                    OutlinedButton(
                        onClick = { viewModel.setTimerDuration(mins) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duration_preset_$mins"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isActiveSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor = if (isActiveSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Ambient Sounds / Atmosphere Preset Choice
        item {
            var activeAmbientSound by remember { mutableStateOf<String?>(null) }
            val sounds = listOf(
                Triple("Lluvia", "🌧️ Lluvia", Color(0xFF2196F3)),
                Triple("Cafeteria", "☕ Café Jazz", Color(0xFFFF9800)),
                Triple("Zen", "🍃 Bosque", Color(0xFF4CAF50)),
                Triple("Fuego", "🔥 Hoguera", Color(0xFFFF5722))
            )

            val soundPulseTransition = rememberInfiniteTransition(label = "sound_pulse")
            val soundScaleScale by soundPulseTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = ""
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "SONIDOS DE AMBIENTE REAJUSTADOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sounds.forEach { (id, label, accentColor) ->
                        val isPlaying = activeAmbientSound == id
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    activeAmbientSound = if (isPlaying) null else id
                                    viewModel.addNotification(
                                        if (isPlaying) "Sonido ambiental detenido." 
                                        else "Reproduciendo sonido relajante: $id"
                                    )
                                }
                                .scale(if (isPlaying) soundScaleScale else 1.0f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, 
                                if (isPlaying) accentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isPlaying) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPlaying) "ACTIVO" else "PAUSA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Scientific Study Tips block
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Tip",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Consejo del Pomodoro:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Estudiar en bloques de 25 minutos con 5 de descanso previene la fatiga intelectual y maximiza el recuerdo a largo plazo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
