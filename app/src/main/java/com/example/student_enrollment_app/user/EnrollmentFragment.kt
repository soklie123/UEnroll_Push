package com.example.student_enrollment_app.user

import android.net.Uri
import android.os.Bundle

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentEnrollmentBinding // IMPORTANT: Name changed
import com.example.student_enrollment_app.model.Enrollment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EnrollmentFragment : Fragment() {

    // Use FragmentEnrollmentBinding, which is generated from fragment_enrollment.xml
    private var _binding: FragmentEnrollmentBinding? = null
    private val binding get() = _binding!!


    // Firebase instances
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    // State variables
    private var idCardUri: Uri? = null
    private var photoUri: Uri? = null
    private var currentUploadType = ""

    // Activity Result Launcher for picking images
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup all UI interactions
        setupDisplayData()
        setupClickListeners()
        setupDocumentLabels()
    }

    private fun setupClickListeners() {
        // Use findNavController to go back
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

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
                updateUploadUI(binding.cardIdUpload, "ID Card Selected")
            }
            "PHOTO" -> {
                photoUri = uri
                updateUploadUI(binding.cardPhotoUpload, "Photo Added")
            }
        }
    }

    // This function correctly uses the binding object for the included layout
    private fun updateUploadUI(uploadItemBinding: com.example.student_enrollment_app.databinding.ViewUploadItemSmallBinding, status: String) {
        uploadItemBinding.txtDocLabel.text = status
        // Use requireContext() to get a color safely
        uploadItemBinding.txtDocLabel.setTextColor(requireContext().getColor(R.color.success_green))
        uploadItemBinding.imgIcon.setImageResource(R.drawable.ic_check)
        uploadItemBinding.imgIcon.setColorFilter(requireContext().getColor(R.color.success_green))
    }

    private fun setupDocumentLabels() {
        binding.cardIdUpload.txtDocLabel.text = "ID Card"
        binding.cardPhotoUpload.txtDocLabel.text = "Personal Photo"
    }

    private fun setupDisplayData() {
        // Get arguments passed from the previous fragment
        val facultyName = arguments?.getString("facultyName") ?: "Faculty"
        val departmentName = arguments?.getString("departmentName") ?: "Department"

        binding.txtSelectedFaculty.text = "Faculty of $facultyName"
        binding.txtSelectedMajor.text = "Major: $departmentName"
    }

    private fun validateAndUpload() {
        val fName = binding.edtFirstName.text.toString().trim()
        val lName = binding.edtLastName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty()) {
            showToast("Please fill in all personal details")
            return
        }
        if (idCardUri == null || photoUri == null) {
            showToast("Please upload both documents")
            return
        }

        binding.btnSubmitAnswer.isEnabled = false
        binding.btnSubmitAnswer.text = "Saving..."

        // You can add your file upload logic here before saving to Firestore
        saveToFirestore(fName, lName, email, "placeholder", "placeholder")
    }

    private fun saveToFirestore(fName: String, lName: String, email: String, idUrl: String?, photoUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val facultyName = arguments?.getString("facultyName") ?: "Unknown"
        val departmentName = arguments?.getString("departmentName") ?: "Unknown"

        val enrollment = Enrollment(
            userId = userId, firstName = fName, lastName = lName, email = email,
            faculty = facultyName, major = departmentName,
            idCardUrl = idUrl ?: "", photoUrl = photoUrl ?: "",
            transcriptUrl = "", timestamp = System.currentTimeMillis(),
            status = "Pending",
            confirmationNumber = ""
        )

        db.collection("enrollments").add(enrollment)
            .addOnSuccessListener { docRef ->
                Log.d("EnrollmentFragment", "Enrollment successful")
                showToast("Enrollment Successful!")
                // Navigate back to the previous screen on success
                findNavController().popBackStack()
                listenForStatus(docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e("EnrollmentFragment", "Error saving enrollment", e)
                showToast("Error: ${e.message}")
                binding.btnSubmitAnswer.isEnabled = true
                binding.btnSubmitAnswer.text = "Complete Application"
            }
    }
    private fun listenForStatus(enrollmentId: String) {
        // Listen for changes in the enrollment document
        db.collection("enrollments").document(enrollmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EnrollmentFragment", "Listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "Pending"
                    val confirmationNumber = snapshot.getString("confirmationNumber") ?: ""

                    when (status) {
                        "Pending" -> {
                            // Optional: show text in UI
                            Toast.makeText(requireContext(), "Enrollment is still pending approval", Toast.LENGTH_SHORT).show()
                        }
                        "Confirmed" -> {
                            // Student sees confirmation immediately
                            Toast.makeText(requireContext(), "Enrollment confirmed! Number: $confirmationNumber", Toast.LENGTH_LONG).show()
                            // Optional: you can also update a TextView in your layout
                            binding.txtStatus.text = "Enrollment confirmed! Number: $confirmationNumber"
                        }
                    }
                }
            }
    }
    private fun showToast(message: String) {
        // Use requireContext() in a Fragment
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding to prevent memory leaks
        _binding = null
    }
}
