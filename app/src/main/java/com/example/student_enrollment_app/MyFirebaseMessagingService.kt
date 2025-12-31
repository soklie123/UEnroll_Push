package com.example.student_enrollment_app.user

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.student_enrollment_app.HomeActivity
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.ui.EnrollActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "student_enrollment_channel"
        private const val CHANNEL_NAME = "Student Enrollment Notifications"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")


        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message from: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification title: ${it.title}")
            Log.d(TAG, "Notification body: ${it.body}")

            showNotification(
                it.title ?: "Student Enrollment",
                it.body ?: "",
                remoteMessage.data
            )
        }

        // If only data payload (no notification), show notification manually
        if (remoteMessage.notification == null && remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Student Enrollment"
            val body = remoteMessage.data["body"] ?: "You have a new update"
            showNotification(title, body, remoteMessage.data)
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for student enrollment updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent based on notification type
        val intent = createIntentFromData(data)
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createIntentFromData(data: Map<String, String>): Intent {
        // Route to appropriate screen based on notification type
        val intent = when (data["type"]) {
            "enrollment" -> {
                Intent(this, EnrollActivity::class.java).apply {
                    putExtra("enrollmentId", data["enrollmentId"])
                }
            }
            "status" -> {
                Intent(this, HomeActivity::class.java).apply {
                    putExtra("openFragment", "status")
                    putExtra("statusId", data["statusId"])
                }
            }
            "notification" -> {
                Intent(this, HomeActivity::class.java).apply {
                    putExtra("openFragment", "notifications")
                }
            }
            else -> Intent(this, HomeActivity::class.java)
        }

        data.forEach { (key, value) ->
            if (key != "type") {
                intent.putExtra(key, value)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return intent
    }

    private fun handleDataPayload(data: Map<String, String>) {
        // Handle background data processing
        when (data["type"]) {
            "enrollment_update" -> {
                Log.d(TAG, "Handling enrollment update")

            }
            "status_change" -> {
                Log.d(TAG, "Handling status change")

            }
            "new_department" -> {
                Log.d(TAG, "New department available")
            }
        }
    }

    private fun sendTokenToServer(token: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            // Save token to Firestore
            val userTokenData = hashMapOf(
                "fcmToken" to token,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(userId)
                .update(userTokenData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token saved to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving FCM token to Firestore", e)

                    // If document doesn't exist, set it
                    firestore.collection("users")
                        .document(userId)
                        .set(userTokenData)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM Token created in Firestore successfully")
                        }
                        .addOnFailureListener { error ->
                            Log.e(TAG, "Error creating FCM token in Firestore", error)
                        }
                }
        } else {
            Log.w(TAG, "Cannot save token: User not authenticated")
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}