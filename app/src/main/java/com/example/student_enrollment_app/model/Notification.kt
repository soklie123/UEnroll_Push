
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType = NotificationType.INFO,
    val invoiceId: String? = null
)

enum class NotificationType {
    INFO,
    ALERT,
    UPDATE,
    MESSAGE
}
