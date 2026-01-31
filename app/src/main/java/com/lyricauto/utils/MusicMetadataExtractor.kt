package com.lyricauto.utils

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.lyricauto.NotificationListenerService
import com.lyricauto.model.MusicInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MusicMetadataExtractor(private val context: Context) {

    private var mediaSessionManager: MediaSessionManager? = null

    init {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        mediaSessionManager = null
    }

    suspend fun getCurrentPlayingMusic(): MusicInfo? = suspendCancellableCoroutine { continuation ->
        try {
            val componentName = android.content.ComponentName(context, NotificationListenerService::class.java)
            if (!isNotificationListenerEnabled()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            sessions?.forEach { controller ->
                val playbackState = controller.playbackState
                val metadata = controller.metadata
                if (playbackState?.state == PlaybackState.STATE_PLAYING && metadata != null) {
                    val musicInfo = extractMusicInfo(metadata, playbackState)
                    if (musicInfo.isValid()) {
                        continuation.resume(musicInfo)
                        return@suspendCancellableCoroutine
                    }
                }
            }
            continuation.resume(null)
        } catch (e: SecurityException) {
            e.printStackTrace()
            continuation.resume(null)
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = context.packageName
        val enabledListeners = android.provider.Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    private fun extractMusicInfo(metadata: MediaMetadata, playbackState: PlaybackState?): MusicInfo {
        return MusicInfo(
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            position = playbackState?.position ?: 0
        )
    }
}
