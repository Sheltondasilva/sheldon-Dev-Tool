package com.example.model

enum class ThreatSeverity(val label: String) {
    CRITICAL("Critical Risk"),
    HIGH("High Risk"),
    MEDIUM("Suspicious"),
    LOW("Notice"),
    CLEAN("Verified Safe")
}

enum class ThreatCategory(val title: String) {
    VIRUS("Known Virus / EICAR"),
    TROJAN("Backdoor / Trojan"),
    ADWARE("Intrusive Adware"),
    MINER("Crypto Miner"),
    EXPLOIT_SCRIPT("Suspicious Script"),
    VULNERABLE_PACKAGE("Excessive Permission App")
}

data class ThreatItem(
    val id: String,
    val name: String,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val filePath: String,
    val fileSize: Long,
    val description: String,
    val signatureMatched: String,
    val isCleaned: Boolean = false
)

data class ScanProgressState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val currentItem: String = "",
    val scannedFilesCount: Int = 0,
    val threatsFoundCount: Int = 0
)

data class CleanResult(
    val removedCount: Int,
    val bytesReclaimed: Long,
    val timestamp: Long = System.currentTimeMillis()
)
