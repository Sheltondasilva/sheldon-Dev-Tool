package com.example.compiler

import com.example.model.CodeLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeRunner {
    private val pythonEngine = PythonEngine()
    private val javaEngine = JavaEngine()
    private val cppEngine = CppEngine()

    suspend fun runCode(
        language: CodeLanguage,
        code: String,
        stdin: String = ""
    ): ExecutionResult = withContext(Dispatchers.Default) {
        when (language) {
            CodeLanguage.PYTHON -> pythonEngine.execute(code, stdin)
            CodeLanguage.JAVA -> javaEngine.execute(code, stdin)
            CodeLanguage.CPP -> cppEngine.execute(code, stdin)
        }
    }
}
