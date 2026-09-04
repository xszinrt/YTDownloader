package com.arabi.ytdownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YtDlpApplication : Application() {

    companion object {
        const val CHANNEL_ID = "download_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@YtDlpApplication)
                YoutubeDL.getInstance().updateYoutubeDL(this@YtDlpApplication)
            } catch (e: YoutubeDLException) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميل فيديو",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم تحميل الفيديو"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
