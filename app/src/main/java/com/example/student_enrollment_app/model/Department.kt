package com.example.student_enrollment_app.model


data class Department(
    val id: String = "",           // Ensure this is exactly "id"
    val name: String = "",
    val seats: Long = 0,
    val seatsAvailable: Long = 0,
    val color: String = "",
    val facultyId: String = ""
)