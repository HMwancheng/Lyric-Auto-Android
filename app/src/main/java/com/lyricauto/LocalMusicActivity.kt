package com.lyricauto

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyricauto.adapter.LocalMusicAdapter
import com.lyricauto.databinding.ActivityLocalMusicBinding
import com.lyricauto.model.LocalMusic
import com.lyricauto.utils.LyricCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMusicActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalMusicBinding
    private lateinit var adapter: LocalMusicAdapter
    private val musicList = mutableListOf<LocalMusic>()
    private val cacheManager by lazy { LyricCacheManager(this) }
    private var selectedFolderUri: Uri? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) scanLocalMusic()
        else Toast.makeText(this, "需要存储权限才能扫描本地音乐", Toast.LENGTH_SHORT).show()
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            selectedFolderUri = it
            binding.currentFolderText.text = "当前文件夹: $it"
            scanLocalMusic()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = LocalMusicAdapter(musicList) { music -> searchLyricForMusic(music) }
        binding.musicRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.musicRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.scanBtn.setOnClickListener { checkStoragePermissionAndScan() }
        binding.selectFolderBtn.setOnClickListener { folderPickerLauncher.launch(null) }
        binding.currentFolderText.setOnLongClickListener {
            selectedFolderUri = null
            binding.currentFolderText.text = "当前文件夹: 全部音乐"
            Toast.makeText(this, "已清除文件夹选择", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun checkStoragePermissionAndScan() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            scanLocalMusic()
        } else storagePermissionLauncher.launch(permission)
    }

    private fun scanLocalMusic() {
        binding.scanBtn.isEnabled = false
        binding.scanStatusText.visibility = android.view.View.VISIBLE
        binding.scanStatusText.text = getString(R.string.scanning)
        musicList.clear()
        adapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.ALBUM,
                    android.provider.MediaStore.Audio.Media.DATA
                )

                val folderPath = getFolderPathFromUri(selectedFolderUri)
                val selection: String
                val selectionArgs: Array<String>

                if (folderPath != null) {
                    selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} = ? AND ${android.provider.MediaStore.Audio.Media.DATA} LIKE ?"
                    selectionArgs = arrayOf("1", "$folderPath%")
                } else {
                    selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} = ?"
                    selectionArgs = arrayOf("1")
                }

                contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs,
                    "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID))
                            val title = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)) ?: ""
                            val artist = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)) ?: ""
                            val album = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)) ?: ""
                            val path = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)) ?: ""

                            if (title.isNotEmpty()) {
                                musicList.add(LocalMusic(id, title, artist, album, path))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.scanBtn.isEnabled = true
                    binding.scanStatusText.text = getString(R.string.scan_complete, musicList.size)
                    adapter.notifyDataSetChanged()
                    if (musicList.isEmpty()) {
                        Toast.makeText(this@LocalMusicActivity, R.string.no_music_found, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.scanBtn.isEnabled = true
                    Toast.makeText(this@LocalMusicActivity, "扫描失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFolderPathFromUri(uri: Uri?): String? {
        if (uri == null) return null
        return try {
            val path = uri.path
            if (path != null && path.contains(":")) {
                val parts = path.split(":")
                if (parts.size >= 2) {
                    "${Environment.getExternalStorageDirectory()}/${parts[1]}"
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun searchLyricForMusic(music: LocalMusic) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val title = music.title.ifEmpty { return@launch }
                val cachedLyric = cacheManager.getLyric(title, music.artist)
                if (cachedLyric != null && !cachedLyric.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LocalMusicActivity, "歌词已缓存", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val downloader = com.lyricauto.network.LyricDownloader()
                val lyric = downloader.searchNeteaseLyric(title, music.artist)
                if (lyric != null && !lyric.isEmpty()) {
                    cacheManager.saveLyric(title, music.artist, lyric)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LocalMusicActivity, R.string.lyric_downloaded, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
