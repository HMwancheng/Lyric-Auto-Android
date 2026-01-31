package com.lyricauto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lyricauto.R
import com.lyricauto.model.MusicInfo
import com.lyricauto.utils.MusicMetadataExtractor
import kotlinx.coroutines.*

class MusicListenerService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "music_listener_channel"
        const val ACTION_MUSIC_CHANGED = "com.lyricauto.ACTION_MUSIC_CHANGED"
        const val EXTRA_MUSIC_INFO = "music_info"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var metadataExtractor: MusicMetadataExtractor? = null
    private var currentMusicInfo: MusicInfo? = null
    private var lastBroadcastTime: Long = 0
    private val broadcastDebounceDelay: Long = 1000
    private var checkMusicJob: Job? = null

    private val musicStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.android.music.metachanged",
                "com.android.music.playstatechanged",
                "com.htc.music.metachanged",
                "com.sec.android.app.music.metachanged",
                "com.miui.player.metachanged",
                "com.samsung.sec.android.MusicPlayer.metachanged" -> {
                    handleMusicMetadataChanged(intent)
                }
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> startMusicCheckLoop()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            metadataExtractor = MusicMetadataExtractor(this)
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            registerMusicStateReceiver()
            registerAudioFocusListener()
            startMusicCheckLoop()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            checkMusicJob?.cancel()
            serviceScope.cancel()
            unregisterMusicStateReceiver()
            metadataExtractor?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "音乐监听服务", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监听系统音乐播放状态"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮歌词")
            .setContentText("正在监听音乐播放")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun registerMusicStateReceiver() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction("com.android.music.metachanged")
                addAction("com.android.music.playstatechanged")
                addAction("com.htc.music.metachanged")
                addAction("com.sec.android.app.music.metachanged")
                addAction("com.miui.player.metachanged")
                addAction("com.samsung.sec.android.MusicPlayer.metachanged")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(musicStateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(musicStateReceiver, intentFilter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterMusicStateReceiver() {
        try {
            unregisterReceiver(musicStateReceiver)
        } catch (e: Exception) {}
    }

    private fun registerAudioFocusListener() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(
                    android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleMusicMetadataChanged(intent: Intent) {
        val title = intent.getStringExtra("track") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val album = intent.getStringExtra("album") ?: ""
        val isPlaying = intent.getBooleanExtra("playing", true)
        val duration = intent.getLongExtra("duration", 0)
        val position = intent.getLongExtra("position", 0)

        val musicInfo = MusicInfo(title, artist, album, duration, isPlaying, position)
        if (musicInfo.isValid() && musicInfo != currentMusicInfo) {
            currentMusicInfo = musicInfo
            broadcastMusicChanged(musicInfo)
        }
    }

    private fun startMusicCheckLoop() {
        checkMusicJob?.cancel()
        checkMusicJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val musicInfo = metadataExtractor?.getCurrentPlayingMusic()
                    if (musicInfo?.isValid() == true && musicInfo != currentMusicInfo) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBroadcastTime > broadcastDebounceDelay) {
                            currentMusicInfo = musicInfo
                            lastBroadcastTime = currentTime
                            withContext(Dispatchers.Main) {
                                broadcastMusicChanged(musicInfo)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(2000)
            }
        }
    }

    private fun broadcastMusicChanged(musicInfo: MusicInfo) {
        try {
            val intent = Intent(ACTION_MUSIC_CHANGED).apply {
                putExtra(EXTRA_MUSIC_INFO, musicInfo)
                `package` = packageName
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
