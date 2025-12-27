package com.example.student_enrollment_app.model

// Use "data class" and ensure it is in a .kt file

data class FacultyGroup(
    val facultyId: String = "",   // Changed from 'id'
    val facultyName: String = "No Name Faculty", // Changed from 'name'
    val departments: List<Department> = emptyList()
)
