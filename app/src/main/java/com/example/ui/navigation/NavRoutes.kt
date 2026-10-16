package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de navegación tipados para Jetpack Compose Navigation.
 * Permite alternar de forma estructurada entre Inicio (Home), Calendario (Calendar)
 * y Estadísticas (Statistics), así como módulos complementarios.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    // Destinos Principales
    data object Home : Screen("home", "Inicio", Icons.Default.Home, "nav_home")
    data object Dashboard : Screen("dashboard", "Inicio", Icons.Default.Dashboard, "nav_dashboard")
    data object Calendar : Screen("calendar", "Calendario", Icons.Default.CalendarToday, "nav_calendar")
    data object Stats : Screen("stats", "Estadísticas", Icons.Default.BarChart, "nav_stats")
    data object Statistics : Screen("statistics", "Estadísticas", Icons.Default.Analytics, "nav_statistics")
    data object HomeworkList : Screen("homework_list", "Deberes", Icons.AutoMirrored.Filled.Assignment, "nav_homework")
    data object TaskList : Screen("task_list", "Tareas", Icons.Default.Checklist, "nav_task_list")
    data object ExamList : Screen("exam_list", "Exámenes", Icons.Default.School, "nav_exams")
    data object Focus : Screen("focus", "Enfoque", Icons.Default.HourglassEmpty, "nav_focus")
    data object More : Screen("more", "Menú", Icons.Default.Menu, "nav_more")

    // Destinos Secundarios accesibles desde el menú o accesos directos
    data object Subjects : Screen("subjects", "Asignaturas", Icons.AutoMirrored.Filled.MenuBook, "nav_subjects")
    data object WeeklyPlanner : Screen("weekly_planner", "Planificador", Icons.Default.ViewWeek, "nav_weekly_planner")
    data object Notes : Screen("notes", "Apuntes", Icons.Default.EditNote, "nav_notes")
    data object Settings : Screen("settings", "Configuración", Icons.Default.Settings, "nav_settings")
    data object DashboardSummary : Screen("dashboard_summary", "Resumen General", Icons.Default.Analytics, "nav_dashboard_summary")

    companion object {
        /**
         * Pantallas principales de la barra de navegación inferior:
         * Inicio (Home), Calendario (Calendar), Estadísticas (Statistics), Académico y Menú.
         */
        val bottomNavScreens = listOf(
            Home,
            Calendar,
            Stats,
            HomeworkList,
            More
        )
    }
}
