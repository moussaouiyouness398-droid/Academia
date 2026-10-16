package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.NotificationStore
import com.example.utils.ReminderManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado UI sellado (Sealed Interface) para la gestión reactiva de la lista de tareas.
 * Representa de forma inmutable los distintos estados del ciclo de vida (Loading, Success, Error).
 */
sealed interface TaskListUiState {
    data object Loading : TaskListUiState

    data class Success(
        val allTasks: List<Homework>,
        val filteredTasks: List<Homework>,
        val pendingTasks: List<Homework>,
        val completedTasks: List<Homework>,
        val highPriorityTasks: List<Homework>,
        val totalCount: Int,
        val pendingCount: Int,
        val completedCount: Int,
        val highPriorityCount: Int,
        val filterStatus: String,
        val filterPriority: String,
        val searchQuery: String,
        val selectedSubjectId: Long?
    ) : TaskListUiState

    data class Error(val message: String) : TaskListUiState
}

/**
 * Eventos UI únicos (One-shot) emitidos por MainViewModel para Snackbars y navegación.
 */
sealed interface MainUiEvent {
    data class ShowMessage(val message: String) : MainUiEvent
    data class TaskCreated(val id: Long, val title: String) : MainUiEvent
    data class TaskStatusChanged(val id: Long, val isCompleted: Boolean) : MainUiEvent
    data class TaskDeleted(val title: String) : MainUiEvent
    data class ErrorOccurred(val error: String) : MainUiEvent
}

/**
 * MainViewModel: Gestiona el estado de la lista de tareas utilizando Flow para
 * observar los cambios en la base de datos de manera reactiva e inmediata.
 *
 * Utiliza StateFlow para exponer el estado a la UI con ciclo de vida seguro (WhileSubscribed(5000)).
 * Permite filtrar por estado (TODAS, PENDIENTE, COMPLETADO), prioridad, materia y búsqueda de texto.
 */
