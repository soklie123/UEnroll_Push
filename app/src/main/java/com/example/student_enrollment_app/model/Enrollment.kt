package com.example.student_enrollment_app.model

import java.util.Date
import com.google.firebase.Timestamp

data class Enrollment(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val faculty: String = "",
    val major: String = "",
    val idCardUrl: String = "",
    val transcriptUrl: String = "",
    val photoUrl: String = "",
    val timestamp: Long = 0L,
    val status: String = "Pending",
    val confirmationNumber: String = "",

)
