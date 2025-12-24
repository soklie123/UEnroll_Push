package com.example.student_enrollment_app.model

import java.util.Date
import com.google.firebase.Timestamp

data class Enrollment(
    val id: String = "", // Auto-generated ID
    val studentId: String = "", // Links to User UID
    val departmentId: String = "", // Links to Department ID
    val status: String = "pending", // "pending", "approved", or "rejected"
    val appliedAt: Timestamp? = null // Firestore Timestamp for tracking application time
)