package com.example.student_enrollment_app.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",

    val enrolledDepartmentId: String? = null,
    val profileImageUrl: String? = null // Only this for profile picture
)
