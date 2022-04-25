package hr.ferit.kristinadudjak.whereami

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat


const val CHANNEL_ID = "1"
const val CHANNEL_NAME = "Channel"

@RequiresApi(api = Build.VERSION_CODES.O)
fun createNotificationChannel(name: String, description: String, importance: Int): NotificationChannel {
    val channel = NotificationChannel(CHANNEL_ID, name, importance)
    channel.description = description
    channel.setShowBadge(true)
    return channel
}
@RequiresApi(api = Build.VERSION_CODES.O)
fun createNotificationChannels(context: Context) {
    val channels = mutableListOf<NotificationChannel>()
    channels.add(createNotificationChannel(
        CHANNEL_NAME,
        "Info",
        NotificationManagerCompat.IMPORTANCE_DEFAULT
    ))
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannels(channels)
}

