package com.lyricauto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lyricauto.R
import com.lyricauto.model.Lyric
import com.lyricauto.network.LyricDownloader
import com.lyricauto.utils.LyricCacheManager
import com.lyricauto.utils.SharedPreferencesManager
import kotlinx.coroutines.*

class LyricDownloadService : Service() {

    companion object {
        const val ACTION_LYRIC_DOWNLOADED = "com.lyricauto.ACTION_LYRIC_DOWNLOADED"
        const val EXTRA_LYRIC = "lyric"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "lyric_download_channel"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cacheManager: LyricCacheManager
    private lateinit var prefsManager: SharedPreferencesManager
    private val lyricDownloader = LyricDownloader()

    override fun onCreate() {
        super.onCreate()
        cacheManager = LyricCacheManager(this)
        prefsManager = SharedPreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: ""
        val artist = intent?.getStringExtra("artist") ?: ""

        if (title.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, createNotification(title, artist))
            }
            downloadLyric(title, artist)
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "歌词下载服务", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "下载歌词文件"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, artist: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在下载歌词")
            .setContentText("$title - $artist")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun downloadLyric(title: String, artist: String) {
        serviceScope.launch {
            try {
                val settings = prefsManager.getFloatWindowSettings()

                if (settings.enableCache) {
                    val cachedLyric = cacheManager.getLyric(title, artist)
                    if (cachedLyric != null) {
                        broadcastLyricDownloaded(cachedLyric, title, artist)
                        stopSelf()
                        return@launch
                    }
                }

                val lyric = lyricDownloader.searchNeteaseLyric(title, artist)
                if (lyric != null && !lyric.isEmpty()) {
                    if (settings.enableCache) {
                        cacheManager.saveLyric(title, artist, lyric)
                    }
                    broadcastLyricDownloaded(lyric, title, artist)
                } else {
                    broadcastLyricDownloaded(Lyric(), title, artist)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                broadcastLyricDownloaded(Lyric(), title, artist)
            } finally {
                stopSelf()
            }
        }
    }

    private fun broadcastLyricDownloaded(lyric: Lyric, title: String, artist: String) {
        try {
            val intent = Intent(ACTION_LYRIC_DOWNLOADED).apply {
                putExtra(EXTRA_LYRIC, lyric)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                `package` = packageName
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
