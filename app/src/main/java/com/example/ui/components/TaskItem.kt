package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Homework
import com.example.data.Subject
import com.example.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Componente TaskItem reutilizable diseñado con especificaciones Material 3:
 * - Soporte para selección y expansión detallada con animaciones fluidas
 * - Botón de completado interactivo con animación de escala/pulsación
 * - Chips para Asignatura (color de la materia), Prioridad (Alta, Media, Baja) y Fecha de Vencimiento
 * - Alerta visual si la tarea está atrasada (overdue)
 * - Botones de acción accesibles con tamaño mínimo de 48dp (editar, eliminar)
 * - Diseñado para su reutilización en TaskListScreen, MainScreen, Dashboard y Calendario
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    homework: Homework,
    subject: Subject? = null,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isCompleted = homework.status == "COMPLETADO"
    val now = System.currentTimeMillis()
    val isOverdue = homework.dueDate < now && !isCompleted

    // Color temático de la materia
    val subjectColor = remember(subject?.colorHex) {
        ColorUtils.parseColorSafe(subject?.colorHex, fallback = Color(0xFF6750A4))
    }

    // Colores y badges según la prioridad
    val (priorityColor, priorityBg) = when (homework.priority.uppercase()) {
        "ALTA" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        "BAJA" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
        else -> Color(0xFFE65100) to Color(0xFFFFF3E0) // MEDIA
    }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault()) }
    val formattedDueDate = remember(homework.dueDate) {
        dateFormat.format(Date(homework.dueDate))
    }

    // Animación de escala para el botón de completado
    val checkmarkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_scale"
    )

    // Animación de elevación y color al completarse
    val containerColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        } else if (isOverdue) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 260),
        label = "card_container_color"
    )

    val cardBorder = if (isOverdue) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    } else if (isExpanded) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else if (isCompleted) 0.5.dp else 1.5.dp
        ),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("task_item_${homework.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox interactivo Material 3 con área táctil accesible >= 48dp
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onToggleCompletion() }
                        .testTag("task_toggle_complete_${homework.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) "Marcar como pendiente" else "Completar tarea",
                        tint = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else if (isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .size(26.dp)
                            .scale(checkmarkScale)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp)
                ) {
                    // Fila superior de chips: Materia, Prioridad y Estado Atrasado si corresponde
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip Asignatura
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = subjectColor.copy(alpha = 0.15f),
                            contentColor = subjectColor,
                            modifier = Modifier.testTag("task_subject_chip_${homework.id}")
                        ) {
                            Text(
                                text = subject?.name ?: "General",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isOverdue) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Atrasada",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // Badge Prioridad
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = priorityBg,
                                modifier = Modifier.testTag("task_priority_chip_${homework.id}")
                            ) {
                                Text(
                                    text = homework.priority.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = priorityColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Título de la tarea
                    Text(
                        text = homework.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("task_title_${homework.id}")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Fecha de entrega / recordatorio
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDueDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (homework.notificationDaysBefore >= 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Recordatorio activo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Sección Expandible: Notas y Acciones (Editar/Eliminar)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 240)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    if (homework.notes.isNotBlank()) {
                        Text(
                            text = homework.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }

                    // Acciones rápidas al expandir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onEdit != null) {
                            OutlinedButton(
                                onClick = onEdit,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("task_edit_button_${homework.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (onDelete != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("task_delete_button_${homework.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
