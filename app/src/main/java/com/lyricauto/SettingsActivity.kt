package com.lyricauto

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lyricauto.databinding.ActivitySettingsBinding
import com.lyricauto.model.FloatWindowSettings
import com.lyricauto.service.LyricFloatService
import com.lyricauto.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: SharedPreferencesManager
    private var settings: FloatWindowSettings = FloatWindowSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager(this)
        settings = prefsManager.getFloatWindowSettings()

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        binding.fontSizeSeekBar.progress = settings.fontSize - 12

        when (settings.animationType) {
            FloatWindowSettings.AnimationType.NONE -> binding.animationNone.isChecked = true
            FloatWindowSettings.AnimationType.FADE -> binding.animationFade.isChecked = true
            FloatWindowSettings.AnimationType.SLIDE -> binding.animationSlide.isChecked = true
            FloatWindowSettings.AnimationType.SCALE -> binding.animationScale.isChecked = true
        }

        when (settings.positionType) {
            FloatWindowSettings.PositionType.TOP -> binding.positionTop.isChecked = true
            FloatWindowSettings.PositionType.CENTER -> binding.positionCenter.isChecked = true
            FloatWindowSettings.PositionType.BOTTOM -> binding.positionBottom.isChecked = true
            FloatWindowSettings.PositionType.CUSTOM -> binding.positionCustom.isChecked = true
        }

        binding.showInStatusBarCheckBox.isChecked = settings.showInStatusBar
        binding.autoDownloadCheckBox.isChecked = settings.autoDownload
        binding.enableCacheCheckBox.isChecked = settings.enableCache
    }

    private fun setupListeners() {
        binding.fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.fontSize = progress + 12
                saveSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.textColorBtn.setOnClickListener {
            showColorPicker { color ->
                settings.textColor = color
                saveSettings()
            }
        }

        binding.currentLineColorBtn.setOnClickListener {
            showColorPicker { color ->
                settings.currentLineColor = color
                saveSettings()
            }
        }

        binding.backgroundColorBtn.setOnClickListener {
            showColorPicker { color ->
                settings.backgroundColor = color
                saveSettings()
            }
        }

        binding.animationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            settings.animationType = when (checkedId) {
                R.id.animationNone -> FloatWindowSettings.AnimationType.NONE
                R.id.animationFade -> FloatWindowSettings.AnimationType.FADE
                R.id.animationSlide -> FloatWindowSettings.AnimationType.SLIDE
                R.id.animationScale -> FloatWindowSettings.AnimationType.SCALE
                else -> FloatWindowSettings.AnimationType.FADE
            }
            saveSettings()
        }

        binding.positionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            settings.positionType = when (checkedId) {
                R.id.positionTop -> FloatWindowSettings.PositionType.TOP
                R.id.positionCenter -> FloatWindowSettings.PositionType.CENTER
                R.id.positionBottom -> FloatWindowSettings.PositionType.BOTTOM
                R.id.positionCustom -> FloatWindowSettings.PositionType.CUSTOM
                else -> FloatWindowSettings.PositionType.CENTER
            }
            saveSettings()
        }

        binding.showInStatusBarCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settings.showInStatusBar = isChecked
            saveSettings()
        }

        binding.autoDownloadCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settings.autoDownload = isChecked
            saveSettings()
        }

        binding.enableCacheCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settings.enableCache = isChecked
            saveSettings()
        }

        binding.clearCacheBtn.setOnClickListener {
            clearCache()
        }
    }

    private fun showColorPicker(onColorSelected: (Int) -> Unit) {
        val colors = intArrayOf(
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.parseColor("#FFD700"), Color.parseColor("#FF69B4"),
            Color.parseColor("#00CED1"), Color.parseColor("#FF6347")
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("选择颜色")
            .setItems(colors.map { getColorName(it) }.toTypedArray()) { _, which ->
                onColorSelected(colors[which])
            }
            .show()
    }

    private fun getColorName(color: Int): String {
        return when (color) {
            Color.WHITE -> "白色"
            Color.BLACK -> "黑色"
            Color.RED -> "红色"
            Color.GREEN -> "绿色"
            Color.BLUE -> "蓝色"
            Color.YELLOW -> "黄色"
            Color.CYAN -> "青色"
            Color.MAGENTA -> "品红"
            Color.parseColor("#FFD700") -> "金色"
            Color.parseColor("#FF69B4") -> "粉色"
            Color.parseColor("#00CED1") -> "深青色"
            Color.parseColor("#FF6347") -> "番茄红"
            else -> "其他"
        }
    }

    private fun saveSettings() {
        prefsManager.saveFloatWindowSettings(settings)
        notifyFloatWindowService()
    }

    private fun notifyFloatWindowService() {
        val intent = Intent(LyricFloatService.ACTION_UPDATE_SETTINGS)
        sendBroadcast(intent)
    }

    private fun clearCache() {
        android.app.AlertDialog.Builder(this)
            .setTitle("清除缓存")
            .setMessage("确定要清除所有缓存的歌词吗？")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val cacheManager = com.lyricauto.utils.LyricCacheManager(this@SettingsActivity)
                    val success = cacheManager.clearCache()
                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(this@SettingsActivity, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, "清除缓存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
