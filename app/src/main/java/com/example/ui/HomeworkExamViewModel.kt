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
 * Estados sellados (Sealed UI States) para Deberes y Tareas.
 * Garantizan seguridad de tipos y control completo del ciclo de vida UI (Loading, Success, Error).
 */
sealed interface HomeworkUiState {
    data object Loading : HomeworkUiState
    data class Success(
        val allHomework: List<Homework>,
        val filteredHomework: List<Homework>,
        val pendingCount: Int,
        val inProgressCount: Int,
        val completedCount: Int,
        val highPriorityCount: Int
    ) : HomeworkUiState
    data class Error(val message: String) : HomeworkUiState
}

/**
 * Estados sellados (Sealed UI States) para Exámenes.
 */
sealed interface ExamsUiState {
    data object Loading : ExamsUiState
    data class Success(
        val allExams: List<Exam>,
        val filteredExams: List<Exam>,
        val upcomingCount: Int,
        val preparationPendingCount: Int
    ) : ExamsUiState
    data class Error(val message: String) : ExamsUiState
}

/**
 * Estado de operaciones asíncronas (creación, edición, eliminación).
 */
sealed interface AsyncOperationResult {
    data object Idle : AsyncOperationResult
    data class InProgress(val operation: String) : AsyncOperationResult
    data class Success(val message: String) : AsyncOperationResult
    data class Error(val message: String) : AsyncOperationResult
}

/**
 * Eventos puntuales de un solo disparo (One-shot UI Events) para navegación y notificaciones/Snackbars.
 */
sealed interface AcademicUiEvent {
    data class ShowMessage(val message: String) : AcademicUiEvent
    data class HomeworkCreated(val homeworkId: Long, val title: String) : AcademicUiEvent
    data class ExamCreated(val examId: Long, val title: String) : AcademicUiEvent
    data class ItemDeleted(val title: String) : AcademicUiEvent
    data class OperationFailed(val error: String) : AcademicUiEvent
}

/**
 * ViewModel especializado en gestionar el estado de los deberes y exámenes.
 * Asegura que todas las operaciones de inserción, actualización, eliminación y recuperación
 * de datos se realicen de forma asíncrona mediante Kotlin Coroutines (Dispatchers.IO) y flujos reactivos Flow de Room.
 */
