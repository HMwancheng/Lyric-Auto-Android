package com.lyricauto.model

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FloatWindowSettings(
    var fontSize: Int = 16,
    var textColor: Int = Color.WHITE,
    var currentLineColor: Int = Color.parseColor("#FFD700"),
    var backgroundColor: Int = Color.parseColor("#80000000"),
    var animationType: AnimationType = AnimationType.FADE,
    var positionType: PositionType = PositionType.CENTER,
    var customX: Int = 0,
    var customY: Int = 0,
    var showInStatusBar: Boolean = false,
    var autoDownload: Boolean = true,
    var enableCache: Boolean = true
) : Parcelable {
    enum class AnimationType { NONE, FADE, SLIDE, SCALE }
    enum class PositionType { TOP, CENTER, BOTTOM, CUSTOM }
}
