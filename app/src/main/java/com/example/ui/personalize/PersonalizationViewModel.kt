package com.example.ui.personalize

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceMetrics(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuArch: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availRamMb: Long,
    val batteryPct: Int,
    val isCharging: Boolean,
    val batteryTempC: Float
)

data class PersonalizationState(
    val currentPreset: ThemePreset = ThemePreset.CYBERPUNK,
    val isDarkMode: Boolean = true,
    val isDynamicColor: Boolean = false,
    val selectedWallpaper: WallpaperStyle = WallpaperStyle.MATRIX_RAIN,
    val isApplyingWallpaper: Boolean = false,
    val wallpaperApplyMessage: String? = null,
    val selectedIconPackIndex: Int = 0,
    val customIconSymbol: String = "</>",
    val customIconGradientIndex: Int = 0,
    val deviceMetrics: DeviceMetrics? = null
)

class PersonalizationViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalizationState())
    val uiState: StateFlow<PersonalizationState> = _uiState.asStateFlow()

    init {
        loadDeviceMetrics()
    }

    fun setThemePreset(preset: ThemePreset) {
        _uiState.update { it.copy(currentPreset = preset) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(isDynamicColor = enabled) }
    }

    fun selectWallpaperStyle(style: WallpaperStyle) {
        _uiState.update { it.copy(selectedWallpaper = style) }
    }

    fun applyWallpaperToDevice(target: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingWallpaper = true) }
            val success = withContext(Dispatchers.IO) {
                try {
                    val fullBmp = WallpaperGenerator.generateWallpaperBitmap(
                        style = _uiState.value.selectedWallpaper,
                        width = 1080,
                        height = 2400
                    )
                    WallpaperGenerator.applyToWallpaper(context, fullBmp, target)
                } catch (e: Exception) {
                    false
                }
            }
            val targetName = when (target) {
                WallpaperManager.FLAG_SYSTEM -> "Home Screen"
                WallpaperManager.FLAG_LOCK -> "Lock Screen"
                else -> "Home & Lock Screen"
            }
            _uiState.update {
                it.copy(
                    isApplyingWallpaper = false,
                    wallpaperApplyMessage = if (success) "Applied to $targetName successfully!" else "Failed to apply wallpaper"
                )
            }
        }
    }

    fun clearWallpaperMessage() {
        _uiState.update { it.copy(wallpaperApplyMessage = null) }
    }

    fun selectIconPack(index: Int) {
        _uiState.update { it.copy(selectedIconPackIndex = index) }
    }

    fun setCustomIconSymbol(symbol: String) {
        _uiState.update { it.copy(customIconSymbol = symbol) }
    }

    fun setCustomIconGradient(gradientIndex: Int) {
        _uiState.update { it.copy(customIconGradientIndex = gradientIndex) }
    }

    fun refreshMetrics() {
        loadDeviceMetrics()
    }

    private fun loadDeviceMetrics() {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)

            var batteryPct = 100
            var isCharging = false
            var batteryTempC = 25.0f

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (bm != null) {
                val cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (cap in 0..100) batteryPct = cap
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isCharging = bm.isCharging
                }
            }

            try {
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
                if (batteryStatus != null) {
                    val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryPct = (level * 100 / scale.toFloat()).toInt()
                    }
                    val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    val tempRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    if (tempRaw > 0) batteryTempC = tempRaw / 10f
                }
            } catch (ignored: Exception) {}

            val metrics = DeviceMetrics(
                deviceModel = Build.MODEL,
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                cpuCores = Runtime.getRuntime().availableProcessors(),
                totalRamMb = memInfo.totalMem / (1024 * 1024),
                availRamMb = memInfo.availMem / (1024 * 1024),
                batteryPct = batteryPct,
                isCharging = isCharging,
                batteryTempC = batteryTempC
            )
            _uiState.update { it.copy(deviceMetrics = metrics) }
        } catch (e: Exception) {
            // Safe fallback
            _uiState.update {
                it.copy(
                    deviceMetrics = DeviceMetrics(
                        deviceModel = Build.MODEL,
                        manufacturer = "Android",
                        androidVersion = Build.VERSION.RELEASE,
                        apiLevel = Build.VERSION.SDK_INT,
                        cpuArch = "arm64",
                        cpuCores = 8,
                        totalRamMb = 4096,
                        availRamMb = 2048,
                        batteryPct = 100,
                        isCharging = false,
                        batteryTempC = 25f
                    )
                )
            }
        }
    }

    fun openDevSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ignored: Exception) {}
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PersonalizationViewModel(context) as T
        }
    }
}
