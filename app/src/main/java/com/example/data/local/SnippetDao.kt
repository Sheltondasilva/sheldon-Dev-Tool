package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY lastModified DESC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE language = :lang ORDER BY lastModified DESC")
    fun getSnippetsByLanguage(lang: String): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)

    @Query("SELECT COUNT(*) FROM snippets")
    suspend fun getCount(): Int
}
