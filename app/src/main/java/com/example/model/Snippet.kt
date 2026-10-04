package com.example.model

enum class CodeLanguage(val displayName: String, val extension: String) {
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    CPP("C++", "cpp")
}

data class CodeSnippet(
    val id: Long = 0,
    val title: String,
    val language: CodeLanguage,
    val code: String,
    val isFavorite: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
