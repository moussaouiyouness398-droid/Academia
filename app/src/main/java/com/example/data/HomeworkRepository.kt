package com.example.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Interfaz que define el contrato del repositorio de Deberes (Homework).
 * Actúa como capa de abstracción entre HomeworkDao de Room y los ViewModels,
 * facilitando la gestión reactiva y asíncrona de datos en la interfaz.
 */
interface HomeworkRepository {
    val allHomework: Flow<List<Homework>>
    val allHomeworkWithSubject: Flow<List<HomeworkWithSubject>>

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
    suspend fun updateHomeworkStatus(id: Long, status: String)
    suspend fun toggleHomeworkStatus(homework: Homework): String
    suspend fun deleteHomework(homework: Homework)
    suspend fun deleteHomeworkById(id: Long)
    suspend fun deleteCompletedHomework(): Int
}

/**
 * Propiedades de extensión convenientes para acceder a los flujos de tareas pendientes.
 */
val HomeworkRepository.pendingHomework: Flow<List<Homework>>
    get() = getPendingHomework()

val HomeworkRepository.pendingHomeworkCount: Flow<Int>
    get() = getPendingHomeworkCount()

/**
 * Implementación predeterminada de [HomeworkRepository].
 * Despacha de forma asíncrona y segura todas las operaciones de base de datos en [Dispatchers.IO].
 */
class DefaultHomeworkRepository(
    private val homeworkDao: HomeworkDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HomeworkRepository {

    override val allHomework: Flow<List<Homework>> = homeworkDao.getAllHomework()
    override val allHomeworkWithSubject: Flow<List<HomeworkWithSubject>> = homeworkDao.getAllHomeworkWithSubject()

    override fun getPendingHomework(): Flow<List<Homework>> = homeworkDao.getPendingHomework()
    override fun getPendingHomeworkCount(): Flow<Int> = homeworkDao.getPendingCount()

    override fun getHomeworkById(id: Long): Flow<Homework?> = homeworkDao.getHomeworkById(id)

    override suspend fun getHomeworkByIdDirect(id: Long): Homework? = withContext(ioDispatcher) {
        homeworkDao.getHomeworkByIdDirect(id)
    }

    override fun getHomeworkBySubject(subjectId: Long): Flow<List<Homework>> =
        homeworkDao.getHomeworkBySubject(subjectId)

    override fun getHomeworkByStatus(status: String): Flow<List<Homework>> =
        homeworkDao.getHomeworkByStatus(status)

    override fun getHomeworkBetweenDates(startDate: Long, endDate: Long): Flow<List<Homework>> =
        homeworkDao.getHomeworkBetweenDates(startDate, endDate)

    override fun searchHomework(query: String): Flow<List<Homework>> =
        homeworkDao.searchHomework(query)

    override suspend fun insertHomework(homework: Homework): Long = withContext(ioDispatcher) {
        homeworkDao.insertHomework(homework)
    }

    override suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long> = withContext(ioDispatcher) {
        homeworkDao.insertAllHomework(homeworkList)
    }

    override suspend fun updateHomework(homework: Homework): Unit = withContext(ioDispatcher) {
        homeworkDao.updateHomework(homework)
    }

    override suspend fun updateHomeworkStatus(id: Long, status: String): Unit = withContext(ioDispatcher) {
        homeworkDao.updateHomeworkStatus(id, status)
    }

    override suspend fun toggleHomeworkStatus(homework: Homework): String = withContext(ioDispatcher) {
        val nextStatus = if (homework.status == "COMPLETADO") "PENDIENTE" else "COMPLETADO"
        homeworkDao.updateHomework(homework.copy(status = nextStatus))
        nextStatus
    }

    override suspend fun deleteHomework(homework: Homework): Unit = withContext(ioDispatcher) {
        homeworkDao.deleteHomework(homework)
    }

    override suspend fun deleteHomeworkById(id: Long): Unit = withContext(ioDispatcher) {
        homeworkDao.deleteHomeworkById(id)
    }

    override suspend fun deleteCompletedHomework(): Int = withContext(ioDispatcher) {
        homeworkDao.deleteCompletedHomework()
    }

    companion object {
        fun create(context: android.content.Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): HomeworkRepository {
            val database = AppDatabase.getInstance(context)
            return DefaultHomeworkRepository(database.homeworkDao(), ioDispatcher)
        }

        fun create(database: AppDatabase, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): HomeworkRepository {
            return DefaultHomeworkRepository(database.homeworkDao(), ioDispatcher)
        }
    }
}
