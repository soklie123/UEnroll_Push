package com.example.student_enrollment_app.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.ActivityEnrollBinding
import com.example.student_enrollment_app.model.Enrollment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EnrollActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnrollBinding

    // Firebase Instances
    private val db = FirebaseFirestore.getInstance()

    private val storage = FirebaseStorage.getInstance().reference

    private var idCardUri: Uri? = null
    private var photoUri: Uri? = null

    private var currentUploadType = ""

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = android.graphics.Color.parseColor("#F8FAFC")

        setupDisplayData()
        setupDocumentLabels()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardIdUpload.root.setOnClickListener {
            currentUploadType = "ID"
            getContent.launch("image/*")
        }

        binding.cardPhotoUpload.root.setOnClickListener {
            currentUploadType = "PHOTO"
            getContent.launch("image/*")
        }

        binding.btnSubmitAnswer.setOnClickListener { validateAndUpload() }
    }

    private fun handleSelectedFile(uri: Uri) {
        when (currentUploadType) {
            "ID" -> {
                idCardUri = uri
                updateUploadUI(binding.cardIdUpload.root, "ID Selected")
            }
            "PHOTO" -> {
                photoUri = uri
                updateUploadUI(binding.cardPhotoUpload.root, "Photo Added")
            }
        }
    }

    private fun updateUploadUI(view: View, status: String) {
        val label = view.findViewById<TextView>(R.id.txtDocLabel)
        label.text = status
        label.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

        val icon = view.findViewById<ImageView>(R.id.imgIcon)
        icon.setImageResource(R.drawable.ic_check)
        icon.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
    }

    private fun validateAndUpload() {
        val fName = binding.edtFirstName.text.toString().trim()
        val lName = binding.edtLastName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()

        if (fName.isEmpty() || email.isEmpty()) {
            showToast("Please fill in your personal details")
            return
        }


        binding.btnSubmitAnswer.isEnabled = false
        binding.btnSubmitAnswer.text = "Saving to Database..."


        saveToFirestore(
            fName = fName,
            lName = lName,
            email = email,
            idUrl = "skipped_storage",
            transUrl = "skipped_storage",
            photoUrl = "skipped_storage"
        )
    }


    private fun uploadFile(uri: Uri, path: String, onSuccess: (String) -> Unit) {
        val fileRef = storage.child("enrollments/$path")

        fileRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                showToast("Upload failed: ${e.message}")
                binding.btnSubmitAnswer.isEnabled = true
            }
    }


    private fun saveToFirestore(fName: String, lName: String, email: String, idUrl: String?, transUrl: String?, photoUrl: String?) {
        val enrollment = Enrollment(
            firstName = fName,
            lastName = lName,
            email = email,
            faculty = intent.getStringExtra("FACULTY_NAME") ?: "Engineering",
            major = intent.getStringExtra("MAJOR_NAME") ?: "Data Science",
            idCardUrl = idUrl ?: "",
            transcriptUrl = transUrl ?: "",
            photoUrl = photoUrl ?: "",
            timestamp = System.currentTimeMillis()
        )

        db.collection("enrollments")
            .add(enrollment)
            .addOnSuccessListener {
                showToast("Enrollment Successful! Check Firestore now.")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error saving data: ${e.message}")
                binding.btnSubmitAnswer.isEnabled = true
                binding.btnSubmitAnswer.text = "Complete Application"
            }
    }

    private fun setupDisplayData() {
        val faculty = intent.getStringExtra("FACULTY_NAME") ?: "Engineering"
        val major = intent.getStringExtra("MAJOR_NAME") ?: "Data Science & Engineering"
        binding.txtSelectedFaculty.text = "Faculty of $faculty"
        binding.txtSelectedMajor.text = "Major: $major"
    }

    private fun setupDocumentLabels() {
        binding.cardIdUpload.root.findViewById<TextView>(R.id.txtDocLabel).text = "ID Card"
        binding.cardPhotoUpload.root.findViewById<TextView>(R.id.txtDocLabel).text = "Personal Photo"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}