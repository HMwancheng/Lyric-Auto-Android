package com.lyricauto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalMusic(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String
) : Parcelable
