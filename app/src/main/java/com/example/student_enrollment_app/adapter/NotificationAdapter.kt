import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.R

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imageView_notification_icon)
        val title: TextView = itemView.findViewById(R.id.textView_notification_title)
        val timestamp: TextView = itemView.findViewById(R.id.textView_notification_timestamp)
        val card: CardView = itemView.findViewById(R.id.card_notification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun getItemCount() = notifications.size

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.title.text = notification.title
        holder.timestamp.text = notification.timestamp

        holder.icon.setImageResource(
            when (notification.type) {
                NotificationType.INFO -> R.drawable.ic_notification_info
                NotificationType.ALERT -> R.drawable.ic_notification_alert
            }
        )

        holder.itemView.setOnClickListener { onClick(notification) }
    }
}
