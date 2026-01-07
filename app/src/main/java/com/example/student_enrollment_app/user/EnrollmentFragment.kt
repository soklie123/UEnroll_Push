package com.example.student_enrollment_app.user

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentEnrollmentBinding
import com.example.student_enrollment_app.model.Enrollment
import com.example.student_enrollment_app.utils.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EnrollmentFragment : Fragment() {
    private var _binding: FragmentEnrollmentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private var idCardUri: Uri? = null
    private var photoUri: Uri? = null
    private var currentUploadType = ""

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()

        NotificationHelper.createNotificationChannel(requireContext())

        setupDisplayData()
        setupClickListeners()
        setupDocumentLabels()
    }
    private fun setupEdgeToEdge() {
        activity?.window?.let { window ->
            // Tell system we will handle insets ourselves
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Transparent status bar
            window.statusBarColor = Color.TRANSPARENT

            // Transparent navigation bar + light icons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = Color.parseColor("#F2FFFFFF")
                WindowCompat.getInsetsController(window, window.decorView)
                    ?.isAppearanceLightNavigationBars = true
            }
        }
    }


    private fun setupClickListeners() {
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

    private fun updateUploadUI(uploadItemBinding: com.example.student_enrollment_app.databinding.ViewUploadItemSmallBinding, status: String) {
        uploadItemBinding.txtDocLabel.text = status
        uploadItemBinding.txtDocLabel.setTextColor(requireContext().getColor(R.color.success_green))
        uploadItemBinding.imgIcon.setImageResource(R.drawable.ic_check)
        uploadItemBinding.imgIcon.setColorFilter(requireContext().getColor(R.color.success_green))
    }

    private fun setupDocumentLabels() {
        binding.cardIdUpload.txtDocLabel.text = "ID Card"
        binding.cardPhotoUpload.txtDocLabel.text = "Personal Photo"
    }

    private fun setupDisplayData() {
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

                val title = "Enrollment Submitted Successfully! ✅"
                val body = "Your enrollment for $facultyName - $departmentName has been submitted and is pending approval."

                // ✅ 1. Save notification to Firestore (so it appears in NotificationScreenFragment)
                saveNotificationToFirestore(title, body, "info")

                // ✅ 2. Show system notification
                NotificationHelper.showEnrollmentNotification(
                    requireContext(),
                    title,
                    body,
                    docRef.id
                )

                showToast("Enrollment Successful!")
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e("EnrollmentFragment", "Error saving enrollment", e)
                showToast("Error: ${e.message}")
                binding.btnSubmitAnswer.isEnabled = true
                binding.btnSubmitAnswer.text = "Complete Application"
            }
    }

    // ✅ Save notification to Firestore so it appears in NotificationScreenFragment
    private fun saveNotificationToFirestore(title: String, body: String, type: String) {
        val userId = auth.currentUser?.uid ?: return

        val notificationData = hashMapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("EnrollmentFragment", "Notification saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("EnrollmentFragment", "Error saving notification", e)
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}