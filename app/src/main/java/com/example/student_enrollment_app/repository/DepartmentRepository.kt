package com.example.student_enrollment_app.repository

import com.example.student_enrollment_app.model.Department
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DepartmentRepository {

    private val db = FirebaseFirestore.getInstance()

    // Get all departments across all faculties
    suspend fun getAllDepartments(): List<Department> {
        return try {
            val result = db.collectionGroup("departments").get().await()
            result.map { doc ->
                Department(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    seats = doc.getLong("seats") ?: 0L,
                    seatsAvailable = doc.getLong("seatsAvailable") ?: 0L,
                    // Since all departments have colors, just get it directly
                    color = doc.getString("color") ?: "#2196F3", // Fallback just in case
                    facultyId = doc.reference.parent.parent?.id ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get departments by faculty ID
    suspend fun getDepartmentsByFaculty(facultyId: String): List<Department> {
        return try {
            val result = db.collection("faculties")
                .document(facultyId)
                .collection("departments")
                .get()
                .await()

            result.map { doc ->
                Department(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    seats = doc.getLong("seats") ?: 0L,
                    seatsAvailable = doc.getLong("seatsAvailable") ?: 0L,
                    // Since all departments have colors, just get it directly
                    color = doc.getString("color") ?: "#2196F3", // Fallback just in case
                    facultyId = facultyId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}