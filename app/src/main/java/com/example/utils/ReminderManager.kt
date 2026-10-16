package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.ReminderReceiver

object ReminderManager {
    const val CHANNEL_ID = "academia_reminders_channel"
    const val CHANNEL_NAME = "Recordatorios de Academia"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para entregas de tareas y exámenes"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun sendImmediateTestNotification(
        context: Context,
        title: String = "🔔 ¡Notificación de Academia activa!",
        message: String = "El sistema de alertas y recordatorios funciona correctamente en tu dispositivo."
    ) {
        ensureChannel(context)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", (System.currentTimeMillis() % 100000).toInt() + 70000)
        }
        context.sendBroadcast(intent)
    }

    fun scheduleTestNotificationInSeconds(
        context: Context,
        seconds: Int = 5
    ) {
        ensureChannel(context)
        val triggerTime = System.currentTimeMillis() + (seconds * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "⏰ Alerta de Prueba (en $seconds seg)")
            putExtra("message", "¡Verificación de alarma completada con éxito! El sistema en segundo plano está 100% operativo.")
            putExtra("id", 99999)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            99999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        tryScheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun scheduleReminder(
        context: Context,
        id: Long,
        isExam: Boolean,
        title: String,
        dueDateMs: Long,
        daysBefore: Int
    ) {
        if (daysBefore < 0) return

        ensureChannel(context)

        // Calculate trigger timestamp
        var triggerTime = dueDateMs - (daysBefore * 24L * 60L * 60L * 1000L)
        val now = System.currentTimeMillis()

        // If the calculated advance date is already in the past, but the due date is still ahead,
        // schedule an immediate reminder so the student doesn't miss it!
        if (triggerTime <= now) {
            if (dueDateMs > now) {
                // Trigger in 10 seconds
                triggerTime = now + 10000L
            } else {
                // Completely past event
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", if (isExam) "¡Examen Próximo! 📝" else "¡Entrega de Tarea! 🎒")
            val daysStr = if (daysBefore == 0) "hoy" else "en $daysBefore día(s)"
            putExtra("message", "Recuerda: '$title' vence $daysStr.")
            putExtra("id", id.toInt() + (if (isExam) 20000 else 10000))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt() + (if (isExam) 20000 else 10000),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        tryScheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    private fun tryScheduleAlarm(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Handle cases where exact alarm permission is restricted
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (ignored: Exception) {
            }
        } catch (ignored: Exception) {
        }
    }

    fun cancelReminder(context: Context, id: Long, isExam: Boolean) {
        val notificationId = id.toInt() + (if (isExam) 20000 else 10000)

        // 1. Quitar de la barra de notificaciones del sistema cualquier notificación activa de esta tarea/examen
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        } catch (ignored: Exception) {
        }

        // 2. Cancelar la alarma futura programada en AlarmManager
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (ignored: Exception) {
        }
    }
}
