package com.arabi.ytdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var btnDownload: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.etUrl)
        btnDownload = findViewById(R.id.btnDownload)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        // طلب إذن الإشعارات (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        btnDownload.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال رابط", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startDownload(url)
        }
    }

    private fun startDownload(url: String) {
        btnDownload.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        tvStatus.text = "جاري التحميل..."

        val intent = Intent(this, DownloadService::class.java).apply {
            putExtra("url", url)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // استعادة الأزرار بعد بدء التحميل (المستخدم يمكنه إغلاق التطبيق)
        btnDownload.isEnabled = true
    }
}
