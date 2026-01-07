package com.example.student_enrollment_app.user

import NotificationAdapter
import NotificationItem
import NotificationType
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.databinding.FragmentNotificationScreenBinding
import com.example.student_enrollment_app.utils.NotificationEventManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class NotificationScreenFragment : Fragment() {

    private var _binding: FragmentNotificationScreenBinding? = null
    private val binding get() = _binding!!

    private val notifications = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        listenForNotificationsFromFirestore()
        listenForLocalNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifications) { notificationItem ->
            // Optional: handle notification click
            Log.d("NotificationFragment", "Clicked: ${notificationItem.title}")
        }
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = adapter
    }

    /** Listen for notifications from Firestore in real-time */
    private fun listenForNotificationsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("NotificationFragment", "Error listening to notifications", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { docChange ->
                    val doc = docChange.document
                    val title = doc.getString("title") ?: "Notification"
                    val body = doc.getString("body") ?: ""
                    val type = doc.getString("type") ?: "info"
                    val timestamp = doc.getTimestamp("timestamp")

                    val notificationItem = NotificationItem(
                        id = doc.id,
                        title = title,
                        message = body,
                        timestamp = formatTimestamp(timestamp),
                        type = getNotificationType(type)
                    )

                    when (docChange.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            notifications.add(0, notificationItem)
                            adapter.notifyItemInserted(0)
                            binding.recyclerViewNotifications.scrollToPosition(0)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val index = notifications.indexOfFirst { it.id == doc.id }
                            if (index != -1) {
                                notifications[index] = notificationItem
                                adapter.notifyItemChanged(index)
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val index = notifications.indexOfFirst { it.id == doc.id }
                            if (index != -1) {
                                notifications.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }

                updateEmptyState()
            }
    }

    /** Listen for local notifications triggered inside the app */
    private fun listenForLocalNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationEventManager.notificationFlow.collect { event ->
                notifications.add(
                    0, NotificationItem(
                        id = System.currentTimeMillis().toString(),
                        title = event.title,
                        message = event.message,
                        timestamp = "Just now",
                        type = getNotificationType(event.type)
                    )
                )
                adapter.notifyItemInserted(0)
                binding.recyclerViewNotifications.scrollToPosition(0)
                updateEmptyState()
            }
        }
    }

    /** Format Firestore Timestamp to readable string */
    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return "Unknown"
        val now = System.currentTimeMillis()
        val diff = now - timestamp.toDate().time

        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    /** Map type string to NotificationType enum */
    private fun getNotificationType(type: String): NotificationType {
        return when (type.lowercase()) {
            "alert" -> NotificationType.ALERT
            "info" -> NotificationType.INFO
            "update" -> NotificationType.UPDATE
            "message" -> NotificationType.MESSAGE
            else -> NotificationType.INFO
        }
    }

    /** Show or hide empty state */
    private fun updateEmptyState() {
        binding.tvEmptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
