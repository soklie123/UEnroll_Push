import com.example.student_enrollment_app.R

data class Notification(
    val title: String,
    val timestamp: String,
    val iconRes: Int = R.drawable.ic_notification_info
)