class HomeworkExamViewModel @JvmOverloads constructor(
    application: Application,
    private val homeworkRepository: HomeworkRepository = DefaultHomeworkRepository(AppDatabase.getDatabase(application).homeworkDao()),
    private val examRepository: ExamRepository = DefaultExamRepository(AppDatabase.getDatabase(application).examDao()),
    private val repository: AcademicRepository = AppRepository(AppDatabase.getDatabase(application)),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    // ==========================================
    // Filtros para Deberes
    // ==========================================
    private val _homeworkFilterStatus = MutableStateFlow("TODOS") // "TODOS", "PENDIENTE", "EN_PROGRESO", "COMPLETADO"
    val homeworkFilterStatus = _homeworkFilterStatus.asStateFlow()

    private val _homeworkFilterSubjectId = MutableStateFlow<Long?>(null)
    val homeworkFilterSubjectId = _homeworkFilterSubjectId.asStateFlow()

    private val _homeworkFilterPriority = MutableStateFlow("TODAS") // "TODAS", "ALTA", "MEDIA", "BAJA"
    val homeworkFilterPriority = _homeworkFilterPriority.asStateFlow()

    private val _homeworkSearchQuery = MutableStateFlow("")
    val homeworkSearchQuery = _homeworkSearchQuery.asStateFlow()

    // ==========================================
    // Filtros para Exámenes
    // ==========================================
    private val _examsFilterSubjectId = MutableStateFlow<Long?>(null)
    val examsFilterSubjectId = _examsFilterSubjectId.asStateFlow()

    private val _examsFilterDifficulty = MutableStateFlow("TODAS") // "TODAS", "FÁCIL", "MEDIA", "DIFÍCIL"
    val examsFilterDifficulty = _examsFilterDifficulty.asStateFlow()

    private val _examsSearchQuery = MutableStateFlow("")
    val examsSearchQuery = _examsSearchQuery.asStateFlow()

    // ==========================================
    // Estado de operaciones asíncronas y eventos
    // ==========================================
    private val _asyncOperationState = MutableStateFlow<AsyncOperationResult>(AsyncOperationResult.Idle)
    val asyncOperationState = _asyncOperationState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AcademicUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // Flujo reactivo asíncrono de materias
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .flowOn(ioDispatcher)
        .catch { e ->
            android.util.Log.e("HomeworkExamViewModel", "Error recuperando asignaturas", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ==========================================
    // Estado UI Asíncrono de Deberes (Homework)
    // Combina de forma asíncrona la lista de la BD con los filtros seleccionados
    // ==========================================
    val homeworkUiState: StateFlow<HomeworkUiState> = combine(
        homeworkRepository.allHomework.flowOn(ioDispatcher),
        _homeworkFilterStatus,
        _homeworkFilterSubjectId,
        _homeworkFilterPriority,
        _homeworkSearchQuery
    ) { allList, status, subjectId, priority, query ->
        try {
            val filtered = allList.filter { hw ->
                val matchesStatus = when (status) {
                    "TODOS" -> true
                    else -> hw.status.equals(status, ignoreCase = true)
                }
                val matchesSubject = subjectId == null || hw.subjectId == subjectId
                val matchesPriority = priority == "TODAS" || hw.priority.equals(priority, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        hw.title.contains(query, ignoreCase = true) ||
                        hw.notes.contains(query, ignoreCase = true)

                matchesStatus && matchesSubject && matchesPriority && matchesQuery
            }

            val pending = allList.count { it.status == "PENDIENTE" }
            val inProgress = allList.count { it.status == "EN_PROGRESO" }
            val completed = allList.count { it.status == "COMPLETADO" }
            val highPriority = allList.count { it.priority == "ALTA" && it.status != "COMPLETADO" }

            HomeworkUiState.Success(
                allHomework = allList,
                filteredHomework = filtered,
                pendingCount = pending,
                inProgressCount = inProgress,
                completedCount = completed,
                highPriorityCount = highPriority
            )
        } catch (e: Throwable) {
            HomeworkUiState.Error("Error al procesar los deberes: ${e.localizedMessage}")
        }
    }
    .catch { e ->
        emit(HomeworkUiState.Error("Fallo al recuperar los deberes: ${e.localizedMessage}"))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeworkUiState.Loading
    )

    // ==========================================
    // Estado UI Asíncrono de Exámenes (Exams)
    // Combina de forma asíncrona la lista de la BD con los filtros de examen
    // ==========================================
    val examsUiState: StateFlow<ExamsUiState> = combine(
        examRepository.allExams.flowOn(ioDispatcher),
        _examsFilterSubjectId,
        _examsFilterDifficulty,
        _examsSearchQuery
    ) { allExams, subjectId, difficulty, query ->
        try {
            val filtered = allExams.filter { ex ->
                val matchesSubject = subjectId == null || ex.subjectId == subjectId
                val matchesDifficulty = difficulty == "TODAS" || ex.difficulty.equals(difficulty, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        ex.title.contains(query, ignoreCase = true) ||
                        ex.materials.contains(query, ignoreCase = true)

                matchesSubject && matchesDifficulty && matchesQuery
            }

            val now = System.currentTimeMillis()
            val upcoming = allExams.count { it.date >= now }
            val pendingPrep = allExams.count { it.preparationStatus != "LISTO" && it.date >= now }

            ExamsUiState.Success(
                allExams = allExams,
                filteredExams = filtered,
                upcomingCount = upcoming,
                preparationPendingCount = pendingPrep
            )
        } catch (e: Throwable) {
            ExamsUiState.Error("Error al procesar exámenes: ${e.localizedMessage}")
        }
    }
    .catch { e ->
        emit(ExamsUiState.Error("Fallo al recuperar los exámenes: ${e.localizedMessage}"))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamsUiState.Loading
    )

    // ==========================================
    // Inserción Asíncrona de Deberes
    // ==========================================
    fun insertHomework(
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
        viewModelScope.launch {
            _asyncOperationState.value = AsyncOperationResult.InProgress("Guardando tarea...")
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
                    homeworkRepository.insertHomework(homework)
                }

                // Programación defensiva de recordatorio
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

                _asyncOperationState.value = AsyncOperationResult.Success("Tarea creada correctamente")
                _uiEvents.emit(AcademicUiEvent.HomeworkCreated(newId, title))
                onComplete?.invoke(Result.success(newId))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error insertando deber", e)
                val msg = "Error al guardar el deber: ${e.localizedMessage ?: "desconocido"}"
                _asyncOperationState.value = AsyncOperationResult.Error(msg)
                _uiEvents.emit(AcademicUiEvent.OperationFailed(msg))
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    // ==========================================
    // Inserción Asíncrona de Exámenes
    // ==========================================
    fun insertExam(
        title: String,
        subjectId: Long,
        date: Long,
        difficulty: String = "MEDIA",
        preparationStatus: String = "PENDIENTE",
        materials: String = "",
        notificationDaysBefore: Int = -1,
        onComplete: ((Result<Long>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _asyncOperationState.value = AsyncOperationResult.InProgress("Programando examen...")
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
                    examRepository.insertExam(exam)
                }

                // Programar recordatorio de examen de forma segura
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

                _asyncOperationState.value = AsyncOperationResult.Success("Examen programado correctamente")
                _uiEvents.emit(AcademicUiEvent.ExamCreated(newId, title))
                onComplete?.invoke(Result.success(newId))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error insertando examen", e)
                val msg = "Error al programar el examen: ${e.localizedMessage ?: "desconocido"}"
                _asyncOperationState.value = AsyncOperationResult.Error(msg)
                _uiEvents.emit(AcademicUiEvent.OperationFailed(msg))
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    // ==========================================
    // Actualizaciones Asíncronas
    // ==========================================
    fun updateHomework(homework: Homework) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    homeworkRepository.updateHomework(homework)
                }
                ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                if (homework.notificationDaysBefore >= 0 && homework.status != "COMPLETADO") {
                    ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = homework.id,
                        isExam = false,
                        title = homework.title,
                        dueDateMs = homework.dueDate,
                        daysBefore = homework.notificationDaysBefore
                    )
                }
                _uiEvents.emit(AcademicUiEvent.ShowMessage("Tarea actualizada"))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error actualizando deber", e)
                _uiEvents.emit(AcademicUiEvent.OperationFailed("Error al actualizar tarea"))
            }
        }
    }

    fun toggleHomeworkCompletion(homework: Homework) {
        viewModelScope.launch {
            try {
                val nextStatus = withContext(ioDispatcher) {
                    homeworkRepository.toggleHomeworkStatus(homework)
                }

                if (nextStatus == "COMPLETADO") {
                    ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                    _uiEvents.emit(AcademicUiEvent.ShowMessage("¡Tarea completada! 🎉: ${homework.title}"))
                } else {
                    if (homework.notificationDaysBefore >= 0) {
                        ReminderManager.scheduleReminder(
                            context = getApplication(),
                            id = homework.id,
                            isExam = false,
                            title = homework.title,
                            dueDateMs = homework.dueDate,
                            daysBefore = homework.notificationDaysBefore
                        )
                    }
                    _uiEvents.emit(AcademicUiEvent.ShowMessage("Tarea marcada como pendiente"))
                }
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error alternando estado de tarea", e)
            }
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    examRepository.updateExam(exam)
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
                _uiEvents.emit(AcademicUiEvent.ShowMessage("Examen actualizado"))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error actualizando examen", e)
            }
        }
    }

    fun updateExamPreparationStatus(examId: Long, status: String) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    examRepository.updatePreparationStatus(examId, status)
                }
                _uiEvents.emit(AcademicUiEvent.ShowMessage("Estado de preparación actualizado a $status"))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error actualizando estado de preparación", e)
            }
        }
    }

    // ==========================================
    // Eliminación Asíncrona
    // ==========================================
    fun deleteHomework(homework: Homework) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    homeworkRepository.deleteHomework(homework)
                }
                ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                NotificationStore.removeNotificationsForTitle(homework.title)
                _uiEvents.emit(AcademicUiEvent.ItemDeleted("Deber eliminado: ${homework.title}"))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error eliminando deber", e)
            }
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    examRepository.deleteExam(exam)
                }
                ReminderManager.cancelReminder(getApplication(), exam.id, isExam = true)
                NotificationStore.removeNotificationsForTitle(exam.title)
                _uiEvents.emit(AcademicUiEvent.ItemDeleted("Examen eliminado: ${exam.title}"))
            } catch (e: Throwable) {
                android.util.Log.e("HomeworkExamViewModel", "Error eliminando examen", e)
            }
        }
    }

    // ==========================================
    // Métodos Asíncronos de Recuperación Específica
    // ==========================================
    fun getHomeworkByIdAsync(id: Long): Flow<Homework?> =
        homeworkRepository.getHomeworkById(id).flowOn(ioDispatcher)

    suspend fun getHomeworkByIdDirectAsync(id: Long): Homework? =
        homeworkRepository.getHomeworkByIdDirect(id)

    fun getHomeworkBySubjectAsync(subjectId: Long): Flow<List<Homework>> =
        homeworkRepository.getHomeworkBySubject(subjectId).flowOn(ioDispatcher)

    fun getPendingHomeworkAsync(): Flow<List<Homework>> =
        homeworkRepository.pendingHomework.flowOn(ioDispatcher)

    fun getExamByIdAsync(id: Long): Flow<Exam?> =
        examRepository.getExamById(id).flowOn(ioDispatcher)

    suspend fun getExamByIdDirectAsync(id: Long): Exam? =
        examRepository.getExamByIdDirect(id)

    fun getExamsBySubjectAsync(subjectId: Long): Flow<List<Exam>> =
        examRepository.getExamsBySubject(subjectId).flowOn(ioDispatcher)

    fun getUpcomingExamsAsync(timestamp: Long = System.currentTimeMillis()): Flow<List<Exam>> =
        examRepository.getUpcomingExams(timestamp).flowOn(ioDispatcher)

    // ==========================================
    // Controladores de Filtro
    // ==========================================
    fun setHomeworkFilterStatus(status: String) {
        _homeworkFilterStatus.value = status
    }

    fun setHomeworkFilterSubject(subjectId: Long?) {
        _homeworkFilterSubjectId.value = subjectId
    }

    fun setHomeworkFilterPriority(priority: String) {
        _homeworkFilterPriority.value = priority
    }

    fun setHomeworkSearchQuery(query: String) {
        _homeworkSearchQuery.value = query
    }

    fun setExamsFilterSubject(subjectId: Long?) {
        _examsFilterSubjectId.value = subjectId
    }

    fun setExamsFilterDifficulty(difficulty: String) {
        _examsFilterDifficulty.value = difficulty
    }

    fun setExamsSearchQuery(query: String) {
        _examsSearchQuery.value = query
    }

    fun clearAsyncOperationState() {
        _asyncOperationState.value = AsyncOperationResult.Idle
    }
}
