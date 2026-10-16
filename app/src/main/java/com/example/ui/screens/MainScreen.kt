package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Exam
import com.example.data.Homework
import com.example.data.Subject
import com.example.ui.AcademiaViewModel
import com.example.ui.MainViewModel
import com.example.utils.ColorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sobrecarga de MainScreen que consume 'MainViewModel' para gestionar
 * el estado de la lista de tareas utilizando Flow de manera reactiva.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onNavigateToHomework: () -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subjects by mainViewModel.subjects.collectAsState()
    val homeworkList by mainViewModel.homeworkList.collectAsState()
    val exams by mainViewModel.exams.collectAsState()

    MainScreenContent(
        subjects = subjects,
        homeworkList = homeworkList,
        exams = exams,
        onToggleHomework = { mainViewModel.toggleHomeworkCompletion(it) },
        onUpdateExam = { mainViewModel.updateExam(it) },
        onCreateHomework = { title, subjectId, dueDate, priority, notes, notifyDays ->
            mainViewModel.createHomework(
                title = title,
                subjectId = subjectId,
                dueDate = dueDate,
                priority = priority,
                notes = notes,
                links = "",
                subtasks = "",
                notificationDaysBefore = notifyDays
            )
        },
        onCreateExam = { title, subjectId, date, difficulty, materials ->
            mainViewModel.createExam(
                title = title,
                subjectId = subjectId,
                date = date,
                difficulty = difficulty,
                preparationStatus = "NO_EMPEZADO",
                materials = materials,
                notificationDaysBefore = 2
            )
        },
        onNavigateToHomework = onNavigateToHomework,
        onNavigateToExams = onNavigateToExams,
        onNavigateToFocus = onNavigateToFocus,
        modifier = modifier
    )
}

