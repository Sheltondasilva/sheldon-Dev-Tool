package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalFilesScanned: Int,
    val threatsFound: Int,
    val threatsCleaned: Int,
    val bytesReclaimed: Long,
    val scanMode: String
)

@Dao
interface ScanRecordDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC LIMIT 20")
    fun getRecentScans(): Flow<List<ScanRecordEntity>>

    @Insert
    suspend fun insertScan(record: ScanRecordEntity): Long

    @Query("DELETE FROM scan_records")
    suspend fun clearHistory()
}
