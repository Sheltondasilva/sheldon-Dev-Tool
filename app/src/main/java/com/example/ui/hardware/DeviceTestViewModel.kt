package com.example.ui.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

data class CameraHardwareInfo(
    val id: String,
    val facing: String,
    val hasFlash: Boolean,
    val orientation: Int,
    val maxResolution: String
)

data class BatteryHardwareStatus(
    val isPlugged: Boolean,
    val plugType: String,
    val level: Int,
    val voltageMv: Int,
    val temperatureC: Float,
    val health: String,
    val status: String
)

data class DeviceTestState(
    // Flashlight
    val isTorchOn: Boolean = false,
    val isTorchStrobeActive: Boolean = false,
    val hasTorch: Boolean = false,

    // Cameras
    val cameras: List<CameraHardwareInfo> = emptyList(),
    val hasCameraPermission: Boolean = false,

    // Audio Test
    val isAudioTonePlaying: Boolean = false,
    val audioFrequencyHz: Int = 440,
    val audioChannelMode: String = "Stereo (Both)", // "Left", "Right", "Stereo"

    // Mic Test
    val hasMicPermission: Boolean = false,
    val isRecordingMic: Boolean = false,
    val micAmplitudeLevel: Float = 0f, // 0..1
    val micDecibels: Int = 0,

    // Battery / Charger
    val batteryInfo: BatteryHardwareStatus? = null,

    // General Message
    val message: String? = null
)

class DeviceTestViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceTestState())
    val uiState: StateFlow<DeviceTestState> = _uiState.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private var micJob: Job? = null
    private var strobeJob: Job? = null
    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryStatus(intent)
            }
        }
    }

    init {
        initCameraAndFlash()
        initBattery()
        checkPermissions()
    }

    fun checkPermissions() {
        val cam = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasCameraPermission = cam, hasMicPermission = mic) }
    }

    private fun initCameraAndFlash() {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val camList = mutableListOf<CameraHardwareInfo>()
            var foundTorchId: String? = null

            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                val facingInt = chars?.get(CameraCharacteristics.LENS_FACING)
                val facing = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front (Selfie)"
                    CameraCharacteristics.LENS_FACING_BACK -> "Rear (Main)"
                    else -> "External"
                }
                val hasFlash = chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && foundTorchId == null) {
                    foundTorchId = id
                }
                val orientation = chars?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                camList.add(
                    CameraHardwareInfo(
                        id = id,
                        facing = facing,
                        hasFlash = hasFlash,
                        orientation = orientation,
                        maxResolution = "Full HD / 4K sensor"
                    )
                )
            }

            torchCameraId = foundTorchId
            _uiState.update {
                it.copy(
                    cameras = camList,
                    hasTorch = foundTorchId != null
                )
            }
        } catch (e: Exception) {
            // Ignore camera query failures in simulator
        }
    }

    private fun initBattery() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(batteryReceiver, filter)
            if (intent != null) {
                updateBatteryStatus(intent)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun updateBatteryStatus(intent: Intent) {
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val isPlugged = plugged > 0
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable / Computer"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi Dock"
            else -> "Not Plugged (On Battery)"
        }
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good Condition"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated!"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage Warning"
            else -> "Normal"
        }
        val statusInt = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging actively"
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged (100%)"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> "Idle"
        }

        _uiState.update {
            it.copy(
                batteryInfo = BatteryHardwareStatus(
                    isPlugged = isPlugged,
                    plugType = plugType,
                    level = pct,
                    voltageMv = voltageMv,
                    temperatureC = tempRaw / 10f,
                    health = health,
                    status = status
                )
            )
        }
    }

    // --- FLASHLIGHT ---
    fun toggleFlashlight() {
        val torchId = torchCameraId ?: return
        val current = _uiState.value.isTorchOn
        try {
            stopStrobe()
            cameraManager?.setTorchMode(torchId, !current)
            _uiState.update { it.copy(isTorchOn = !current, isTorchStrobeActive = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "Flashlight error: ${e.message}") }
        }
    }

    fun toggleStrobeFlashlight() {
        val torchId = torchCameraId ?: return
        if (_uiState.value.isTorchStrobeActive) {
            stopStrobe()
        } else {
            startStrobe(torchId)
        }
    }

    private fun startStrobe(torchId: String) {
        strobeJob?.cancel()
        _uiState.update { it.copy(isTorchStrobeActive = true, isTorchOn = true) }
        strobeJob = viewModelScope.launch(Dispatchers.IO) {
            var on = false
            try {
                while (isActive) {
                    on = !on
                    cameraManager?.setTorchMode(torchId, on)
                    delay(120)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                try { cameraManager?.setTorchMode(torchId, false) } catch (ignored: Exception) {}
            }
        }
    }

    private fun stopStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        torchCameraId?.let {
            try { cameraManager?.setTorchMode(it, false) } catch (ignored: Exception) {}
        }
        _uiState.update { it.copy(isTorchStrobeActive = false, isTorchOn = false) }
    }

    // --- AUDIO TEST (SPEAKER) ---
    fun playAudioTestTone(freq: Int = 440, channel: String = "Stereo") {
        stopAudioTone()
        _uiState.update { it.copy(isAudioTonePlaying = true, audioFrequencyHz = freq, audioChannelMode = channel) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSec = 3
                val numSamples = durationSec * sampleRate
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq.toDouble())
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                audioTrack = track

                delay(3000)
                stopAudioTone()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Audio playback failed: ${e.message}") }
                stopAudioTone()
            }
        }
    }

    fun stopAudioTone() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        _uiState.update { it.copy(isAudioTonePlaying = false) }
    }

    // --- MICROPHONE TEST ---
    fun startMicTest() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(message = "Microphone permission required for mic test") }
            return
        }

        stopMicTest()
        _uiState.update { it.copy(isRecordingMic = true) }

        micJob = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                audioRecord = record
                record.startRecording()

                val buffer = ShortArray(bufferSize)
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var maxAmp = 0
                        for (i in 0 until read) {
                            val abs = kotlin.math.abs(buffer[i].toInt())
                            if (abs > maxAmp) maxAmp = abs
                        }
                        val normalized = (maxAmp / 32767f).coerceIn(0f, 1f)
                        val db = (20 * kotlin.math.log10(maxAmp.coerceAtLeast(1).toDouble())).toInt()

                        _uiState.update {
                            it.copy(
                                micAmplitudeLevel = normalized,
                                micDecibels = db
                            )
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Mic test error: ${e.message}") }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun stopMicTest() {
        micJob?.cancel()
        micJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        _uiState.update { it.copy(isRecordingMic = false, micAmplitudeLevel = 0f, micDecibels = 0) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopStrobe()
        stopAudioTone()
        stopMicTest()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (ignored: Exception) {}
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceTestViewModel(context) as T
        }
    }
}
