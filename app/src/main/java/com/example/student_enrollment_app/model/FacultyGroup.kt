package com.example.student_enrollment_app.model
data class FacultyGroup(
    val facultyId: String = "",
    val facultyName: String = "No Name Faculty",
    val departments: List<Department> = emptyList(),
)
