package com.lyricauto.utils

import android.content.Context
import com.google.gson.Gson
import com.lyricauto.model.Lyric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LyricCacheManager(private val context: Context) {

    private val cacheDir: File by lazy {
        File(context.filesDir, "lyrics").apply { if (!exists()) mkdirs() }
    }
    private val gson = Gson()

    suspend fun saveLyric(title: String, artist: String, lyric: Lyric): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(title, artist)
            File(cacheDir, fileName).writeText(gson.toJson(lyric))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getLyric(title: String, artist: String): Lyric? = withContext(Dispatchers.IO) {
        try {
            val fileName = generateFileName(title, artist)
            val file = File(cacheDir, fileName)
            if (file.exists()) gson.fromJson(file.readText(), Lyric::class.java) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generateFileName(title: String, artist: String): String {
        val key = if (artist.isNotEmpty()) "$title-$artist" else title
        return key.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5-]"), "_") + ".json"
    }
}
