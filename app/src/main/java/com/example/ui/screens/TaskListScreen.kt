package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Homework
import com.example.data.Subject
import com.example.ui.MainUiEvent
import com.example.ui.MainViewModel
import com.example.ui.TaskListUiState
import com.example.ui.components.TaskItem
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

/**
 * TaskListScreen: Pantalla dedicada para la visualización y gestión de tareas que
 * utiliza [MainViewModel] para observar la base de datos de manera reactiva mediante Kotlin Flow.
 *
 * Características principales:
 * - Lista de tareas reactiva con animaciones de ítem automáticas
 * - Componente reutilizable [TaskItem] con soporte para expandir, editar y alternar completado
 * - Barra de búsqueda reactiva por título o notas
 * - Filtros rápidos por estado (Todas, Pendientes, Completadas) y prioridad (Todas, Alta, Media, Baja)
 * - Filtros por materia con colores temáticos
 * - Contador de tareas pendientes y completadas en tiempo real
 * - Diálogo completo para crear y editar tareas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: MainViewModel,
    onNavigateToExams: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.taskListUiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Homework?>(null) }
    var expandedTaskId by remember { mutableStateOf<Long?>(null) }
    var taskToDelete by remember { mutableStateOf<Homework?>(null) }

    if (onNavigateBack != null) {
        BackHandler {
            onNavigateBack()
        }
    }

    // Escuchar eventos puntuales (One-Shot UI Events) para Snackbars
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is MainUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is MainUiEvent.TaskCreated -> snackbarHostState.showSnackbar("Tarea creada: ${event.title}")
                is MainUiEvent.TaskStatusChanged -> {
                    val statusText = if (event.isCompleted) "marcada como completada" else "marcada como pendiente"
                    snackbarHostState.showSnackbar("Tarea $statusText")
                }
                is MainUiEvent.TaskDeleted -> snackbarHostState.showSnackbar("Tarea eliminada: ${event.title}")
                is MainUiEvent.ErrorOccurred -> snackbarHostState.showSnackbar("Error: ${event.error}")
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_list_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir Tarea") },
                text = { Text("Nueva Tarea") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("task_list_add_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cabecera Principal con Navegación y Acciones uniforme a Menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("task_list_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Mis Tareas",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Acciones rápidas (Acceso a exámenes o limpiar completadas)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onNavigateToExams != null) {
                        FilledTonalButton(
                            onClick = onNavigateToExams,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("nav_to_exams_button")
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exámenes", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Text(
                text = "Organización académica y deberes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                    // Buscador Reactivo
                    when (val state = uiState) {
                        is TaskListUiState.Success -> {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Buscar tareas por título o apuntes...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("task_search_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Resumen de Métricas Rápidas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatBadge(
                                    label = "Pendientes",
                                    count = state.pendingCount,
                                    color = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    label = "Completadas",
                                    count = state.completedCount,
                                    color = Color(0xFF2E7D32),
                                    containerColor = Color(0xFFE8F5E9),
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    label = "Alta Prioridad",
                                    count = state.highPriorityCount,
                                    color = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {}
                    }
                }

            // Filtros rápidos horizontales (Estado, Prioridad y Asignaturas)
            when (val state = uiState) {
                is TaskListUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Filtros de Estado
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val statusFilters = listOf(
                                "TODAS" to "Todas (${state.totalCount})",
                                "PENDIENTE" to "Pendientes (${state.pendingCount})",
                                "COMPLETADO" to "Completadas (${state.completedCount})"
                            )
                            items(statusFilters) { (status, label) ->
                                val selected = state.filterStatus == status
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setFilterStatus(status) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_status_$status")
                                )
                            }

                            item {
                                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))
                            }

                            // Filtros de Prioridad
                            val priorityFilters = listOf("TODAS", "ALTA", "MEDIA", "BAJA")
                            items(priorityFilters) { priority ->
                                val selected = state.filterPriority == priority
                                val label = if (priority == "TODAS") "Cualquier Prioridad" else priority
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setFilterPriority(priority) },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_priority_$priority")
                                )
                            }
                        }

                        // Filtros por Asignatura si existen
                        if (subjects.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = state.selectedSubjectId == null
                                    SuggestionChip(
                                        onClick = { viewModel.setFilterSubject(null) },
                                        label = { Text("Todas las asignaturas", style = MaterialTheme.typography.labelSmall) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isAllSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                items(subjects) { subject ->
                                    val isSelected = state.selectedSubjectId == subject.id
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.setFilterSubject(if (isSelected) null else subject.id)
                                        },
                                        label = { Text(subject.name, style = MaterialTheme.typography.labelSmall) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("filter_subject_${subject.id}")
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }

            // Contenido Principal de la Lista de Tareas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is TaskListUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                        }
                    }
                    is TaskListUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is TaskListUiState.Success -> {
                        val displayTasks = state.filteredTasks

                        if (displayTasks.isEmpty()) {
                            // Estado Vacío Informativo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (state.searchQuery.isNotBlank()) "Sin coincidencias" else "¡Todo al día!",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = if (state.searchQuery.isNotBlank()) {
                                            "No se encontraron tareas para \"${state.searchQuery}\""
                                        } else if (state.filterStatus == "COMPLETADO") {
                                            "No hay tareas completadas todavía."
                                        } else {
                                            "No tienes tareas pendientes con los filtros actuales."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    FilledTonalButton(
                                        onClick = {
                                            taskToEdit = null
                                            showAddDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Crear Tarea Ahora")
                                    }
                                }
                            }
                        } else {
                            // Lista Reactiva con LazyColumn y componente TaskItem
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 6.dp,
                                    bottom = 96.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("task_lazy_column")
                            ) {
                                items(
                                    items = displayTasks,
                                    key = { it.id }
                                ) { task ->
                                    val subject = subjects.find { it.id == task.subjectId }
                                    TaskItem(
                                        homework = task,
                                        subject = subject,
                                        isExpanded = expandedTaskId == task.id,
                                        onToggleCompletion = {
                                            viewModel.toggleTaskCompletion(task)
                                        },
                                        onClick = {
                                            expandedTaskId = if (expandedTaskId == task.id) null else task.id
                                        },
                                        onEdit = {
                                            taskToEdit = task
                                            showAddDialog = true
                                        },
                                        onDelete = {
                                            taskToDelete = task
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar Tarea
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar Tarea") },
            text = { Text("¿Deseas eliminar permanentemente la tarea \"${task.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para Crear o Editar Tarea
    if (showAddDialog) {
        TaskFormDialog(
            taskToEdit = taskToEdit,
            subjects = subjects,
            onDismiss = {
                showAddDialog = false
                taskToEdit = null
            },
            onSave = { title, subjectId, dueDate, priority, notes, notifyDays ->
                if (taskToEdit == null) {
                    viewModel.insertTask(
                        title = title,
                        subjectId = subjectId,
                        dueDate = dueDate,
                        priority = priority,
                        notes = notes,
                        notificationDaysBefore = notifyDays
                    )
                } else {
                    viewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            subjectId = subjectId,
                            dueDate = dueDate,
                            priority = priority,
                            notes = notes,
                            notificationDaysBefore = notifyDays
                        )
                    )
                }
                showAddDialog = false
                taskToEdit = null
            }
        )
    }
}

/**
 * Badge estadístico reutilizable para el panel superior.
 */
