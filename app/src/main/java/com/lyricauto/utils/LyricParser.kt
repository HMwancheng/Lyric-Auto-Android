package com.lyricauto.utils

import com.lyricauto.model.Lyric
import com.lyricauto.model.LyricLine

object LyricParser {
    fun parseLrc(lrcContent: String): Lyric {
        val lines = mutableListOf<LyricLine>()
        val title = extractTag(lrcContent, "ti")
        val artist = extractTag(lrcContent, "ar")
        val album = extractTag(lrcContent, "al")

        val linePattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        lrcContent.lines().forEach { line ->
            val match = linePattern.find(line)
            match?.let {
                val minutes = it.groupValues[1].toInt()
                val seconds = it.groupValues[2].toInt()
                val milliseconds = it.groupValues[3].toInt()
                val text = it.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    val time = minutes * 60000L + seconds * 1000L + milliseconds * 10L
                    lines.add(LyricLine(time, text))
                }
            }
        }
        lines.sortBy { it.time }
        return Lyric(lines, title, artist, album)
    }

    private fun extractTag(content: String, tag: String): String {
        val pattern = Regex("""\[$tag:(.*)\]""")
        val match = pattern.find(content)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
}
