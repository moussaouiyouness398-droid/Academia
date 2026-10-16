package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Homework
import com.example.data.Subject
import com.example.ui.AcademicUiEvent
import com.example.ui.AsyncOperationResult
import com.example.ui.HomeworkExamViewModel
import com.example.ui.HomeworkUiState
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkListScreen(
    viewModel: HomeworkExamViewModel,
    onNavigateToExams: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homeworkUiState by viewModel.homeworkUiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val currentFilterStatus by viewModel.homeworkFilterStatus.collectAsStateWithLifecycle()
    val currentFilterPriority by viewModel.homeworkFilterPriority.collectAsStateWithLifecycle()
    val currentFilterSubjectId by viewModel.homeworkFilterSubjectId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.homeworkSearchQuery.collectAsStateWithLifecycle()
    val asyncOpState by viewModel.asyncOperationState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedHomeworkId by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<Homework?>(null) }

    // Escuchar eventos puntuales para mostrar Snackbars
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is AcademicUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is AcademicUiEvent.HomeworkCreated -> snackbarHostState.showSnackbar("Tarea creada: ${event.title}")
                is AcademicUiEvent.ItemDeleted -> snackbarHostState.showSnackbar(event.title)
                is AcademicUiEvent.OperationFailed -> snackbarHostState.showSnackbar("Error: ${event.error}")
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("homework_list_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir Deber") },
                text = { Text("Nueva Tarea") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_homework_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header con Título, Botón de Regreso y Acceso Directo a Exámenes
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Lista de Deberes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("homework_list_title")
                            )
                            Text(
                                text = "Control y entrega de tareas escolares",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Botón para alternar a la pantalla de Exámenes
                    FilledTonalButton(
                        onClick = onNavigateToExams,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("switch_to_exams_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exámenes",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Barra de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setHomeworkSearchQuery(it) },
                placeholder = { Text("Buscar tareas por título o notas...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setHomeworkSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("homework_search_field")
            )

            // Resumen de Métricas / Estado
            if (homeworkUiState is HomeworkUiState.Success) {
                val success = homeworkUiState as HomeworkUiState.Success
                HomeworkStatsRow(
                    pendingCount = success.pendingCount,
                    inProgressCount = success.inProgressCount,
                    completedCount = success.completedCount,
                    highPriorityCount = success.highPriorityCount,
                    currentFilter = currentFilterStatus,
                    onSelectFilter = { viewModel.setHomeworkFilterStatus(it) }
                )
            }

            // Filtros rápidos por Prioridad y Materia
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip para ver Todos los estados
                item {
                    FilterChip(
                        selected = currentFilterStatus == "TODOS" && currentFilterPriority == "TODAS" && currentFilterSubjectId == null,
                        onClick = {
                            viewModel.setHomeworkFilterStatus("TODOS")
                            viewModel.setHomeworkFilterPriority("TODAS")
                            viewModel.setHomeworkFilterSubject(null)
                        },
                        label = { Text("Todos") },
                        leadingIcon = if (currentFilterStatus == "TODOS" && currentFilterPriority == "TODAS" && currentFilterSubjectId == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // Filtro Prioridad Alta
                item {
                    FilterChip(
                        selected = currentFilterPriority == "ALTA",
                        onClick = {
                            viewModel.setHomeworkFilterPriority(
                                if (currentFilterPriority == "ALTA") "TODAS" else "ALTA"
                            )
                        },
                        label = { Text("Prioridad Alta") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.PriorityHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }

                // Filtros por Materia
                items(subjects) { subject ->
                    val isSelected = currentFilterSubjectId == subject.id
                    val subjectColor = try {
                        Color(android.graphics.Color.parseColor(subject.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setHomeworkFilterSubject(if (isSelected) null else subject.id)
                        },
                        label = { Text(subject.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contenido dinámico según el Estado UI
            when (val state = homeworkUiState) {
                is HomeworkUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("homework_loading_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Cargando deberes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is HomeworkUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("homework_error_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is HomeworkUiState.Success -> {
                    val homeworkList = state.filteredHomework
                    if (homeworkList.isEmpty()) {
                        EmptyHomeworkState(
                            hasFilters = searchQuery.isNotBlank() || currentFilterStatus != "TODOS" || currentFilterPriority != "TODAS" || currentFilterSubjectId != null,
                            onResetFilters = {
                                viewModel.setHomeworkSearchQuery("")
                                viewModel.setHomeworkFilterStatus("TODOS")
                                viewModel.setHomeworkFilterPriority("TODAS")
                                viewModel.setHomeworkFilterSubject(null)
                            },
                            onAddNew = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .testTag("homework_items_list"),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = homeworkList,
                                key = { it.id }
                            ) { homework ->
                                val subject = subjects.find { it.id == homework.subjectId }
                                val isExpanded = expandedHomeworkId == homework.id

                                HomeworkItemCard(
                                    homework = homework,
                                    subject = subject,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedHomeworkId = if (isExpanded) null else homework.id
                                    },
                                    onToggleCompletion = {
                                        viewModel.toggleHomeworkCompletion(homework)
                                    },
                                    onUpdateStatus = { nextStatus ->
                                        viewModel.updateHomework(homework.copy(status = nextStatus))
                                    },
                                    onDelete = {
                                        itemToDelete = homework
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación de Eliminación
    itemToDelete?.let { hw ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar Tarea") },
            text = { Text("¿Deseas eliminar definitivamente '${hw.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHomework(hw)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para Agregar Nueva Tarea
    if (showAddDialog) {
        AddHomeworkDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectId, dueDate, priority, notes, reminderDays ->
                viewModel.insertHomework(
                    title = title,
                    subjectId = subjectId,
                    dueDate = dueDate,
                    priority = priority,
                    notes = notes,
                    notificationDaysBefore = reminderDays
                )
                showAddDialog = false
            }
        )
    }
}

/**
 * Fila de estadísticas interactivas con conteo en tiempo real.
 */
@Composable
private fun HomeworkStatsRow(
    pendingCount: Int,
    inProgressCount: Int,
    completedCount: Int,
    highPriorityCount: Int,
    currentFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(
            title = "Pendientes",
            count = pendingCount,
            isSelected = currentFilter == "PENDIENTE",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(if (currentFilter == "PENDIENTE") "TODOS" else "PENDIENTE") }
        )
        StatPill(
            title = "En Proceso",
            count = inProgressCount,
            isSelected = currentFilter == "EN_PROGRESO",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(if (currentFilter == "EN_PROGRESO") "TODOS" else "EN_PROGRESO") }
        )
        StatPill(
            title = "Completadas",
            count = completedCount,
            isSelected = currentFilter == "COMPLETADO",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(if (currentFilter == "COMPLETADO") "TODOS" else "COMPLETADO") }
        )
    }
}

@Composable
private fun StatPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = if (isSelected) 3.dp else 0.dp,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, contentColor) else null,
        modifier = modifier.height(56.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tarjeta individual de Deber con animación al expandir.
 */
@Composable
private fun HomeworkItemCard(
    homework: Homework,
    subject: Subject?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleCompletion: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = homework.status == "COMPLETADO"
    val subjectColor = subject?.let {
        try {
            Color(android.graphics.Color.parseColor(it.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val isOverdue = homework.dueDate < now && !isCompleted

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("homework_item_${homework.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Botón circular de completado interactivo
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_complete_${homework.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Marcar como pendiente" else "Completar tarea",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Fila superior: Materia y Prioridad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = subjectColor.copy(alpha = 0.15f),
                            contentColor = subjectColor
                        ) {
                            Text(
                                text = subject?.name ?: "General",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Badge de Prioridad
                        val (priorityColor, priorityBg) = when (homework.priority) {
                            "ALTA" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            "BAJA" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
                            else -> Color(0xFFE65100) to Color(0xFFFFF3E0)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = priorityBg
                        ) {
                            Text(
                                text = homework.priority,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = priorityColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Título de la Tarea
                    Text(
                        text = homework.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 5 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Fecha de Entrega y Alerta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = dateFormat.format(Date(homework.dueDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOverdue) {
                            Text(
                                text = "• ¡Vencida!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Sección Desplegable con Notas, Estado y Acciones
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (homework.notes.isNotBlank()) {
                        Text(
                            text = "Notas y descripción:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = homework.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                    }

                    // Selector de Estado Rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("PENDIENTE", "EN_PROGRESO", "COMPLETADO").forEach { status ->
                                val selected = homework.status == status
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateStatus(status) },
                                    label = {
                                        Text(
                                            when (status) {
                                                "PENDIENTE" -> "Pendiente"
                                                "EN_PROGRESO" -> "En Proceso"
                                                else -> "Listo"
                                            },
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón de Eliminación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar Tarea")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista de Estado Vacío
 */
@Composable
private fun EmptyHomeworkState(
    hasFilters: Boolean,
    onResetFilters: () -> Unit,
    onAddNew: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("homework_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = if (hasFilters) "Sin tareas con los filtros actuales" else "¡Todo al día!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (hasFilters)
                    "Intenta ajustar tus criterios de búsqueda o seleccionar otros filtros."
                else
                    "No tienes ninguna tarea pendiente registrada. Agrega una nueva para empezar a planificar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (hasFilters) {
                OutlinedButton(onClick = onResetFilters) {
                    Text("Restablecer Filtros")
                }
            } else {
                Button(onClick = onAddNew) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear Primera Tarea")
                }
            }
        }
    }
}

/**
 * Diálogo para crear una nueva tarea con selector de fecha, hora y materia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHomeworkDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectId: Long, dueDate: Long, priority: String, notes: String, reminderDays: Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var selectedPriority by remember { mutableStateOf("MEDIA") }
    var notes by remember { mutableStateOf("") }
    var reminderDays by remember { mutableStateOf(1) } // 1 día antes por defecto

    val calendar = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) } }
    var dueDateMs by remember { mutableStateOf(calendar.timeInMillis) }
    val displayFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Tarea",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Título de la tarea *") },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("El título es obligatorio") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homework_input_title")
                )

                // Selector de Materia
                if (subjects.isNotEmpty()) {
                    Text(
                        text = "Asignatura:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubjectId == subj.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubjectId = subj.id },
                                label = { Text(subj.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Selector de Fecha y Hora de Entrega
                OutlinedCard(
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = dueDateMs }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                currentCal.set(Calendar.YEAR, year)
                                currentCal.set(Calendar.MONTH, month)
                                currentCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        currentCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        currentCal.set(Calendar.MINUTE, minute)
                                        dueDateMs = currentCal.timeInMillis
                                    },
                                    currentCal.get(Calendar.HOUR_OF_DAY),
                                    currentCal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            currentCal.get(Calendar.YEAR),
                            currentCal.get(Calendar.MONTH),
                            currentCal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fecha de entrega",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayFormat.format(Date(dueDateMs)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Selector de Prioridad
                Text(
                    text = "Prioridad:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("BAJA", "MEDIA", "ALTA").forEach { p ->
                        val selected = selectedPriority == p
                        FilterChip(
                            selected = selected,
                            onClick = { selectedPriority = p },
                            label = { Text(p, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Notas adicionales
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o detalles (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onConfirm(
                            title.trim(),
                            selectedSubjectId,
                            dueDateMs,
                            selectedPriority,
                            notes.trim(),
                            reminderDays
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_homework_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