@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

/**
 * Diálogo de creación/edición de tareas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    taskToEdit: Homework?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subjectId: Long,
        dueDate: Long,
        priority: String,
        notes: String,
        notificationDaysBefore: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(taskToEdit?.subjectId ?: subjects.firstOrNull()?.id ?: 1L) }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "MEDIA") }
    var notes by remember { mutableStateOf(taskToEdit?.notes ?: "") }
    var dueDate by remember { mutableStateOf(taskToEdit?.dueDate ?: (System.currentTimeMillis() + 86400000L)) }
    var notifyDays by remember { mutableStateOf(taskToEdit?.notificationDaysBefore ?: 1) }

    val calendar = remember(dueDate) {
        Calendar.getInstance().apply { timeInMillis = dueDate }
    }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "Nueva Tarea" else "Editar Tarea",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Campo Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea *") },
                    placeholder = { Text("Ej: Resolver ejercicios de cálculo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_title")
                )

                // Selección de Asignatura
                Text(
                    text = "Asignatura",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (subjects.isEmpty()) {
                    Text(
                        text = "General (Sin materias creadas)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubjectId == subj.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubjectId = subj.id },
                                label = { Text(subj.name) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Selección de Prioridad
                Text(
                    text = "Prioridad",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALTA", "MEDIA", "BAJA").forEach { p ->
                        val isSelected = priority.equals(p, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = p },
                            label = { Text(p) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Fecha y Hora de Vencimiento
                Text(
                    text = "Fecha y Hora de Entrega",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    calendar.set(Calendar.YEAR, y)
                                    calendar.set(Calendar.MONTH, m)
                                    calendar.set(Calendar.DAY_OF_MONTH, d)
                                    dueDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dateFormat.format(calendar.time), style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    calendar.set(Calendar.HOUR_OF_DAY, h)
                                    calendar.set(Calendar.MINUTE, min)
                                    dueDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(timeFormat.format(calendar.time), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Apuntes / Notas adicionales
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o instrucciones adicionales") },
                    placeholder = { Text("Ej: Páginas 40 a 45, formato PDF") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), selectedSubjectId, dueDate, priority, notes.trim(), notifyDays)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("task_dialog_save_button")
            ) {
                Text(if (taskToEdit == null) "Crear Tarea" else "Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
