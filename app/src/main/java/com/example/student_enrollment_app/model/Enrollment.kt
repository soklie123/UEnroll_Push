package com.example.student_enrollment_app.model

data class Enrollment(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val faculty: String = "",
    val major: String = "",
    val idCardUrl: String = "",
    val photoUrl: String = "",
    val transcriptUrl: String = "",
    val timestamp: Long = 0L,
    val status: String = "Pending", // Pending, Approved, Rejected
    val confirmationNumber: String = "",
    val invoiceId: String = "" // Link to invoice document
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", "", "", 0L, "Pending", "", "")
}