package com.lyricauto.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.lyricauto.model.Lyric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun searchNeteaseLyric(title: String, artist: String): Lyric? = withContext(Dispatchers.IO) {
        try {
            val query = if (artist.isNotEmpty()) "$title $artist" else title
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            val searchRequest = Request.Builder()
                .url("https://music.163.com/api/search/pc?s=$encodedQuery&type=1&limit=10")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val searchResponse = client.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val searchBody = searchResponse.body?.string() ?: ""
                val songId = parseNeteaseSearchResult(searchBody)

                if (songId != null) {
                    val lyricRequest = Request.Builder()
                        .url("https://music.163.com/api/song/lyric?id=$songId&lv=1&kv=1&tv=-1")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build()

                    val lyricResponse = client.newCall(lyricRequest).execute()
                    if (lyricResponse.isSuccessful) {
                        val lyricBody = lyricResponse.body?.string() ?: ""
                        parseNeteaseLyric(lyricBody)
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseNeteaseSearchResult(json: String): String? {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            val resultObj = jsonObject.getAsJsonObject("result")
            val songsArray = resultObj?.getAsJsonArray("songs") ?: return null
            if (songsArray.size() > 0) songsArray[0].asJsonObject.get("id")?.asString else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseNeteaseLyric(json: String): Lyric? {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            val lrcObj = jsonObject.getAsJsonObject("lrc")
            val lyric = lrcObj?.get("lyric")?.asString ?: ""
            com.lyricauto.utils.LyricParser.parseLrc(lyric)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class LyricSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String
)
