package com.example.student_enrollment_app.user

import NotificationAdapter
import NotificationItem
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.student_enrollment_app.databinding.FragmentNotificationScreenBinding

class NotificationScreenFragment : Fragment() {

    private var _binding: FragmentNotificationScreenBinding? = null
    private val binding get() = _binding!!

    private val notifications = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    // BroadcastReceiver to handle in-app notifications
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.getStringExtra("title") ?: return
            val message = intent.getStringExtra("message") ?: return

            // Add new notification
            notifications.add(
                NotificationItem(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    message = message,
                    timestamp = "Just now",
                    type = NotificationType.ALERT
                )
            )
            adapter.notifyItemInserted(notifications.size - 1)
            binding.recyclerViewNotifications.scrollToPosition(notifications.size - 1)

            // Optional: show Toast
            Toast.makeText(context, "$title: $message", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter
        adapter = NotificationAdapter(notifications) { notification ->
            // Handle click on notification if needed
        }

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = adapter

        adapter.notifyItemInserted(notifications.size - 1)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(notificationReceiver, IntentFilter("NEW_NOTIFICATION"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(notificationReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
