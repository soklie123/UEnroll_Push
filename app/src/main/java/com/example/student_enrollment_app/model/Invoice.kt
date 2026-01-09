package com.example.student_enrollment_app.model

import com.google.firebase.Timestamp

data class Invoice(
    val invoiceId: String = "",
    val enrollmentId: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val faculty: String = "",
    val department: String = "",
    val enrollmentDate: Timestamp = Timestamp.now(),
    val invoiceNumber: String = "", // e.g., "INV-2026-001234"
    val confirmationNumber: String = "", // e.g., "CONF-ABC123"
    val amount: Double = 0.0, // Enrollment fee (if any)
    val status: String = "Issued", // Issued, Paid, Cancelled
    val issueDate: Timestamp = Timestamp.now(),
    val notes: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", Timestamp.now(), "", "", 0.0, "Issued", Timestamp.now(), "")
}