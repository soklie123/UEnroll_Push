import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.R

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconBackground: View = itemView.findViewById(R.id.view_icon_background)
        val icon: ImageView = itemView.findViewById(R.id.imageView_notification_icon)
        val title: TextView = itemView.findViewById(R.id.textView_notification_title)
        val message: TextView = itemView.findViewById(R.id.textView_notification_message)
        val timestamp: TextView = itemView.findViewById(R.id.textView_notification_timestamp)
        val unreadIndicator: View = itemView.findViewById(R.id.view_unread_indicator)
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
        val context = holder.itemView.context

        holder.title.text = notification.title
        holder.timestamp.text = notification.timestamp

        // Show/hide message
        if (notification.message.isNotEmpty()) {
            holder.message.text = notification.message
            holder.message.visibility = View.VISIBLE
        } else {
            holder.message.visibility = View.GONE
        }

        // Set icon and colors based on type
        when (notification.type) {
            NotificationType.INFO -> {
                holder.icon.setImageResource(R.drawable.ic_notification_info)
                holder.iconBackground.setBackgroundResource(R.drawable.bg_notification_icon_info)
            }
            NotificationType.ALERT -> {
                holder.icon.setImageResource(R.drawable.ic_notification_alert)
                holder.iconBackground.setBackgroundResource(R.drawable.bg_notification_icon_alert)
            }
            NotificationType.UPDATE -> {
                holder.icon.setImageResource(R.drawable.ic_notification_info)
                holder.iconBackground.setBackgroundResource(R.drawable.bg_notification_icon_update)
            }
            NotificationType.MESSAGE -> {
                holder.icon.setImageResource(R.drawable.ic_notification_info)
                holder.iconBackground.setBackgroundResource(R.drawable.bg_notification_icon_message)
            }
        }

        // You can add unread indicator logic here
        // holder.unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(notification) }
    }
}