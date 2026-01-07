package com.example.student_enrollment_app.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class NotificationEvent(
    val title: String,
    val message: String,
    val type: String = "notification"
)

object NotificationEventManager {
    private val _notificationFlow = MutableSharedFlow<NotificationEvent>(replay = 0)
    val notificationFlow = _notificationFlow.asSharedFlow()

    suspend fun sendNotification(title: String, message: String, type: String = "notification") {
        _notificationFlow.emit(NotificationEvent(title, message, type))
    }
}