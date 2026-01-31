package com.lyricauto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MusicInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val position: Long = 0
) : Parcelable {
    fun getSearchQuery(): String = if (artist.isNotEmpty()) "$title $artist" else title
    fun isValid(): Boolean = title.isNotEmpty() || artist.isNotEmpty()
}
