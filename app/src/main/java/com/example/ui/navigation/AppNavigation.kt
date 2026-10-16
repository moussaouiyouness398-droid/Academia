package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.AcademiaViewModel
import com.example.ui.HomeworkExamViewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.*

/**
 * Orden relativo de las pantallas para determinar la dirección del desplazamiento (slide).
 * Las pestañas de la barra inferior se ordenan de izquierda a derecha:
 * Inicio (0) -> Calendario (1) -> Académico (2) -> Enfoque (3) -> Menú (4)
 */
private fun getScreenOrder(route: String?): Float {
    return when (route) {
        Screen.Home.route, Screen.Dashboard.route, "home", "dashboard" -> 0f
        Screen.Calendar.route, "calendar" -> 1f
        Screen.Stats.route, Screen.Statistics.route, "stats", "statistics" -> 2f
        Screen.HomeworkList.route, Screen.TaskList.route, "homework", "task_list" -> 3f
        Screen.ExamList.route, "exams" -> 3.5f
        Screen.More.route, "more" -> 4f
        else -> 10f // Subpantallas abiertas desde el menú o tarjetas (Focus, Subjects, Notes, Planner, Settings, etc.)
    }
}

/**
 * Estructura de navegación principal usando Jetpack Compose Navigation.
 * Incorpora animaciones de desplazamiento (slide) fluidas, continuas y naturales
 * con especificaciones completas de Material Design 3 (FastOutSlowInEasing) y desplazamiento de ancho completo
 * al alternar entre las secciones principales (Inicio, Calendario, Estadísticas, Académico, Menú)
 * y al abrir o volver de subpantallas.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    academiaViewModel: AcademiaViewModel,
    homeworkExamViewModel: HomeworkExamViewModel,
    mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            val initialOrder = getScreenOrder(initialState.destination.route)
            val targetOrder = getScreenOrder(targetState.destination.route)

            if (targetOrder >= 10f) {
                // Entrando a una subpantalla: deslizamiento limpio desde el borde derecho con fade
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.75f).toInt() },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else if (initialOrder >= 10f) {
                // Regresando de una subpantalla a una pestaña principal
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 240))
            } else if (targetOrder > initialOrder) {
                // Desplazamiento fluido hacia la derecha entre pestañas con sutil paralaje
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else if (targetOrder < initialOrder) {
                // Desplazamiento fluido hacia la izquierda entre pestañas con sutil paralaje
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else {
                fadeIn(animationSpec = tween(durationMillis = 220))
            }
        },
        exitTransition = {
            val initialOrder = getScreenOrder(initialState.destination.route)
            val targetOrder = getScreenOrder(targetState.destination.route)

            if (targetOrder >= 10f) {
                // Pantalla padre retrocede suavemente hacia la izquierda (-20%)
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else if (initialOrder >= 10f) {
                // Subpantalla saliendo hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.75f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else if (targetOrder > initialOrder) {
                // Pestaña saliendo suavemente hacia la izquierda
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else if (targetOrder < initialOrder) {
                // Pestaña saliendo suavemente hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else {
                fadeOut(animationSpec = tween(durationMillis = 220))
            }
        },
        popEnterTransition = {
            val initialOrder = getScreenOrder(initialState.destination.route)
            val targetOrder = getScreenOrder(targetState.destination.route)

            if (initialOrder >= 10f) {
                // Regresando desde una subpantalla hacia la pantalla anterior
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.20f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else if (targetOrder < initialOrder) {
                // Regresando a una pestaña a la izquierda
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else if (targetOrder > initialOrder) {
                // Regresando a una pestaña a la derecha
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 260))
            } else {
                fadeIn(animationSpec = tween(durationMillis = 220))
            }
        },
        popExitTransition = {
            val initialOrder = getScreenOrder(initialState.destination.route)
            val targetOrder = getScreenOrder(targetState.destination.route)

            if (initialOrder >= 10f) {
                // Subpantalla saliendo hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.75f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else if (targetOrder < initialOrder) {
                // Pestaña saliendo suavemente hacia la derecha
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else if (targetOrder > initialOrder) {
                // Pestaña saliendo suavemente hacia la izquierda
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            } else {
                fadeOut(animationSpec = tween(durationMillis = 220))
            }
        }
    ) {
        // 1. Pantalla Principal / Inicio / Home (Soporta "home" y "dashboard")
        val homeComposable: @Composable () -> Unit = {
            HomeScreen(
                viewModel = mainViewModel,
                onNavigateToHomework = {
                    navController.navigate(Screen.HomeworkList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToExams = {
                    navController.navigate(Screen.ExamList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToFocus = {
                    navController.navigate(Screen.Focus.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Home.route) { homeComposable() }
        composable(Screen.Dashboard.route) { homeComposable() }
        composable("home") { homeComposable() }
        composable("dashboard") { homeComposable() }

        // 2. Calendario / Calendar
        composable(Screen.Calendar.route) {
            CalendarScreen(viewModel = academiaViewModel)
        }
        composable("calendar") {
            CalendarScreen(viewModel = academiaViewModel)
        }

        // 3. Pantalla Dedicada: HomeworkListScreen (consumiendo HomeworkExamViewModel)
        composable(Screen.HomeworkList.route) {
            TaskListScreen(
                viewModel = mainViewModel,
                onNavigateToExams = {
                    navController.navigate(Screen.ExamList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Ruta dedicada para TaskListScreen
        composable(Screen.TaskList.route) {
            TaskListScreen(
                viewModel = mainViewModel,
                onNavigateToExams = {
                    navController.navigate(Screen.ExamList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Alias para ruta legacy "homework"
        composable("homework") {
            TaskListScreen(
                viewModel = mainViewModel,
                onNavigateToExams = {
                    navController.navigate(Screen.ExamList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // 4. Pantalla Dedicada: ExamListScreen (consumiendo HomeworkExamViewModel)
        composable(Screen.ExamList.route) {
            ExamListScreen(
                viewModel = homeworkExamViewModel,
                onNavigateToHomework = {
                    navController.navigate(Screen.HomeworkList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Alias para ruta legacy "exams"
        composable("exams") {
            ExamListScreen(
                viewModel = homeworkExamViewModel,
                onNavigateToHomework = {
                    navController.navigate(Screen.HomeworkList.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Enfoque / Pomodoro
        composable(Screen.Focus.route) {
            FocusScreen(viewModel = academiaViewModel)
        }

        // 6. Menú Principal de Opciones
        composable(Screen.More.route) {
            MoreScreen(onNavigateTo = { destinationRoute ->
                val target = when (destinationRoute) {
                    "homework" -> Screen.HomeworkList.route
                    "exams" -> Screen.ExamList.route
                    else -> destinationRoute
                }
                navController.navigate(target) {
                    launchSingleTop = true
                }
            })
        }

        // 7. Resumen de Dashboard
        composable(Screen.DashboardSummary.route) {
            DashboardScreen(
                viewModel = academiaViewModel,
                onNavigateToHomework = { navController.navigate(Screen.HomeworkList.route) },
                onNavigateToExams = { navController.navigate(Screen.ExamList.route) },
                onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                onAddHomeworkClick = { navController.navigate(Screen.HomeworkList.route) },
                onAddExamClick = { navController.navigate(Screen.ExamList.route) }
            )
        }

        // 8. Pantallas Secundarias
        composable(Screen.Subjects.route) {
            SubjectsScreen(viewModel = academiaViewModel)
        }

        composable(Screen.WeeklyPlanner.route) {
            WeeklyPlannerScreen(viewModel = academiaViewModel)
        }

        composable(Screen.Notes.route) {
            NotesScreen(viewModel = academiaViewModel)
        }

        // 8. Estadísticas / Statistics (Soporta "stats" y "statistics")
        composable(Screen.Stats.route) {
            StatsScreen(viewModel = academiaViewModel)
        }
        composable(Screen.Statistics.route) {
            StatsScreen(viewModel = academiaViewModel)
        }
        composable("statistics") {
            StatsScreen(viewModel = academiaViewModel)
        }
        composable("stats") {
            StatsScreen(viewModel = academiaViewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = academiaViewModel)
        }
    }
}
