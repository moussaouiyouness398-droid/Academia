package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Homework
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    viewModel: AcademiaViewModel,
    showAddDialogInit: Boolean = false,
    onDialogClosed: () -> Unit = {}
) {
    val subjects by viewModel.subjects.collectAsState()
    val homeworkList by viewModel.homeworkList.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("PENDIENTES", "EN PROGRESO", "COMPLETADOS")

    val filteredList = when (selectedTab) {
        0 -> homeworkList.filter { it.status == "PENDIENTE" }
        1 -> homeworkList.filter { it.status == "EN_PROGRESO" }
        else -> homeworkList.filter { it.status == "COMPLETADO" }
    }

    var showAddDialog by remember { mutableStateOf(showAddDialogInit) }
    
    // Expand state for task cards
    var expandedTaskId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(showAddDialogInit) {
        if (showAddDialogInit) {
            showAddDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir") },
                text = { Text("Añadir Deber") },
                modifier = Modifier.testTag("add_homework_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("homework_layout")
        ) {
            // Screen Title
            Text(
                text = "Gestor de Deberes",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Modern tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("homework_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "No tareas",
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay deberes en esta categoría",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "¡Disfruta de tu tiempo libre o planifica algo nuevo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredList) { homework ->
                        val subject = subjects.find { it.id == homework.subjectId }
                        val subjectColor = com.example.utils.ColorUtils.parseColorSafe(subject?.colorHex, MaterialTheme.colorScheme.primary)
                        val isExpanded = expandedTaskId == homework.id

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedTaskId = if (isExpanded) null else homework.id
                                }
                                .testTag("homework_card_${homework.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = if (isExpanded) 4.dp else 1.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(subjectColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = subject?.name ?: "Materia de Estudio",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = subjectColor
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = homework.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = if (homework.status == "COMPLETADO") TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Status controls or quick click toggle
                                    IconButton(
                                        onClick = { viewModel.toggleHomeworkCompletion(homework) }
                                    ) {
                                        Icon(
                                            imageVector = if (homework.status == "COMPLETADO") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Marcar estado",
                                            tint = if (homework.status == "COMPLETADO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Due date details
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val dateObj = Date(homework.dueDate)
                                        val formatted = SimpleDateFormat("dd 'de' MMM, HH:mm", Locale("es")).format(dateObj)
                                        Text(
                                            text = formatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Priority pill
                                    val priorityColor = when (homework.priority) {
                                        "ALTA" -> Color(0xFFF44336)
                                        "MEDIA" -> Color(0xFFFF9800)
                                        else -> Color(0xFF4CAF50)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(priorityColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = homework.priority,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            color = priorityColor
                                        )
                                    }
                                }

                                // Expandable area for subtasks, notes, links, delete button
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = tween(200)),
                                    exit = shrinkVertically(animationSpec = tween(200))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Status cycler
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Estado de tarea:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row {
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.updateHomework(homework.copy(status = "PENDIENTE"))
                                                    },
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = if (homework.status == "PENDIENTE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Pendiente", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.updateHomework(homework.copy(status = "EN_PROGRESO"))
                                                    },
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = if (homework.status == "EN_PROGRESO") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("En Progreso", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Notes
                                        if (homework.notes.isNotEmpty()) {
                                            Text(
                                                text = "Instrucciones:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = homework.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }

                                        // External link
                                        if (homework.links.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = homework.links,
                                                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Subtasks section
                                        val subtasksList = homework.getSubtaskList()
                                        if (subtasksList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Subtareas (${subtasksList.filter { it.isCompleted }.size}/${subtasksList.size}):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            subtasksList.forEachIndexed { sIdx, sub ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = sub.isCompleted,
                                                        onCheckedChange = { isChecked ->
                                                            // update subtask serialization
                                                            val updatedList = subtasksList.toMutableList()
                                                            updatedList[sIdx] = sub.copy(isCompleted = isChecked)
                                                            val serialized = updatedList.joinToString(",") { "${it.title}|${it.isCompleted}" }
                                                            viewModel.updateHomework(homework.copy(subtasks = serialized))
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = sub.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                        ),
                                                        color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Action Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.deleteHomework(homework) },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Eliminar", style = MaterialTheme.typography.labelMedium)
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

    // Creating Dialog for Adding Homework
    if (showAddDialog) {
        var hwTitle by remember { mutableStateOf("") }
        var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var hwPriority by remember { mutableStateOf("ALTA") }
        var hwNotes by remember { mutableStateOf("") }
        var hwLinks by remember { mutableStateOf("") }
        var hwSubtasksLine by remember { mutableStateOf("") } // comma-separated, e.g., "Leer pdf, resolver, subir"
        var selectedDueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000 * 3) }
        var hwReminderDaysBefore by remember { mutableStateOf(-1) }
        var showReminderDropdown by remember { mutableStateOf(false) }
        
        var showSubjectDropdown by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedDueDate } }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        selectedDueDate = calendar.timeInMillis
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                onDialogClosed()
            },
            title = {
                Text(
                    text = "Nuevo Deber Escolar",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = hwTitle,
                            onValueChange = { hwTitle = it },
                            label = { Text("Título del Deber") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hw_add_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Subject picker dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedSub = subjects.find { it.id == selectedSubjectId }
                            val fieldText = selectedSub?.name ?: "Seleccionar Materia"
                            
                            OutlinedTextField(
                                value = fieldText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Materia") },
                                trailingIcon = {
                                    IconButton(onClick = { showSubjectDropdown = !showSubjectDropdown }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSubjectDropdown = true },
                                shape = RoundedCornerShape(10.dp)
                            )

                            DropdownMenu(
                                expanded = showSubjectDropdown,
                                onDismissRequest = { showSubjectDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            showSubjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date and Time selection
                    item {
                        val formattedDate = remember(selectedDueDate) {
                            SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale("es")).format(Date(selectedDueDate))
                        }
                        OutlinedTextField(
                            value = formattedDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha de Entrega") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "Elegir fecha"
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Cambiar fecha")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Notification Reminder selection
                    item {
                        Column {
                            val reminderOptions = listOf(
                                -1 to "Desactivado",
                                0 to "El mismo día",
                                1 to "1 día antes",
                                2 to "2 días antes",
                                3 to "3 días antes",
                                5 to "5 días antes",
                                7 to "1 semana antes"
                            )
                            val selectedOptionText = reminderOptions.find { it.first == hwReminderDaysBefore }?.second ?: "Desactivado"
                            
                            Text(
                                "Alerta / Recordatorio anticipado:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showReminderDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (hwReminderDaysBefore >= 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = null,
                                                tint = if (hwReminderDaysBefore >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = selectedOptionText,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showReminderDropdown,
                                    onDismissRequest = { showReminderDropdown = false }
                                ) {
                                    reminderOptions.forEach { (days, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                hwReminderDaysBefore = days
                                                showReminderDropdown = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (days >= 0) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Priority toggle
                    item {
                        Column {
                            Text(
                                "Prioridad:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("BAJA", "MEDIA", "ALTA").forEach { priority ->
                                    val isSelected = hwPriority == priority
                                    val btnColor = when (priority) {
                                        "ALTA" -> Color(0xFFF44336)
                                        "MEDIA" -> Color(0xFFFF9800)
                                        else -> Color(0xFF4CAF50)
                                    }
                                    FilledTonalButton(
                                        onClick = { hwPriority = priority },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) btnColor else btnColor.copy(alpha = 0.1f),
                                            contentColor = if (isSelected) Color.White else btnColor
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(priority, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Notes details
                    item {
                        OutlinedTextField(
                            value = hwNotes,
                            onValueChange = { hwNotes = it },
                            label = { Text("Notas / Instrucciones") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = hwLinks,
                            onValueChange = { hwLinks = it },
                            label = { Text("Enlace / URL de Apoyo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Quick subtasks input
                    item {
                        OutlinedTextField(
                            value = hwSubtasksLine,
                            onValueChange = { hwSubtasksLine = it },
                            label = { Text("Subtareas (separadas por coma)") },
                            placeholder = { Text("Leer sección, Hacer boceto, Revisar ortografía") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hwTitle.isNotBlank()) {
                            // serialize subtasks
                            val serializedSubtasks = if (hwSubtasksLine.isNotBlank()) {
                                hwSubtasksLine.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .joinToString(",") { "$it|false" }
                            } else ""

                            // Insert
                            viewModel.createHomework(
                                title = hwTitle,
                                subjectId = selectedSubjectId,
                                dueDate = selectedDueDate,
                                priority = hwPriority,
                                notes = hwNotes,
                                links = hwLinks,
                                subtasks = serializedSubtasks,
                                notificationDaysBefore = hwReminderDaysBefore
                            )
                            showAddDialog = false
                            onDialogClosed()
                        }
                    },
                    modifier = Modifier.testTag("hw_save_confirm")
                ) {
                    Text("Programar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        onDialogClosed()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