class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AcademicRepository = AppRepository(AppDatabase.getDatabase(application)),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: kotlinx.coroutines.CoroutineScope? = null
) : AndroidViewModel(application) {

    private val scope: kotlinx.coroutines.CoroutineScope = externalScope ?: viewModelScope

    // ==========================================
    // Filtros Reactivos de la Lista de Tareas
    // ==========================================
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow("TODAS") // "TODAS", "PENDIENTE", "COMPLETADO", "EN_PROGRESO"
    val filterStatus = _filterStatus.asStateFlow()

    private val _filterPriority = MutableStateFlow("TODAS") // "TODAS", "ALTA", "MEDIA", "BAJA"
    val filterPriority = _filterPriority.asStateFlow()

    private val _filterSubjectId = MutableStateFlow<Long?>(null)
    val filterSubjectId = _filterSubjectId.asStateFlow()

    // Eventos UI únicos (one-shot events)
    private val _uiEvents = MutableSharedFlow<MainUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // ==========================================
    // Flujos Reactivos de Materias y Exámenes
    // ==========================================
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .flowOn(ioDispatcher)
        .catch { e ->
            android.util.Log.e("MainViewModel", "Error observando asignaturas", e)
            emit(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val exams: StateFlow<List<Exam>> = repository.allExams
        .flowOn(ioDispatcher)
        .catch { e ->
            android.util.Log.e("MainViewModel", "Error observando exámenes", e)
            emit(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista completa de tareas observada directamente desde Room mediante Flow
    val homeworkList: StateFlow<List<Homework>> = repository.allHomework
        .flowOn(ioDispatcher)
        .catch { e ->
            android.util.Log.e("MainViewModel", "Error en flow reactivo de tareas", e)
            emit(emptyList())
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alias directo de tareas
    val tasks: StateFlow<List<Homework>> = homeworkList

    // Flujo reactivo de tareas pendientes
    val pendingTasks: StateFlow<List<Homework>> = repository.getPendingHomework()
        .flowOn(ioDispatcher)
        .catch { emit(emptyList()) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flujo reactivo de contador de tareas pendientes
    val pendingCount: StateFlow<Int> = repository.getPendingHomeworkCount()
        .flowOn(ioDispatcher)
        .catch { emit(0) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // ==========================================
    // Estado UI Combinado y Reactivo (Flow Combine)
    // ==========================================
    val taskListUiState: StateFlow<TaskListUiState> = combine(
        repository.allHomework.flowOn(ioDispatcher),
        _searchQuery,
        _filterStatus,
        _filterPriority,
        _filterSubjectId
    ) { allTasks, query, status, priority, subjectId ->
        try {
            val pendingList = allTasks.filter { it.status != "COMPLETADO" }
            val completedList = allTasks.filter { it.status == "COMPLETADO" }
            val highPriorityList = allTasks.filter { it.priority == "ALTA" && it.status != "COMPLETADO" }

            val filtered = allTasks.filter { task ->
                val matchesStatus = when (status) {
                    "TODAS" -> true
                    "PENDIENTE" -> task.status != "COMPLETADO"
                    "COMPLETADO" -> task.status == "COMPLETADO"
                    else -> task.status.equals(status, ignoreCase = true)
                }

                val matchesPriority = when (priority) {
                    "TODAS" -> true
                    else -> task.priority.equals(priority, ignoreCase = true)
                }

                val matchesSubject = subjectId == null || task.subjectId == subjectId

                val matchesQuery = query.isBlank() ||
                        task.title.contains(query, ignoreCase = true) ||
                        task.notes.contains(query, ignoreCase = true)

                matchesStatus && matchesPriority && matchesSubject && matchesQuery
            }.sortedWith(
                compareBy<Homework> { it.status == "COMPLETADO" }
                    .thenBy { it.dueDate }
            )

            TaskListUiState.Success(
                allTasks = allTasks,
                filteredTasks = filtered,
                pendingTasks = pendingList,
                completedTasks = completedList,
                highPriorityTasks = highPriorityList,
                totalCount = allTasks.size,
                pendingCount = pendingList.size,
                completedCount = completedList.size,
                highPriorityCount = highPriorityList.size,
                filterStatus = status,
                filterPriority = priority,
                searchQuery = query,
                selectedSubjectId = subjectId
            )
        } catch (e: Throwable) {
            TaskListUiState.Error("Error al procesar la lista de tareas: ${e.localizedMessage}")
        }
    }
    .catch { e ->
        emit(TaskListUiState.Error("Fallo al observar las tareas de la base de datos: ${e.localizedMessage}"))
    }
    .stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState.Loading
    )

    // ==========================================
    // Acciones de Filtro y Búsqueda
    // ==========================================
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setFilterPriority(priority: String) {
        _filterPriority.value = priority
    }

    fun setFilterSubject(subjectId: Long?) {
        _filterSubjectId.value = subjectId
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _filterStatus.value = "TODAS"
        _filterPriority.value = "TODAS"
        _filterSubjectId.value = null
    }

    // ==========================================
    // Operaciones Reactivas con la Base de Datos
    // ==========================================

    /**
     * Alterna el estado de completado de una tarea de forma reactiva y actualiza recordatorios.
     */
    fun toggleTaskCompletion(task: Homework) {
        scope.launch {
            try {
                val nextStatus = withContext(ioDispatcher) {
                    repository.toggleHomeworkStatus(task)
                }

                if (nextStatus == "COMPLETADO") {
                    ReminderManager.cancelReminder(getApplication(), task.id, isExam = false)
                    _uiEvents.emit(MainUiEvent.ShowMessage("¡Tarea completada! 🎉: ${task.title}"))
                    _uiEvents.emit(MainUiEvent.TaskStatusChanged(task.id, isCompleted = true))
                } else {
                    if (task.notificationDaysBefore >= 0) {
                        ReminderManager.scheduleReminder(
                            context = getApplication(),
                            id = task.id,
                            isExam = false,
                            title = task.title,
                            dueDateMs = task.dueDate,
                            daysBefore = task.notificationDaysBefore
                        )
                    }
                    _uiEvents.emit(MainUiEvent.ShowMessage("Tarea marcada como pendiente"))
                    _uiEvents.emit(MainUiEvent.TaskStatusChanged(task.id, isCompleted = false))
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error alternando estado de tarea", e)
                _uiEvents.emit(MainUiEvent.ErrorOccurred("No se pudo actualizar el estado de la tarea."))
            }
        }
    }

    // Alias para compatibilidad con llamadas existentes
    fun toggleHomeworkCompletion(homework: Homework) = toggleTaskCompletion(homework)

    /**
     * Inserta una nueva tarea de forma asíncrona en la base de datos Room.
     * Al utilizar Flow, todos los observadores recibirán la actualización inmediatamente.
     */
    fun insertTask(
        title: String,
        subjectId: Long,
        dueDate: Long,
        priority: String = "MEDIA",
        notes: String = "",
        links: String = "",
        subtasks: String = "",
        notificationDaysBefore: Int = -1,
        onComplete: ((Result<Long>) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val newId = withContext(ioDispatcher) {
                    var finalSubjectId = subjectId
                    if (finalSubjectId <= 0L) {
                        finalSubjectId = subjects.value.firstOrNull()?.id ?: 1L
                    }

                    val homework = Homework(
                        title = title.trim(),
                        subjectId = finalSubjectId,
                        dueDate = dueDate,
                        priority = priority.uppercase(),
                        status = "PENDIENTE",
                        notes = notes.trim(),
                        links = links.trim(),
                        subtasks = subtasks,
                        notificationDaysBefore = notificationDaysBefore
                    )
                    repository.insertHomework(homework)
                }

                if (notificationDaysBefore >= 0) {
                    ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = newId,
                        isExam = false,
                        title = title,
                        dueDateMs = dueDate,
                        daysBefore = notificationDaysBefore
                    )
                }

                _uiEvents.emit(MainUiEvent.ShowMessage("Tarea creada: $title"))
                _uiEvents.emit(MainUiEvent.TaskCreated(newId, title))
                onComplete?.invoke(Result.success(newId))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error creando tarea", e)
                val msg = "Error al crear la tarea: ${e.localizedMessage ?: "desconocido"}"
                _uiEvents.emit(MainUiEvent.ErrorOccurred(msg))
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    // Alias para creación de deberes/tareas
    fun createHomework(
        title: String,
        subjectId: Long,
        dueDate: Long,
        priority: String = "MEDIA",
        notes: String = "",
        links: String = "",
        subtasks: String = "",
        notificationDaysBefore: Int = -1
    ) {
        insertTask(
            title = title,
            subjectId = subjectId,
            dueDate = dueDate,
            priority = priority,
            notes = notes,
            links = links,
            subtasks = subtasks,
            notificationDaysBefore = notificationDaysBefore
        )
    }

    /**
     * Actualiza una tarea existente.
     */
    fun updateTask(task: Homework) {
        scope.launch {
            try {
                withContext(ioDispatcher) {
                    repository.updateHomework(task)
                }
                ReminderManager.cancelReminder(getApplication(), task.id, isExam = false)
                if (task.notificationDaysBefore >= 0 && task.status != "COMPLETADO") {
                    ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = task.id,
                        isExam = false,
                        title = task.title,
                        dueDateMs = task.dueDate,
                        daysBefore = task.notificationDaysBefore
                    )
                }
                _uiEvents.emit(MainUiEvent.ShowMessage("Tarea actualizada"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error actualizando tarea", e)
                _uiEvents.emit(MainUiEvent.ErrorOccurred("Error al actualizar la tarea"))
            }
        }
    }

    /**
     * Elimina una tarea de la base de datos.
     */
    fun deleteTask(task: Homework) {
        scope.launch {
            try {
                withContext(ioDispatcher) {
                    repository.deleteHomework(task)
                }
                ReminderManager.cancelReminder(getApplication(), task.id, isExam = false)
                NotificationStore.removeNotificationsForTitle(task.title)
                _uiEvents.emit(MainUiEvent.TaskDeleted(task.title))
                _uiEvents.emit(MainUiEvent.ShowMessage("Tarea eliminada"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error eliminando tarea", e)
                _uiEvents.emit(MainUiEvent.ErrorOccurred("Error al eliminar la tarea"))
            }
        }
    }

    /**
     * Elimina una tarea por su identificador.
     */
    fun deleteTaskById(id: Long) {
        scope.launch {
            try {
                val task = homeworkList.value.find { it.id == id }
                withContext(ioDispatcher) {
                    repository.deleteHomeworkById(id)
                }
                ReminderManager.cancelReminder(getApplication(), id, isExam = false)
                task?.let { NotificationStore.removeNotificationsForTitle(it.title) }
                _uiEvents.emit(MainUiEvent.ShowMessage("Tarea eliminada"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error eliminando tarea por id", e)
            }
        }
    }

    /**
     * Elimina todas las tareas marcadas como completadas.
     */
    fun clearCompletedTasks() {
        scope.launch {
            try {
                val completedList = homeworkList.value.filter { it.status == "COMPLETADO" }

                val count = withContext(ioDispatcher) {
                    repository.deleteCompletedHomework()
                }

                completedList.forEach { task ->
                    ReminderManager.cancelReminder(getApplication(), task.id, isExam = false)
                    NotificationStore.removeNotificationsForTitle(task.title)
                }

                _uiEvents.emit(MainUiEvent.ShowMessage("$count tareas completadas eliminadas"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error limpiando tareas completadas", e)
            }
        }
    }

    // ==========================================
    // Exámenes (Integración complementaria para la pantalla principal)
    // ==========================================
    fun createExam(
        title: String,
        subjectId: Long,
        date: Long,
        difficulty: String = "MEDIA",
        preparationStatus: String = "PENDIENTE",
        materials: String = "",
        notificationDaysBefore: Int = -1
    ) {
        scope.launch {
            try {
                val newId = withContext(ioDispatcher) {
                    var finalSubjectId = subjectId
                    if (finalSubjectId <= 0L) {
                        finalSubjectId = subjects.value.firstOrNull()?.id ?: 1L
                    }

                    val exam = Exam(
                        title = title.trim(),
                        subjectId = finalSubjectId,
                        date = date,
                        difficulty = difficulty.uppercase(),
                        preparationStatus = preparationStatus,
                        materials = materials.trim(),
                        notificationDaysBefore = notificationDaysBefore
                    )
                    repository.insertExam(exam)
                }

                if (notificationDaysBefore >= 0) {
                    ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = newId,
                        isExam = true,
                        title = title,
                        dueDateMs = date,
                        daysBefore = notificationDaysBefore
                    )
                }

                _uiEvents.emit(MainUiEvent.ShowMessage("Examen programado: $title"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error programando examen", e)
            }
        }
    }

    fun updateExam(exam: Exam) {
        scope.launch {
            try {
                withContext(ioDispatcher) {
                    repository.updateExam(exam)
                }
                ReminderManager.cancelReminder(getApplication(), exam.id, isExam = true)
                if (exam.notificationDaysBefore >= 0) {
                    ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = exam.id,
                        isExam = true,
                        title = exam.title,
                        dueDateMs = exam.date,
                        daysBefore = exam.notificationDaysBefore
                    )
                }
                _uiEvents.emit(MainUiEvent.ShowMessage("Examen actualizado"))
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error actualizando examen", e)
            }
        }
    }
}
