package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Exam
import com.example.data.Homework
import com.example.data.ScheduleEntry
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import com.example.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: AcademiaViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val homeworkList by viewModel.homeworkList.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val scheduleEntries by viewModel.scheduleEntries.collectAsState()

    // Tab de vista: 0 = Agenda Diaria, 1 = Horario Semanal
    var selectedViewMode by remember { mutableIntStateOf(0) }

    // Fecha actualmente seleccionada en el calendario
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Día de la semana seleccionado en la pestaña de Horario Semanal (1 = Lunes, ..., 7 = Domingo)
    var selectedWeeklyDay by remember { mutableIntStateOf(getDayOfWeekInt(Calendar.getInstance())) }

    // Diálogo para agregar nueva clase / bloque al horario
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    // Generar días horizontales de la semana actual (Lunes a Domingo)
    val calendarDays = remember(selectedDate.get(Calendar.WEEK_OF_YEAR), selectedDate.get(Calendar.YEAR)) {
        val list = mutableListOf<Calendar>()
        val startCal = (selectedDate.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        for (i in 0..6) {
            list.add(startCal.clone() as Calendar)
            startCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val selectedDayOfWeek = getDayOfWeekInt(selectedDate)

    // Filtros para la fecha seleccionada
    val daySchedule = remember(scheduleEntries, selectedDayOfWeek) {
        scheduleEntries.filter { it.dayOfWeek == selectedDayOfWeek }.sortedBy { it.startTime }
    }

    val dayHomework = remember(homeworkList, selectedDate) {
        homeworkList.filter { hw -> isSameDay(hw.dueDate, selectedDate.timeInMillis) }
    }

    val dayExams = remember(exams, selectedDate) {
        exams.filter { ex -> isSameDay(ex.date, selectedDate.timeInMillis) }
    }

    // Filtros para la vista de Horario Semanal
    val weeklyDaySchedule = remember(scheduleEntries, selectedWeeklyDay) {
        scheduleEntries.filter { it.dayOfWeek == selectedWeeklyDay }.sortedBy { it.startTime }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_layout"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddScheduleDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir") },
                text = { Text("Agregar al Horario") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("btn_add_to_schedule")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cabecera Principal con diseño uniforme a Menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendario y Horario",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                FilledTonalButton(
                    onClick = { showAddScheduleDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("quick_add_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir", style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                text = "Organiza tus clases y actividades semanales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            // Selector de modo de vista: Agenda Diaria vs Horario Semanal
            PrimaryTabRow(
                selectedTabIndex = selectedViewMode,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                    Tab(
                        selected = selectedViewMode == 0,
                        onClick = { selectedViewMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Agenda Diaria", fontWeight = if (selectedViewMode == 0) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    )
                    Tab(
                        selected = selectedViewMode == 1,
                        onClick = { selectedViewMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Horario Semanal", fontWeight = if (selectedViewMode == 1) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    )
                }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = selectedViewMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "calendar_view_transition",
                modifier = Modifier.weight(1f)
            ) { mode ->
                if (mode == 0) {
                    // MODO 0: AGENDA DIARIA
                    DailyAgendaView(
                        selectedDate = selectedDate,
                        calendarDays = calendarDays,
                        daySchedule = daySchedule,
                        dayExams = dayExams,
                        dayHomework = dayHomework,
                        subjects = subjects,
                        homeworkList = homeworkList,
                        exams = exams,
                        onSelectDate = { selectedDate = it },
                        onChangeWeek = { deltaDays ->
                            val newCal = selectedDate.clone() as Calendar
                            newCal.add(Calendar.DAY_OF_YEAR, deltaDays)
                            selectedDate = newCal
                        },
                        onDeleteScheduleEntry = { viewModel.deleteScheduleEntry(it) },
                        onAddScheduleClick = { showAddScheduleDialog = true },
                        onToggleHomework = { viewModel.toggleHomeworkCompletion(it) },
                        onSwapSchedule = { e1, e2 -> viewModel.swapScheduleEntriesTimes(e1, e2) },
                        onSaveScheduleOrder = { viewModel.updateScheduleEntries(it) }
                    )
                } else {
                    // MODO 1: HORARIO SEMANAL
                    WeeklyScheduleView(
                        selectedDay = selectedWeeklyDay,
                        scheduleEntries = weeklyDaySchedule,
                        allScheduleEntries = scheduleEntries,
                        subjects = subjects,
                        onSelectDay = { selectedWeeklyDay = it },
                        onDeleteScheduleEntry = { viewModel.deleteScheduleEntry(it) },
                        onAddScheduleClick = { showAddScheduleDialog = true },
                        onSwapSchedule = { e1, e2 -> viewModel.swapScheduleEntriesTimes(e1, e2) },
                        onSaveScheduleOrder = { viewModel.updateScheduleEntries(it) }
                    )
                }
            }
        }
    }

    // Diálogo para agregar nueva clase / bloque al horario
    if (showAddScheduleDialog) {
        val initialDay = if (selectedViewMode == 0) selectedDayOfWeek else selectedWeeklyDay
        AddScheduleDialog(
            subjects = subjects,
            initialDayOfWeek = initialDay,
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { dayOfWeek, startTime, endTime, subjectId, customTitle, classroom, professor, colorHex, type ->
                viewModel.createScheduleEntry(
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    subjectId = subjectId,
                    customTitle = customTitle,
                    classroom = classroom,
                    professor = professor,
                    colorHex = colorHex,
                    type = type
                )
                showAddScheduleDialog = false
            }
        )
    }
}

/**
 * Vista de la Agenda Diaria.
 */
@Composable
private fun DailyAgendaView(
    selectedDate: Calendar,
    calendarDays: List<Calendar>,
    daySchedule: List<ScheduleEntry>,
    dayExams: List<Exam>,
    dayHomework: List<Homework>,
    subjects: List<Subject>,
    homeworkList: List<Homework>,
    exams: List<Exam>,
    onSelectDate: (Calendar) -> Unit,
    onChangeWeek: (Int) -> Unit,
    onDeleteScheduleEntry: (ScheduleEntry) -> Unit,
    onAddScheduleClick: () -> Unit,
    onToggleHomework: (Homework) -> Unit = {},
    onSwapSchedule: (ScheduleEntry, ScheduleEntry) -> Unit = { _, _ -> },
    onSaveScheduleOrder: (List<ScheduleEntry>) -> Unit = {}
) {
    val monthFormat = remember { SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "ES")) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")) }
    val selectedDayStr = fullDateFormat.format(selectedDate.time).replaceFirstChar { it.uppercase() }
    var selectedSwapEntryId by remember { mutableStateOf<Long?>(null) }
    var stagedSchedule by remember { mutableStateOf<List<ScheduleEntry>?>(null) }

    LaunchedEffect(selectedDate) {
        stagedSchedule = null
        selectedSwapEntryId = null
    }

    val currentDaySchedule = stagedSchedule ?: daySchedule
    val hasStagedChanges = remember(stagedSchedule, daySchedule) {
        if (stagedSchedule == null) false
        else {
            stagedSchedule!!.any { staged ->
                val orig = daySchedule.find { it.id == staged.id }
                orig?.startTime != staged.startTime || orig?.endTime != staged.endTime
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de mes y controles de semana
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthFormat.format(selectedDate.time).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                IconButton(
                    onClick = { onChangeWeek(-7) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Semana anterior",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onChangeWeek(7) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Semana siguiente",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Cinta horizontal de días de la semana
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(calendarDays) { cal ->
                val isSelected = isSameDay(cal.timeInMillis, selectedDate.timeInMillis)
                val dayNameFormat = SimpleDateFormat("EEE", Locale("es", "ES"))
                val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())

                val dayName = dayNameFormat.format(cal.time).uppercase().replace(".", "")
                val dayNum = dayNumFormat.format(cal.time)

                val hasEvents = homeworkList.any { isSameDay(it.dueDate, cal.timeInMillis) } ||
                        exams.any { isSameDay(it.date, cal.timeInMillis) }

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onSelectDate(cal) }
                        .padding(vertical = 8.dp)
                        .testTag("day_cell_${dayNum}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )

                    if (hasEvents) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(9.dp))
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Lista de eventos y horario para el día seleccionado
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDayStr,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${daySchedule.size} clases • ${dayExams.size} exámenes • ${dayHomework.size} deberes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // SECCIÓN: HORARIO DE CLASES DEL DÍA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HORARIO DE CLASES (${daySchedule.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onAddScheduleClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir al horario", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Banner explicativo e interactivo con botones Cancelar y Confirmar
            if (selectedSwapEntryId != null || stagedSchedule != null) {
                item {
                    val activeEntry = currentDaySchedule.find { it.id == selectedSwapEntryId }
                    val activeTitle = activeEntry?.customTitle?.ifBlank {
                        subjects.find { it.id == activeEntry.subjectId }?.name ?: "Clase"
                    } ?: "Clase"

                    Surface(
                        color = if (hasStagedChanges) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().animateContentSize()
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
                                            "Pulsa 'Confirmar' para aplicar los cambios al horario o 'Cancelar' para descartar."
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
                                            onSaveScheduleOrder(updatedList)
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

            if (currentDaySchedule.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sin clases programadas",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "No tienes clases registradas en tu horario para este día.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            FilledTonalButton(
                                onClick = onAddScheduleClick,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Añadir", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                items(currentDaySchedule, key = { it.id }) { entry ->
                    val subject = subjects.find { it.id == entry.subjectId }
                    val index = currentDaySchedule.indexOfFirst { it.id == entry.id }
                    val prevEntry = if (index > 0) currentDaySchedule[index - 1] else null
                    val nextEntry = if (index >= 0 && index < currentDaySchedule.size - 1) currentDaySchedule[index + 1] else null

                    ScheduleEntryCard(
                        entry = entry,
                        subject = subject,
                        onDelete = { onDeleteScheduleEntry(entry) },
                        isSelectedForSwap = selectedSwapEntryId == entry.id,
                        isSwapModeActive = selectedSwapEntryId != null || stagedSchedule != null,
                        onStartSwap = {
                            if (stagedSchedule == null) {
                                stagedSchedule = daySchedule.toList()
                            }
                            selectedSwapEntryId = entry.id
                        },
                        onExecuteSwap = {
                            val list = (stagedSchedule ?: daySchedule).toMutableList()
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
                        },
                        onMoveUp = prevEntry?.let { prev ->
                            {
                                val list = (stagedSchedule ?: daySchedule).toMutableList()
                                val idxActive = list.indexOfFirst { it.id == entry.id }
                                val idxPrev = list.indexOfFirst { it.id == prev.id }
                                if (idxActive != -1 && idxPrev != -1) {
                                    val updatedActive = entry.copy(startTime = prev.startTime, endTime = prev.endTime)
                                    val updatedPrev = prev.copy(startTime = entry.startTime, endTime = entry.endTime)
                                    list[idxActive] = updatedActive
                                    list[idxPrev] = updatedPrev
                                    stagedSchedule = list.sortedBy { it.startTime }
                                    selectedSwapEntryId = entry.id
                                }
                            }
                        },
                        onMoveDown = nextEntry?.let { next ->
                            {
                                val list = (stagedSchedule ?: daySchedule).toMutableList()
                                val idxActive = list.indexOfFirst { it.id == entry.id }
                                val idxNext = list.indexOfFirst { it.id == next.id }
                                if (idxActive != -1 && idxNext != -1) {
                                    val updatedActive = entry.copy(startTime = next.startTime, endTime = next.endTime)
                                    val updatedNext = next.copy(startTime = entry.startTime, endTime = entry.endTime)
                                    list[idxActive] = updatedActive
                                    list[idxNext] = updatedNext
                                    stagedSchedule = list.sortedBy { it.startTime }
                                    selectedSwapEntryId = entry.id
                                }
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // SECCIÓN: EXÁMENES
            if (dayExams.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EXÁMENES (${dayExams.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                items(dayExams, key = { it.id }) { exam ->
                    val subject = subjects.find { it.id == exam.subjectId }
                    val subjectColor = ColorUtils.parseColorSafe(subject?.colorHex, MaterialTheme.colorScheme.tertiary)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, subjectColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(subjectColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${subject?.name ?: "Materia"} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(exam.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    text = exam.difficulty,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN: DEBERES Y ENTREGAS
            if (dayHomework.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ENTREGAS DE DEBERES (${dayHomework.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                items(dayHomework, key = { it.id }) { homework ->
                    val subject = subjects.find { it.id == homework.subjectId }
                    com.example.ui.components.TaskItem(
                        homework = homework,
                        subject = subject,
                        onToggleCompletion = {
                            onToggleHomework(homework)
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Vista del Horario Semanal Completo.
 */
@Composable
private fun WeeklyScheduleView(
    selectedDay: Int,
    scheduleEntries: List<ScheduleEntry>,
    allScheduleEntries: List<ScheduleEntry>,
    subjects: List<Subject>,
    onSelectDay: (Int) -> Unit,
    onDeleteScheduleEntry: (ScheduleEntry) -> Unit,
    onAddScheduleClick: () -> Unit,
    onSwapSchedule: (ScheduleEntry, ScheduleEntry) -> Unit = { _, _ -> },
    onSaveScheduleOrder: (List<ScheduleEntry>) -> Unit = {}
) {
    val daysOfWeek = listOf(
        1 to "Lunes",
        2 to "Martes",
        3 to "Miércoles",
        4 to "Jueves",
        5 to "Viernes",
        6 to "Sábado",
        7 to "Domingo"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de días de la semana
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysOfWeek) { (dayInt, dayName) ->
                val isSelected = selectedDay == dayInt
                val classCount = allScheduleEntries.count { it.dayOfWeek == dayInt }

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectDay(dayInt) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (classCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$classCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        val currentDayName = daysOfWeek.find { it.first == selectedDay }?.second ?: "Día"
        var selectedSwapEntryId by remember { mutableStateOf<Long?>(null) }
        var stagedSchedule by remember { mutableStateOf<List<ScheduleEntry>?>(null) }

        LaunchedEffect(selectedDay) {
            stagedSchedule = null
            selectedSwapEntryId = null
        }

        val currentScheduleEntries = stagedSchedule ?: scheduleEntries
        val hasStagedChanges = remember(stagedSchedule, scheduleEntries) {
            if (stagedSchedule == null) false
            else {
                stagedSchedule!!.any { staged ->
                    val orig = scheduleEntries.find { it.id == staged.id }
                    orig?.startTime != staged.startTime || orig?.endTime != staged.endTime
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HORARIO: ${currentDayName.uppercase()} (${currentScheduleEntries.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onAddScheduleClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir clase al $currentDayName")
                    }
                }
            }

            // Banner explicativo e interactivo con botones Cancelar y Confirmar
            if (selectedSwapEntryId != null || stagedSchedule != null) {
                item {
                    val activeEntry = currentScheduleEntries.find { it.id == selectedSwapEntryId }
                    val activeTitle = activeEntry?.customTitle?.ifBlank {
                        subjects.find { it.id == activeEntry.subjectId }?.name ?: "Clase"
                    } ?: "Clase"

                    Surface(
                        color = if (hasStagedChanges) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().animateContentSize()
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
                                            onSaveScheduleOrder(updatedList)
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

            if (currentScheduleEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sin clases los $currentDayName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Agrega tus materias, talleres o sesiones de estudio para este día.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onAddScheduleClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Añadir clase para el $currentDayName")
                            }
                        }
                    }
                }
            } else {
                items(currentScheduleEntries, key = { it.id }) { entry ->
                    val subject = subjects.find { it.id == entry.subjectId }
                    val index = currentScheduleEntries.indexOfFirst { it.id == entry.id }
                    val prevEntry = if (index > 0) currentScheduleEntries[index - 1] else null
                    val nextEntry = if (index >= 0 && index < currentScheduleEntries.size - 1) currentScheduleEntries[index + 1] else null

                    ScheduleEntryCard(
                        entry = entry,
                        subject = subject,
                        onDelete = { onDeleteScheduleEntry(entry) },
                        isSelectedForSwap = selectedSwapEntryId == entry.id,
                        isSwapModeActive = selectedSwapEntryId != null || stagedSchedule != null,
                        onStartSwap = {
                            if (stagedSchedule == null) {
                                stagedSchedule = scheduleEntries.toList()
                            }
                            selectedSwapEntryId = entry.id
                        },
                        onExecuteSwap = {
                            val list = (stagedSchedule ?: scheduleEntries).toMutableList()
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
                        },
                        onMoveUp = prevEntry?.let { prev ->
                            {
                                val list = (stagedSchedule ?: scheduleEntries).toMutableList()
                                val idxActive = list.indexOfFirst { it.id == entry.id }
                                val idxPrev = list.indexOfFirst { it.id == prev.id }
                                if (idxActive != -1 && idxPrev != -1) {
                                    val updatedActive = entry.copy(startTime = prev.startTime, endTime = prev.endTime)
                                    val updatedPrev = prev.copy(startTime = entry.startTime, endTime = entry.endTime)
                                    list[idxActive] = updatedActive
                                    list[idxPrev] = updatedPrev
                                    stagedSchedule = list.sortedBy { it.startTime }
                                    selectedSwapEntryId = entry.id
                                }
                            }
                        },
                        onMoveDown = nextEntry?.let { next ->
                            {
                                val list = (stagedSchedule ?: scheduleEntries).toMutableList()
                                val idxActive = list.indexOfFirst { it.id == entry.id }
                                val idxNext = list.indexOfFirst { it.id == next.id }
                                if (idxActive != -1 && idxNext != -1) {
                                    val updatedActive = entry.copy(startTime = next.startTime, endTime = next.endTime)
                                    val updatedNext = next.copy(startTime = entry.startTime, endTime = entry.endTime)
                                    list[idxActive] = updatedActive
                                    list[idxNext] = updatedNext
                                    stagedSchedule = list.sortedBy { it.startTime }
                                    selectedSwapEntryId = entry.id
                                }
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta para mostrar un bloque/clase del horario.
 */
@Composable
private fun ScheduleEntryCard(
    entry: ScheduleEntry,
    subject: Subject?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectedForSwap: Boolean = false,
    isSwapModeActive: Boolean = false,
    onStartSwap: () -> Unit = {},
    onExecuteSwap: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                    onStartSwap()
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

    val effectiveColor = remember(entry.colorHex, subject?.colorHex) {
        val colorSource = subject?.colorHex ?: entry.colorHex
        ColorUtils.parseColorSafe(colorSource, fallback = Color(0xFF2196F3))
    }

    val title = entry.customTitle.ifBlank { subject?.name ?: "Clase sin título" }
    val classroom = entry.classroom.ifBlank { subject?.classroom ?: "" }
    val professor = entry.professor.ifBlank { subject?.professor ?: "" }

    val cardBorder = when {
        isSelectedForSwap -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isSwapModeActive -> androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
        else -> androidx.compose.foundation.BorderStroke(1.dp, effectiveColor.copy(alpha = 0.35f))
    }

    val containerColor = when {
        isSelectedForSwap -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        isSwapModeActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_card_${entry.id}")
            .then(
                if (isSwapModeActive && !isSelectedForSwap) {
                    Modifier.clickable { onExecuteSwap() }
                } else if (!isSwapModeActive) {
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
                                onStartSwap()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelectedForSwap) 6.dp else 1.dp),
        border = cardBorder
    ) {
        Column {
            // Cabecera interactiva cuando está seleccionada para mover / intercambiar
            if (isSelectedForSwap) {
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
                        if (onMoveUp != null) {
                            FilledTonalIconButton(
                                onClick = onMoveUp,
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
                        if (onMoveDown != null) {
                            FilledTonalIconButton(
                                onClick = onMoveDown,
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
            } else if (isSwapModeActive) {
                // Barra indicadora para tocar e intercambiar
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

            // Indicador visual de mantener presionado 2 segundos
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
                // Columna de Horas
                Column(
                    modifier = Modifier.width(88.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = entry.startTime,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "a",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = entry.endTime,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Barra divisora de color
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(effectiveColor)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Datos de la clase
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Chip de tipo
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(effectiveColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = entry.type,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = effectiveColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (classroom.isNotBlank() || professor.isNotBlank()) {
                        val infoText = buildString {
                            if (classroom.isNotBlank()) append("📍 $classroom")
                            if (classroom.isNotBlank() && professor.isNotBlank()) append(" • ")
                            if (professor.isNotBlank()) append("👨‍🏫 $professor")
                        }
                        Text(
                            text = infoText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Acciones: Botón de reordenar o eliminar
                if (!isSwapModeActive) {
                    IconButton(
                        onClick = onStartSwap,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Mover e intercambiar horario",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botón de eliminar
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar clase",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar clase del horario") },
            text = { Text("¿Deseas eliminar '$title' de este horario semanal?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Diálogo para agregar una nueva clase o bloque al Horario Semanal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    subjects: List<Subject>,
    initialDayOfWeek: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        subjectId: Long,
        customTitle: String,
        classroom: String,
        professor: String,
        colorHex: String,
        type: String
    ) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(initialDayOfWeek) }
    var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var isCustomSubject by remember { mutableStateOf(subjects.isEmpty()) }
    var customTitle by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:30") }
    var endTime by remember { mutableStateOf("10:00") }
    var classroom by remember { mutableStateOf(subjects.firstOrNull()?.classroom ?: "") }
    var professor by remember { mutableStateOf(subjects.firstOrNull()?.professor ?: "") }
    var selectedType by remember { mutableStateOf("CLASE") }

    val daysOfWeek = listOf(
        1 to "Lun",
        2 to "Mar",
        3 to "Mié",
        4 to "Jue",
        5 to "Vie",
        6 to "Sáb",
        7 to "Dom"
    )

    val sessionTypes = listOf("CLASE", "PRÁCTICA", "LABORATORIO", "TALLER", "ESTUDIO", "TUTORÍA")

    // Actualizar aula y profesor al cambiar asignatura
    LaunchedEffect(selectedSubjectId) {
        if (!isCustomSubject) {
            val sub = subjects.find { it.id == selectedSubjectId }
            if (sub != null) {
                classroom = sub.classroom
                professor = sub.professor
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir al Horario", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selección del día de la semana
                item {
                    Text(
                        text = "DÍA DE LA SEMANA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeek.forEach { (dInt, dLabel) ->
                            val isSelected = selectedDay == dInt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedDay = dInt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Selección de Asignatura o Personalizado
                item {
                    Text(
                        text = "MATERIA O ACTIVIDAD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (subjects.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isCustomSubject,
                                onClick = { isCustomSubject = false },
                                label = { Text("Asignatura") }
                            )
                            FilterChip(
                                selected = isCustomSubject,
                                onClick = { isCustomSubject = true },
                                label = { Text("Personalizado / Estudio") }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!isCustomSubject) {
                            // Selector de materia existente
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                subjects.forEach { sub ->
                                    val isSubSelected = selectedSubjectId == sub.id
                                    val subColor = ColorUtils.parseColorSafe(sub.colorHex, MaterialTheme.colorScheme.primary)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSubSelected) subColor.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .clickable {
                                                selectedSubjectId = sub.id
                                                classroom = sub.classroom
                                                professor = sub.professor
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(subColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = sub.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSubSelected) subColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSubSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = subColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text("Nombre de la clase o actividad") },
                                placeholder = { Text("Ej. Taller de Robótica, Biblioteca...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Nombre de la clase o actividad") },
                            placeholder = { Text("Ej. Matemáticas, Física, Programación...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Horas: Inicio y Fin
                item {
                    Text(
                        text = "HORARIOS (FORMATO 24H)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Hora Inicio") },
                            placeholder = { Text("08:30") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Hora Fin") },
                            placeholder = { Text("10:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Tipo de bloque
                item {
                    Text(
                        text = "TIPO DE SESIÓN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sessionTypes) { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Aula y Profesor
                item {
                    Text(
                        text = "UBICACIÓN Y PROFESOR (OPCIONAL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Aula / Salón / Laboratorio") },
                        placeholder = { Text("Ej. Aula 204") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = professor,
                        onValueChange = { professor = it },
                        label = { Text("Profesor / Instructor") },
                        placeholder = { Text("Ej. Dr. Rodríguez") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sub = if (!isCustomSubject) subjects.find { it.id == selectedSubjectId } else null
                    val finalSubjectId = if (!isCustomSubject) selectedSubjectId else 0L
                    val finalTitle = if (isCustomSubject) customTitle.ifBlank { "Clase" } else sub?.name ?: "Clase"
                    val finalColor = sub?.colorHex ?: "#2196F3"

                    onConfirm(
                        selectedDay,
                        startTime.ifBlank { "08:30" },
                        endTime.ifBlank { "10:00" },
                        finalSubjectId,
                        finalTitle,
                        classroom,
                        professor,
                        finalColor,
                        selectedType
                    )
                },
                modifier = Modifier.testTag("dialog_confirm_schedule")
            ) {
                Text("Guardar en Horario")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Convierte un objeto [Calendar] a un entero de día de la semana (1 = Lunes, ..., 7 = Domingo).
 */
private fun getDayOfWeekInt(cal: Calendar): Int {
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}

/**
 * Verifica si dos marcas de tiempo caen en el mismo día natural.
 */
private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
