package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Interfaz que define el contrato de acceso a datos para la aplicación.
 * Desacopla la lógica de presentación (ViewModels) de la persistencia directa en Room.
 */
interface AcademicRepository {
    // Flujos reactivos de datos
    val allSubjects: Flow<List<Subject>>
    val allHomework: Flow<List<Homework>>
    val allHomeworkWithSubject: Flow<List<HomeworkWithSubject>>
    val allExams: Flow<List<Exam>>
    val allExamsWithSubject: Flow<List<ExamWithSubject>>
    val allNotes: Flow<List<AcademicNote>>
    val allFocusSessions: Flow<List<FocusSession>>

    // Operaciones para Deberes (Homework)
    fun getHomeworkById(id: Long): Flow<Homework?>
    suspend fun getHomeworkByIdDirect(id: Long): Homework?
    fun getHomeworkBySubject(subjectId: Long): Flow<List<Homework>>
    fun getHomeworkByStatus(status: String): Flow<List<Homework>>
    fun getPendingHomework(): Flow<List<Homework>>
    fun getHomeworkBetweenDates(startDate: Long, endDate: Long): Flow<List<Homework>>
    fun searchHomework(query: String): Flow<List<Homework>>
    fun getPendingHomeworkCount(): Flow<Int>
    suspend fun insertHomework(homework: Homework): Long
    suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long>
    suspend fun updateHomework(homework: Homework)
    suspend fun deleteHomework(homework: Homework)
    suspend fun deleteHomeworkById(id: Long)
    suspend fun updateHomeworkStatus(id: Long, status: String)
    suspend fun toggleHomeworkStatus(homework: Homework): String
    suspend fun deleteCompletedHomework(): Int

    // Operaciones para Exámenes (Exams)
    fun getExamById(id: Long): Flow<Exam?>
    suspend fun getExamByIdDirect(id: Long): Exam?
    fun getExamsBySubject(subjectId: Long): Flow<List<Exam>>
    fun getUpcomingExams(timestamp: Long): Flow<List<Exam>>
    fun getExamsBetweenDates(startDate: Long, endDate: Long): Flow<List<Exam>>
    fun searchExams(query: String): Flow<List<Exam>>
    fun getUpcomingExamsCount(timestamp: Long): Flow<Int>
    suspend fun insertExam(exam: Exam): Long
    suspend fun insertAllExams(exams: List<Exam>): List<Long>
    suspend fun updateExam(exam: Exam)
    suspend fun deleteExam(exam: Exam)
    suspend fun deleteExamById(id: Long)
    suspend fun updatePreparationStatus(id: Long, status: String)

    // Operaciones para Asignaturas (Subjects)
    suspend fun getSubjectById(id: Long): Subject?
    suspend fun insertSubject(subject: Subject): Long
    suspend fun updateSubject(subject: Subject)
    suspend fun deleteSubject(subject: Subject)

    // Operaciones para Apuntes (Notes)
    fun searchNotes(query: String): Flow<List<AcademicNote>>
    suspend fun insertNote(note: AcademicNote): Long
    suspend fun updateNote(note: AcademicNote)
    suspend fun deleteNote(note: AcademicNote)

    // Operaciones para Sesiones de Estudio / Pomodoro
    suspend fun insertFocusSession(session: FocusSession): Long

    // Operaciones para Horario Semanal (Schedule)
    val allScheduleEntries: Flow<List<ScheduleEntry>>
    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleEntry>>
    suspend fun insertScheduleEntry(entry: ScheduleEntry): Long
    suspend fun updateScheduleEntry(entry: ScheduleEntry)
    suspend fun deleteScheduleEntry(entry: ScheduleEntry)
    suspend fun deleteScheduleEntryById(id: Long)
}

/**
 * Clase Repository que interactúa con los DAOs de Room (HomeworkDao, ExamDao, SubjectDao, etc.).
 * Facilita la gestión, mapeo y despacho de datos en segundo plano entre la base de datos y los ViewModels.
 */
