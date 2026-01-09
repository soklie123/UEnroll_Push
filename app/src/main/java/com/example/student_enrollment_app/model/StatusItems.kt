package com.example.student_enrollment_app.model

// Enum for status types
enum class StatusType {
    COMPLETED,
    IN_PROGRESS,
    PENDING,
    FAILED,
    REJECTED
}

// Model for Status Screen items (simple list)
data class StatusItem(
    val title: String,
    val subtitle: String? = null,
    val type: StatusType
)

// Model for Status Detail Screen items (detailed cards)
data class StatusDetailItem(
    val title: String,
    val subtitle: String,
    val description: String? = null,
    val type: StatusType
)