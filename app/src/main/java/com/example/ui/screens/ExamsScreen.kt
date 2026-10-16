package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Exam
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    viewModel: AcademiaViewModel,
    showAddDialogInit: Boolean = false,
    onDialogClosed: () -> Unit = {}
) {
    val subjects by viewModel.subjects.collectAsState()
    val exams by viewModel.exams.collectAsState()

    var showAddDialog by remember { mutableStateOf(showAddDialogInit) }
    var expandedExamId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(showAddDialogInit) {
        if (showAddDialogInit) {
            showAddDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Programar") },
                text = { Text("Programar Examen") },
                modifier = Modifier.testTag("add_exam_fab"),
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("exams_layout")
        ) {
            // Title Header
            Text(
                text = "Gestor de Exámenes",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (exams.isEmpty()) {
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
                            imageVector = Icons.Default.GolfCourse,
                            contentDescription = "No exámenes",
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "¡Sin exámenes a la vista!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Estás completamente al día con tus evaluaciones académicas.",
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
                    items(exams) { exam ->
                        val subject = subjects.find { it.id == exam.subjectId }
                        val subjectColor = com.example.utils.ColorUtils.parseColorSafe(subject?.colorHex, MaterialTheme.colorScheme.tertiary)
                        val isExpanded = expandedExamId == exam.id

                        // Calculate countdown details
                        val diffMills = exam.date - System.currentTimeMillis()
                        val isOverdue = diffMills < 0
                        val daysLeft = (diffMills / 86400000L).coerceAtLeast(0)
                        val hoursLeft = ((diffMills % 86400000L) / 3600000L).coerceAtLeast(0)
                        
                        val countdownCaption = when {
                            isOverdue -> {
                                val overdueDays = (-diffMills / 86400000L)
                                if (overdueDays > 0) "Pasado hace $overdueDays día(s)" else "Finalizado hoy"
                            }
                            daysLeft > 0 -> "Faltan $daysLeft días, $hoursLeft h"
                            else -> "¡Hoy a las ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(exam.date))}!"
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedExamId = if (isExpanded) null else exam.id
                                }
                                .testTag("exam_card_${exam.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isOverdue) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(subjectColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = subject?.name ?: "Materia",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = subjectColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Countdown Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isOverdue) MaterialTheme.colorScheme.outlineVariant 
                                                else MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = countdownCaption,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isOverdue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = exam.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Scheduled date & hour
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val examDateStr = SimpleDateFormat("dd 'de' MMM, yyyy - HH:mm", Locale("es")).format(Date(exam.date))
                                        Text(
                                            text = examDateStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Preparation indicator pill
                                    val prepColor = when (exam.preparationStatus) {
                                        "PREPARADO" -> Color(0xFF4CAF50)
                                        "ESTUDIANDO" -> Color(0xFF1E88E5)
                                        else -> Color(0xFFF44336)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(prepColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = exam.preparationStatus,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            color = prepColor
                                        )
                                    }
                                }

                                // Interactive studying layout expanding click-through
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Preparation level adjust row
                                        Text(
                                            text = "Control de preparación:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("NO_EMPEZADO", "ESTUDIANDO", "PREPARADO").forEach { status ->
                                                val isSelected = exam.preparationStatus == status
                                                val col = when (status) {
                                                    "PREPARADO" -> Color(0xFF4CAF50)
                                                    "ESTUDIANDO" -> Color(0xFF1E88E5)
                                                    else -> Color(0xFFF44336)
                                                }
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.updateExam(exam.copy(preparationStatus = status))
                                                    },
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = if (isSelected) col else col.copy(alpha = 0.08f),
                                                        contentColor = if (isSelected) Color.White else col
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    val displayStatus = when(status) {
                                                        "NO_EMPEZADO" -> "Sin Empezar"
                                                        "ESTUDIANDO" -> "Estudiando"
                                                        else -> "Listo"
                                                    }
                                                    Text(displayStatus, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Materiales / Temario de estudio
                                        Text(
                                            text = "Temas & Guías de Estudio:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.background
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (exam.materials.trim().isEmpty()) "Aún no se han añadido temas de estudio." else exam.materials,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Quick Details details & cancel button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Difficulty Rating Badge
                                            val diffColor = when (exam.difficulty) {
                                                "DIFÍCIL" -> Color(0xFFD32F2F)
                                                "MEDIO" -> Color(0xFFF57C00)
                                                else -> Color(0xFF388E3C)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Dificultad:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(diffColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = exam.difficulty,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = diffColor
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.deleteExam(exam) },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cancelar Examen", style = MaterialTheme.typography.labelMedium)
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

    // Creating Dialog for Adding Exams
    if (showAddDialog) {
        var examTitle by remember { mutableStateOf("") }
        var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var examDifficulty by remember { mutableStateOf("MEDIO") }
        var examMaterials by remember { mutableStateOf("") }
        var selectedExamDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000 * 5) }
        var examReminderDaysBefore by remember { mutableStateOf(-1) }
        var showReminderDropdown by remember { mutableStateOf(false) }
        
        var showSubjectDropdown by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedExamDate } }

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
                        selectedExamDate = calendar.timeInMillis
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
                    text = "Próxima Evaluación Escolar",
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
                            value = examTitle,
                            onValueChange = { examTitle = it },
                            label = { Text("Materia o Tema del Examen") },
                            placeholder = { Text("Examen Final de Física, Control 2...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exam_add_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Subject dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedSub = subjects.find { it.id == selectedSubjectId }
                            val fieldText = selectedSub?.name ?: "Seleccionar Materia"
                            
                            OutlinedTextField(
                                value = fieldText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Materia del Examen") },
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
                        val formattedDate = remember(selectedExamDate) {
                            SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale("es")).format(Date(selectedExamDate))
                        }
                        OutlinedTextField(
                            value = formattedDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha del Examen") },
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
                            val selectedOptionText = reminderOptions.find { it.first == examReminderDaysBefore }?.second ?: "Desactivado"
                            
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
                                                imageVector = if (examReminderDaysBefore >= 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = null,
                                                tint = if (examReminderDaysBefore >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                                examReminderDaysBefore = days
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

                    // Difficulty selector
                    item {
                        Column {
                            Text(
                                "Nivel de Dificultad:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("FÁCIL", "MEDIO", "DIFÍCIL").forEach { level ->
                                    val isSelected = examDifficulty == level
                                    val btnColor = when (level) {
                                        "DIFÍCIL" -> Color(0xFFD32F2F)
                                        "MEDIO" -> Color(0xFFF57C00)
                                        else -> Color(0xFF388E3C)
                                    }
                                    FilledTonalButton(
                                        onClick = { examDifficulty = level },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) btnColor else btnColor.copy(alpha = 0.1f),
                                            contentColor = if (isSelected) Color.White else btnColor
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(level, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Study Guides
                    item {
                        OutlinedTextField(
                            value = examMaterials,
                            onValueChange = { examMaterials = it },
                            label = { Text("Materiales de Estudio / Temario") },
                            placeholder = { Text("Listar capítulos del libro, fórmulas clave...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (examTitle.isNotBlank()) {
                            viewModel.createExam(
                                title = examTitle,
                                subjectId = selectedSubjectId,
                                date = selectedExamDate,
                                difficulty = examDifficulty,
                                preparationStatus = "NO_EMPEZADO",
                                materials = examMaterials,
                                notificationDaysBefore = examReminderDaysBefore
                            )
                            showAddDialog = false
                            onDialogClosed()
                        }
                    },
                    modifier = Modifier.testTag("exam_save_confirm")
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
