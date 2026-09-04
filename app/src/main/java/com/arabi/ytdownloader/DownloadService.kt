package com.arabi.ytdownloader

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY

        startForeground(YtDlpApplication.NOTIFICATION_ID, createNotification("بدء التحميل...", 0))

        serviceScope.launch {
            try {
                downloadVideo(url)
            } catch (e: Exception) {
                updateNotification("فشل التحميل: ${e.message}", 0)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun downloadVideo(url: String) {
        val request = YoutubeDLRequest(url).apply {
            addOption("-o", "${filesDir.absolutePath}/%(title)s.%(ext)s")
            addOption("-f", "best")
            addOption("--no-playlist")
        }

        // ✅ التصحيح: استخدام 3 معاملات (progress, eta, line)
        val response = YoutubeDL.getInstance().execute(request) { progress, eta, line ->
            val percent = progress.toInt()
            updateNotification("تحميل: $percent%", percent)
        }

        val outputFile = File(filesDir, response.out)
        if (outputFile.exists()) {
            val saved = MediaStoreHelper.saveVideoToGallery(this, outputFile)
            if (saved) {
                updateNotification("✅ تم التحميل وحفظه في المعرض", 100)
            } else {
                updateNotification("❌ فشل حفظ الفيديو", 0)
            }
        }

        // إيقاف الخدمة بعد 5 ثواني
        Thread.sleep(5000)
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification(text: String, progress: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(YtDlpApplication.NOTIFICATION_ID, createNotification(text, progress))
    }

    private fun createNotification(text: String, progress: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, YtDlpApplication.CHANNEL_ID)
            .setContentTitle("📥 تحميل فيديو")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext.cancel()
    }
}
