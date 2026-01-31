package com.lyricauto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lyricauto.databinding.ItemLocalMusicBinding
import com.lyricauto.model.LocalMusic
import com.lyricauto.utils.LyricCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMusicAdapter(
    private val musicList: List<LocalMusic>,
    private val onItemClick: (LocalMusic) -> Unit
) : RecyclerView.Adapter<LocalMusicAdapter.ViewHolder>() {

    private var cacheManager: LyricCacheManager? = null

    inner class ViewHolder(private val binding: ItemLocalMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(music: LocalMusic) {
            binding.songTitleText.text = music.title
            binding.artistText.text = music.artist
            binding.searchLyricBtn.setOnClickListener { onItemClick(music) }
            checkLyricCache(music)
        }

        private fun checkLyricCache(music: LocalMusic) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val hasLyric = cacheManager?.getLyric(music.title, music.artist) != null
                    withContext(Dispatchers.Main) {
                        if (hasLyric) {
                            binding.hasLyricIndicator.visibility = android.view.View.VISIBLE
                            binding.searchLyricBtn.text = "重新搜索"
                        } else {
                            binding.hasLyricIndicator.visibility = android.view.View.GONE
                            binding.searchLyricBtn.text = "搜索歌词"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocalMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (cacheManager == null) cacheManager = LyricCacheManager(parent.context)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(musicList[position])
    }

    override fun getItemCount(): Int = musicList.size
}