/**
 * Pantalla Principal (MainScreen) diseñada con Material 3.
 * Presenta una lista de tareas y entregas próximas y una navegación básica
 * e intuitiva mediante pestañas/Segmented Tabs para alternar entre Deberes y Exámenes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AcademiaViewModel,
    onNavigateToHomework: () -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val homeworkList by viewModel.homeworkList.collectAsState()
    val exams by viewModel.exams.collectAsState()

    MainScreenContent(
        subjects = subjects,
        homeworkList = homeworkList,
        exams = exams,
        onToggleHomework = { viewModel.toggleHomeworkCompletion(it) },
        onUpdateExam = { viewModel.updateExam(it) },
        onCreateHomework = { title, subjectId, dueDate, priority, notes, notifyDays ->
            viewModel.createHomework(
                title = title,
                subjectId = subjectId,
                dueDate = dueDate,
                priority = priority,
                notes = notes,
                links = "",
                subtasks = "",
                notificationDaysBefore = notifyDays
            )
        },
        onCreateExam = { title, subjectId, date, difficulty, materials ->
            viewModel.createExam(
                title = title,
                subjectId = subjectId,
                date = date,
                difficulty = difficulty,
                preparationStatus = "NO_EMPEZADO",
                materials = materials,
                notificationDaysBefore = 2
            )
        },
        onNavigateToHomework = onNavigateToHomework,
        onNavigateToExams = onNavigateToExams,
        onNavigateToFocus = onNavigateToFocus,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    subjects: List<Subject>,
    homeworkList: List<Homework>,
    exams: List<Exam>,
    onToggleHomework: (Homework) -> Unit,
    onUpdateExam: (Exam) -> Unit,
    onCreateHomework: (String, Long, Long, String, String, Int) -> Unit,
    onCreateExam: (String, Long, Long, String, String) -> Unit,
    onNavigateToHomework: () -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    // Estados de filtrado y navegación entre Deberes (0), Exámenes (1) y Todo Próximo (2)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Diálogos de creación rápida
    var showAddHomeworkDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

    // Tareas pendientes y próximas
    val currentTime = System.currentTimeMillis()
    val pendingHomework = remember(homeworkList, searchQuery) {
        homeworkList
            .filter { it.status != "COMPLETADO" }
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.dueDate }
    }
    val completedHomework = remember(homeworkList) {
        homeworkList.filter { it.status == "COMPLETADO" }
    }

    // Exámenes próximos
    val upcomingExams = remember(exams, searchQuery) {
        exams
            .filter { it.date >= (currentTime - 24 * 3600 * 1000L) } // Incluye los de hoy
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.date }
    }

    // Formateadores de fecha
    val dayFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")) }
    val todayHeader = remember { dayFormat.format(Date()).replaceFirstChar { it.uppercase() } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Cabecera con diseño y margen idéntico a Menú
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tareas Próximas",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Contador resumen de pendientes
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("main_summary_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val totalPending = pendingHomework.size + upcomingExams.size
                            Text(
                                text = "$totalPending pendientes",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = todayHeader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )

                // Barra de búsqueda rápida
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("main_search_bar"),
                    placeholder = { Text("Buscar tarea o examen...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // NAVEGACIÓN BÁSICA MATERIAL 3 PARA ALTERNAR ENTRE DEBERES Y EXÁMENES
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("academic_tab_row")
                ) {
                    // Pestaña 0: Deberes
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_deberes"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Deberes",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${pendingHomework.size}",
                                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "Deberes"
                            )
                        }
                    )

                    // Pestaña 1: Exámenes
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_examenes"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Exámenes",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${upcomingExams.size}",
                                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Exámenes"
                            )
                        }
                    )

                    // Pestaña 2: Todo Próximo
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_todo"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Todo",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = if (selectedTab == 2) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${pendingHomework.size + upcomingExams.size}",
                                        color = if (selectedTab == 2) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = "Todo Próximo"
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 1) {
                        showAddExamDialog = true
                    } else {
                        showAddHomeworkDialog = true
                    }
                },
                modifier = Modifier.testTag("main_fab_add"),
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir") },
                text = {
                    Text(
                        text = when (selectedTab) {
                            1 -> "Nuevo Examen"
                            else -> "Nuevo Deber"
                        }
                    )
                },
                containerColor = when (selectedTab) {
                    1 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when (selectedTab) {
                    1 -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "tab_content_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeworkListView(
                        homeworkList = pendingHomework,
                        completedCount = completedHomework.size,
                        subjects = subjects,
                        onToggleHomework = onToggleHomework,
                        onNavigateToAll = onNavigateToHomework,
                        onAddClick = { showAddHomeworkDialog = true }
                    )

                    1 -> ExamsListView(
                        exams = upcomingExams,
                        subjects = subjects,
                        onUpdateExam = onUpdateExam,
                        onNavigateToAll = onNavigateToExams,
                        onAddClick = { showAddExamDialog = true }
                    )

                    2 -> CombinedUpcomingListView(
                        homeworkList = pendingHomework,
                        exams = upcomingExams,
                        subjects = subjects,
                        onToggleHomework = onToggleHomework,
                        onNavigateToHomework = onNavigateToHomework,
                        onNavigateToExams = onNavigateToExams
                    )
                }
            }
        }
    }

    // Diálogos de adición rápida
    if (showAddHomeworkDialog) {
        QuickAddHomeworkDialog(
            subjects = subjects,
            onDismiss = { showAddHomeworkDialog = false },
            onConfirm = { title, subjectId, dueDate, priority, notes, notifyDays ->
                onCreateHomework(title, subjectId, dueDate, priority, notes, notifyDays)
                showAddHomeworkDialog = false
            }
        )
    }

    if (showAddExamDialog) {
        QuickAddExamDialog(
            subjects = subjects,
            onDismiss = { showAddExamDialog = false },
            onConfirm = { title, subjectId, date, difficulty, materials ->
                onCreateExam(title, subjectId, date, difficulty, materials)
                showAddExamDialog = false
            }
        )
    }
}

/**
 * Vista de lista de deberes próximos.
 */