class AppRepository(
    private val homeworkDao: HomeworkDao,
    private val examDao: ExamDao,
    private val subjectDao: SubjectDao,
    private val academicNoteDao: AcademicNoteDao,
    private val focusSessionDao: FocusSessionDao,
    private val scheduleDao: ScheduleDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AcademicRepository, HomeworkRepository, ExamRepository {

    /**
     * Constructor secundario que obtiene los DAOs a partir de la instancia de [AppDatabase].
     */
    constructor(db: AppDatabase, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        homeworkDao = db.homeworkDao(),
        examDao = db.examDao(),
        subjectDao = db.subjectDao(),
        academicNoteDao = db.academicNoteDao(),
        focusSessionDao = db.focusSessionDao(),
        scheduleDao = db.scheduleDao(),
        ioDispatcher = ioDispatcher
    )

    // ==========================================
    // Flujos Reactivos de Datos
    // ==========================================
    override val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    override val allHomework: Flow<List<Homework>> = homeworkDao.getAllHomework()
    override val allHomeworkWithSubject: Flow<List<HomeworkWithSubject>> = homeworkDao.getAllHomeworkWithSubject()
    override val allExams: Flow<List<Exam>> = examDao.getAllExams()
    override val allExamsWithSubject: Flow<List<ExamWithSubject>> = examDao.getAllExamsWithSubject()
    override val allNotes: Flow<List<AcademicNote>> = academicNoteDao.getAllNotes()
    override val allFocusSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    override val allScheduleEntries: Flow<List<ScheduleEntry>> = scheduleDao.getAllScheduleEntries()

    // ==========================================
    // Deberes / Tareas (HomeworkDao)
    // ==========================================
    override fun getHomeworkById(id: Long): Flow<Homework?> = homeworkDao.getHomeworkById(id)

    override suspend fun getHomeworkByIdDirect(id: Long): Homework? = withContext(ioDispatcher) {
        homeworkDao.getHomeworkByIdDirect(id)
    }

    override fun getHomeworkBySubject(subjectId: Long): Flow<List<Homework>> =
        homeworkDao.getHomeworkBySubject(subjectId)

    override fun getHomeworkByStatus(status: String): Flow<List<Homework>> =
        homeworkDao.getHomeworkByStatus(status)

    override fun getPendingHomework(): Flow<List<Homework>> =
        homeworkDao.getPendingHomework()

    override fun getHomeworkBetweenDates(startDate: Long, endDate: Long): Flow<List<Homework>> =
        homeworkDao.getHomeworkBetweenDates(startDate, endDate)

    override fun searchHomework(query: String): Flow<List<Homework>> =
        homeworkDao.searchHomework(query)

    override fun getPendingHomeworkCount(): Flow<Int> =
        homeworkDao.getPendingCount()

    override suspend fun insertHomework(homework: Homework): Long = withContext(ioDispatcher) {
        homeworkDao.insertHomework(homework)
    }

    override suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long> = withContext(ioDispatcher) {
        homeworkDao.insertAllHomework(homeworkList)
    }

    override suspend fun updateHomework(homework: Homework): Unit = withContext(ioDispatcher) {
        homeworkDao.updateHomework(homework)
    }

    override suspend fun deleteHomework(homework: Homework): Unit = withContext(ioDispatcher) {
        homeworkDao.deleteHomework(homework)
    }

    override suspend fun deleteHomeworkById(id: Long): Unit = withContext(ioDispatcher) {
        homeworkDao.deleteHomeworkById(id)
    }

    override suspend fun updateHomeworkStatus(id: Long, status: String): Unit = withContext(ioDispatcher) {
        homeworkDao.updateHomeworkStatus(id, status)
    }

    override suspend fun toggleHomeworkStatus(homework: Homework): String = withContext(ioDispatcher) {
        val nextStatus = if (homework.status == "COMPLETADO") "PENDIENTE" else "COMPLETADO"
        homeworkDao.updateHomework(homework.copy(status = nextStatus))
        nextStatus
    }

    override suspend fun deleteCompletedHomework(): Int = withContext(ioDispatcher) {
        homeworkDao.deleteCompletedHomework()
    }

    // ==========================================
    // Exámenes (ExamDao)
    // ==========================================
    override fun getExamById(id: Long): Flow<Exam?> = examDao.getExamById(id)

    override suspend fun getExamByIdDirect(id: Long): Exam? = withContext(ioDispatcher) {
        examDao.getExamByIdDirect(id)
    }

    override fun getExamsBySubject(subjectId: Long): Flow<List<Exam>> =
        examDao.getExamsBySubject(subjectId)

    override fun getUpcomingExams(timestamp: Long): Flow<List<Exam>> =
        examDao.getUpcomingExams(timestamp)

    override fun getExamsBetweenDates(startDate: Long, endDate: Long): Flow<List<Exam>> =
        examDao.getExamsBetweenDates(startDate, endDate)

    override fun searchExams(query: String): Flow<List<Exam>> =
        examDao.searchExams(query)

    override fun getUpcomingExamsCount(timestamp: Long): Flow<Int> =
        examDao.getUpcomingCount(timestamp)

    override suspend fun insertExam(exam: Exam): Long = withContext(ioDispatcher) {
        examDao.insertExam(exam)
    }

    override suspend fun insertAllExams(exams: List<Exam>): List<Long> = withContext(ioDispatcher) {
        examDao.insertAllExams(exams)
    }

    override suspend fun updateExam(exam: Exam): Unit = withContext(ioDispatcher) {
        examDao.updateExam(exam)
    }

    override suspend fun deleteExam(exam: Exam): Unit = withContext(ioDispatcher) {
        examDao.deleteExam(exam)
    }

    override suspend fun deleteExamById(id: Long): Unit = withContext(ioDispatcher) {
        examDao.deleteExamById(id)
    }

    override suspend fun updatePreparationStatus(id: Long, status: String): Unit = withContext(ioDispatcher) {
        examDao.updatePreparationStatus(id, status)
    }

    // ==========================================
    // Asignaturas (SubjectDao)
    // ==========================================
    override suspend fun getSubjectById(id: Long): Subject? = withContext(ioDispatcher) {
        subjectDao.getSubjectById(id)
    }

    override suspend fun insertSubject(subject: Subject): Long = withContext(ioDispatcher) {
        subjectDao.insertSubject(subject)
    }

    override suspend fun updateSubject(subject: Subject): Unit = withContext(ioDispatcher) {
        subjectDao.updateSubject(subject)
    }

    override suspend fun deleteSubject(subject: Subject): Unit = withContext(ioDispatcher) {
        subjectDao.deleteSubject(subject)
    }

    // ==========================================
    // Apuntes (AcademicNoteDao)
    // ==========================================
    override fun searchNotes(query: String): Flow<List<AcademicNote>> =
        academicNoteDao.searchNotes(query)

    override suspend fun insertNote(note: AcademicNote): Long = withContext(ioDispatcher) {
        academicNoteDao.insertNote(note)
    }

    override suspend fun updateNote(note: AcademicNote): Unit = withContext(ioDispatcher) {
        academicNoteDao.updateNote(note)
    }

    override suspend fun deleteNote(note: AcademicNote): Unit = withContext(ioDispatcher) {
        academicNoteDao.deleteNote(note)
    }

    // ==========================================
    // Sesiones de Concentración (FocusSessionDao)
    // ==========================================
    override suspend fun insertFocusSession(session: FocusSession): Long = withContext(ioDispatcher) {
        focusSessionDao.insertSession(session)
    }

    // ==========================================
    // Horario Semanal (ScheduleDao)
    // ==========================================
    override fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleEntry>> =
        scheduleDao.getScheduleForDay(dayOfWeek)

    override suspend fun insertScheduleEntry(entry: ScheduleEntry): Long = withContext(ioDispatcher) {
        scheduleDao.insertScheduleEntry(entry)
    }

    override suspend fun updateScheduleEntry(entry: ScheduleEntry): Unit = withContext(ioDispatcher) {
        scheduleDao.updateScheduleEntry(entry)
    }

    override suspend fun deleteScheduleEntry(entry: ScheduleEntry): Unit = withContext(ioDispatcher) {
        scheduleDao.deleteScheduleEntry(entry)
    }

    override suspend fun deleteScheduleEntryById(id: Long): Unit = withContext(ioDispatcher) {
        scheduleDao.deleteScheduleEntryById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        /**
         * Retorna la instancia singleton de [AppRepository] dada una base de datos.
         */
        fun getInstance(database: AppDatabase): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppRepository(database)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Retorna la instancia singleton de [AppRepository] a partir del contexto.
         */
        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getInstance(context)
                val instance = AppRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Alias para mantener compatibilidad total con código existente.
 */
typealias AcademiaRepository = AppRepository

