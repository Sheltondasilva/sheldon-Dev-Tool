package com.example.ui.security

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DevDatabase
import com.example.data.local.ScanRecordEntity
import com.example.model.CleanResult
import com.example.model.ScanProgressState
import com.example.model.ThreatItem
import com.example.security.MalwareDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityState(
    val scanProgress: ScanProgressState = ScanProgressState(),
    val detectedThreats: List<ThreatItem> = emptyList(),
    val lastCleanResult: CleanResult? = null,
    val securityScore: Int = 98,
    val isCleaning: Boolean = false,
    val testThreatMessage: String? = null,
    val isTestGenerating: Boolean = false
)

class SecurityViewModel(
    private val context: Context,
    private val database: DevDatabase
) : ViewModel() {

    private val detector = MalwareDetector(context)

    private val _uiState = MutableStateFlow(SecurityState())
    val uiState: StateFlow<SecurityState> = _uiState.asStateFlow()

    val scanHistory: StateFlow<List<ScanRecordEntity>> = database.scanRecordDao().getRecentScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startScan(deepScan: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    detectedThreats = emptyList(),
                    lastCleanResult = null,
                    testThreatMessage = null
                )
            }

            detector.scanDevice(deepScan).collect { (progress, threats) ->
                val score = calculateSecurityScore(threats)
                _uiState.update {
                    it.copy(
                        scanProgress = progress,
                        detectedThreats = threats,
                        securityScore = score
                    )
                }

                // If scan finished, persist record to Room
                if (!progress.isScanning && progress.progress >= 1.0f) {
                    database.scanRecordDao().insertScan(
                        ScanRecordEntity(
                            totalFilesScanned = progress.scannedFilesCount,
                            threatsFound = threats.size,
                            threatsCleaned = 0,
                            bytesReclaimed = 0L,
                            scanMode = if (deepScan) "Full Deep Scan" else "Storage Quick Scan"
                        )
                    )
                }
            }
        }
    }

    fun cleanAllThreats() {
        val currentThreats = _uiState.value.detectedThreats
        if (currentThreats.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            val cleanResult = detector.cleanThreats(currentThreats)

            // Update database record
            database.scanRecordDao().insertScan(
                ScanRecordEntity(
                    totalFilesScanned = _uiState.value.scanProgress.scannedFilesCount,
                    threatsFound = currentThreats.size,
                    threatsCleaned = cleanResult.removedCount,
                    bytesReclaimed = cleanResult.bytesReclaimed,
                    scanMode = "Threat Remediation"
                )
            )

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    detectedThreats = emptyList(),
                    lastCleanResult = cleanResult,
                    securityScore = 100
                )
            }
        }
    }

    fun generateTestThreat(type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestGenerating = true) }
            val file = detector.generateTestThreat(type)
            _uiState.update {
                it.copy(
                    isTestGenerating = false,
                    testThreatMessage = "Created test sample: ${file.name} (${file.length()} bytes). Run 'Scan Storage' to detect and clean it!"
                )
            }
        }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(testThreatMessage = null) }
    }

    private fun calculateSecurityScore(threats: List<ThreatItem>): Int {
        if (threats.isEmpty()) return 100
        val penalty = threats.sumOf {
            when (it.severity) {
                com.example.model.ThreatSeverity.CRITICAL -> 35
                com.example.model.ThreatSeverity.HIGH -> 20
                com.example.model.ThreatSeverity.MEDIUM -> 10
                com.example.model.ThreatSeverity.LOW -> 5
                com.example.model.ThreatSeverity.CLEAN -> 0
            }
        }
        return (100 - penalty).coerceAtLeast(15)
    }

    class Factory(
        private val context: Context,
        private val database: DevDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecurityViewModel(context, database) as T
        }
    }
}
