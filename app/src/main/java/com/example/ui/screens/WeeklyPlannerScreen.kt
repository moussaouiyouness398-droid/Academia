package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScheduleEntry
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import com.example.utils.ColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlannerScreen(viewModel: AcademiaViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val homeworkList by viewModel.homeworkList.collectAsState()
    val scheduleEntries by viewModel.scheduleEntries.collectAsState()

    val pendingHwCount = homeworkList.count { it.status != "COMPLETADO" }

    // Selected Day (1 = Lunes, 2 = Martes, 3 = Miércoles, 4 = Jueves, 5 = Viernes, 6 = Sábado, 7 = Domingo)
    var selectedDayIndex by remember { mutableIntStateOf(1) }
    val daysOfWeek = listOf(
        1 to "Lunes",
        2 to "Martes",
        3 to "Miércoles",
        4 to "Jueves",
        5 to "Viernes",
        6 to "Sábado",
        7 to "Domingo"
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSwapEntryId by remember { mutableStateOf<Long?>(null) }
    var stagedSchedule by remember { mutableStateOf<List<ScheduleEntry>?>(null) }

    LaunchedEffect(selectedDayIndex) {
        stagedSchedule = null
        selectedSwapEntryId = null
    }

    val originalDayEntries = remember(scheduleEntries, selectedDayIndex) {
        scheduleEntries.filter { it.dayOfWeek == selectedDayIndex }.sortedBy { it.startTime }
    }
    val currentDayEntries = stagedSchedule ?: originalDayEntries
    val hasStagedChanges = remember(stagedSchedule, originalDayEntries) {
        if (stagedSchedule == null) false
        else {
            stagedSchedule!!.any { staged ->
                val orig = originalDayEntries.find { it.id == staged.id }
                orig?.startTime != staged.startTime || orig?.endTime != staged.endTime
            }
        }
    }

    val currentDayName = daysOfWeek.find { it.first == selectedDayIndex }?.second ?: "Día"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weekly_planner_layout"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir clase al horario")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Heading Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Planificador Semanal",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Distribuye tus horas de clases y estudio eficientemente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Days Filter Ribbon
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysOfWeek.forEach { (dayInt, dayName) ->
                        val isSelected = selectedDayIndex == dayInt
                        val count = scheduleEntries.count { it.dayOfWeek == dayInt }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedDayIndex = dayInt }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayName.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Planning Advisor Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = "Asistente",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asistente de Planificación",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val suggestionText = when {
                            pendingHwCount > 2 -> "Tienes $pendingHwCount deberes pendientes. Te sugerimos programar bloques de estudio dedicados después de tus clases."
                            currentDayEntries.isEmpty() -> "No tienes clases registradas para los $currentDayName. Pulsa el botón '+' para agregar una materia o taller a tu horario."
                            else -> "Tienes ${currentDayEntries.size} bloque(s) programados para el $currentDayName. Mantén hidratación y realiza pausas activas."
                        }

                        Text(
                            text = suggestionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Schedule Timeblocks Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BLOQUES DE CLASES PARA EL ${currentDayName.uppercase()} (${currentDayEntries.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Banner explicativo e interactivo con botones Cancelar y Confirmar
            if (selectedSwapEntryId != null || stagedSchedule != null) {
                item {
                    val activeEntry = currentDayEntries.find { it.id == selectedSwapEntryId }
                    val activeTitle = activeEntry?.customTitle?.ifBlank {
                        subjects.find { it.id == activeEntry.subjectId }?.name ?: "Clase"
                    } ?: "Clase"

                    Surface(
                        color = if (hasStagedChanges) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (hasStagedChanges) Icons.Default.CheckCircle else Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = if (hasStagedChanges) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (hasStagedChanges)
                                            "Horario modificado (sin guardar)"
                                        else
                                            "Moviendo: $activeTitle (${activeEntry?.startTime ?: ""} - ${activeEntry?.endTime ?: ""})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (hasStagedChanges) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (hasStagedChanges)
                                            "Pulsa 'Confirmar' para guardar el nuevo horario o 'Cancelar' para descartar."
                                        else
                                            "Toca otra clase para intercambiar sus horas o usa las flechas arriba/abajo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = (if (hasStagedChanges) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        stagedSchedule = null
                                        selectedSwapEntryId = null
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancelar", fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        stagedSchedule?.let { updatedList ->
                                            viewModel.updateScheduleEntries(updatedList)
                                        }
                                        stagedSchedule = null
                                        selectedSwapEntryId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirmar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (currentDayEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sin clases los $currentDayName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pulsa el botón de abajo para registrar tus clases o talleres.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Añadir clase al horario")
                            }
                        }
                    }
                }
            } else {
                items(currentDayEntries, key = { it.id }) { entry ->
                    val subject = subjects.find { it.id == entry.subjectId }
                    val effectiveColor = remember(entry.colorHex, subject?.colorHex) {
                        val src = subject?.colorHex ?: entry.colorHex
                        ColorUtils.parseColorSafe(src, Color(0xFF2196F3))
                    }
                    val title = entry.customTitle.ifBlank { subject?.name ?: "Clase" }
                    val isSelected = selectedSwapEntryId == entry.id
                    val isSwapMode = selectedSwapEntryId != null || stagedSchedule != null

                    val index = currentDayEntries.indexOfFirst { it.id == entry.id }
                    val prevEntry = if (index > 0) currentDayEntries[index - 1] else null
                    val nextEntry = if (index >= 0 && index < currentDayEntries.size - 1) currentDayEntries[index + 1] else null

                    var isHolding by remember { mutableStateOf(false) }
                    var holdProgress by remember { mutableFloatStateOf(0f) }
                    val haptic = LocalHapticFeedback.current

                    LaunchedEffect(isHolding) {
                        if (isHolding) {
                            val startTime = System.currentTimeMillis()
                            val totalDuration = 1800L
                            while (isHolding) {
                                val elapsed = System.currentTimeMillis() - startTime
                                holdProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                                if (elapsed >= totalDuration) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (stagedSchedule == null) {
                                        stagedSchedule = originalDayEntries.toList()
                                    }
                                    selectedSwapEntryId = entry.id
                                    isHolding = false
                                    holdProgress = 0f
                                    break
                                }
                                delay(20)
                            }
                        } else {
                            holdProgress = 0f
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateItem()
                            .then(
                                if (isSwapMode && !isSelected) {
                                    Modifier.clickable {
                                        val list = (stagedSchedule ?: originalDayEntries).toMutableList()
                                        val active = list.find { it.id == selectedSwapEntryId }
                                        if (active != null && active.id != entry.id) {
                                            val idxActive = list.indexOfFirst { it.id == active.id }
                                            val idxTarget = list.indexOfFirst { it.id == entry.id }
                                            if (idxActive != -1 && idxTarget != -1) {
                                                val updatedActive = active.copy(startTime = entry.startTime, endTime = entry.endTime)
                                                val updatedTarget = entry.copy(startTime = active.startTime, endTime = active.endTime)
                                                list[idxActive] = updatedActive
                                                list[idxTarget] = updatedTarget
                                                stagedSchedule = list.sortedBy { it.startTime }
                                                selectedSwapEntryId = active.id
                                            }
                                        }
                                    }
                                } else if (!isSwapMode) {
                                    Modifier.pointerInput(entry.id) {
                                        detectTapGestures(
                                            onPress = {
                                                isHolding = true
                                                try {
                                                    awaitRelease()
                                                } finally {
                                                    isHolding = false
                                                }
                                            },
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (stagedSchedule == null) {
                                                    stagedSchedule = originalDayEntries.toList()
                                                }
                                                selectedSwapEntryId = entry.id
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                isSwapMode -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else if (isSwapMode) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isSwapMode -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                else -> effectiveColor.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Column {
                            // Cabecera interactiva cuando está seleccionada
                            if (isSelected) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.SwapVert,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Moviendo horario (${entry.startTime} - ${entry.endTime})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (prevEntry != null) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    val list = (stagedSchedule ?: originalDayEntries).toMutableList()
                                                    val idxActive = list.indexOfFirst { it.id == entry.id }
                                                    val idxPrev = list.indexOfFirst { it.id == prevEntry.id }
                                                    if (idxActive != -1 && idxPrev != -1) {
                                                        val updatedActive = entry.copy(startTime = prevEntry.startTime, endTime = prevEntry.endTime)
                                                        val updatedPrev = prevEntry.copy(startTime = entry.startTime, endTime = entry.endTime)
                                                        list[idxActive] = updatedActive
                                                        list[idxPrev] = updatedPrev
                                                        stagedSchedule = list.sortedBy { it.startTime }
                                                        selectedSwapEntryId = entry.id
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = "Subir hora",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        if (nextEntry != null) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    val list = (stagedSchedule ?: originalDayEntries).toMutableList()
                                                    val idxActive = list.indexOfFirst { it.id == entry.id }
                                                    val idxNext = list.indexOfFirst { it.id == nextEntry.id }
                                                    if (idxActive != -1 && idxNext != -1) {
                                                        val updatedActive = entry.copy(startTime = nextEntry.startTime, endTime = nextEntry.endTime)
                                                        val updatedNext = nextEntry.copy(startTime = entry.startTime, endTime = entry.endTime)
                                                        list[idxActive] = updatedActive
                                                        list[idxNext] = updatedNext
                                                        stagedSchedule = list.sortedBy { it.startTime }
                                                        selectedSwapEntryId = entry.id
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = "Bajar hora",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (isSwapMode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Toca para intercambiar con ${entry.startTime} - ${entry.endTime}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Barra de progreso mientras se mantiene presionado 2s
                            if (holdProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { holdProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(90.dp)) {
                                    Text(
                                        text = "${entry.startTime} - ${entry.endTime}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = entry.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = effectiveColor
                                    )
                                }

                                VerticalDivider(
                                    color = effectiveColor.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .padding(horizontal = 8.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val location = entry.classroom.ifBlank { subject?.classroom ?: "" }
                                    val prof = entry.professor.ifBlank { subject?.professor ?: "" }
                                    if (location.isNotBlank() || prof.isNotBlank()) {
                                        Text(
                                            text = listOfNotNull(
                                                location.takeIf { it.isNotBlank() }?.let { "📍 $it" },
                                                prof.takeIf { it.isNotBlank() }?.let { "👨‍🏫 $it" }
                                            ).joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (!isSwapMode) {
                                    IconButton(
                                        onClick = { selectedSwapEntryId = entry.id },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SwapVert,
                                            contentDescription = "Mover e intercambiar hora",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteScheduleEntry(entry) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    if (showAddDialog) {
        var customTitle by remember { mutableStateOf("") }
        var startTime by remember { mutableStateOf("08:30") }
        var endTime by remember { mutableStateOf("10:00") }
        var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var classroom by remember { mutableStateOf(subjects.firstOrNull()?.classroom ?: "") }
        var professor by remember { mutableStateOf(subjects.firstOrNull()?.professor ?: "") }
        var isCustom by remember { mutableStateOf(subjects.isEmpty()) }
        var type by remember { mutableStateOf("CLASE") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir Clase al Horario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Día seleccionado: $currentDayName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (subjects.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isCustom,
                                onClick = { isCustom = false },
                                label = { Text("Materia") }
                            )
                            FilterChip(
                                selected = isCustom,
                                onClick = { isCustom = true },
                                label = { Text("Personalizado") }
                            )
                        }

                        if (!isCustom) {
                            OutlinedTextField(
                                value = subjects.find { it.id == selectedSubjectId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Materia actual") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val idx = subjects.indexOfFirst { it.id == selectedSubjectId }
                                        val next = subjects[(idx + 1) % subjects.size]
                                        selectedSubjectId = next.id
                                        classroom = next.classroom
                                        professor = next.professor
                                    }) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar materia")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text("Título de la clase / sesión") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Título de la clase") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Hora Inicio") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Hora Fin") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Aula (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sub = if (!isCustom) subjects.find { it.id == selectedSubjectId } else null
                    val finalTitle = if (isCustom) customTitle.ifBlank { "Clase" } else sub?.name ?: "Clase"
                    val finalColor = sub?.colorHex ?: "#2196F3"

                    viewModel.createScheduleEntry(
                        dayOfWeek = selectedDayIndex,
                        startTime = startTime.ifBlank { "08:30" },
                        endTime = endTime.ifBlank { "10:00" },
                        subjectId = if (!isCustom) selectedSubjectId else 0L,
                        customTitle = finalTitle,
                        classroom = classroom,
                        professor = professor,
                        colorHex = finalColor,
                        type = type
                    )
                    showAddDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
