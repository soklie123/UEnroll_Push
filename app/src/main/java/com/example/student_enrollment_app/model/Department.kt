package com.example.student_enrollment_app.model

import com.google.firebase.firestore.DocumentId
data class Department(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val seats: Long = 0,
    val seatsAvailable: Long = 0,
    val color: String = "",
    val logoUrl: String = "",
    val facultyId: String = "",
    val description: String = "",
    val tuition: Long = 0,
    val duration: String = "",
    val requirements: String = ""
)