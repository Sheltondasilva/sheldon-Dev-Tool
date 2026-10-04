package com.example.ui.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.compiler.CodeRunner
import com.example.compiler.ExecutionResult
import com.example.data.local.DevDatabase
import com.example.data.local.SnippetEntity
import com.example.model.CodeLanguage
import com.example.model.CodeSnippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompilerState(
    val currentLanguage: CodeLanguage = CodeLanguage.PYTHON,
    val codeText: String = "",
    val stdinText: String = "",
    val executionResult: ExecutionResult? = null,
    val isRunning: Boolean = false,
    val activeOutputTab: Int = 0, // 0: Output stdout, 1: Compiler Diagnostics, 2: Standard In
    val currentSnippetTitle: String = "Untitled Script",
    val statusMessage: String? = null
)

class CompilerViewModel(
    private val database: DevDatabase
) : ViewModel() {

    private val runner = CodeRunner()
    private val snippetDao = database.snippetDao()

    private val _uiState = MutableStateFlow(CompilerState())
    val uiState: StateFlow<CompilerState> = _uiState.asStateFlow()

    val savedSnippets: StateFlow<List<CodeSnippet>> = snippetDao.getAllSnippets()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load default code for initial language
        loadLanguageTemplate(CodeLanguage.PYTHON)
    }

    fun setLanguage(lang: CodeLanguage) {
        if (_uiState.value.currentLanguage == lang) return
        _uiState.update { it.copy(currentLanguage = lang) }
        loadLanguageTemplate(lang)
    }

    fun updateCode(newCode: String) {
        _uiState.update { it.copy(codeText = newCode) }
    }

    fun updateStdin(stdin: String) {
        _uiState.update { it.copy(stdinText = stdin) }
    }

    fun setActiveOutputTab(tab: Int) {
        _uiState.update { it.copy(activeOutputTab = tab) }
    }

    fun executeCode() {
        val current = _uiState.value
        if (current.isRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    activeOutputTab = 0,
                    statusMessage = "Compiling & executing ${current.currentLanguage.displayName}..."
                )
            }

            val result = runner.runCode(
                language = current.currentLanguage,
                code = current.codeText,
                stdin = current.stdinText
            )

            _uiState.update {
                it.copy(
                    isRunning = false,
                    executionResult = result,
                    statusMessage = if (result.isSuccess) "Finished in ${result.executionTimeMs}ms" else "Failed with exit code ${result.exitCode}"
                )
            }
        }
    }

    fun loadLanguageTemplate(lang: CodeLanguage) {
        val template = when (lang) {
            CodeLanguage.PYTHON -> """# Python 3.12 Developer Playground
def fibonacci_primes(limit):
    primes = []
    a, b = 0, 1
    while len(primes) < limit:
        a, b = b, a + b
        # Check primality
        if a > 1:
            is_p = True
            for d in range(2, int(a ** 0.5) + 1):
                if a % d == 0:
                    is_p = False
                    break
            if is_p:
                primes.append(a)
    return primes

print("=== Python Runtime Running ===")
results = fibonacci_primes(7)
print("First 7 Fibonacci Primes:", results)

squares = [x * x for x in range(1, 11)]
print("Squares 1..10:", squares)
print("Sum of squares:", sum(squares))
""".trimIndent()

            CodeLanguage.JAVA -> """public class Main {
    public static void main(String[] args) {
        System.out.println("=== Java 21 Micro-Runtime ===");
        int[] dataset = {42, 17, 88, 9, 33, 71, 5};
        
        System.out.println("Original elements: 42, 17, 88, 9, 33, 71, 5");
        
        int maxVal = dataset[0];
        long totalSum = 0;
        for (int i = 0; i < 7; i++) {
            totalSum += dataset[i];
            if (dataset[i] > maxVal) {
                maxVal = dataset[i];
            }
        }
        
        System.out.println("Maximum Value: " + maxVal);
        System.out.println("Computed Sum:  " + totalSum);
        System.out.println("Status: Java JVM Execution Completed.");
    }
}
""".trimIndent()

            CodeLanguage.CPP -> """#include <iostream>
#include <vector>

using namespace std;

int main() {
    cout << "=== GCC 13.2 C++17 Compiler Engine ===" << endl;
    vector<int> numbers = {12, 24, 36, 48, 60};
    
    cout << "Iterating vector: ";
    for (int n : numbers) {
        cout << n << " ";
    }
    cout << endl;
    
    int sum = 0;
    for (int n : numbers) {
        sum += n;
    }
    cout << "Calculated Sum: " << sum << endl;
    cout << "Vector Size: " << numbers.size() << endl;
    cout << "Process finished with exit code 0." << endl;
    return 0;
}
""".trimIndent()
        }

        _uiState.update {
            it.copy(
                codeText = template,
                currentSnippetTitle = "Demo ${lang.displayName} Script",
                executionResult = null
            )
        }
    }

    fun saveCurrentSnippet(title: String) {
        val current = _uiState.value
        viewModelScope.launch {
            val snippet = SnippetEntity(
                title = title.ifBlank { "Snippet_${System.currentTimeMillis() % 1000}" },
                language = current.currentLanguage.name,
                code = current.codeText,
                isFavorite = false,
                lastModified = System.currentTimeMillis()
            )
            snippetDao.insertSnippet(snippet)
            _uiState.update {
                it.copy(
                    currentSnippetTitle = snippet.title,
                    statusMessage = "Saved snippet '${snippet.title}'"
                )
            }
        }
    }

    fun loadSnippet(snippet: CodeSnippet) {
        _uiState.update {
            it.copy(
                currentLanguage = snippet.language,
                codeText = snippet.code,
                currentSnippetTitle = snippet.title,
                executionResult = null,
                statusMessage = "Loaded '${snippet.title}'"
            )
        }
    }

    fun deleteSnippet(snippet: CodeSnippet) {
        viewModelScope.launch {
            snippetDao.deleteSnippet(SnippetEntity.fromDomain(snippet))
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(private val database: DevDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompilerViewModel(database) as T
        }
    }
}
