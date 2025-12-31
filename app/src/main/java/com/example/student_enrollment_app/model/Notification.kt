data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType = NotificationType.INFO // For icon type
)

enum class NotificationType {
    INFO, ALERT
}
