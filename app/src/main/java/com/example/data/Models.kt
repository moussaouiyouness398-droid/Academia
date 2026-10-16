package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val professor: String,
    val classroom: String,
    val iconName: String = "School"
) : Serializable

@Entity(
    tableName = "homework",
    indices = [
        androidx.room.Index(value = ["subjectId"]),
        androidx.room.Index(value = ["dueDate"]),
        androidx.room.Index(value = ["status"]),
        androidx.room.Index(value = ["priority"])
    ]
)
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long,
    val dueDate: Long,
    val priority: String, // "ALTA", "MEDIA", "BAJA"
    val status: String,   // "PENDIENTE", "EN_PROGRESO", "COMPLETADO"
    val notes: String = "",
    val links: String = "",
    val subtasks: String = "", // format "Subtask1|true,Subtask2|false"
    val notificationDaysBefore: Int = -1 // -1 = disabled, 0 = same day, 1 = 1 day before, etc.
) : Serializable {
    fun getSubtaskList(): List<SubTaskItem> {
        if (subtasks.isEmpty()) return emptyList()
        return try {
            subtasks.split(",").mapNotNull {
                val parts = it.split("|")
                if (parts.size == 2) {
                    SubTaskItem(parts[0], parts[1].toBoolean())
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class SubTaskItem(val title: String, val isCompleted: Boolean)

@Entity(
    tableName = "exams",
    indices = [
        androidx.room.Index(value = ["subjectId"]),
        androidx.room.Index(value = ["date"]),
        androidx.room.Index(value = ["difficulty"]),
        androidx.room.Index(value = ["preparationStatus"])
    ]
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long,
    val date: Long,
    val difficulty: String, // "FÁCIL", "MEDIO", "DIFÍCIL"
    val preparationStatus: String, // "NO_EMPEZADO", "ESTUDIANDO", "PREPARADO"
    val materials: String = "",
    val notificationDaysBefore: Int = -1 // -1 = disabled, 0 = same day, 1 = 1 day before, etc.
) : Serializable

/**
 * Entidad relacional para unir Deberes con su Asignatura correspondiente de forma eficiente en Room.
 */
data class HomeworkWithSubject(
    @androidx.room.Embedded val homework: Homework,
    @androidx.room.Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: Subject? = null
) : Serializable

/**
 * Entidad relacional para unir Exámenes con su Asignatura correspondiente de forma eficiente en Room.
 */
data class ExamWithSubject(
    @androidx.room.Embedded val exam: Exam,
    @androidx.room.Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: Subject? = null
) : Serializable

@Entity(tableName = "notes")
data class AcademicNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "", // comma separated
    val subjectId: Long = 0, // optional link to a subject
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val minutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val subjectId: Long = 0 // optional
) : Serializable

@Entity(
    tableName = "schedule_entries",
    indices = [
        androidx.room.Index(value = ["dayOfWeek"]),
        androidx.room.Index(value = ["subjectId"])
    ]
)
data class ScheduleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Lunes, 2 = Martes, 3 = Miércoles, 4 = Jueves, 5 = Viernes, 6 = Sábado, 7 = Domingo
    val startTime: String, // e.g. "08:30"
    val endTime: String,   // e.g. "10:00"
    val subjectId: Long = 0,
    val customTitle: String = "",
    val classroom: String = "",
    val professor: String = "",
    val colorHex: String = "#2196F3",
    val type: String = "CLASE" // "CLASE", "PRÁCTICA", "LABORATORIO", "TALLER", "ESTUDIO", "TUTORÍA", "OTRO"
) : Serializable
