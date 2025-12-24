package com.example.student_enrollment_app.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val profilePic: String = "",
    val enrolledDepartmentId: String? = null
)