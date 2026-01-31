package com.lyricauto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LyricLine(
    val time: Long,
    val text: String
) : Parcelable {
    override fun toString(): String = text
}

@Parcelize
data class Lyric(
    val lines: List<LyricLine> = emptyList(),
    val title: String = "",
    val artist: String = "",
    val album: String = ""
) : Parcelable {
    fun getLineAtTime(time: Long): Int {
        var result = -1
        for (i in lines.indices) {
            if (lines[i].time <= time) result = i else break
        }
        return result
    }

    fun getCurrentLine(time: Long): LyricLine? {
        val index = getLineAtTime(time)
        return if (index >= 0 && index < lines.size) lines[index] else null
    }

    fun isEmpty(): Boolean = lines.isEmpty()
}
