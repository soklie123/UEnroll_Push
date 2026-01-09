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
import com.example.student_enrollment_app.databinding.FragmentEnrollmentBinding
import com.example.student_enrollment_app.databinding.ViewUploadItemSmallBinding
import com.example.student_enrollment_app.model.Enrollment
import com.example.student_enrollment_app.model.Invoice
import com.example.student_enrollment_app.utils.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class EnrollmentFragment : Fragment() {

    private var _binding: FragmentEnrollmentBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    private var idCardUri: Uri? = null
    private var photoUri: Uri? = null

    private var currentUploadType = ""

    companion object {
        private const val TAG = "EnrollmentFragment"
        private const val UPLOAD_ID = "ID"
        private const val UPLOAD_PHOTO = "PHOTO"
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSelectedFile(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnrollmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NotificationHelper.createNotificationChannel(requireContext())

        setupDisplayData()
        setupDocumentLabels()
        setupClickListeners()
    }

    // ---------------- CLICK LISTENERS ----------------

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardIdUpload.root.setOnClickListener {
            currentUploadType = UPLOAD_ID
            getContent.launch("image/*")
        }

        binding.cardPhotoUpload.root.setOnClickListener {
            currentUploadType = UPLOAD_PHOTO
            getContent.launch("image/*")
        }

        binding.btnSubmitAnswer.setOnClickListener {
            validateAndUpload()
        }
    }

    // ---------------- DOCUMENT HANDLING ----------------

    private fun handleSelectedFile(uri: Uri) {
        when (currentUploadType) {
            UPLOAD_ID -> {
                idCardUri = uri
                updateUploadUI(binding.cardIdUpload, "ID Card Selected")
            }
            UPLOAD_PHOTO -> {
                photoUri = uri
                updateUploadUI(binding.cardPhotoUpload, "Photo Added")
            }
        }
    }

    private fun updateUploadUI(
        uploadItem: ViewUploadItemSmallBinding,
        status: String
    ) {
        uploadItem.txtDocLabel.text = status
        uploadItem.txtDocLabel.setTextColor(
            requireContext().getColor(R.color.success_green)
        )

        uploadItem.imgIcon.setImageResource(R.drawable.ic_check)
        uploadItem.imgIcon.setColorFilter(
            requireContext().getColor(R.color.success_green)
        )
    }

    private fun setupDocumentLabels() {
        binding.cardIdUpload.txtDocLabel.text = "ID Card"
        binding.cardIdUpload.img.setImageResource(R.drawable.ic_id_card)

        binding.cardPhotoUpload.txtDocLabel.text = "Personal Photo"
        binding.cardPhotoUpload.img.setImageResource(R.drawable.ic_photo)
    }

    // ---------------- DISPLAY DATA ----------------

    private fun setupDisplayData() {
        val args = requireArguments()
        val facultyName = args.getString("facultyName")
            ?: throw IllegalStateException("facultyName missing")
        val departmentName = args.getString("departmentName")
            ?: throw IllegalStateException("departmentName missing")

        binding.txtSelectedFaculty.text = "Faculty of $facultyName"
        binding.txtSelectedMajor.text = "Major: $departmentName"
    }

    // ---------------- VALIDATION ----------------

    private fun validateAndUpload() {
        val fName = binding.edtFirstName.text.toString().trim()
        val lName = binding.edtLastName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()

        when {
            fName.isEmpty() || lName.isEmpty() || email.isEmpty() ->
                showToast("Please fill in all personal details")

            idCardUri == null || photoUri == null ->
                showToast("Please upload both documents")

            else -> {
                binding.btnSubmitAnswer.isEnabled = false
                binding.btnSubmitAnswer.text = "Saving..."
                saveToFirestore(fName, lName, email)
            }
        }
    }

    // ---------------- FIRESTORE ----------------

    private fun saveToFirestore(
        fName: String,
        lName: String,
        email: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        val args = requireArguments()

        val facultyName = args.getString("facultyName")!!
        val departmentName = args.getString("departmentName")!!

        // Generate unique confirmation number
        val confirmationNumber = generateConfirmationNumber()

        val enrollment = Enrollment(
            userId = userId,
            firstName = fName,
            lastName = lName,
            email = email,
            faculty = facultyName,
            major = departmentName,
            idCardUrl = "",
            photoUrl = "",
            transcriptUrl = "",
            timestamp = System.currentTimeMillis(),
            status = "Pending",
            confirmationNumber = confirmationNumber
        )

        db.collection("enrollments")
            .add(enrollment)
            .addOnSuccessListener { docRef ->
                // Generate invoice after enrollment is saved
                generateInvoice(
                    enrollmentId = docRef.id,
                    userId = userId,
                    studentName = "$fName $lName",
                    studentEmail = email,
                    faculty = facultyName,
                    department = departmentName,
                    confirmationNumber = confirmationNumber
                )
            }
            .addOnFailureListener { e ->
                handleError(e)
            }
    }

    // ---------------- INVOICE GENERATION ----------------

    private fun generateInvoice(
        enrollmentId: String,
        userId: String,
        studentName: String,
        studentEmail: String,
        faculty: String,
        department: String,
        confirmationNumber: String
    ) {
        // Generate unique invoice number
        val invoiceNumber = generateInvoiceNumber()

        val invoice = Invoice(
            invoiceId = "", // Will be set by Firestore
            enrollmentId = enrollmentId,
            userId = userId,
            studentName = studentName,
            studentEmail = studentEmail,
            faculty = faculty,
            department = department,
            enrollmentDate = Timestamp.now(),
            invoiceNumber = invoiceNumber,
            confirmationNumber = confirmationNumber,
            amount = 0.0, // Set to 0 or actual enrollment fee
            status = "Issued",
            issueDate = Timestamp.now(),
            notes = "Enrollment confirmation for $department in Faculty of $faculty"
        )

        // Save invoice to Firestore
        db.collection("invoices")
            .add(invoice)
            .addOnSuccessListener { invoiceDocRef ->
                Log.d(TAG, "Invoice created: ${invoiceDocRef.id}")

                // Update enrollment with invoice reference
                db.collection("enrollments")
                    .document(enrollmentId)
                    .update("invoiceId", invoiceDocRef.id)

                // Now update user profile and complete the process
                updateUserDepartment(faculty, department, enrollmentId, invoiceNumber, confirmationNumber)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create invoice: ${e.message}", e)
                // Continue with enrollment even if invoice fails
                updateUserDepartment(faculty, department, enrollmentId, "", confirmationNumber)
            }
    }

    private fun generateInvoiceNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (10000..99999).random()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        return "INV-$year-$random"
    }

    private fun generateConfirmationNumber(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return "CONF-${(1..8).map { chars.random() }.joinToString("")}"
    }

    // ---------------- USER PROFILE UPDATE ----------------

    private fun updateUserDepartment(
        faculty: String,
        department: String,
        enrollmentDocId: String,
        invoiceNumber: String,
        confirmationNumber: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        Log.d(TAG, "Updating user department to: $department")

        // Update the user's profile with the enrolled department
        val userUpdate = hashMapOf(
            "enrolledDepartmentId" to department,
            "enrolledFaculty" to faculty,
            "enrollmentTimestamp" to Timestamp.now(),
            "confirmationNumber" to confirmationNumber,
            "invoiceNumber" to invoiceNumber
        )

        db.collection("users")
            .document(userId)
            .set(userUpdate, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User department updated successfully")
                handleSuccess(faculty, department, enrollmentDocId, invoiceNumber, confirmationNumber)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update user department: ${e.message}", e)
                handleSuccess(faculty, department, enrollmentDocId, invoiceNumber, confirmationNumber)
            }
    }

    private fun handleSuccess(
        faculty: String,
        department: String,
        docId: String,
        invoiceNumber: String,
        confirmationNumber: String
    ) {
        val title = "Enrollment Successful!"
        val body = """
            Your enrollment has been confirmed!
            
            Department: $department
            Faculty: $faculty
            Confirmation: $confirmationNumber
            Invoice: $invoiceNumber
            
            Your invoice has been generated and is available in your notifications.
        """.trimIndent()

        saveNotificationToFirestore(title, body, "success")

        NotificationHelper.showEnrollmentNotification(
            requireContext(),
            title,
            "Confirmation #$confirmationNumber - Invoice #$invoiceNumber",
            docId
        )

        showToast("Enrollment Successful! Viewing your invoice...")

        // Navigate to invoice detail screen
        val bundle = Bundle().apply {
            putString("invoiceId", null) // null = load latest invoice for user
        }
        findNavController().navigate(
            R.id.action_enrollment_to_invoice,
            bundle
        )
    }

    private fun handleError(e: Exception) {
        Log.e(TAG, "Error saving enrollment", e)
        showToast("Error: ${e.message}")
        binding.btnSubmitAnswer.isEnabled = true
        binding.btnSubmitAnswer.text = "Complete Application"
    }

    // ---------------- NOTIFICATION ----------------

    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        type: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        val data = hashMapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .add(data)
    }

    // ---------------- UTIL ----------------

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}