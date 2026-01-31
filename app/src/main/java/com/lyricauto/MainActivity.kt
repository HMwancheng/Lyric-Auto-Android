package com.lyricauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lyricauto.databinding.ActivityMainBinding
import com.lyricauto.service.LyricDownloadService
import com.lyricauto.service.LyricFloatService
import com.lyricauto.service.MusicListenerService
import com.lyricauto.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isFloatWindowEnabled = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (PermissionHelper.hasOverlayPermission(this)) {
            checkAndRequestNotificationPermission()
        } else {
            Toast.makeText(this, "需要悬浮窗权限才能显示歌词", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startFloatWindow()
        } else {
            Toast.makeText(this, "通知权限被拒绝，但服务仍可运行", Toast.LENGTH_SHORT).show()
            startFloatWindow()
        }
    }

    private val lyricDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LyricDownloadService.ACTION_LYRIC_DOWNLOADED -> {
                    val lyric = intent.getParcelableExtra<com.lyricauto.model.Lyric>(LyricDownloadService.EXTRA_LYRIC)
                    val title = intent.getStringExtra(LyricDownloadService.EXTRA_TITLE) ?: ""
                    val artist = intent.getStringExtra(LyricDownloadService.EXTRA_ARTIST) ?: ""

                    if (lyric != null && !lyric.isEmpty()) {
                        Toast.makeText(this@MainActivity, "歌词下载成功: $title - $artist", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "未找到歌词: $title - $artist", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val musicChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicListenerService.ACTION_MUSIC_CHANGED -> {
                    val musicInfo = intent.getParcelableExtra<com.lyricauto.model.MusicInfo>(MusicListenerService.EXTRA_MUSIC_INFO)
                    musicInfo?.let {
                        updateCurrentMusicDisplay(it)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupClickListeners()
            checkFloatWindowStatus()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkFloatWindowStatus()
        registerReceivers()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceivers()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun setupClickListeners() {
        binding.toggleFloatWindowBtn.setOnClickListener {
            toggleFloatWindow()
        }

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.searchLyricsBtn.setOnClickListener {
            startActivity(Intent(this, LyricSearchActivity::class.java))
        }

        binding.localMusicBtn.setOnClickListener {
            startActivity(Intent(this, LocalMusicActivity::class.java))
        }
    }

    private fun toggleFloatWindow() {
        if (isFloatWindowEnabled) {
            stopFloatWindow()
        } else {
            if (PermissionHelper.hasOverlayPermission(this)) {
                checkAndRequestNotificationPermission()
            } else {
                PermissionHelper.requestOverlayPermission(this, overlayPermissionLauncher)
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                PermissionHelper.hasNotificationPermission(this) -> {
                    startFloatWindow()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("需要通知权限")
                        .setMessage("为了显示前台服务通知，需要通知权限")
                        .setPositiveButton("确定") { _, _ ->
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("取消") { _, _ ->
                            startFloatWindow()
                        }
                        .show()
                }
                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startFloatWindow()
        }
    }

    private fun startFloatWindow() {
        try {
            ContextCompat.startForegroundService(this, Intent(this, MusicListenerService::class.java))
            ContextCompat.startForegroundService(this, Intent(this, LyricFloatService::class.java))
            isFloatWindowEnabled = true
            updateFloatWindowButton()
            Toast.makeText(this, "悬浮歌词已开启", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFloatWindow() {
        try {
            stopService(Intent(this, LyricFloatService::class.java))
            stopService(Intent(this, MusicListenerService::class.java))
            isFloatWindowEnabled = false
            updateFloatWindowButton()
            Toast.makeText(this, "悬浮歌词已关闭", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkFloatWindowStatus() {
        isFloatWindowEnabled = isServiceRunning(LyricFloatService::class.java)
        updateFloatWindowButton()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateFloatWindowButton() {
        binding.toggleFloatWindowBtn.text = if (isFloatWindowEnabled) {
            getString(R.string.float_window_enabled)
        } else {
            getString(R.string.enable_float_window)
        }
    }

    private fun updateCurrentMusicDisplay(musicInfo: com.lyricauto.model.MusicInfo) {
        binding.currentMusicText.text = getString(
            R.string.music_playing,
            musicInfo.title,
            musicInfo.artist
        )
    }

    private fun registerReceivers() {
        try {
            val lyricFilter = IntentFilter(LyricDownloadService.ACTION_LYRIC_DOWNLOADED)
            val musicFilter = IntentFilter(MusicListenerService.ACTION_MUSIC_CHANGED)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(lyricDownloadReceiver, lyricFilter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(musicChangedReceiver, musicFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(lyricDownloadReceiver, lyricFilter)
                registerReceiver(musicChangedReceiver, musicFilter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(lyricDownloadReceiver)
            unregisterReceiver(musicChangedReceiver)
        } catch (e: Exception) {
        }
    }
}
