package com.lyricauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyricauto.adapter.LyricSearchAdapter
import com.lyricauto.databinding.ActivityLyricSearchBinding
import com.lyricauto.model.Lyric
import com.lyricauto.network.LyricSearchResult
import com.lyricauto.utils.LyricCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LyricSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLyricSearchBinding
    private lateinit var adapter: LyricSearchAdapter
    private val searchResults = mutableListOf<LyricSearchResult>()
    private val cacheManager by lazy { LyricCacheManager(this) }

    private val lyricDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                com.lyricauto.service.LyricDownloadService.ACTION_LYRIC_DOWNLOADED -> {
                    val lyric = intent.getParcelableExtra<Lyric>(com.lyricauto.service.LyricDownloadService.EXTRA_LYRIC)
                    if (lyric != null && !lyric.isEmpty()) {
                        Toast.makeText(this@LyricSearchActivity, "歌词下载成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LyricSearchActivity, "歌词下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLyricSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceivers()
    }

    private fun setupRecyclerView() {
        adapter = LyricSearchAdapter(searchResults) { result, action ->
            when (action) {
                "preview" -> previewLyric(result)
                "download" -> downloadLyric(result)
            }
        }
        binding.lyricRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.lyricRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.searchBtn.setOnClickListener { performSearch() }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = binding.searchInput.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show()
            return
        }
        binding.searchBtn.isEnabled = false
        searchResults.clear()
        adapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloader = com.lyricauto.network.LyricDownloader()
                val parts = query.split(" - ", "-", " ")
                val title = parts.firstOrNull() ?: query
                val artist = parts.getOrNull(1) ?: ""
                val lyric = downloader.searchNeteaseLyric(title, artist)

                withContext(Dispatchers.Main) {
                    binding.searchBtn.isEnabled = true
                    if (lyric != null && !lyric.isEmpty()) {
                        val result = LyricSearchResult(
                            id = "0",
                            title = lyric.title.ifEmpty { title },
                            artist = lyric.artist.ifEmpty { artist },
                            album = lyric.album
                        )
                        searchResults.add(result)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this@LyricSearchActivity, "找到歌词", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LyricSearchActivity, "未找到歌词", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.searchBtn.isEnabled = true
                    Toast.makeText(this@LyricSearchActivity, "搜索失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun previewLyric(result: LyricSearchResult) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloader = com.lyricauto.network.LyricDownloader()
                val lyric = downloader.searchNeteaseLyric(result.title, result.artist)
                withContext(Dispatchers.Main) {
                    if (lyric != null && !lyric.isEmpty()) {
                        showLyricPreview(lyric)
                    } else {
                        Toast.makeText(this@LyricSearchActivity, "无法预览歌词", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLyricPreview(lyric: Lyric) {
        val text = lyric.lines.joinToString("\n") { "[${formatTime(it.time)}] ${it.text}" }
        android.app.AlertDialog.Builder(this)
            .setTitle("歌词预览")
            .setMessage(text)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun downloadLyric(result: LyricSearchResult) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloader = com.lyricauto.network.LyricDownloader()
                val lyric = downloader.searchNeteaseLyric(result.title, result.artist)
                if (lyric != null && !lyric.isEmpty()) {
                    cacheManager.saveLyric(result.title, result.artist, lyric)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LyricSearchActivity, "歌词已缓存", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatTime(time: Long): String {
        val minutes = time / 60000
        val seconds = (time % 60000) / 1000
        val milliseconds = (time % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
    }

    private fun registerReceivers() {
        try {
            val filter = IntentFilter(com.lyricauto.service.LyricDownloadService.ACTION_LYRIC_DOWNLOADED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(lyricDownloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(lyricDownloadReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(lyricDownloadReceiver)
        } catch (e: Exception) {}
    }
}
