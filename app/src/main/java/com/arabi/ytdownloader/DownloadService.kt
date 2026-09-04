package com.arabi.ytdownloader

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import java.io.File

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    companion object {
        const val ACTION_DOWNLOAD = "DOWNLOAD"
        const val EXTRA_URL = "url"
        const val EXTRA_QUALITY = "quality"
        const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DOWNLOAD) {
            val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
            val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "best"
            startDownload(url, quality)
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String, quality: String) {
        startForeground(NOTIFICATION_ID, createNotification("جاري التحميل...", 0))

        downloadJob = serviceScope.launch {
            try {
                val outputDir = File(filesDir, "downloads").apply { mkdirs() }
                val request = YoutubeDLRequest(url).apply {
                    addOption("-f", quality)
                    addOption("-o", "${outputDir.absolutePath}/%(title)s.%(ext)s")
                    addOption("--no-playlist")
                }

                val response = YoutubeDL.getInstance().execute(request) { progress, _ ->
                    updateNotification(progress)
                }

                val outputFile = outputDir.listFiles()?.maxByOrNull { it.lastModified() }
                if (outputFile != null && outputFile.exists()) {
                    saveToGallery(outputFile)
                }

                stopForeground(true)
                stopSelf()

            } catch (e: Exception) {
                e.printStackTrace()
                val notification = createNotification("فشل التحميل: ${e.message}", 0)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun saveToGallery(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/YTDownloader")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(it, values, null, null)
            }
            file.delete()
        } else {
            // Android 9 and below
            @Suppress("DEPRECATION")
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        }

        showCompletionNotification()
    }

    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(this, YtDlpApplication.CHANNEL_ID)
            .setContentTitle("✅ تم التحميل")
            .setContentText("تم حفظ الفيديو في المعرض")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotification(text: String, progress: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, YtDlpApplication.CHANNEL_ID)
            .setContentTitle("📥 تحميل فيديو")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun updateNotification(progress: Float) {
        val percent = progress.toInt()
        val notification = createNotification("جاري التحميل... $percent%", percent)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel()
        serviceScope.cancel()
    }
}
