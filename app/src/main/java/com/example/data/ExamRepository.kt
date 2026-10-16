package com.example.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Interfaz que define el contrato del repositorio de Exámenes (Exam).
 * Actúa como capa de abstracción entre ExamDao de Room y los ViewModels,
 * facilitando la gestión reactiva y asíncrona de las evaluaciones escolares.
 */
interface ExamRepository {
    val allExams: Flow<List<Exam>>
    val allExamsWithSubject: Flow<List<ExamWithSubject>>

    fun getUpcomingExams(timestamp: Long = System.currentTimeMillis()): Flow<List<Exam>>
    fun getUpcomingExamsCount(timestamp: Long = System.currentTimeMillis()): Flow<Int>
    fun getExamById(id: Long): Flow<Exam?>
    suspend fun getExamByIdDirect(id: Long): Exam?
    fun getExamsBySubject(subjectId: Long): Flow<List<Exam>>
    fun getExamsBetweenDates(startDate: Long, endDate: Long): Flow<List<Exam>>
    fun searchExams(query: String): Flow<List<Exam>>

    suspend fun insertExam(exam: Exam): Long
    suspend fun insertAllExams(exams: List<Exam>): List<Long>
    suspend fun updateExam(exam: Exam)
    suspend fun updatePreparationStatus(id: Long, status: String)
    suspend fun deleteExam(exam: Exam)
    suspend fun deleteExamById(id: Long)
}

/**
 * Implementación predeterminada de [ExamRepository].
 * Despacha de forma asíncrona y segura todas las operaciones de base de datos en [Dispatchers.IO].
 */
class DefaultExamRepository(
    private val examDao: ExamDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExamRepository {

    override val allExams: Flow<List<Exam>> = examDao.getAllExams()
    override val allExamsWithSubject: Flow<List<ExamWithSubject>> = examDao.getAllExamsWithSubject()

    override fun getUpcomingExams(timestamp: Long): Flow<List<Exam>> =
        examDao.getUpcomingExams(timestamp)

    override fun getUpcomingExamsCount(timestamp: Long): Flow<Int> =
        examDao.getUpcomingCount(timestamp)

    override fun getExamById(id: Long): Flow<Exam?> = examDao.getExamById(id)

    override suspend fun getExamByIdDirect(id: Long): Exam? = withContext(ioDispatcher) {
        examDao.getExamByIdDirect(id)
    }

    override fun getExamsBySubject(subjectId: Long): Flow<List<Exam>> =
        examDao.getExamsBySubject(subjectId)

    override fun getExamsBetweenDates(startDate: Long, endDate: Long): Flow<List<Exam>> =
        examDao.getExamsBetweenDates(startDate, endDate)

    override fun searchExams(query: String): Flow<List<Exam>> =
        examDao.searchExams(query)

    override suspend fun insertExam(exam: Exam): Long = withContext(ioDispatcher) {
        examDao.insertExam(exam)
    }

    override suspend fun insertAllExams(exams: List<Exam>): List<Long> = withContext(ioDispatcher) {
        examDao.insertAllExams(exams)
    }

    override suspend fun updateExam(exam: Exam): Unit = withContext(ioDispatcher) {
        examDao.updateExam(exam)
    }

    override suspend fun updatePreparationStatus(id: Long, status: String): Unit = withContext(ioDispatcher) {
        examDao.updatePreparationStatus(id, status)
    }

    override suspend fun deleteExam(exam: Exam): Unit = withContext(ioDispatcher) {
        examDao.deleteExam(exam)
    }

    override suspend fun deleteExamById(id: Long): Unit = withContext(ioDispatcher) {
        examDao.deleteExamById(id)
    }

    companion object {
        fun create(context: android.content.Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): ExamRepository {
            val database = AppDatabase.getInstance(context)
            return DefaultExamRepository(database.examDao(), ioDispatcher)
        }

        fun create(database: AppDatabase, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): ExamRepository {
            return DefaultExamRepository(database.examDao(), ioDispatcher)
        }
    }
}
