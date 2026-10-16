package com.example

import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAppRepositoryDelegation() = runBlocking {
        // Implementación dummy de los DAOs para test unitario rápido
        val fakeHomeworkList = mutableListOf<Homework>()
        val fakeExamList = mutableListOf<Exam>()

        val fakeHomeworkDao = object : HomeworkDao {
            override fun getAllHomework() = flowOf(fakeHomeworkList.toList())
            override fun getHomeworkById(id: Long) = flowOf(fakeHomeworkList.find { it.id == id })
            override suspend fun getHomeworkByIdDirect(id: Long) = fakeHomeworkList.find { it.id == id }
            override fun getHomeworkBySubject(subjectId: Long) = flowOf(fakeHomeworkList.filter { it.subjectId == subjectId })
            override fun getHomeworkByStatus(status: String) = flowOf(fakeHomeworkList.filter { it.status == status })
            override fun getPendingHomework() = flowOf(fakeHomeworkList.filter { it.status != "COMPLETADO" })
            override suspend fun insertHomework(homework: Homework): Long {
                fakeHomeworkList.add(homework)
                return homework.id
            }
            override suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long> {
                fakeHomeworkList.addAll(homeworkList)
                return homeworkList.map { it.id }
            }
            override suspend fun updateHomework(homework: Homework) {
                val index = fakeHomeworkList.indexOfFirst { it.id == homework.id }
                if (index >= 0) fakeHomeworkList[index] = homework
            }
            override suspend fun deleteHomework(homework: Homework) {
                fakeHomeworkList.removeIf { it.id == homework.id }
            }
            override suspend fun deleteHomeworkById(id: Long) {
                fakeHomeworkList.removeIf { it.id == id }
            }
            override suspend fun updateHomeworkStatus(id: Long, status: String) {
                val index = fakeHomeworkList.indexOfFirst { it.id == id }
                if (index >= 0) fakeHomeworkList[index] = fakeHomeworkList[index].copy(status = status)
            }
            override fun getAllHomeworkWithSubject() = flowOf(emptyList<HomeworkWithSubject>())
            override fun getHomeworkBetweenDates(startDate: Long, endDate: Long) = flowOf(fakeHomeworkList.filter { it.dueDate in startDate..endDate })
            override fun searchHomework(query: String) = flowOf(fakeHomeworkList.filter { it.title.contains(query, true) })
            override fun getPendingCount() = flowOf(fakeHomeworkList.count { it.status != "COMPLETADO" })
            override suspend fun getAllHomeworkDirectList(): List<Homework> = fakeHomeworkList.toList()
            override suspend fun deleteAllHomework() { fakeHomeworkList.clear() }
            override suspend fun deleteCompletedHomework(): Int {
                val count = fakeHomeworkList.count { it.status == "COMPLETADO" }
                fakeHomeworkList.removeIf { it.status == "COMPLETADO" }
                return count
            }
        }

        val fakeExamDao = object : ExamDao {
            override fun getAllExams() = flowOf(fakeExamList.toList())
            override fun getAllExamsWithSubject() = flowOf(emptyList<ExamWithSubject>())
            override fun getExamById(id: Long) = flowOf(fakeExamList.find { it.id == id })
            override suspend fun getExamByIdDirect(id: Long) = fakeExamList.find { it.id == id }
            override fun getExamsBySubject(subjectId: Long) = flowOf(fakeExamList.filter { it.subjectId == subjectId })
            override fun getUpcomingExams(timestamp: Long) = flowOf(fakeExamList.filter { it.date >= timestamp })
            override fun getExamsBetweenDates(startDate: Long, endDate: Long) = flowOf(fakeExamList.filter { it.date in startDate..endDate })
            override fun searchExams(query: String) = flowOf(fakeExamList.filter { it.title.contains(query, true) })
            override fun getUpcomingCount(timestamp: Long) = flowOf(fakeExamList.count { it.date >= timestamp })
            override suspend fun getAllExamsDirectList(): List<Exam> = fakeExamList.toList()
            override suspend fun deleteAllExams() { fakeExamList.clear() }
            override suspend fun insertExam(exam: Exam): Long {
                fakeExamList.add(exam)
                return exam.id
            }
            override suspend fun insertAllExams(exams: List<Exam>): List<Long> {
                fakeExamList.addAll(exams)
                return exams.map { it.id }
            }
            override suspend fun updateExam(exam: Exam) {
                val index = fakeExamList.indexOfFirst { it.id == exam.id }
                if (index >= 0) fakeExamList[index] = exam
            }
            override suspend fun deleteExam(exam: Exam) {
                fakeExamList.removeIf { it.id == exam.id }
            }
            override suspend fun deleteExamById(id: Long) {
                fakeExamList.removeIf { it.id == id }
            }
            override suspend fun updatePreparationStatus(id: Long, status: String) {
                val index = fakeExamList.indexOfFirst { it.id == id }
                if (index >= 0) fakeExamList[index] = fakeExamList[index].copy(preparationStatus = status)
            }
        }

        val fakeSubjectDao = object : SubjectDao {
            override fun getAllSubjects() = flowOf(emptyList<Subject>())
            override suspend fun getAllSubjectsDirect(): List<Subject> = emptyList()
            override suspend fun getSubjectById(id: Long) = null
            override suspend fun insertSubject(subject: Subject) = 1L
            override suspend fun insertAllSubjects(subjects: List<Subject>) = emptyList<Long>()
            override suspend fun updateSubject(subject: Subject) {}
            override suspend fun deleteSubject(subject: Subject) {}
            override suspend fun deleteAllSubjects() {}
        }

        val fakeNoteDao = object : AcademicNoteDao {
            override fun getAllNotes() = flowOf(emptyList<AcademicNote>())
            override suspend fun getAllNotesDirect(): List<AcademicNote> = emptyList()
            override fun searchNotes(query: String) = flowOf(emptyList<AcademicNote>())
            override suspend fun insertNote(note: AcademicNote) = 1L
            override suspend fun insertAllNotes(notes: List<AcademicNote>) = emptyList<Long>()
            override suspend fun updateNote(note: AcademicNote) {}
            override suspend fun deleteNote(note: AcademicNote) {}
            override suspend fun deleteAllNotes() {}
        }

        val fakeFocusDao = object : FocusSessionDao {
            override fun getAllSessions() = flowOf(emptyList<FocusSession>())
            override suspend fun getAllSessionsDirect(): List<FocusSession> = emptyList()
            override suspend fun insertSession(session: FocusSession) = 1L
            override suspend fun insertAllSessions(sessions: List<FocusSession>) = emptyList<Long>()
            override suspend fun deleteAllSessions() {}
        }

        val fakeScheduleList = mutableListOf<ScheduleEntry>()
        val fakeScheduleDao = object : ScheduleDao {
            override fun getAllScheduleEntries() = flowOf(fakeScheduleList.toList())
            override suspend fun getAllScheduleEntriesDirect(): List<ScheduleEntry> = fakeScheduleList.toList()
            override fun getScheduleForDay(dayOfWeek: Int) = flowOf(fakeScheduleList.filter { it.dayOfWeek == dayOfWeek })
            override suspend fun insertScheduleEntry(entry: ScheduleEntry): Long {
                fakeScheduleList.add(entry)
                return entry.id
            }
            override suspend fun insertAllScheduleEntries(entries: List<ScheduleEntry>) = emptyList<Long>()
            override suspend fun updateScheduleEntry(entry: ScheduleEntry) {
                val idx = fakeScheduleList.indexOfFirst { it.id == entry.id }
                if (idx >= 0) fakeScheduleList[idx] = entry
            }
            override suspend fun deleteScheduleEntry(entry: ScheduleEntry) {
                fakeScheduleList.removeIf { it.id == entry.id }
            }
            override suspend fun deleteScheduleEntryById(id: Long) {
                fakeScheduleList.removeIf { it.id == id }
            }
            override suspend fun deleteAllScheduleEntries() {
                fakeScheduleList.clear()
            }
        }

        val repository: AcademicRepository = AppRepository(
            homeworkDao = fakeHomeworkDao,
            examDao = fakeExamDao,
            subjectDao = fakeSubjectDao,
            academicNoteDao = fakeNoteDao,
            focusSessionDao = fakeFocusDao,
            scheduleDao = fakeScheduleDao
        )

        // Verificar inserción y recuperación de Deberes
        val homework = Homework(id = 1, title = "Tarea Álgebra", subjectId = 10, dueDate = 1000L, priority = "ALTA", status = "PENDIENTE")
        repository.insertHomework(homework)
        val retrievedHw = repository.getHomeworkByIdDirect(1)
        assertNotNull(retrievedHw)
        assertEquals("Tarea Álgebra", retrievedHw?.title)

        // Actualizar estado de tarea
        repository.updateHomeworkStatus(1, "COMPLETADO")
        val updatedHw = repository.getHomeworkByIdDirect(1)
        assertEquals("COMPLETADO", updatedHw?.status)

        // Verificar inserción y actualización de Exámenes
        val exam = Exam(id = 5, title = "Parcial Química", subjectId = 20, date = 2000L, difficulty = "MEDIA", preparationStatus = "PENDIENTE")
        repository.insertExam(exam)
        val retrievedExam = repository.getExamByIdDirect(5)
        assertNotNull(retrievedExam)
        assertEquals("Parcial Química", retrievedExam?.title)

        repository.updatePreparationStatus(5, "PREPARADO")
        val updatedExam = repository.getExamByIdDirect(5)
        assertEquals("PREPARADO", updatedExam?.preparationStatus)
    }

    @Test
    fun testHomeworkUiStateCalculations() {
        val hwList = listOf(
            Homework(id = 1, title = "T1", subjectId = 1, dueDate = 1000L, status = "PENDIENTE", priority = "ALTA"),
            Homework(id = 2, title = "T2", subjectId = 1, dueDate = 2000L, status = "EN_PROGRESO", priority = "MEDIA"),
            Homework(id = 3, title = "T3", subjectId = 2, dueDate = 3000L, status = "COMPLETADO", priority = "BAJA")
        )

        val pending = hwList.count { it.status == "PENDIENTE" }
        val inProgress = hwList.count { it.status == "EN_PROGRESO" }
        val completed = hwList.count { it.status == "COMPLETADO" }
        val highPriority = hwList.count { it.priority == "ALTA" && it.status != "COMPLETADO" }

        val state = com.example.ui.HomeworkUiState.Success(
            allHomework = hwList,
            filteredHomework = hwList,
            pendingCount = pending,
            inProgressCount = inProgress,
            completedCount = completed,
            highPriorityCount = highPriority
        )

        assertEquals(1, state.pendingCount)
        assertEquals(1, state.inProgressCount)
        assertEquals(1, state.completedCount)
        assertEquals(1, state.highPriorityCount)
        assertEquals(3, state.allHomework.size)
    }

    @Test
    fun testHomeworkAndExamEntitiesAndSubtasks() {
        val hw = Homework(
            id = 10,
            title = "Práctica de Física",
            subjectId = 3,
            dueDate = 1700000000000L,
            priority = "ALTA",
            status = "PENDIENTE",
            subtasks = "Parte 1|true,Parte 2|false"
        )
        val subtaskList = hw.getSubtaskList()
        assertEquals(2, subtaskList.size)
        assertEquals("Parte 1", subtaskList[0].title)
        assertTrue(subtaskList[0].isCompleted)
        assertEquals("Parte 2", subtaskList[1].title)
        assertFalse(subtaskList[1].isCompleted)

        val subject = Subject(id = 3, name = "Física", colorHex = "#9C27B0", professor = "Dr. Test", classroom = "101")
        val hwWithSubject = HomeworkWithSubject(homework = hw, subject = subject)
        assertEquals("Práctica de Física", hwWithSubject.homework.title)
        assertEquals("Física", hwWithSubject.subject?.name)

        val exam = Exam(
            id = 20,
            title = "Final Álgebra",
            subjectId = 3,
            date = 1700500000000L,
            difficulty = "DIFÍCIL",
            preparationStatus = "ESTUDIANDO",
            materials = "Temas 1-5"
        )
        val examWithSubject = ExamWithSubject(exam = exam, subject = subject)
        assertEquals("Final Álgebra", examWithSubject.exam.title)
        assertEquals("DIFÍCIL", examWithSubject.exam.difficulty)
        assertEquals("Física", examWithSubject.subject?.name)
    }

    @Test
    fun testDedicatedHomeworkAndExamRepositories() = runBlocking {
        val hwStore = mutableListOf<Homework>()
        val examStore = mutableListOf<Exam>()

        val fakeHomeworkDao = object : HomeworkDao {
            override fun getAllHomework() = flowOf(hwStore.toList())
            override fun getHomeworkById(id: Long) = flowOf(hwStore.find { it.id == id })
            override suspend fun getHomeworkByIdDirect(id: Long) = hwStore.find { it.id == id }
            override fun getHomeworkBySubject(subjectId: Long) = flowOf(hwStore.filter { it.subjectId == subjectId })
            override fun getHomeworkByStatus(status: String) = flowOf(hwStore.filter { it.status == status })
            override fun getPendingHomework() = flowOf(hwStore.filter { it.status != "COMPLETADO" })
            override suspend fun insertHomework(homework: Homework): Long {
                hwStore.add(homework)
                return homework.id
            }
            override suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long> {
                hwStore.addAll(homeworkList)
                return homeworkList.map { it.id }
            }
            override suspend fun updateHomework(homework: Homework) {
                val index = hwStore.indexOfFirst { it.id == homework.id }
                if (index >= 0) hwStore[index] = homework
            }
            override suspend fun deleteHomework(homework: Homework) {
                hwStore.removeIf { it.id == homework.id }
            }
            override suspend fun deleteHomeworkById(id: Long) {
                hwStore.removeIf { it.id == id }
            }
            override suspend fun updateHomeworkStatus(id: Long, status: String) {
                val index = hwStore.indexOfFirst { it.id == id }
                if (index >= 0) hwStore[index] = hwStore[index].copy(status = status)
            }
            override fun getAllHomeworkWithSubject() = flowOf(emptyList<HomeworkWithSubject>())
            override fun getHomeworkBetweenDates(startDate: Long, endDate: Long) = flowOf(hwStore.filter { it.dueDate in startDate..endDate })
            override fun searchHomework(query: String) = flowOf(hwStore.filter { it.title.contains(query, true) })
            override fun getPendingCount() = flowOf(hwStore.count { it.status != "COMPLETADO" })
            override suspend fun getAllHomeworkDirectList(): List<Homework> = hwStore.toList()
            override suspend fun deleteAllHomework() { hwStore.clear() }
            override suspend fun deleteCompletedHomework(): Int {
                val count = hwStore.count { it.status == "COMPLETADO" }
                hwStore.removeIf { it.status == "COMPLETADO" }
                return count
            }
        }

        val fakeExamDao = object : ExamDao {
            override fun getAllExams() = flowOf(examStore.toList())
            override fun getAllExamsWithSubject() = flowOf(emptyList<ExamWithSubject>())
            override fun getExamById(id: Long) = flowOf(examStore.find { it.id == id })
            override suspend fun getExamByIdDirect(id: Long) = examStore.find { it.id == id }
            override fun getExamsBySubject(subjectId: Long) = flowOf(examStore.filter { it.subjectId == subjectId })
            override fun getUpcomingExams(timestamp: Long) = flowOf(examStore.filter { it.date >= timestamp })
            override fun getExamsBetweenDates(startDate: Long, endDate: Long) = flowOf(examStore.filter { it.date in startDate..endDate })
            override fun searchExams(query: String) = flowOf(examStore.filter { it.title.contains(query, true) })
            override fun getUpcomingCount(timestamp: Long) = flowOf(examStore.count { it.date >= timestamp })
            override suspend fun getAllExamsDirectList(): List<Exam> = examStore.toList()
            override suspend fun deleteAllExams() { examStore.clear() }
            override suspend fun insertExam(exam: Exam): Long {
                examStore.add(exam)
                return exam.id
            }
            override suspend fun insertAllExams(exams: List<Exam>): List<Long> {
                examStore.addAll(exams)
                return exams.map { it.id }
            }
            override suspend fun updateExam(exam: Exam) {
                val index = examStore.indexOfFirst { it.id == exam.id }
                if (index >= 0) examStore[index] = exam
            }
            override suspend fun deleteExam(exam: Exam) {
                examStore.removeIf { it.id == exam.id }
            }
            override suspend fun deleteExamById(id: Long) {
                examStore.removeIf { it.id == id }
            }
            override suspend fun updatePreparationStatus(id: Long, status: String) {
                val index = examStore.indexOfFirst { it.id == id }
                if (index >= 0) examStore[index] = examStore[index].copy(preparationStatus = status)
            }
        }

        val homeworkRepo: HomeworkRepository = DefaultHomeworkRepository(fakeHomeworkDao)
        val examRepo: ExamRepository = DefaultExamRepository(fakeExamDao)

        // Test Homework Repository
        val hw = Homework(id = 101, title = "Ejercicios Biología", subjectId = 2, dueDate = 25000L, priority = "MEDIA", status = "PENDIENTE")
        homeworkRepo.insertHomework(hw)
        val loadedHw = homeworkRepo.getHomeworkByIdDirect(101)
        assertNotNull(loadedHw)
        assertEquals("Ejercicios Biología", loadedHw?.title)

        // Test toggle status
        val toggled = homeworkRepo.toggleHomeworkStatus(loadedHw!!)
        assertEquals("COMPLETADO", toggled)
        assertEquals("COMPLETADO", homeworkRepo.getHomeworkByIdDirect(101)?.status)

        // Test Exam Repository
        val ex = Exam(id = 202, title = "Examen Literatura", subjectId = 4, date = 35000L, difficulty = "FÁCIL", preparationStatus = "PENDIENTE")
        examRepo.insertExam(ex)
        val loadedExam = examRepo.getExamByIdDirect(202)
        assertNotNull(loadedExam)
        assertEquals("Examen Literatura", loadedExam?.title)

        examRepo.updatePreparationStatus(202, "LISTO")
        val updatedExam = examRepo.getExamByIdDirect(202)
        assertEquals("LISTO", updatedExam?.preparationStatus)
    }

    @Test
    fun testReactiveTaskFlowPipeline() = runBlocking {
        val taskStore = mutableListOf<Homework>()
        val flowTrigger = kotlinx.coroutines.flow.MutableSharedFlow<List<Homework>>(replay = 1)
        flowTrigger.emit(emptyList())

        fun refreshFlow() {
            flowTrigger.tryEmit(taskStore.toList())
        }

        val testDao = object : HomeworkDao {
            override fun getAllHomework(): Flow<List<Homework>> = flowTrigger
            override fun getHomeworkById(id: Long): Flow<Homework?> = flowTrigger.map { list -> list.find { it.id == id } }
            override suspend fun getHomeworkByIdDirect(id: Long): Homework? = taskStore.find { it.id == id }
            override fun getHomeworkBySubject(subjectId: Long): Flow<List<Homework>> = flowTrigger.map { list -> list.filter { it.subjectId == subjectId } }
            override fun getHomeworkByStatus(status: String): Flow<List<Homework>> = flowTrigger.map { list -> list.filter { it.status == status } }
            override fun getPendingHomework(): Flow<List<Homework>> = flowTrigger.map { list -> list.filter { it.status != "COMPLETADO" } }
            override fun getAllHomeworkWithSubject() = flowOf(emptyList<HomeworkWithSubject>())
            override fun getHomeworkBetweenDates(startDate: Long, endDate: Long) = flowTrigger.map { list -> list.filter { it.dueDate in startDate..endDate } }
            override fun searchHomework(query: String) = flowTrigger.map { list -> list.filter { it.title.contains(query, true) } }
            override fun getPendingCount() = flowTrigger.map { list -> list.count { it.status != "COMPLETADO" } }
            override suspend fun insertHomework(homework: Homework): Long {
                taskStore.add(homework)
                refreshFlow()
                return homework.id
            }
            override suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long> {
                taskStore.addAll(homeworkList)
                refreshFlow()
                return homeworkList.map { it.id }
            }
            override suspend fun updateHomework(homework: Homework) {
                val idx = taskStore.indexOfFirst { it.id == homework.id }
                if (idx >= 0) {
                    taskStore[idx] = homework
                    refreshFlow()
                }
            }
            override suspend fun deleteHomework(homework: Homework) {
                taskStore.removeIf { it.id == homework.id }
                refreshFlow()
            }
            override suspend fun deleteHomeworkById(id: Long) {
                taskStore.removeIf { it.id == id }
                refreshFlow()
            }
            override suspend fun updateHomeworkStatus(id: Long, status: String) {
                val idx = taskStore.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    taskStore[idx] = taskStore[idx].copy(status = status)
                    refreshFlow()
                }
            }
            override suspend fun getAllHomeworkDirectList(): List<Homework> = taskStore.toList()
            override suspend fun deleteAllHomework() {
                taskStore.clear()
                refreshFlow()
            }
            override suspend fun deleteCompletedHomework(): Int {
                val count = taskStore.count { it.status == "COMPLETADO" }
                taskStore.removeIf { it.status == "COMPLETADO" }
                refreshFlow()
                return count
            }
        }

        val testRepo: HomeworkRepository = DefaultHomeworkRepository(testDao)

        // Verificar reactividad del Flow de tareas
        val initialTasks = testRepo.allHomework.first()
        assertTrue(initialTasks.isEmpty())

        // Insertar tarea y verificar emisión reactiva
        val task1 = Homework(id = 10, title = "Tarea Física Cuántica", subjectId = 1, dueDate = 50000L, priority = "ALTA", status = "PENDIENTE")
        testRepo.insertHomework(task1)

        val updatedTasks = testRepo.allHomework.first()
        assertEquals(1, updatedTasks.size)
        assertEquals("Tarea Física Cuántica", updatedTasks[0].title)

        // Cambiar estado a COMPLETADO y verificar que pendingHomework se actualiza reactivamente
        testRepo.toggleHomeworkStatus(task1)
        val pendingAfterToggle = testRepo.getPendingHomework().first()
        assertTrue(pendingAfterToggle.isEmpty())

        val completedAfterToggle = testRepo.getHomeworkByStatus("COMPLETADO").first()
        assertEquals(1, completedAfterToggle.size)
        assertEquals("COMPLETADO", completedAfterToggle[0].status)
    }

    @Test
    fun testHomeScreenTaskSummaryAndQuickAddLogic() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600 * 1000L

        val homeworkList = listOf(
            Homework(id = 1, title = "Deber Urgente Hoy", subjectId = 1, dueDate = now + 3600 * 1000L, priority = "ALTA", status = "PENDIENTE"),
            Homework(id = 2, title = "Deber Mañana", subjectId = 1, dueDate = now + dayMs, priority = "MEDIA", status = "PENDIENTE"),
            Homework(id = 3, title = "Deber Semana Próxima", subjectId = 2, dueDate = now + 4 * dayMs, priority = "BAJA", status = "PENDIENTE"),
            Homework(id = 4, title = "Deber Ya Terminado", subjectId = 2, dueDate = now - 3600 * 1000L, priority = "MEDIA", status = "COMPLETADO")
        )

        val pending = homeworkList.filter { it.status != "COMPLETADO" }
        val completed = homeworkList.filter { it.status == "COMPLETADO" }
        val highPriority = pending.filter { it.priority == "ALTA" }

        assertEquals(3, pending.size)
        assertEquals(1, completed.size)
        assertEquals(1, highPriority.size)
        assertEquals("Deber Urgente Hoy", highPriority.first().title)

        // Simular Quick Add de nuevo deber
        val newHw = Homework(
            id = 5,
            title = "Nuevo Deber Rápido",
            subjectId = 1,
            dueDate = now + dayMs,
            priority = "MEDIA",
            status = "PENDIENTE"
        )
        val updatedList = homeworkList + newHw
        val newPending = updatedList.filter { it.status != "COMPLETADO" }
        assertEquals(4, newPending.size)
        assertTrue(newPending.any { it.title == "Nuevo Deber Rápido" })
    }
}


