package com.example.student_enrollment_app.user

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentInvoiceDetailBinding
import com.example.student_enrollment_app.model.Invoice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class InvoiceDetailFragment : Fragment() {

    private var _binding: FragmentInvoiceDetailBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentInvoice: Invoice? = null

    companion object {
        private const val TAG = "InvoiceDetailFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadInvoice()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDownloadInvoice.setOnClickListener {
            currentInvoice?.let { generatePDF(it) }
        }
    }

    private fun loadInvoice() {
        val invoiceId = arguments?.getString("invoiceId")

        if (invoiceId.isNullOrEmpty()) {
            // Load latest invoice for current user
            loadLatestInvoice()
        } else {
            // Load specific invoice by ID
            loadInvoiceById(invoiceId)
        }
    }

    private fun loadLatestInvoice() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        db.collection("invoices")
            .whereEqualTo("userId", userId)
            .orderBy("issueDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                if (!documents.isEmpty) {
                    val invoice = documents.documents[0].toObject(Invoice::class.java)
                    invoice?.let { displayInvoice(it) }
                } else {
                    showToast("No invoice found")
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading invoice: ${e.message}", e)
                showToast("Failed to load invoice")
            }
    }

    private fun loadInvoiceById(invoiceId: String) {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("invoices")
            .document(invoiceId)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                val invoice = document.toObject(Invoice::class.java)
                invoice?.let { displayInvoice(it) }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading invoice: ${e.message}", e)
                showToast("Failed to load invoice")
            }
    }

    private fun displayInvoice(invoice: Invoice) {
        currentInvoice = invoice

        // Display invoice details
        binding.tvInvoiceNumber.text = invoice.invoiceNumber
        binding.tvConfirmationNumber.text = invoice.confirmationNumber
        binding.tvStudentName.text = invoice.studentName
        binding.tvStudentEmail.text = invoice.studentEmail
        binding.tvFaculty.text = invoice.faculty
        binding.tvDepartment.text = invoice.department

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.tvIssueDate.text = dateFormat.format(invoice.issueDate.toDate())
        binding.tvEnrollmentDate.text = dateFormat.format(invoice.enrollmentDate.toDate())

        binding.tvStatus.text = invoice.status

        // Set status color
        val statusColor = when (invoice.status) {
            //"Issued" -> R.color.status_pending
            "Paid" -> R.color.success_green
            //"Cancelled" -> R.color.status_rejected
            else -> R.color.text_secondary
        }
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

        if (invoice.notes.isNotEmpty()) {
            binding.tvNotes.text = invoice.notes
            binding.tvNotes.visibility = View.VISIBLE
        }
    }

    private fun generatePDF(invoice: Invoice) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas
            val paint = Paint()

            // Title
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("ENROLLMENT INVOICE", 50f, 80f, paint)

            // Invoice details
            paint.textSize = 14f
            paint.isFakeBoldText = false

            var yPos = 140f
            canvas.drawText("Invoice Number: ${invoice.invoiceNumber}", 50f, yPos, paint)
            yPos += 30f
            canvas.drawText("Confirmation: ${invoice.confirmationNumber}", 50f, yPos, paint)
            yPos += 30f
            canvas.drawText("Status: ${invoice.status}", 50f, yPos, paint)

            yPos += 50f
            paint.isFakeBoldText = true
            canvas.drawText("Student Information", 50f, yPos, paint)
            paint.isFakeBoldText = false

            yPos += 30f
            canvas.drawText("Name: ${invoice.studentName}", 50f, yPos, paint)
            yPos += 25f
            canvas.drawText("Email: ${invoice.studentEmail}", 50f, yPos, paint)

            yPos += 50f
            paint.isFakeBoldText = true
            canvas.drawText("Enrollment Details", 50f, yPos, paint)
            paint.isFakeBoldText = false

            yPos += 30f
            canvas.drawText("Faculty: ${invoice.faculty}", 50f, yPos, paint)
            yPos += 25f
            canvas.drawText("Department: ${invoice.department}", 50f, yPos, paint)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            yPos += 25f
            canvas.drawText("Enrollment Date: ${dateFormat.format(invoice.enrollmentDate.toDate())}", 50f, yPos, paint)
            yPos += 25f
            canvas.drawText("Issue Date: ${dateFormat.format(invoice.issueDate.toDate())}", 50f, yPos, paint)

            pdfDocument.finishPage(page)

            // Save PDF
            val fileName = "Invoice_${invoice.invoiceNumber}.pdf"
            val file = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            showToast("Invoice downloaded: $fileName")
            Log.d(TAG, "PDF saved at: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}", e)
            showToast("Failed to generate PDF")
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