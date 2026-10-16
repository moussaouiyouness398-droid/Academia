package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen

/**
 * Representa un elemento de navegación para la barra inferior (NavigationBar).
 * Define icono seleccionado (Filled) y no seleccionado (Outlined) según Material 3.
 */
data class NavigationTabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val matchingRoutes: List<String> = listOf(route)
)

/**
 * Pestañas base solicitadas por la arquitectura:
 * - Home (Inicio)
 * - Calendar (Calendario)
 * - Statistics (Estadísticas)
 */
val CoreNavigationTabs = listOf(
    NavigationTabItem(
        route = Screen.Home.route,
        title = "Inicio",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home",
        matchingRoutes = listOf(
            Screen.Home.route,
            Screen.Dashboard.route,
            "home",
            "dashboard"
        )
    ),
    NavigationTabItem(
        route = Screen.Calendar.route,
        title = "Calendario",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_calendar",
        matchingRoutes = listOf(
            Screen.Calendar.route,
            "calendar"
        )
    ),
    NavigationTabItem(
        route = Screen.Stats.route,
        title = "Estadísticas",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_stats",
        matchingRoutes = listOf(
            Screen.Stats.route,
            Screen.Statistics.route,
            "stats",
            "statistics"
        )
    )
)

/**
 * Pestañas completas de navegación incluyendo módulos complementarios (Académico y Menú).
 */
val AllNavigationTabs = CoreNavigationTabs + listOf(
    NavigationTabItem(
        route = Screen.HomeworkList.route,
        title = "Académico",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
        testTag = "nav_homework",
        matchingRoutes = listOf(
            Screen.HomeworkList.route,
            Screen.ExamList.route,
            Screen.TaskList.route,
            "homework",
            "exams",
            "task_list"
        )
    ),
    NavigationTabItem(
        route = Screen.More.route,
        title = "Menú",
        selectedIcon = Icons.Filled.Menu,
        unselectedIcon = Icons.Outlined.Menu,
        testTag = "nav_more",
        matchingRoutes = listOf(
            Screen.More.route,
            Screen.Focus.route,
            Screen.Subjects.route,
            Screen.WeeklyPlanner.route,
            Screen.Notes.route,
            Screen.Settings.route,
            Screen.DashboardSummary.route
        )
    )
)

/**
 * Obtiene el título legible para la barra superior en subpantallas.
 */
fun getScreenTitle(route: String): String {
    return when (route) {
        Screen.ExamList.route, "exams" -> "Exámenes"
        Screen.HomeworkList.route, "homework" -> "Deberes"
        Screen.DashboardSummary.route -> "Dashboard General"
        Screen.Subjects.route -> "Asignaturas"
        Screen.WeeklyPlanner.route -> "Planificador Semanal"
        Screen.Notes.route -> "Apuntes y Notas"
        Screen.Stats.route, Screen.Statistics.route, "stats", "statistics" -> "Estadísticas"
        Screen.Settings.route -> "Configuración"
        Screen.Focus.route -> "Modo Enfoque"
        else -> "Atrás"
    }
}

/**
 * Layout de pantalla base usando Material 3 Scaffolding con una NavigationBar inferior
 * para acceder a Home, Calendar y Statistics con iconos de Material 3.
 *
 * @param currentRoute Ruta activa actual de navegación.
 * @param onNavigateToTab Callback para alternar pestañas principales.
 * @param onNavigateBack Callback para volver a la pantalla anterior.
 * @param navigationTabs Lista de pestañas a mostrar en la NavigationBar (por defecto [AllNavigationTabs] con el trío [CoreNavigationTabs]).
 * @param showBottomBar Control de visibilidad de la NavigationBar.
 * @param content Contenido composable envuelto en el Scaffold con insets adecuados.
 */
@Composable
fun BaseScreenLayout(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationTabs: List<NavigationTabItem> = AllNavigationTabs,
    showBottomBar: Boolean = true,
    customTopBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    // Determinar si es una pantalla principal de las pestañas
    val isPrimaryScreen = navigationTabs.any { currentRoute in it.matchingRoutes }
    val showBackButton = !isPrimaryScreen

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (customTopBar != null) {
                customTopBar()
            } else if (showBackButton) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("btn_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = getScreenTitle(currentRoute),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    tonalElevation = 2.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_navigation_bar")
                ) {
                    navigationTabs.forEach { tab ->
                        val isSelected = currentRoute in tab.matchingRoutes
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { onNavigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (tab.title.length > 8) 9.5.sp else 11.sp,
                                        letterSpacing = if (tab.title.length > 8) (-0.5).sp else (-0.2).sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            floatingActionButton?.invoke()
        },
        snackbarHost = {
            snackbarHost?.invoke()
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
