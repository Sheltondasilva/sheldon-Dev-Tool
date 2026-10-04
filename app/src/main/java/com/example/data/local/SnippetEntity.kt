package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CodeLanguage
import com.example.model.CodeSnippet

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val language: String,
    val code: String,
    val isFavorite: Boolean,
    val lastModified: Long
) {
    fun toDomain(): CodeSnippet {
        val lang = try {
            CodeLanguage.valueOf(language)
        } catch (e: Exception) {
            CodeLanguage.PYTHON
        }
        return CodeSnippet(
            id = id,
            title = title,
            language = lang,
            code = code,
            isFavorite = isFavorite,
            lastModified = lastModified
        )
    }

    companion object {
        fun fromDomain(snippet: CodeSnippet): SnippetEntity {
            return SnippetEntity(
                id = snippet.id,
                title = snippet.title,
                language = snippet.language.name,
                code = snippet.code,
                isFavorite = snippet.isFavorite,
                lastModified = snippet.lastModified
            )
        }
    }
}
