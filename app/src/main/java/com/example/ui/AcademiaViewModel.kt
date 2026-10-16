package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class AcademiaViewModel(application: Application) : AndroidViewModel(application), Serializable {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AcademiaRepository(database)

    // Database flows exposed into StateFlow with asynchronous IO dispatching
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeworkList: StateFlow<List<Homework>> = repository.allHomework
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<Exam>> = repository.allExams
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<AcademicNote>> = repository.allNotes
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSession>> = repository.allFocusSessions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleEntries: StateFlow<List<ScheduleEntry>> = repository.allScheduleEntries
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estados UI Asíncronos para Deberes y Exámenes
    val homeworkUiState: StateFlow<HomeworkUiState> = repository.allHomework
        .flowOn(Dispatchers.IO)
        .map { list ->
            val pending = list.count { it.status == "PENDIENTE" }
            val inProgress = list.count { it.status == "EN_PROGRESO" }
            val completed = list.count { it.status == "COMPLETADO" }
            val highPriority = list.count { it.priority == "ALTA" && it.status != "COMPLETADO" }
            HomeworkUiState.Success(
                allHomework = list,
                filteredHomework = list,
                pendingCount = pending,
                inProgressCount = inProgress,
                completedCount = completed,
                highPriorityCount = highPriority
            ) as HomeworkUiState
        }
        .catch { emit(HomeworkUiState.Error("Error al cargar tareas: ${it.localizedMessage}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeworkUiState.Loading)

    val examsUiState: StateFlow<ExamsUiState> = repository.allExams
        .flowOn(Dispatchers.IO)
        .map { list ->
            val now = System.currentTimeMillis()
            val upcoming = list.count { it.date >= now }
            val pendingPrep = list.count { it.preparationStatus != "LISTO" && it.date >= now }
            ExamsUiState.Success(
                allExams = list,
                filteredExams = list,
                upcomingCount = upcoming,
                preparationPendingCount = pendingPrep
            ) as ExamsUiState
        }
        .catch { emit(ExamsUiState.Error("Error al cargar exámenes: ${it.localizedMessage}")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExamsUiState.Loading)

    // Pomodoro/Focus Timer State
    private val _timerSecondsLeft = MutableStateFlow(25 * 60)
    val timerSecondsLeft = _timerSecondsLeft.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds = _timerTotalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _timerMode = MutableStateFlow("ESTUDIO") // "ESTUDIO" o "DESCANSO"
    val timerMode = _timerMode.asStateFlow()

    private val _timerSubjectId = MutableStateFlow<Long?>(null)
    val timerSubjectId = _timerSubjectId.asStateFlow()

    private var timerJob: Job? = null

    // Theme customization states
    private val _isDarkMode = MutableStateFlow(true) // Default to premium dark as requested!
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _selectedThemeColor = MutableStateFlow("#6750A4") // Purple M3
    val selectedThemeColor = _selectedThemeColor.asStateFlow()

    private val backupManager = BackupManager(application, database)

    // Cloud Vault information state
    private val _cloudVaultInfo = MutableStateFlow(backupManager.getCloudVaultInfo())
    val cloudVaultInfo = _cloudVaultInfo.asStateFlow()

    // Sync cloud state
    private val _syncState = MutableStateFlow("IDLE") // "IDLE", "SYNCING", "SUCCESS", "ERROR"
    val syncState = _syncState.asStateFlow()

    // Notification message list (centralized in NotificationStore)
    val notifications = com.example.utils.NotificationStore.notifications

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredNotes: StateFlow<List<AcademicNote>> = _searchQuery
        .combine(notes) { query, allNotes ->
            if (query.isEmpty()) {
                allNotes
            } else {
                allNotes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSubjects = repository.allSubjects.first()
                if (currentSubjects.isEmpty()) {
                    val s1 = Subject(name = "Matemáticas Avanzadas", colorHex = "#2196F3", professor = "Dr. Alberto Ramírez", classroom = "Aula 302", iconName = "Functions")
                    val s2 = Subject(name = "Literatura Universal", colorHex = "#E91E63", professor = "Dra. Sofía Galván", classroom = "Aula 104", iconName = "Book")
                    val s3 = Subject(name = "Física Cuántica I", colorHex = "#9C27B0", professor = "Prof. Juan Meléndez", classroom = "Laboratorio de Ciencias", iconName = "Science")
                    val s4 = Subject(name = "Programación Móvil", colorHex = "#4CAF50", professor = "MSc. Carlos Martínez", classroom = "Aula de Cómputo B", iconName = "Computer")

                    val id1 = repository.insertSubject(s1)
                    val id2 = repository.insertSubject(s2)
                    val id3 = repository.insertSubject(s3)
                    val id4 = repository.insertSubject(s4)

                    val now = System.currentTimeMillis()
                    val twoDays = now + 86400000L * 2
                    val fiveDays = now + 86400000L * 5
                    val sevenDays = now + 86400000L * 7

                    repository.insertHomework(Homework(
                        title = "Ecuaciones Diferenciales Parciales",
                        subjectId = id1,
                        dueDate = twoDays,
                        priority = "ALTA",
                        status = "PENDIENTE",
                        notes = "Resolver ejercicios 1 al 10 del capítulo 4",
                        subtasks = "Hacer bosquejos|false,Escribir desarrollos|false"
                    ))
                    repository.insertHomework(Homework(
                        title = "Proyecto final: App Android",
                        subjectId = id4,
                        dueDate = sevenDays,
                        priority = "ALTA",
                        status = "EN_PROGRESO",
                        notes = "Estructurar la persistencia de datos y pulir el diseño usando Jetpack Compose",
                        subtasks = "Diseñar base de datos Room|true,Añadir vistas Navigation|true"
                    ))
                    repository.insertExam(Exam(
                        title = "Primer Examen Parcial de Electromagnetismo",
                        subjectId = id3,
                        date = fiveDays,
                        difficulty = "DIFÍCIL",
                        preparationStatus = "ESTUDIANDO",
                        materials = "Leyes de Maxwell, potencial eléctrico"
                    ))
                }
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error in init seeding", e)
            }
        }
    }

    // Actions & Handlers
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setThemeColor(colorHex: String) {
        _selectedThemeColor.value = colorHex
    }

    // Real Cloud Synchronization
    fun syncData(onFinished: ((CloudVaultInfo) -> Unit)? = null) {
        viewModelScope.launch {
            _syncState.value = "SYNCING"
            try {
                delay(800)
                val info = backupManager.syncWithCloudVault()
                _cloudVaultInfo.value = info
                _syncState.value = "SUCCESS"
                addNotification("Sincronización en la nube completada: ${info.totalItems} elementos asegurados.")
                onFinished?.invoke(info)
                delay(2200)
            } catch (e: Exception) {
                _syncState.value = "ERROR"
                addNotification("Error al sincronizar con la nube: ${e.localizedMessage}")
                delay(2000)
            } finally {
                _syncState.value = "IDLE"
            }
        }
    }

    fun restoreFromCloud(onResult: (RestoreSummary) -> Unit) {
        viewModelScope.launch {
            _syncState.value = "SYNCING"
            try {
                delay(500)
                val result = backupManager.restoreFromCloudVault()
                if (result.success) {
                    addNotification("Restaurados ${result.totalItems} elementos desde la copia en la nube.")
                } else {
                    addNotification("Error al restaurar: ${result.errorMessage ?: "Fallo desconocido"}")
                }
                onResult(result)
            } catch (e: Exception) {
                onResult(RestoreSummary(success = false, errorMessage = e.localizedMessage))
            } finally {
                _syncState.value = "IDLE"
            }
        }
    }

    suspend fun exportBackupJson(): String = backupManager.createFullBackupJson()

    suspend fun importBackupJson(jsonString: String, replaceAll: Boolean = false): RestoreSummary {
        val result = backupManager.restoreBackupFromJson(jsonString, replaceAll)
        if (result.success) {
            addNotification("Copia importada: ${result.totalItems} elementos restaurados en Academia.")
            val info = backupManager.syncWithCloudVault()
            _cloudVaultInfo.value = info
        } else {
            addNotification("Error al importar copia: ${result.errorMessage ?: "Archivo no compatible"}")
        }
        return result
    }

    fun addNotification(message: String) {
        com.example.utils.NotificationStore.addNotification(message)
    }

    fun clearNotifications() {
        com.example.utils.NotificationStore.clearNotifications()
    }

    // Timer management
    fun setTimerDuration(minutes: Int) {
        _timerTotalSeconds.value = minutes * 60
        _timerSecondsLeft.value = minutes * 60
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun setTimerSubject(subjectId: Long?) {
        _timerSubjectId.value = subjectId
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch(Dispatchers.Main) {
            while (_timerSecondsLeft.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerSecondsLeft.value -= 1
            }
            if (_timerSecondsLeft.value == 0) {
                finishTimerPeriod()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSecondsLeft.value = _timerTotalSeconds.value
    }

    private suspend fun finishTimerPeriod() {
        _isTimerRunning.value = false
        val mode = _timerMode.value
        val minutesStudied = _timerTotalSeconds.value / 60

        if (mode == "ESTUDIO") {
            // Save focus session
            val session = FocusSession(
                minutes = minutesStudied,
                subjectId = _timerSubjectId.value ?: 0
            )
            repository.insertFocusSession(session)
            addNotification("¡Enhorabuena! Has completado una sesión de estudio de $minutesStudied minutos.")
            
            // Switch to break
            _timerMode.value = "DESCANSO"
            setTimerDuration(5)
        } else {
            addNotification("¡Fin del descanso! Es hora de volver a enfocarse.")
            _timerMode.value = "ESTUDIO"
            setTimerDuration(25)
        }
    }

    // Subject operations
    fun createSubject(name: String, colorHex: String, professor: String, classroom: String, iconName: String = "School") {
        viewModelScope.launch {
            val subject = Subject(
                name = name,
                colorHex = colorHex,
                professor = professor,
                classroom = classroom,
                iconName = iconName
            )
            repository.insertSubject(subject)
            addNotification("Nueva materia añadida: $name")
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            addNotification("Materia eliminada: ${subject.name}")
        }
    }

    // Homework operations
    fun createHomework(title: String, subjectId: Long, dueDate: Long, priority: String, notes: String, links: String, subtasks: String, notificationDaysBefore: Int = -1) {
        viewModelScope.launch {
            try {
                var actualSubjectId = subjectId
                if (actualSubjectId <= 0L) {
                    actualSubjectId = subjects.value.firstOrNull()?.id ?: 1L
                }
                val h = Homework(
                    title = title.trim(),
                    subjectId = actualSubjectId,
                    dueDate = dueDate,
                    priority = priority,
                    status = "PENDIENTE",
                    notes = notes,
                    links = links,
                    subtasks = subtasks,
                    notificationDaysBefore = notificationDaysBefore
                )
                val id = withContext(Dispatchers.IO) {
                    repository.insertHomework(h)
                }
                if (notificationDaysBefore >= 0) {
                    com.example.utils.ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = id,
                        isExam = false,
                        title = title,
                        dueDateMs = dueDate,
                        daysBefore = notificationDaysBefore
                    )
                    val daysStr = if (notificationDaysBefore == 0) "el mismo día" else "con $notificationDaysBefore día(s) de antelación"
                    addNotification("Recordatorio programado para la tarea '$title' ($daysStr).")
                }
                addNotification("Nuevo deber programado: $title")
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error creating homework", e)
                addNotification("Error al guardar la tarea: ${e.message}")
            }
        }
    }

    fun updateHomework(homework: Homework) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateHomework(homework)
                }
                com.example.utils.ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                if (homework.notificationDaysBefore >= 0) {
                    com.example.utils.ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = homework.id,
                        isExam = false,
                        title = homework.title,
                        dueDateMs = homework.dueDate,
                        daysBefore = homework.notificationDaysBefore
                    )
                }
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error updating homework", e)
            }
        }
    }

    fun toggleHomeworkCompletion(homework: Homework) {
        viewModelScope.launch {
            try {
                val nextStatus = if (homework.status == "COMPLETADO") "PENDIENTE" else "COMPLETADO"
                val updated = homework.copy(status = nextStatus)
                withContext(Dispatchers.IO) {
                    repository.updateHomework(updated)
                }
                if (nextStatus == "COMPLETADO") {
                    com.example.utils.ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                    addNotification("¡Deber completado! 🎉: ${homework.title}")
                } else {
                    if (homework.notificationDaysBefore >= 0) {
                        com.example.utils.ReminderManager.scheduleReminder(
                            context = getApplication(),
                            id = homework.id,
                            isExam = false,
                            title = homework.title,
                            dueDateMs = homework.dueDate,
                            daysBefore = homework.notificationDaysBefore
                        )
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error toggling homework", e)
            }
        }
    }

    fun deleteHomework(homework: Homework) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteHomework(homework)
                }
                com.example.utils.ReminderManager.cancelReminder(getApplication(), homework.id, isExam = false)
                com.example.utils.NotificationStore.removeNotificationsForTitle(homework.title)
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error deleting homework", e)
            }
        }
    }

    // Exam operations
    fun createExam(title: String, subjectId: Long, date: Long, difficulty: String, preparationStatus: String, materials: String, notificationDaysBefore: Int = -1) {
        viewModelScope.launch {
            try {
                var actualSubjectId = subjectId
                if (actualSubjectId <= 0L) {
                    actualSubjectId = subjects.value.firstOrNull()?.id ?: 1L
                }
                val exam = Exam(
                    title = title.trim(),
                    subjectId = actualSubjectId,
                    date = date,
                    difficulty = difficulty,
                    preparationStatus = preparationStatus,
                    materials = materials,
                    notificationDaysBefore = notificationDaysBefore
                )
                val id = withContext(Dispatchers.IO) {
                    repository.insertExam(exam)
                }
                if (notificationDaysBefore >= 0) {
                    com.example.utils.ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = id,
                        isExam = true,
                        title = title,
                        dueDateMs = date,
                        daysBefore = notificationDaysBefore
                    )
                    val daysStr = if (notificationDaysBefore == 0) "el mismo día" else "con $notificationDaysBefore día(s) de antelación"
                    addNotification("Recordatorio programado para el examen '$title' ($daysStr).")
                }
                addNotification("Examen programado: $title")
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error creating exam", e)
                addNotification("Error al programar el examen: ${e.message}")
            }
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateExam(exam)
                }
                com.example.utils.ReminderManager.cancelReminder(getApplication(), exam.id, isExam = true)
                if (exam.notificationDaysBefore >= 0) {
                    com.example.utils.ReminderManager.scheduleReminder(
                        context = getApplication(),
                        id = exam.id,
                        isExam = true,
                        title = exam.title,
                        dueDateMs = exam.date,
                        daysBefore = exam.notificationDaysBefore
                    )
                }
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error updating exam", e)
            }
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteExam(exam)
                }
                com.example.utils.ReminderManager.cancelReminder(getApplication(), exam.id, isExam = true)
                com.example.utils.NotificationStore.removeNotificationsForTitle(exam.title)
            } catch (e: Throwable) {
                android.util.Log.e("AcademiaViewModel", "Error deleting exam", e)
            }
        }
    }

    // Asynchronous queries and retrievals for Homework and Exams
    fun getHomeworkByIdAsync(id: Long): Flow<Homework?> =
        repository.getHomeworkById(id).flowOn(Dispatchers.IO)

    suspend fun getHomeworkByIdDirect(id: Long): Homework? =
        withContext(Dispatchers.IO) { repository.getHomeworkByIdDirect(id) }

    fun getHomeworkBySubjectAsync(subjectId: Long): Flow<List<Homework>> =
        repository.getHomeworkBySubject(subjectId).flowOn(Dispatchers.IO)

    fun getPendingHomeworkAsync(): Flow<List<Homework>> =
        repository.getPendingHomework().flowOn(Dispatchers.IO)

    fun getExamByIdAsync(id: Long): Flow<Exam?> =
        repository.getExamById(id).flowOn(Dispatchers.IO)

    suspend fun getExamByIdDirect(id: Long): Exam? =
        withContext(Dispatchers.IO) { repository.getExamByIdDirect(id) }

    fun getExamsBySubjectAsync(subjectId: Long): Flow<List<Exam>> =
        repository.getExamsBySubject(subjectId).flowOn(Dispatchers.IO)

    fun getUpcomingExamsAsync(timestamp: Long = System.currentTimeMillis()): Flow<List<Exam>> =
        repository.getUpcomingExams(timestamp).flowOn(Dispatchers.IO)

    // Note operations
    fun createNote(title: String, content: String, tags: String, subjectId: Long) {
        viewModelScope.launch {
            val note = AcademicNote(
                title = title,
                content = content,
                tags = tags,
                subjectId = subjectId
            )
            repository.insertNote(note)
            addNotification("Apunte guardado: $title")
        }
    }

    fun updateNote(note: AcademicNote) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: AcademicNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            addNotification("Apunte eliminado: ${note.title}")
        }
    }

    fun saveFocusSessionManual(minutes: Int, subjectId: Long) {
        viewModelScope.launch {
            val session = FocusSession(
                minutes = minutes,
                subjectId = subjectId
            )
            repository.insertFocusSession(session)
            addNotification("Añadida sesión manual de $minutes min de estudio.")
        }
    }

    // ==========================================
    // Operaciones de Horario Semanal (Schedule)
    // ==========================================
    fun createScheduleEntry(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        subjectId: Long,
        customTitle: String,
        classroom: String,
        professor: String,
        colorHex: String,
        type: String
    ) {
        viewModelScope.launch {
            val entry = ScheduleEntry(
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
            repository.insertScheduleEntry(entry)
            val dayName = when (dayOfWeek) {
                1 -> "Lunes"; 2 -> "Martes"; 3 -> "Miércoles"; 4 -> "Jueves"; 5 -> "Viernes"; 6 -> "Sábado"; else -> "Domingo"
            }
            addNotification("Añadida clase al horario del $dayName: ${customTitle.ifBlank { "Clase" }}")
        }
    }

    fun updateScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch {
            repository.updateScheduleEntry(entry)
            addNotification("Horario actualizado")
        }
    }

    /**
     * Guarda el nuevo orden y horas de una lista de clases del horario cuando el usuario confirma los cambios.
     */
    fun updateScheduleEntries(entries: List<ScheduleEntry>) {
        viewModelScope.launch {
            entries.forEach { entry ->
                repository.updateScheduleEntry(entry)
            }
            addNotification("Horario escolar actualizado con éxito")
        }
    }

    /**
     * Intercambia el orden y las horas de dos asignaturas/bloques del horario.
     * La asignatura 1 toma la hora de inicio y fin de la asignatura 2,
     * y la asignatura 2 toma la hora de inicio y fin de la asignatura 1.
     */
    fun swapScheduleEntriesTimes(entry1: ScheduleEntry, entry2: ScheduleEntry) {
        viewModelScope.launch {
            val updated1 = entry1.copy(startTime = entry2.startTime, endTime = entry2.endTime)
            val updated2 = entry2.copy(startTime = entry1.startTime, endTime = entry1.endTime)
            repository.updateScheduleEntry(updated1)
            repository.updateScheduleEntry(updated2)
            val name1 = entry1.customTitle.ifBlank { "Clase" }
            val name2 = entry2.customTitle.ifBlank { "Clase" }
            addNotification("Horario intercambiado: $name1 (${updated1.startTime}-${updated1.endTime}) ↔ $name2 (${updated2.startTime}-${updated2.endTime})")
        }
    }

    fun deleteScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch {
            repository.deleteScheduleEntry(entry)
            addNotification("Clase eliminada del horario")
        }
    }

    fun deleteScheduleEntryById(id: Long) {
        viewModelScope.launch {
            repository.deleteScheduleEntryById(id)
            addNotification("Clase eliminada del horario")
        }
    }
}
