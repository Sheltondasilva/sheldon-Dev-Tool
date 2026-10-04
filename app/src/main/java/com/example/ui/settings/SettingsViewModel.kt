package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DevDatabase
import com.example.data.local.SnippetEntity
import com.example.model.AppLanguage
import com.example.model.LanguageManager
import com.example.model.LocalizedStrings
import com.example.ui.personalize.PersonalizationViewModel
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val editorFontSize: Int = 13,
    val autoIndent: Boolean = true,
    val wordWrap: Boolean = false,
    val deepScanDefault: Boolean = false,
    val autoQuarantine: Boolean = true,
    val notificationMessage: String? = null
) {
    val localized: LocalizedStrings
        get() = LanguageManager.getStrings(language)
}

class SettingsViewModel(
    private val context: Context,
    private val database: DevDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _uiState.update { 
            it.copy(
                language = language,
                notificationMessage = "Language switched to ${language.displayName}"
            )
        }
    }

    fun setEditorFontSize(sizeSp: Int) {
        _uiState.update { it.copy(editorFontSize = sizeSp) }
    }

    fun toggleAutoIndent(enabled: Boolean) {
        _uiState.update { it.copy(autoIndent = enabled) }
    }

    fun toggleWordWrap(enabled: Boolean) {
        _uiState.update { it.copy(wordWrap = enabled) }
    }

    fun toggleDeepScan(enabled: Boolean) {
        _uiState.update { it.copy(deepScanDefault = enabled) }
    }

    fun toggleAutoQuarantine(enabled: Boolean) {
        _uiState.update { it.copy(autoQuarantine = enabled) }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedBytes = withContext(Dispatchers.IO) {
                var freed = 0L
                val cacheDir = context.cacheDir
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (f in files) {
                        freed += f.length()
                        f.delete()
                    }
                }
                freed
            }
            _uiState.update {
                it.copy(notificationMessage = "Cleared ${freedBytes / 1024} KB of temporary cache!")
            }
        }
    }

    fun resetAllConfiguration(
        personalizeVm: PersonalizationViewModel,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 1. Clear database scan history
                database.scanRecordDao().clearHistory()

                // 2. Clear and repopulate initial snippets
                val snippets = database.snippetDao()
                val existing = database.snippetDao()
                // delete and re-insert defaults
                val now = System.currentTimeMillis()
                existing.insertSnippet(
                    SnippetEntity(
                        title = "Quick Sort Algorithm",
                        language = "PYTHON",
                        code = """# Python Algorithm Demo
def quick_sort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quick_sort(left) + middle + quick_sort(right)

numbers = [64, 34, 25, 12, 22, 11, 90, 88, 45, 5]
print("Original Array:", numbers)
sorted_numbers = quick_sort(numbers)
print("Sorted Array:  ", sorted_numbers)
""".trimIndent(),
                        isFavorite = true,
                        lastModified = now
                    )
                )

                // 3. Clear cache
                context.cacheDir.listFiles()?.forEach { it.delete() }
            }

            // 4. Reset Personalization state
            personalizeVm.setThemePreset(ThemePreset.CYBERPUNK)
            personalizeVm.toggleDarkMode(true)
            personalizeVm.selectWallpaperStyle(com.example.ui.personalize.WallpaperStyle.MATRIX_RAIN)
            personalizeVm.selectIconPack(0)
            personalizeVm.setCustomIconSymbol("</>")

            // 5. Reset Settings state
            _uiState.update {
                SettingsState(
                    language = AppLanguage.ENGLISH,
                    editorFontSize = 13,
                    autoIndent = true,
                    wordWrap = false,
                    notificationMessage = "Dev Tools configuration reset to factory defaults."
                )
            }
            onComplete()
        }
    }

    class Factory(
        private val context: Context,
        private val database: DevDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context, database) as T
        }
    }
}
