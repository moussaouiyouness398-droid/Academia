package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Gestor centralizado de notificaciones y alertas en la aplicación.
 * Permite agregar alertas, limpiarlas y eliminar de forma inmediata todas las
 * notificaciones relacionadas con tareas o exámenes que hayan sido eliminados.
 */
object NotificationStore {
    private val _notifications = MutableStateFlow<List<String>>(
        listOf(
            "¡Bienvenido a Academia! Tu agenda está pre-cargada con tus materias principales.",
            "Recordatorio: Tareas en Matemáticas vencen pronto."
        )
    )
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    fun addNotification(message: String) {
        _notifications.update { current ->
            listOf(message) + current
        }
    }

    /**
     * Remueve del historial de notificaciones cualquier mensaje relacionado
     * con la tarea o examen especificado por su título.
     */
    fun removeNotificationsForTitle(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        _notifications.update { current ->
            current.filterNot { msg ->
                msg.contains("'$trimmed'", ignoreCase = true) ||
                msg.contains("‘$trimmed’", ignoreCase = true) ||
                msg.contains("“$trimmed”", ignoreCase = true) ||
                msg.contains("\"$trimmed\"", ignoreCase = true) ||
                msg.contains(": $trimmed", ignoreCase = true) ||
                msg.contains(" $trimmed ", ignoreCase = true) ||
                msg.endsWith(" $trimmed", ignoreCase = true) ||
                (msg.contains(trimmed, ignoreCase = true) &&
                    (msg.contains("tarea", ignoreCase = true) ||
                     msg.contains("deber", ignoreCase = true) ||
                     msg.contains("examen", ignoreCase = true)))
            }
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }
}
