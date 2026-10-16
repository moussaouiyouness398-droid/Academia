package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Exam
import com.example.data.Subject
import com.example.ui.AcademicUiEvent
import com.example.ui.ExamsUiState
import com.example.ui.HomeworkExamViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    viewModel: HomeworkExamViewModel,
    onNavigateToHomework: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val examsUiState by viewModel.examsUiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val currentFilterDifficulty by viewModel.examsFilterDifficulty.collectAsStateWithLifecycle()
    val currentFilterSubjectId by viewModel.examsFilterSubjectId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.examsSearchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedExamId by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<Exam?>(null) }

    if (onNavigateBack != null) {
        BackHandler {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is AcademicUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is AcademicUiEvent.ExamCreated -> snackbarHostState.showSnackbar("Examen programado: ${event.title}")
                is AcademicUiEvent.ItemDeleted -> snackbarHostState.showSnackbar(event.title)
                is AcademicUiEvent.OperationFailed -> snackbarHostState.showSnackbar("Error: ${event.error}")
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("exam_list_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Programar Examen") },
                text = { Text("Nuevo Examen") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.testTag("add_exam_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header con Título, Botón de Regreso y Acceso Directo a Deberes uniforme a Menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Text(
                        text = "Exámenes",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("exam_list_title")
                    )
                }

                // Botón para alternar a la pantalla de Deberes
                FilledTonalButton(
                    onClick = onNavigateToHomework,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("switch_to_homework_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deberes",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Text(
                text = "Planificación de fechas y temarios de evaluación",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )

            // Barra de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setExamsSearchQuery(it) },
                placeholder = { Text("Buscar exámenes o temarios...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setExamsSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("exam_search_field")
            )

            // Resumen de Métricas de Exámenes
            if (examsUiState is ExamsUiState.Success) {
                val success = examsUiState as ExamsUiState.Success
                ExamsStatsRow(
                    totalCount = success.allExams.size,
                    upcomingCount = success.upcomingCount,
                    pendingPrepCount = success.preparationPendingCount
                )
            }

            // Filtros de Dificultad y Materia
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = currentFilterDifficulty == "TODAS" && currentFilterSubjectId == null,
                        onClick = {
                            viewModel.setExamsFilterDifficulty("TODAS")
                            viewModel.setExamsFilterSubject(null)
                        },
                        label = { Text("Todos") },
                        leadingIcon = if (currentFilterDifficulty == "TODAS" && currentFilterSubjectId == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // Dificultades
                listOf("FÁCIL", "MEDIA", "DIFÍCIL").forEach { diff ->
                    item {
                        val isSelected = currentFilterDifficulty.equals(diff, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setExamsFilterDifficulty(if (isSelected) "TODAS" else diff)
                            },
                            label = { Text(diff) }
                        )
                    }
                }

                // Materias
                items(subjects) { subject ->
                    val isSelected = currentFilterSubjectId == subject.id
                    val subjectColor = try {
                        Color(android.graphics.Color.parseColor(subject.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.tertiary
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setExamsFilterSubject(if (isSelected) null else subject.id)
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
            when (val state = examsUiState) {
                is ExamsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("exam_loading_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                            Text(
                                text = "Cargando exámenes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is ExamsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("exam_error_state"),
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

                is ExamsUiState.Success -> {
                    val examList = state.filteredExams
                    if (examList.isEmpty()) {
                        EmptyExamsState(
                            hasFilters = searchQuery.isNotBlank() || currentFilterDifficulty != "TODAS" || currentFilterSubjectId != null,
                            onResetFilters = {
                                viewModel.setExamsSearchQuery("")
                                viewModel.setExamsFilterDifficulty("TODAS")
                                viewModel.setExamsFilterSubject(null)
                            },
                            onAddNew = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .testTag("exam_items_list"),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = examList,
                                key = { it.id }
                            ) { exam ->
                                val subject = subjects.find { it.id == exam.subjectId }
                                val isExpanded = expandedExamId == exam.id

                                ExamItemCard(
                                    exam = exam,
                                    subject = subject,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedExamId = if (isExpanded) null else exam.id
                                    },
                                    onUpdatePreparationStatus = { newStatus ->
                                        viewModel.updateExamPreparationStatus(exam.id, newStatus)
                                    },
                                    onDelete = {
                                        itemToDelete = exam
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación de Eliminación
    itemToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar Examen") },
            text = { Text("¿Deseas eliminar definitivamente '${exam.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExam(exam)
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

    // Diálogo para Programar Nuevo Examen
    if (showAddDialog) {
        AddExamDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectId, date, difficulty, preparationStatus, materials, reminderDays ->
                viewModel.insertExam(
                    title = title,
                    subjectId = subjectId,
                    date = date,
                    difficulty = difficulty,
                    preparationStatus = preparationStatus,
                    materials = materials,
                    notificationDaysBefore = reminderDays
                )
                showAddDialog = false
            }
        )
    }
}

/**
 * Tarjetas de métricas para la pantalla de exámenes.
 */
@Composable
private fun ExamsStatsRow(
    totalCount: Int,
    upcomingCount: Int,
    pendingPrepCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = upcomingCount.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Próximos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = pendingPrepCount.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Por Estudiar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tarjeta individual de Examen con selector interactivo de preparación.
 */
@Composable
private fun ExamItemCard(
    exam: Exam,
    subject: Subject?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdatePreparationStatus: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = subject?.let {
        try {
            Color(android.graphics.Color.parseColor(it.colorHex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.tertiary
        }
    } ?: MaterialTheme.colorScheme.tertiary

    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM • HH:mm", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val diffMillis = exam.date - now
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

    val (countdownText, countdownColor, countdownBg) = when {
        diffMillis < 0 -> Triple("Finalizado", MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
        diffDays == 0L -> Triple("¡HOY!", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
        diffDays == 1L -> Triple("¡Mañana!", Color(0xFFE65100), Color(0xFFFFF3E0))
        diffDays in 2..7 -> Triple("En $diffDays días", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        else -> Triple("En $diffDays días", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("exam_item_${exam.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icono temático de examen
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = subjectColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = subjectColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Fila superior: Materia y Cuenta Atrás
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
                                text = subject?.name ?: "Materia",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = countdownBg
                        ) {
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = countdownColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Título del examen
                    Text(
                        text = exam.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Fecha completa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = dateFormat.format(Date(exam.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Etiquetas de Estado de Preparación y Dificultad
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Estado de Preparación
                        val (prepLabel, prepColor) = when (exam.preparationStatus) {
                            "LISTO", "PREPARADO" -> "Preparado 🎉" to Color(0xFF2E7D32)
                            "ESTUDIANDO", "EN_PROGRESO" -> "Estudiando 📖" to Color(0xFF1565C0)
                            else -> "Por empezar ⏳" to Color(0xFFE65100)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = prepColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = prepLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = prepColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Dificultad
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Dificultad: ${exam.difficulty}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Desplegable de Temario y Control de Preparación
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (exam.materials.isNotBlank()) {
                        Text(
                            text = "Temas y Material de Estudio:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = exam.materials,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                    }

                    // Selector de estado de preparación
                    Text(
                        text = "Actualizar preparación:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PENDIENTE" to "Por Empezar",
                            "ESTUDIANDO" to "Estudiando",
                            "LISTO" to "Preparado"
                        ).forEach { (key, label) ->
                            val selected = exam.preparationStatus == key
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdatePreparationStatus(key) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                            Text("Eliminar Examen")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Estado vacío para cuando no hay exámenes registrados.
 */
@Composable
private fun EmptyExamsState(
    hasFilters: Boolean,
    onResetFilters: () -> Unit,
    onAddNew: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("exam_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = if (hasFilters) "Sin exámenes que coincidan" else "Sin exámenes próximos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (hasFilters)
                    "Prueba cambiando los filtros de dificultad o materia."
                else
                    "¡Excelente! No tienes evaluaciones programadas. Programa tu próximo examen para organizarte con tiempo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (hasFilters) {
                OutlinedButton(onClick = onResetFilters) {
                    Text("Restablecer Filtros")
                }
            } else {
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Programar Primer Examen")
                }
            }
        }
    }
}

/**
 * Diálogo interactivo para programar un examen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExamDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectId: Long, date: Long, difficulty: String, preparationStatus: String, materials: String, reminderDays: Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var selectedDifficulty by remember { mutableStateOf("MEDIA") }
    var preparationStatus by remember { mutableStateOf("PENDIENTE") }
    var materials by remember { mutableStateOf("") }
    var reminderDays by remember { mutableStateOf(3) } // 3 días antes para exámenes

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
    }
    var examDateMs by remember { mutableStateOf(calendar.timeInMillis) }
    val displayFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Programar Examen",
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
                    label = { Text("Título del examen o evaluación *") },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("El título es obligatorio") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_input_title")
                )

                // Selector de Asignatura
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

                // Selector de Fecha y Hora del Examen
                OutlinedCard(
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = examDateMs }
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
                                        examDateMs = currentCal.timeInMillis
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
                                text = "Fecha y hora del examen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayFormat.format(Date(examDateMs)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // Selector de Dificultad
                Text(
                    text = "Dificultad estimada:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FÁCIL", "MEDIA", "DIFÍCIL").forEach { diff ->
                        val selected = selectedDifficulty == diff
                        FilterChip(
                            selected = selected,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Temario y materiales
                OutlinedTextField(
                    value = materials,
                    onValueChange = { materials = it },
                    label = { Text("Temario o material a estudiar (opcional)") },
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
                            examDateMs,
                            selectedDifficulty,
                            preparationStatus,
                            materials.trim(),
                            reminderDays
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.testTag("confirm_add_exam_button")
            ) {
                Text("Programar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
