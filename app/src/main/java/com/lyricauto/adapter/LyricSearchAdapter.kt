package com.lyricauto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lyricauto.databinding.ItemLyricSearchBinding
import com.lyricauto.network.LyricSearchResult

class LyricSearchAdapter(
    private val results: List<LyricSearchResult>,
    private val onItemClick: (LyricSearchResult, String) -> Unit
) : RecyclerView.Adapter<LyricSearchAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemLyricSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: LyricSearchResult) {
            binding.songTitleText.text = result.title
            binding.artistText.text = result.artist
            binding.previewBtn.setOnClickListener { onItemClick(result, "preview") }
            binding.downloadBtn.setOnClickListener { onItemClick(result, "download") }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLyricSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size
}
