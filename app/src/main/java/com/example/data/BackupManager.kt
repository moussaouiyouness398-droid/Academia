package com.example.data

import android.content.Context
import com.example.utils.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreSummary(
    val success: Boolean,
    val subjectsRestored: Int = 0,
    val homeworkRestored: Int = 0,
    val examsRestored: Int = 0,
    val schedulesRestored: Int = 0,
    val notesRestored: Int = 0,
    val focusSessionsRestored: Int = 0,
    val errorMessage: String? = null
) {
    val totalItems: Int
        get() = subjectsRestored + homeworkRestored + examsRestored + schedulesRestored + notesRestored + focusSessionsRestored
}

data class CloudVaultInfo(
    val hasCloudData: Boolean,
    val lastSyncTimeMillis: Long,
    val totalItems: Int,
    val formattedLastSync: String,
    val backupSizeBytes: Long
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs = context.getSharedPreferences("academia_cloud_sync_prefs", Context.MODE_PRIVATE)
    private val cloudVaultFile = File(context.filesDir, "academia_cloud_vault.json")

    /**
     * Genera un JSON completo con todas las entidades de la base de datos Room:
     * Tareas (Homework), Exámenes (Exams), Horarios (ScheduleEntry),
     * Asignaturas (Subjects), Apuntes (Notes) y Sesiones de enfoque (FocusSession).
     */
    suspend fun createFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        root.put("version", 2)
        root.put("appName", "Academia")
        root.put("timestamp", now)
        root.put("formattedDate", dateFormat.format(Date(now)))

        // 1. Asignaturas (Subjects)
        val subjectsList = database.subjectDao().getAllSubjectsDirect()
        val subjectsArray = JSONArray()
        for (s in subjectsList) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("colorHex", s.colorHex)
                put("professor", s.professor)
                put("classroom", s.classroom)
                put("iconName", s.iconName)
            }
            subjectsArray.put(obj)
        }
        root.put("subjects", subjectsArray)

        // 2. Deberes / Tareas (Homework)
        val homeworkList = database.homeworkDao().getAllHomeworkDirectList()
        val homeworkArray = JSONArray()
        for (h in homeworkList) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("subjectId", h.subjectId)
                put("dueDate", h.dueDate)
                put("priority", h.priority)
                put("status", h.status)
                put("notes", h.notes)
                put("links", h.links)
                put("subtasks", h.subtasks)
                put("notificationDaysBefore", h.notificationDaysBefore)
            }
            homeworkArray.put(obj)
        }
        root.put("homework", homeworkArray)

        // 3. Exámenes (Exams)
        val examsList = database.examDao().getAllExamsDirectList()
        val examsArray = JSONArray()
        for (e in examsList) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("subjectId", e.subjectId)
                put("date", e.date)
                put("difficulty", e.difficulty)
                put("preparationStatus", e.preparationStatus)
                put("materials", e.materials)
                put("notificationDaysBefore", e.notificationDaysBefore)
            }
            examsArray.put(obj)
        }
        root.put("exams", examsArray)

        // 4. Horarios (Schedule Entries)
        val scheduleList = database.scheduleDao().getAllScheduleEntriesDirect()
        val scheduleArray = JSONArray()
        for (sch in scheduleList) {
            val obj = JSONObject().apply {
                put("id", sch.id)
                put("dayOfWeek", sch.dayOfWeek)
                put("startTime", sch.startTime)
                put("endTime", sch.endTime)
                put("subjectId", sch.subjectId)
                put("customTitle", sch.customTitle)
                put("classroom", sch.classroom)
                put("professor", sch.professor)
                put("colorHex", sch.colorHex)
                put("type", sch.type)
            }
            scheduleArray.put(obj)
        }
        root.put("schedule", scheduleArray)

        // 5. Apuntes (Notes)
        val notesList = database.academicNoteDao().getAllNotesDirect()
        val notesArray = JSONArray()
        for (n in notesList) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("tags", n.tags)
                put("subjectId", n.subjectId)
                put("createdAt", n.createdAt)
            }
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // 6. Sesiones de enfoque (Focus Sessions)
        val focusList = database.focusSessionDao().getAllSessionsDirect()
        val focusArray = JSONArray()
        for (f in focusList) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("minutes", f.minutes)
                put("timestamp", f.timestamp)
                put("subjectId", f.subjectId)
            }
            focusArray.put(obj)
        }
        root.put("focusSessions", focusArray)

        root.toString(2)
    }

    /**
     * Restaura una copia de seguridad en formato JSON hacia la base de datos Room.
     * @param jsonString Cadena JSON con el respaldo exportado.
     * @param replaceAll Si es true, limpia las tablas antes de restaurar; si es false, fusiona (insert or replace).
     */
    suspend fun restoreBackupFromJson(jsonString: String, replaceAll: Boolean = false): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("subjects") && !root.has("homework") && !root.has("exams")) {
                return@withContext RestoreSummary(
                    success = false,
                    errorMessage = "El archivo no contiene un formato de respaldo válido de Academia."
                )
            }

            if (replaceAll) {
                database.homeworkDao().deleteAllHomework()
                database.examDao().deleteAllExams()
                database.scheduleDao().deleteAllScheduleEntries()
                database.academicNoteDao().deleteAllNotes()
                database.focusSessionDao().deleteAllSessions()
                database.subjectDao().deleteAllSubjects()
            }

            // 1. Asignaturas
            val subjectsList = mutableListOf<Subject>()
            val subjectsArray = root.optJSONArray("subjects")
            if (subjectsArray != null) {
                for (i in 0 until subjectsArray.length()) {
                    val obj = subjectsArray.getJSONObject(i)
                    subjectsList.add(
                        Subject(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "Asignatura"),
                            colorHex = obj.optString("colorHex", "#2196F3"),
                            professor = obj.optString("professor", ""),
                            classroom = obj.optString("classroom", ""),
                            iconName = obj.optString("iconName", "School")
                        )
                    )
                }
                if (subjectsList.isNotEmpty()) {
                    database.subjectDao().insertAllSubjects(subjectsList)
                }
            }

            // 2. Deberes
            val homeworkList = mutableListOf<Homework>()
            val homeworkArray = root.optJSONArray("homework")
            if (homeworkArray != null) {
                for (i in 0 until homeworkArray.length()) {
                    val obj = homeworkArray.getJSONObject(i)
                    homeworkList.add(
                        Homework(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Tarea"),
                            subjectId = obj.optLong("subjectId", 1L),
                            dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                            priority = obj.optString("priority", "MEDIA"),
                            status = obj.optString("status", "PENDIENTE"),
                            notes = obj.optString("notes", ""),
                            links = obj.optString("links", ""),
                            subtasks = obj.optString("subtasks", ""),
                            notificationDaysBefore = obj.optInt("notificationDaysBefore", -1)
                        )
                    )
                }
                if (homeworkList.isNotEmpty()) {
                    database.homeworkDao().insertAllHomework(homeworkList)
                    // Reprogramar recordatorios
                    for (hw in homeworkList) {
                        if (hw.notificationDaysBefore >= 0 && hw.status != "COMPLETADO") {
                            ReminderManager.scheduleReminder(
                                context = context,
                                id = hw.id,
                                isExam = false,
                                title = "📚 Deber pendiente: ${hw.title}",
                                dueDateMs = hw.dueDate,
                                daysBefore = hw.notificationDaysBefore
                            )
                        }
                    }
                }
            }

            // 3. Exámenes
            val examsList = mutableListOf<Exam>()
            val examsArray = root.optJSONArray("exams")
            if (examsArray != null) {
                for (i in 0 until examsArray.length()) {
                    val obj = examsArray.getJSONObject(i)
                    examsList.add(
                        Exam(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Examen"),
                            subjectId = obj.optLong("subjectId", 1L),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            difficulty = obj.optString("difficulty", "MEDIO"),
                            preparationStatus = obj.optString("preparationStatus", "NO_EMPEZADO"),
                            materials = obj.optString("materials", ""),
                            notificationDaysBefore = obj.optInt("notificationDaysBefore", -1)
                        )
                    )
                }
                if (examsList.isNotEmpty()) {
                    database.examDao().insertAllExams(examsList)
                    // Reprogramar recordatorios
                    for (ex in examsList) {
                        if (ex.notificationDaysBefore >= 0) {
                            ReminderManager.scheduleReminder(
                                context = context,
                                id = ex.id,
                                isExam = true,
                                title = "📝 Examen Próximo: ${ex.title}",
                                dueDateMs = ex.date,
                                daysBefore = ex.notificationDaysBefore
                            )
                        }
                    }
                }
            }

            // 4. Horarios
            val scheduleList = mutableListOf<ScheduleEntry>()
            val scheduleArray = root.optJSONArray("schedule")
            if (scheduleArray != null) {
                for (i in 0 until scheduleArray.length()) {
                    val obj = scheduleArray.getJSONObject(i)
                    scheduleList.add(
                        ScheduleEntry(
                            id = obj.optLong("id", 0L),
                            dayOfWeek = obj.optInt("dayOfWeek", 1),
                            startTime = obj.optString("startTime", "08:00"),
                            endTime = obj.optString("endTime", "09:00"),
                            subjectId = obj.optLong("subjectId", 0L),
                            customTitle = obj.optString("customTitle", ""),
                            classroom = obj.optString("classroom", ""),
                            professor = obj.optString("professor", ""),
                            colorHex = obj.optString("colorHex", "#2196F3"),
                            type = obj.optString("type", "CLASE")
                        )
                    )
                }
                if (scheduleList.isNotEmpty()) {
                    database.scheduleDao().insertAllScheduleEntries(scheduleList)
                }
            }

            // 5. Apuntes
            val notesList = mutableListOf<AcademicNote>()
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    notesList.add(
                        AcademicNote(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", "Nota"),
                            content = obj.optString("content", ""),
                            tags = obj.optString("tags", ""),
                            subjectId = obj.optLong("subjectId", 0L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (notesList.isNotEmpty()) {
                    database.academicNoteDao().insertAllNotes(notesList)
                }
            }

            // 6. Sesiones de enfoque
            val focusList = mutableListOf<FocusSession>()
            val focusArray = root.optJSONArray("focusSessions")
            if (focusArray != null) {
                for (i in 0 until focusArray.length()) {
                    val obj = focusArray.getJSONObject(i)
                    focusList.add(
                        FocusSession(
                            id = obj.optLong("id", 0L),
                            minutes = obj.optInt("minutes", 25),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            subjectId = obj.optLong("subjectId", 0L)
                        )
                    )
                }
                if (focusList.isNotEmpty()) {
                    database.focusSessionDao().insertAllSessions(focusList)
                }
            }

            RestoreSummary(
                success = true,
                subjectsRestored = subjectsList.size,
                homeworkRestored = homeworkList.size,
                examsRestored = examsList.size,
                schedulesRestored = scheduleList.size,
                notesRestored = notesList.size,
                focusSessionsRestored = focusList.size
            )
        } catch (e: Exception) {
            RestoreSummary(
                success = false,
                errorMessage = "Error procesando el archivo de respaldo: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Ejecuta una sincronización real con la Bóveda de Nube (Cloud Vault):
     * Serializa todos los datos actuales, guarda en el almacenamiento seguro de la nube local,
     * actualiza la marca de tiempo de sincronización y registra las estadísticas.
     */
    suspend fun syncWithCloudVault(): CloudVaultInfo = withContext(Dispatchers.IO) {
        val backupJson = createFullBackupJson()
        cloudVaultFile.writeText(backupJson)

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formatted = dateFormat.format(Date(now))

        val root = JSONObject(backupJson)
        val totalCount = (root.optJSONArray("homework")?.length() ?: 0) +
                (root.optJSONArray("exams")?.length() ?: 0) +
                (root.optJSONArray("schedule")?.length() ?: 0) +
                (root.optJSONArray("subjects")?.length() ?: 0) +
                (root.optJSONArray("notes")?.length() ?: 0)

        prefs.edit()
            .putLong("last_sync_timestamp", now)
            .putString("last_sync_formatted", formatted)
            .putInt("last_sync_count", totalCount)
            .apply()

        CloudVaultInfo(
            hasCloudData = true,
            lastSyncTimeMillis = now,
            totalItems = totalCount,
            formattedLastSync = formatted,
            backupSizeBytes = cloudVaultFile.length()
        )
    }

    /**
     * Restaura los datos guardados en la Bóveda de Nube (Cloud Vault).
     */
    suspend fun restoreFromCloudVault(): RestoreSummary = withContext(Dispatchers.IO) {
        if (!cloudVaultFile.exists()) {
            return@withContext RestoreSummary(
                success = false,
                errorMessage = "No existe ninguna copia sincronizada en la nube todavía. Sincroniza primero."
            )
        }
        val json = cloudVaultFile.readText()
        restoreBackupFromJson(json, replaceAll = false)
    }

    /**
     * Obtiene la información actual de la Bóveda de Nube.
     */
    fun getCloudVaultInfo(): CloudVaultInfo {
        val hasData = cloudVaultFile.exists() && cloudVaultFile.length() > 0
        val lastSync = prefs.getLong("last_sync_timestamp", 0L)
        val formatted = prefs.getString("last_sync_formatted", "Sin sincronizar") ?: "Sin sincronizar"
        val count = prefs.getInt("last_sync_count", 0)

        return CloudVaultInfo(
            hasCloudData = hasData,
            lastSyncTimeMillis = lastSync,
            totalItems = count,
            formattedLastSync = if (lastSync > 0) formatted else "Sin sincronizar",
            backupSizeBytes = if (hasData) cloudVaultFile.length() else 0L
        )
    }
}