@Composable
private fun HomeworkListView(
    homeworkList: List<Homework>,
    completedCount: Int,
    subjects: List<Subject>,
    onToggleHomework: (Homework) -> Unit,
    onNavigateToAll: () -> Unit,
    onAddClick: () -> Unit
) {
    val disappearingIds = remember { mutableStateListOf<Long>() }
    val coroutineScope = rememberCoroutineScope()

    if (homeworkList.isEmpty()) {
        EmptyStateCard(
            title = "¡Al día con tus deberes!",
            subtitle = if (completedCount > 0) "Has completado $completedCount deber(es). ¡Buen trabajo!" else "No tienes deberes pendientes programados.",
            icon = Icons.Default.CheckCircleOutline,
            buttonText = "Crear nuevo deber",
            onButtonClick = onAddClick,
            modifier = Modifier.testTag("empty_homework_card")
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENTREGAS PENDIENTES (${homeworkList.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onNavigateToAll) {
                        Text("Ver todos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(homeworkList, key = { it.id }) { homework ->
                val subject = subjects.find { it.id == homework.subjectId }
                val isDisappearing = homework.id in disappearingIds

                AnimatedVisibility(
                    visible = !isDisappearing,
                    enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(280)) +
                            shrinkVertically(animationSpec = tween(280), shrinkTowards = Alignment.Top) +
                            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(280)),
                    modifier = Modifier.animateItem()
                ) {
                    HomeworkItemCard(
                        homework = if (isDisappearing) homework.copy(status = "COMPLETADO") else homework,
                        subject = subject,
                        onToggle = {
                            if (homework.id !in disappearingIds) {
                                disappearingIds.add(homework.id)
                                coroutineScope.launch {
                                    delay(290)
                                    onToggleHomework(homework)
                                    disappearingIds.remove(homework.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Vista de lista de exámenes próximos.
 */
@Composable
private fun ExamsListView(
    exams: List<Exam>,
    subjects: List<Subject>,
    onUpdateExam: (Exam) -> Unit,
    onNavigateToAll: () -> Unit,
    onAddClick: () -> Unit
) {
    if (exams.isEmpty()) {
        EmptyStateCard(
            title = "¡Sin exámenes a la vista!",
            subtitle = "No tienes evaluaciones programadas. Puedes aprovechar para estudiar o descansar.",
            icon = Icons.Default.School,
            buttonText = "Programar examen",
            onButtonClick = onAddClick,
            modifier = Modifier.testTag("empty_exams_card")
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRÓXIMAS EVALUACIONES (${exams.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    TextButton(onClick = onNavigateToAll) {
                        Text("Ver todos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(exams, key = { it.id }) { exam ->
                val subject = subjects.find { it.id == exam.subjectId }
                ExamItemCard(
                    exam = exam,
                    subject = subject,
                    onCyclePreparation = {
                        val nextStatus = when (exam.preparationStatus) {
                            "NO_EMPEZADO" -> "ESTUDIANDO"
                            "ESTUDIANDO" -> "PREPARADO"
                            else -> "NO_EMPEZADO"
                        }
                        onUpdateExam(exam.copy(preparationStatus = nextStatus))
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

/**
 * Sellado de ítems combinados ordenados por fecha.
 */
private sealed class UpcomingItem(val timestamp: Long) {
    data class HomeworkItem(val homework: Homework) : UpcomingItem(homework.dueDate)
    data class ExamItem(val exam: Exam) : UpcomingItem(exam.date)
}

/**
 * Vista combinada cronológica de próximas tareas y exámenes.
 */
@Composable
private fun CombinedUpcomingListView(
    homeworkList: List<Homework>,
    exams: List<Exam>,
    subjects: List<Subject>,
    onToggleHomework: (Homework) -> Unit,
    onNavigateToHomework: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    val disappearingIds = remember { mutableStateListOf<Long>() }
    val coroutineScope = rememberCoroutineScope()

    val combinedList = remember(homeworkList, exams) {
        val items = mutableListOf<UpcomingItem>()
        homeworkList.forEach { items.add(UpcomingItem.HomeworkItem(it)) }
        exams.forEach { items.add(UpcomingItem.ExamItem(it)) }
        items.sortedBy { it.timestamp }
    }

    if (combinedList.isEmpty()) {
        EmptyStateCard(
            title = "¡Todo al día!",
            subtitle = "No tienes deberes ni exámenes pendientes en tu agenda académica.",
            icon = Icons.Default.CheckCircle,
            buttonText = "Ir al calendario",
            onButtonClick = onNavigateToHomework
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CRONOGRAMA UNIFICADO (${combinedList.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(combinedList, key = {
                when (it) {
                    is UpcomingItem.HomeworkItem -> "hw_${it.homework.id}"
                    is UpcomingItem.ExamItem -> "ex_${it.exam.id}"
                }
            }) { item ->
                when (item) {
                    is UpcomingItem.HomeworkItem -> {
                        val subject = subjects.find { it.id == item.homework.subjectId }
                        val isDisappearing = item.homework.id in disappearingIds

                        AnimatedVisibility(
                            visible = !isDisappearing,
                            enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                            exit = fadeOut(animationSpec = tween(280)) +
                                    shrinkVertically(animationSpec = tween(280), shrinkTowards = Alignment.Top) +
                                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(280)),
                            modifier = Modifier.animateItem()
                        ) {
                            HomeworkItemCard(
                                homework = if (isDisappearing) item.homework.copy(status = "COMPLETADO") else item.homework,
                                subject = subject,
                                onToggle = {
                                    if (item.homework.id !in disappearingIds) {
                                        disappearingIds.add(item.homework.id)
                                        coroutineScope.launch {
                                            delay(290)
                                            onToggleHomework(item.homework)
                                            disappearingIds.remove(item.homework.id)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    is UpcomingItem.ExamItem -> {
                        val subject = subjects.find { it.id == item.exam.subjectId }
                        ExamItemCard(
                            exam = item.exam,
                            subject = subject,
                            onCyclePreparation = {},
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de Deber en Material 3.
 */
@Composable
fun HomeworkItemCard(
    homework: Homework,
    subject: Subject?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = remember(subject?.colorHex) {
        ColorUtils.parseColorSafe(subject?.colorHex, fallback = Color(0xFF6750A4))
    }

    val isOverdue = homework.dueDate < System.currentTimeMillis()
    val isCompleted = homework.status == "COMPLETADO"

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault()) }
    val formattedDate = remember(homework.dueDate) { dateFormat.format(Date(homework.dueDate)) }

    // Cálculo de días restantes
    val diffMs = homework.dueDate - System.currentTimeMillis()
    val daysRemaining = diffMs / (1000 * 60 * 60 * 24)
    val timeLabel = when {
        isCompleted -> "Completada"
        isOverdue -> "Atrasada"
        daysRemaining == 0L -> "Hoy"
        daysRemaining == 1L -> "Mañana"
        else -> "En $daysRemaining días"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hw_card_${homework.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOverdue && !isCompleted) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            } else {
                subjectColor.copy(alpha = 0.25f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Banda vertical de color de materia
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(subjectColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject?.name ?: "Materia",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = subjectColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Chip de Prioridad
                    val (prioBg, prioFg) = when (homework.priority.uppercase()) {
                        "ALTA" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        "MEDIA" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(prioBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = homework.priority,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = prioFg
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Chip de cuenta regresiva
                    val (timeBg, timeFg) = when {
                        isCompleted -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        isOverdue -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
                        daysRemaining <= 1L -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(timeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = timeFg
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = homework.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox para alternar estado
            IconButton(
                onClick = onToggle,
                modifier = Modifier.testTag("check_hw_${homework.id}")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Completada" else "Marcar como completada",
                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta de Examen en Material 3.
 */
@Composable
fun ExamItemCard(
    exam: Exam,
    subject: Subject?,
    onCyclePreparation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = remember(subject?.colorHex) {
        ColorUtils.parseColorSafe(subject?.colorHex, fallback = Color(0xFF7D5260))
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault()) }
    val formattedDate = remember(exam.date) { dateFormat.format(Date(exam.date)) }

    // Cuenta regresiva
    val diffMs = exam.date - System.currentTimeMillis()
    val daysRemaining = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    val hoursRemaining = ((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).coerceAtLeast(0)
    val countdownStr = when {
        diffMs < 0 -> "Examen finalizado"
        daysRemaining == 0L -> "¡Hoy a las ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(exam.date))}!"
        daysRemaining == 1L -> "¡Mañana!"
        else -> "Faltan $daysRemaining d, $hoursRemaining h"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_card_${exam.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Banda vertical con color
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject?.name ?: "Materia",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = subjectColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Chip de Dificultad
                    val (diffBg, diffFg) = when (exam.difficulty.uppercase()) {
                        "DIFÍCIL" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        "MEDIO" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(diffBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exam.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = diffFg
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Indicador de tiempo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = countdownStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Botón para ciclar estado de preparación
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (exam.preparationStatus) {
                            "PREPARADO" -> MaterialTheme.colorScheme.primaryContainer
                            "ESTUDIANDO" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCyclePreparation() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (exam.preparationStatus) {
                                "PREPARADO" -> "✓ Preparado"
                                "ESTUDIANDO" -> "⚡ Estudiando"
                                else -> "○ No iniciado"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (exam.preparationStatus) {
                                "PREPARADO" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "ESTUDIANDO" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de Estado Vacío Ilustrativo.
 */
@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonText)
                }
            }
        }
    }
}

/**
 * Diálogo de Creación Rápida de Deber.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddHomeworkDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectId: Long, dueDate: Long, priority: String, notes: String, notificationDaysBefore: Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var priority by remember { mutableStateOf("MEDIA") }
    var notes by remember { mutableStateOf("") }
    var notificationDaysBefore by remember { mutableIntStateOf(1) }

    // Fecha inicial por defecto: mañana a las 23:59
    val initialCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
    }
    var dueDate by remember { mutableLongStateOf(initialCalendar.timeInMillis) }
    var selectedPreset by remember { mutableStateOf<Int?>(1) } // 1 = Mañana por defecto

    val calendar = remember(dueDate) {
        Calendar.getInstance().apply { timeInMillis = dueDate }
    }
    val dateFormat = remember { SimpleDateFormat("EEE, d 'de' MMMM", Locale("es", "ES")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Estado animado para entrada del diálogo
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(vertical = 20.dp)
                    .wrapContentHeight()
                    .testTag("dialog_new_homework"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header con icono distintivo, títulos y botón cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddTask,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Nuevo Deber",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Organiza tu entrega escolar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // 1. Campo de Título
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Título del deber *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Ej: Resolver ejercicios de Matemáticas") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (title.isNotEmpty()) {
                                    IconButton(onClick = { title = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar título",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_hw_title")
                        )
                    }

                    // 2. Selección interactiva de Asignatura
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Asignatura",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subjects.isNotEmpty()) {
                                val currentSubject = subjects.find { it.id == selectedSubjectId }
                                if (currentSubject != null) {
                                    val currentSubjectColor = ColorUtils.parseColorSafe(
                                        currentSubject.colorHex,
                                        MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(currentSubjectColor)
                                        )
                                        Text(
                                            text = currentSubject.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = currentSubjectColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (subjects.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sin asignaturas registradas. Se guardará como General.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(subjects, key = { it.id }) { subject ->
                                    val isSelected = selectedSubjectId == subject.id
                                    val subjectColor = ColorUtils.parseColorSafe(
                                        subject.colorHex,
                                        MaterialTheme.colorScheme.primary
                                    )

                                    val backgroundColor by animateColorAsState(
                                        targetValue = if (isSelected) subjectColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "subject_bg"
                                    )

                                    val borderColor by animateColorAsState(
                                        targetValue = if (isSelected) subjectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "subject_border"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedSubjectId = subject.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = backgroundColor,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = borderColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(subjectColor)
                                            )
                                            Text(
                                                text = subject.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = subjectColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Selección de Prioridad con distintivos visuales y animaciones
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nivel de Prioridad",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val priorityOptions = listOf(
                            Triple("ALTA", Color(0xFFE53935), Icons.Default.PriorityHigh),
                            Triple("MEDIA", Color(0xFFFB8C00), Icons.Default.Flag),
                            Triple("BAJA", Color(0xFF43A047), Icons.Default.LowPriority)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorityOptions.forEach { (pLevel, pColor, pIcon) ->
                                val isSelected = priority.equals(pLevel, ignoreCase = true)

                                val chipBg by animateColorAsState(
                                    targetValue = if (isSelected) pColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "priority_bg"
                                )
                                val chipBorder by animateColorAsState(
                                    targetValue = if (isSelected) pColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "priority_border"
                                )

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { priority = pLevel },
                                    shape = RoundedCornerShape(12.dp),
                                    color = chipBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = chipBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = pIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) pColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pLevel,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Fecha y Hora de Entrega (Presets + Pickers nativos)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Fecha y Hora de Entrega",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Presets rápidos interactivos
                        val presets = listOf(
                            0 to "Hoy",
                            1 to "Mañana",
                            3 to "En 3 días",
                            7 to "1 semana"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (days, label) ->
                                val isSelected = selectedPreset == days
                                val presetBg by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "preset_bg"
                                )
                                val presetBorder by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "preset_border"
                                )

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedPreset = days
                                            val currentH = calendar.get(Calendar.HOUR_OF_DAY)
                                            val currentM = calendar.get(Calendar.MINUTE)
                                            val newCal = Calendar.getInstance().apply {
                                                add(Calendar.DAY_OF_YEAR, days)
                                                set(Calendar.HOUR_OF_DAY, currentH)
                                                set(Calendar.MINUTE, currentM)
                                                set(Calendar.SECOND, 0)
                                            }
                                            dueDate = newCal.timeInMillis
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = presetBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.2.dp else 1.dp,
                                        color = presetBorder
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Botones de selección de Fecha y Hora personalizadas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Selector de Fecha
                            Surface(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val newCal = Calendar.getInstance().apply {
                                                    timeInMillis = dueDate
                                                    set(Calendar.YEAR, y)
                                                    set(Calendar.MONTH, m)
                                                    set(Calendar.DAY_OF_MONTH, d)
                                                }
                                                dueDate = newCal.timeInMillis
                                                selectedPreset = null
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Cambiar fecha",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(calendar.time),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Selector de Hora
                            Surface(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        TimePickerDialog(
                                            context,
                                            { _, h, min ->
                                                val newCal = Calendar.getInstance().apply {
                                                    timeInMillis = dueDate
                                                    set(Calendar.HOUR_OF_DAY, h)
                                                    set(Calendar.MINUTE, min)
                                                }
                                                dueDate = newCal.timeInMillis
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Cambiar hora",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = timeFormat.format(calendar.time),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 5. Selector de Recordatorio / Notificación
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Aviso de recordatorio",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val notificationOptions = listOf(
                            -1 to "Sin aviso",
                            0 to "El mismo día",
                            1 to "1 día antes",
                            2 to "2 días antes"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            notificationOptions.forEach { (days, label) ->
                                val isSelected = notificationDaysBefore == days
                                val chipBg by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "notif_bg"
                                )
                                val chipBorder by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "notif_border"
                                )

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { notificationDaysBefore = days },
                                    shape = RoundedCornerShape(10.dp),
                                    color = chipBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.2.dp else 1.dp,
                                        color = chipBorder
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Notas / Instrucciones adicionales (opcional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Notas adicionales (Opcional)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Instrucciones, páginas, formato de entrega...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 7. Botones de Acción (Cancelar y Guardar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                        }

                        val canSave = title.isNotBlank()
                        val saveAlpha by animateFloatAsState(
                            targetValue = if (canSave) 1f else 0.5f,
                            label = "save_alpha"
                        )

                        Button(
                            onClick = {
                                if (canSave) {
                                    onConfirm(
                                        title.trim(),
                                        selectedSubjectId,
                                        dueDate,
                                        priority.uppercase(),
                                        notes.trim(),
                                        notificationDaysBefore
                                    )
                                }
                            },
                            enabled = canSave,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .scale(saveAlpha)
                                .testTag("save_hw_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar Deber",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de Creación Rápida de Examen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddExamDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectId: Long, date: Long, difficulty: String, materials: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableLongStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var difficulty by remember { mutableStateOf("MEDIA") }
    var daysAhead by remember { mutableIntStateOf(3) }
    var materials by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Examen", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del examen / Parcial") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_exam_title"),
                    singleLine = true
                )

                // Selector de Asignatura
                if (subjects.isNotEmpty()) {
                    Text("Asignatura:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.take(4).forEach { subject ->
                            val isSelected = selectedSubjectId == subject.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubjectId = subject.id },
                                label = { Text(subject.name, maxLines = 1) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Dificultad
                Text("Dificultad estimada:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FÁCIL", "MEDIA", "DIFÍCIL").forEach { d ->
                        FilterChip(
                            selected = difficulty == d,
                            onClick = { difficulty = d },
                            label = { Text(d) }
                        )
                    }
                }

                // Fecha
                Text("Fecha estimada:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "Mañana", 3 to "En 3 días", 7 to "En 1 semana", 14 to "En 2 semanas").forEach { (days, label) ->
                        FilterChip(
                            selected = daysAhead == days,
                            onClick = { daysAhead = days },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val examMs = System.currentTimeMillis() + (daysAhead * 86400000L)
                        onConfirm(title, selectedSubjectId, examMs, difficulty, materials)
                    }
                },
                enabled = title.isNotBlank()
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
