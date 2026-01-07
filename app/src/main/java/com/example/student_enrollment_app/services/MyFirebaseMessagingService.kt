package com.example.student_enrollment_app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.student_enrollment_app.HomeActivity
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.utils.NotificationEventManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Student Enrollment"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new update"
        val type = remoteMessage.data["type"] ?: "notification"

        saveNotificationToFirestore(title, body, type)
        broadcastInAppNotification(title, body, type)
        showNotification(title, body)
    }

    private fun broadcastInAppNotification(title: String, body: String, type: String) {
        // Use SharedFlow instead of LocalBroadcastManager
        CoroutineScope(Dispatchers.Main).launch {
            NotificationEventManager.sendNotification(title, body, type)
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications for student enrollment updates"
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("openFragment", "notifications")

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
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveNotificationToFirestore(title: String, body: String, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val notificationData = hashMapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "read" to false
        )
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d(TAG, "Notification saved to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving notification to Firestore", e)
            }
    }

    private fun sendTokenToServer(token: String) {
        val userId = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "fcmToken" to token,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("users")
            .document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating FCM token", e)
            }
    }
}