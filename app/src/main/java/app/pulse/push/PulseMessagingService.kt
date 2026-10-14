package app.pulse.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PulseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        ensureChannel()
        val title = message.data["senderName"] ?: "Pulse"
        val preview = message.data["preview"] ?: "New message"
        val notification =
            NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(preview)
                .setAutoCancel(true)
                .build()
        getSystemService(NotificationManager::class.java).notify(message.data["chatId"].hashCode(), notification)
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("push", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    private companion object {
        const val CHANNEL_MESSAGES = "messages"
    }
}
