package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        Homework::class,
        Exam::class,
        Subject::class,
        AcademicNote::class,
        FocusSession::class,
        ScheduleEntry::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeworkDao(): HomeworkDao
    abstract fun examDao(): ExamDao
    abstract fun subjectDao(): SubjectDao
    abstract fun academicNoteDao(): AcademicNoteDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "academia_db"
                ).addCallback(AppDatabaseCallback())
                 .fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // Populate subjects
            db.execSQL("INSERT INTO subjects (id, name, colorHex, professor, classroom, iconName) VALUES (1, 'Matemáticas Avanzadas', '#2196F3', 'Dr. Alberto Ramírez', 'Aula 302', 'Functions')")
            db.execSQL("INSERT INTO subjects (id, name, colorHex, professor, classroom, iconName) VALUES (2, 'Literatura Universal', '#E91E63', 'Dra. Sofía Galván', 'Aula 104', 'Book')")
            db.execSQL("INSERT INTO subjects (id, name, colorHex, professor, classroom, iconName) VALUES (3, 'Física Cuántica I', '#9C27B0', 'Prof. Juan Meléndez', 'Laboratorio de Ciencias', 'Science')")
            db.execSQL("INSERT INTO subjects (id, name, colorHex, professor, classroom, iconName) VALUES (4, 'Programación Móvil', '#4CAF50', 'MSc. Carlos Martínez', 'Aula de Cómputo B', 'Computer')")
            
            val now = System.currentTimeMillis()
            val twoDaysLater = now + 86400000 * 2
            val sevenDaysLater = now + 86400000 * 7
            val yesterday = now - 86400000
            
            // Populate homework tasks
            db.execSQL(
                "INSERT INTO homework (title, subjectId, dueDate, priority, status, notes, links, subtasks, notificationDaysBefore) VALUES " +
                "('Ecuaciones Diferenciales Parciales', 1, $twoDaysLater, 'ALTA', 'PENDIENTE', " +
                "'Resolver los ejercicios 1 al 10 del capítulo 4. Entregar en hojas de examen.', " +
                "'https://example.com/math-exercises', " +
                "'Hacer bosquejos de gráficas|false,Escribir desarrollos completos|false,Revisar soluciones con software|false', -1)"
            )
            
            db.execSQL(
                "INSERT INTO homework (title, subjectId, dueDate, priority, status, notes, links, subtasks, notificationDaysBefore) VALUES " +
                "('Proyecto final: App Android', 4, $sevenDaysLater, 'ALTA', 'EN_PROGRESO', " +
                "'Estructurar la persistencia de datos y pulir el diseño usando Jetpack Compose y Material 3.', " +
                "'https://example.com/project-guidelines', " +
                "'Diseñar base de datos Room|true,Añadir vistas Navigation|true,Optimizar colores dinámicos|false', -1)"
            )
            
            db.execSQL(
                "INSERT INTO homework (title, subjectId, dueDate, priority, status, notes, links, subtasks, notificationDaysBefore) VALUES " +
                "('Ensayo de la Generación del 27', 2, $yesterday, 'MEDIA', 'PENDIENTE', " +
                "'Mínimo 3 páginas con análisis crítico del poema ''Romance Sonámbulo''.', '', '', -1)"
            )
            
            // Populate exams
            val fourDaysLater = now + 86400000 * 4
            val tenDaysLater = now + 86400000 * 10
            
            db.execSQL(
                "INSERT INTO exams (title, subjectId, date, difficulty, preparationStatus, materials, notificationDaysBefore) VALUES " +
                "('Primer Examen Parcial de Electromagnetismo', 3, $fourDaysLater, 'DIFÍCIL', 'ESTUDIANDO', " +
                "'Leyes de Maxwell, potencial eléctrico, conductores y campos dieléctricos.', -1)"
            )
            
            db.execSQL(
                "INSERT INTO exams (title, subjectId, date, difficulty, preparationStatus, materials, notificationDaysBefore) VALUES " +
                "('Control de Lectura: Don Quijote', 2, $tenDaysLater, 'MEDIO', 'NO_EMPEZADO', " +
                "'Lectura de la primera parte completa (capítulos 1 al 52).', -1)"
            )
            
            // Populate Notes
            db.execSQL(
                "INSERT INTO notes (title, content, tags, subjectId, createdAt) VALUES " +
                "('Fórmula de Euler y números complejos', " +
                "'La identidad de Euler: e^(i*pi) + 1 = 0 une cinco constantes fundamentales. Es un faro en la física cuántica y señales complejas en ingeniería.', " +
                "'Fórmula,Estudio,Matemáticas', 1, $now)"
            )
            
            db.execSQL(
                "INSERT INTO notes (title, content, tags, subjectId, createdAt) VALUES " +
                "('Comandos básicos de Git para proyectos', " +
                "'• `git status` para ver archivos modificados\\n• `git add .` para preparar cambios\\n• `git commit -m \"msg\"` para registrar la foto del estado\\n• `git push` para subir a la nube', " +
                "'Git,Programación,Guía', 4, $now)"
            )

            // Populate Weekly Schedule Entries (1 = Lunes, 2 = Martes, 3 = Miércoles, 4 = Jueves, 5 = Viernes)
            db.execSQL(
                "INSERT INTO schedule_entries (dayOfWeek, startTime, endTime, subjectId, customTitle, classroom, professor, colorHex, type) VALUES " +
                "(1, '08:30', '10:00', 1, 'Matemáticas Avanzadas', 'Aula 302', 'Dr. Alberto Ramírez', '#2196F3', 'CLASE'), " +
                "(1, '10:15', '11:45', 2, 'Literatura Universal', 'Aula 104', 'Dra. Sofía Galván', '#E91E63', 'CLASE'), " +
                "(1, '12:00', '13:30', 4, 'Programación Móvil', 'Aula de Cómputo B', 'MSc. Carlos Martínez', '#4CAF50', 'LABORATORIO'), " +
                "(2, '09:00', '10:30', 3, 'Física Cuántica I', 'Laboratorio de Ciencias', 'Prof. Juan Meléndez', '#9C27B0', 'CLASE'), " +
                "(2, '11:00', '12:30', 4, 'Programación Móvil', 'Aula de Cómputo B', 'MSc. Carlos Martínez', '#4CAF50', 'PRÁCTICA'), " +
                "(3, '08:30', '10:00', 1, 'Matemáticas Avanzadas', 'Aula 302', 'Dr. Alberto Ramírez', '#2196F3', 'CLASE'), " +
                "(3, '10:30', '12:00', 0, 'Bloque de Estudio y Biblioteca', 'Biblioteca Central', 'Autoestudio', '#FF9800', 'ESTUDIO'), " +
                "(4, '09:00', '10:30', 3, 'Física Cuántica I', 'Laboratorio de Ciencias', 'Prof. Juan Meléndez', '#9C27B0', 'CLASE'), " +
                "(4, '11:00', '12:30', 2, 'Literatura Universal', 'Aula 104', 'Dra. Sofía Galván', '#E91E63', 'CLASE'), " +
                "(5, '08:30', '10:30', 4, 'Laboratorio Móvil', 'Aula de Cómputo B', 'MSc. Carlos Martínez', '#4CAF50', 'TALLER'), " +
                "(5, '11:00', '12:30', 1, 'Tutoría de Matemáticas', 'Aula 302', 'Dr. Alberto Ramírez', '#2196F3', 'TUTORÍA')"
            )
        }
    }
}

typealias AcademiaDatabase = AppDatabase

