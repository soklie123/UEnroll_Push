package com.example.student_enrollment_app.repository

import com.example.student_enrollment_app.model.Department
import com.example.student_enrollment_app.model.FacultyGroup
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DepartmentRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getGroupedDepartments(facultyRepository: FacultyRepository): List<FacultyGroup> {
        return try {
            val faculties = facultyRepository.getAllFaculties()
            faculties.map { faculty ->
                val depts = getDepartmentsByFaculty(faculty.id)
                FacultyGroup(
                    facultyId = faculty.id,
                    facultyName = faculty.name ?: "Unknown Faculty",
                    departments = depts
                )
            } // Removed .filter to ensure new faculties show up even if empty initially
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun getDepartmentsByFaculty(facultyId: String): List<Department> {
        return try {
            val result = db.collection("faculties")
                .document(facultyId)
                .collection("departments")
                .get()
                .await()

            result.map { doc ->
                // Use Document ID if the name field is empty in Firebase
                val nameField = doc.getString("name")
                val name = if (!nameField.isNullOrEmpty()) nameField else doc.id

                Department(
                    id = doc.id,
                    name = name,
                    seats = doc.getLong("seats") ?: 0L,
                    seatsAvailable = doc.getLong("seatsAvailable") ?: 0L,
                    color = doc.getString("color").takeIf { !it.isNullOrEmpty() } ?: "#2196F3",
                    // ADD THIS LINE: This pulls the image link you added in Firestore
                    logoUrl = doc.getString("logoUrl") ?: "",
                    facultyId = facultyId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}