package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY id ASC")
    suspend fun getAllSubjectsDirect(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubjects(subjects: List<Subject>): List<Long>

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework ORDER BY dueDate ASC")
    fun getAllHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework ORDER BY id ASC")
    suspend fun getAllHomeworkDirectList(): List<Homework>

    @Transaction
    @Query("SELECT * FROM homework ORDER BY dueDate ASC")
    fun getAllHomeworkWithSubject(): Flow<List<HomeworkWithSubject>>

    @Query("SELECT * FROM homework WHERE id = :id LIMIT 1")
    fun getHomeworkById(id: Long): Flow<Homework?>

    @Query("SELECT * FROM homework WHERE id = :id LIMIT 1")
    suspend fun getHomeworkByIdDirect(id: Long): Homework?

    @Query("SELECT * FROM homework WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getHomeworkBySubject(subjectId: Long): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE status = :status ORDER BY dueDate ASC")
    fun getHomeworkByStatus(status: String): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE status != 'COMPLETADO' ORDER BY dueDate ASC")
    fun getPendingHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    fun getHomeworkBetweenDates(startDate: Long, endDate: Long): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY dueDate ASC")
    fun searchHomework(query: String): Flow<List<Homework>>

    @Query("SELECT COUNT(*) FROM homework WHERE status != 'COMPLETADO'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: Homework): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHomework(homeworkList: List<Homework>): List<Long>

    @Update
    suspend fun updateHomework(homework: Homework)

    @Delete
    suspend fun deleteHomework(homework: Homework)

    @Query("DELETE FROM homework WHERE id = :id")
    suspend fun deleteHomeworkById(id: Long)

    @Query("UPDATE homework SET status = :status WHERE id = :id")
    suspend fun updateHomeworkStatus(id: Long, status: String)

    @Query("DELETE FROM homework WHERE status = 'COMPLETADO'")
    suspend fun deleteCompletedHomework(): Int

    @Query("DELETE FROM homework")
    suspend fun deleteAllHomework()
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY id ASC")
    suspend fun getAllExamsDirectList(): List<Exam>

    @Transaction
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAllExamsWithSubject(): Flow<List<ExamWithSubject>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    fun getExamById(id: Long): Flow<Exam?>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamByIdDirect(id: Long): Exam?

    @Query("SELECT * FROM exams WHERE subjectId = :subjectId ORDER BY date ASC")
    fun getExamsBySubject(subjectId: Long): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE date >= :timestamp ORDER BY date ASC")
    fun getUpcomingExams(timestamp: Long): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getExamsBetweenDates(startDate: Long, endDate: Long): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE title LIKE '%' || :query || '%' OR materials LIKE '%' || :query || '%' ORDER BY date ASC")
    fun searchExams(query: String): Flow<List<Exam>>

    @Query("SELECT COUNT(*) FROM exams WHERE date >= :timestamp")
    fun getUpcomingCount(timestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExams(exams: List<Exam>): List<Long>

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Long)

    @Query("UPDATE exams SET preparationStatus = :status WHERE id = :id")
    suspend fun updatePreparationStatus(id: Long, status: String)

    @Query("DELETE FROM exams")
    suspend fun deleteAllExams()
}

@Dao
interface AcademicNoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<AcademicNote>>

    @Query("SELECT * FROM notes ORDER BY id ASC")
    suspend fun getAllNotesDirect(): List<AcademicNote>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<AcademicNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: AcademicNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<AcademicNote>): List<Long>

    @Update
    suspend fun updateNote(note: AcademicNote)

    @Delete
    suspend fun deleteNote(note: AcademicNote)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY id ASC")
    suspend fun getAllSessionsDirect(): List<FocusSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<FocusSession>): List<Long>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_entries ORDER BY startTime ASC")
    fun getAllScheduleEntries(): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries ORDER BY id ASC")
    suspend fun getAllScheduleEntriesDirect(): List<ScheduleEntry>

    @Query("SELECT * FROM schedule_entries WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ScheduleEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScheduleEntries(entries: List<ScheduleEntry>): List<Long>

    @Update
    suspend fun updateScheduleEntry(entry: ScheduleEntry)

    @Delete
    suspend fun deleteScheduleEntry(entry: ScheduleEntry)

    @Query("DELETE FROM schedule_entries WHERE id = :id")
    suspend fun deleteScheduleEntryById(id: Long)

    @Query("DELETE FROM schedule_entries")
    suspend fun deleteAllScheduleEntries()
}
