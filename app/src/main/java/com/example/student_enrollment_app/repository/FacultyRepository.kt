package com.example.student_enrollment_app.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FacultyRepository {

    private val db = FirebaseFirestore.getInstance()

    data class Faculty(
        val id: String,
        val name: String,
        val description: String = ""
    )

    suspend fun getAllFaculties(): List<Faculty> {
        return try {
            val result = db.collection("faculties").get().await()
            result.map { doc ->
                Faculty(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}