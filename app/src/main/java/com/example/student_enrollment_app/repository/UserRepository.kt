package com.example.student_enrollment_app.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    data class User(
        val uid: String,
        val email: String? = null,
        val username: String? = null,
        val name: String? = null,
        val fullName: String? = null
    )

    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null

        return try {
            // Try to get user data from Firestore
            val doc = db.collection("users").document(currentUser.uid).get().await()

            User(
                uid = currentUser.uid,
                email = currentUser.email,
                username = doc.getString("username"),
                name = doc.getString("name"),
                fullName = doc.getString("fullName")
            )
        } catch (e: Exception) {
            // If Firestore fails, return basic user info
            User(
                uid = currentUser.uid,
                email = currentUser.email,
                username = currentUser.displayName
            )
        }
    }

    fun extractUsernameFromEmail(email: String?): String {
        if (email.isNullOrEmpty()) return "Student"

        return try {
            email.split("@").first()
        } catch (e: Exception) {
            "Student"
        }
    }
}