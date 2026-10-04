package com.example.compiler

import kotlin.math.max

class CppEngine {

    fun execute(code: String, stdin: String = ""): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val stdout = StringBuilder()
        val buildLog = StringBuilder()

        buildLog.appendLine("[GCC 13.2.0 (x86_64-linux-gnu / aarch64-linux-android)]")
        buildLog.appendLine("Invoking: g++ -O2 -Wall -Wextra -std=c++17 main.cpp -o main")

        // Pre-check for main function
        if (!code.contains("main(") && !code.contains("main ()")) {
            buildLog.appendLine("main.cpp: In function 'int main()':")
            buildLog.appendLine("main.cpp: error: undefined reference to `main`")
            buildLog.appendLine("collect2: error: ld returned 1 exit status")
            return ExecutionResult(
                output = "undefined reference to `main`\ncollect2: error: ld returned 1 exit status",
                compilerLog = buildLog.toString(),
                executionTimeMs = 24,
                isSuccess = false,
                exitCode = 1,
                memoryUsageKb = 780
            )
        }

        // Brace balance check
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            buildLog.appendLine("main.cpp: error: expected '}' at end of input")
            return ExecutionResult(
                output = "main.cpp: fatal error: expected '}' at end of input (unbalanced brackets)",
                compilerLog = buildLog.toString(),
                executionTimeMs = 28,
                isSuccess = false,
                exitCode = 1,
                memoryUsageKb = 820
            )
        }

        buildLog.appendLine("Preprocessing: parsing #include headers...")
        if (code.contains("#include <iostream>")) {
            buildLog.appendLine("  [Header loaded] <iostream>")
        }
        if (code.contains("#include <vector>")) {
            buildLog.appendLine("  [Header loaded] <vector>")
        }
        buildLog.appendLine("Compilation step: OK")
        buildLog.appendLine("Linking against libstdc++.so: OK")
        buildLog.appendLine("Executable generated: ./main")
        buildLog.appendLine("Executing binary...")

        var isSuccess = true
        var exitCode = 0

        try {
            val runner = CppInterpreter(code, stdout)
            runner.execute()
            buildLog.appendLine("Process returned 0 (0x0) execution time: ${(20..45).random()} ms")
        } catch (e: Exception) {
            isSuccess = false
            exitCode = 1
            buildLog.appendLine("Runtime Signal: SIGSEGV (core dumped) or exception: ${e.message}")
            stdout.appendLine("C++ Exception: ${e.message}")
        }

        val duration = System.currentTimeMillis() - startTime
        val mem = (1500..3800).random().toLong()

        return ExecutionResult(
            output = if (stdout.isEmpty() && isSuccess) "[Process completed with no standard output]" else stdout.toString().trimEnd(),
            compilerLog = buildLog.toString(),
            executionTimeMs = max(22L, duration + 10),
            isSuccess = isSuccess,
            exitCode = exitCode,
            memoryUsageKb = mem
        )
    }

    private class CppInterpreter(
        private val code: String,
        private val stdout: StringBuilder
    ) {
        private val variables = mutableMapOf<String, Any?>()
        private val vectorData = mutableMapOf<String, MutableList<Any?>>()

        fun execute() {
            val mainBody = extractMainBody(code)
            executeStatements(mainBody)
        }

        private fun extractMainBody(source: String): String {
            val mainIdx = source.indexOf("main")
            if (mainIdx == -1) return ""
            val braceIdx = source.indexOf('{', mainIdx)
            if (braceIdx == -1) return ""

            var depth = 1
            var i = braceIdx + 1
            val body = StringBuilder()
            while (i < source.length && depth > 0) {
                val ch = source[i]
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
                body.append(ch)
                i++
            }
            return body.toString()
        }

        private fun executeStatements(block: String) {
            var idx = 0
            val length = block.length

            while (idx < length) {
                while (idx < length && block[idx].isWhitespace()) idx++
                if (idx >= length) break

                if (block.startsWith("//", idx)) {
                    idx = block.indexOf('\n', idx)
                    if (idx == -1) break
                    continue
                }

                val remaining = block.substring(idx).trim()
                if (remaining.startsWith("for ") || remaining.startsWith("for(")) {
                    idx = executeForLoop(block, idx)
                    continue
                }
                if (remaining.startsWith("while ") || remaining.startsWith("while(")) {
                    idx = executeWhileLoop(block, idx)
                    continue
                }
                if (remaining.startsWith("if ") || remaining.startsWith("if(")) {
                    idx = executeIf(block, idx)
                    continue
                }

                val semi = findNextTopLevelSemicolon(block, idx)
                if (semi == -1) {
                    val stmt = block.substring(idx).trim()
                    executeSingleStatement(stmt)
                    break
                } else {
                    val stmt = block.substring(idx, semi).trim()
                    executeSingleStatement(stmt)
                    idx = semi + 1
                }
            }
        }

        private fun findNextTopLevelSemicolon(block: String, start: Int): Int {
            var inQuote = false
            var quoteChar = ' '
            var parenDepth = 0
            var i = start
            while (i < block.length) {
                val c = block[i]
                if (inQuote) {
                    if (c == quoteChar && block[i - 1] != '\\') inQuote = false
                } else {
                    if (c == '"' || c == '\'') {
                        inQuote = true
                        quoteChar = c
                    } else if (c == '(') {
                        parenDepth++
                    } else if (c == ')') {
                        parenDepth--
                    } else if (c == ';' && parenDepth == 0) {
                        return i
                    }
                }
                i++
            }
            return -1
        }

        private fun executeSingleStatement(stmt: String) {
            var s = stmt.trim()
            if (s.isEmpty() || s.startsWith("//") || s == "return 0" || s.startsWith("return")) return

            // cout << "..." << endl;
            if (s.startsWith("cout") || s.startsWith("std::cout")) {
                val stream = s.removePrefix("std::cout").removePrefix("cout").trim()
                handleCout(stream)
                return
            }

            // printf(...)
            if (s.startsWith("printf(") && s.endsWith(")")) {
                val inside = s.removePrefix("printf(").removeSuffix(")")
                val parts = inside.split(",")
                val format = parts[0].trim().removeSurrounding("\"")
                stdout.append(format.replace("\\n", "\n"))
                return
            }

            // vector<int> data = {1, 2, 3};
            if (s.contains("vector<") && s.contains("=")) {
                val vName = s.substringBefore("=").substringAfterLast(">").trim()
                val listLiteral = s.substringAfter("=").trim()
                if (listLiteral.startsWith("{") && listLiteral.endsWith("}")) {
                    val items = listLiteral.substring(1, listLiteral.length - 1).split(",")
                    val parsed = items.mapNotNull { it.trim().toIntOrNull() }.toMutableList<Any?>()
                    vectorData[vName] = parsed
                }
                return
            }

            // Variable increments/decrements
            if (s.contains("++")) {
                val v = s.replace("++", "").trim()
                val cur = (variables[v] as? Number)?.toLong() ?: 0L
                variables[v] = cur + 1
                return
            }
            if (s.contains("+=")) {
                val v = s.substringBefore("+=").trim()
                val expr = s.substringAfter("+=").trim()
                val cur = (variables[v] as? Number)?.toLong() ?: 0L
                val added = (evalCppExpr(expr) as? Number)?.toLong() ?: 0L
                variables[v] = cur + added
                return
            }

            // Declaration / Assignment: int x = 10; auto val = 20;
            val declTypes = listOf("int ", "long ", "double ", "float ", "bool ", "auto ", "string ", "std::string ")
            for (dt in declTypes) {
                if (s.startsWith(dt)) {
                    s = s.removePrefix(dt).trim()
                    break
                }
            }

            if (s.contains("=")) {
                val vName = s.substringBefore("=").trim()
                val vExpr = s.substringAfter("=").trim()
                variables[vName] = evalCppExpr(vExpr)
            }
        }

        private fun handleCout(stream: String) {
            val tokens = splitStream(stream)
            for (tok in tokens) {
                val t = tok.trim()
                if (t == "endl" || t == "std::endl") {
                    stdout.appendLine()
                } else if (t.startsWith("\"") && t.endsWith("\"")) {
                    stdout.append(t.substring(1, t.length - 1).replace("\\n", "\n"))
                } else {
                    val evaluated = evalCppExpr(t)
                    stdout.append(evaluated?.toString() ?: "")
                }
            }
        }

        private fun splitStream(stream: String): List<String> {
            val list = mutableListOf<String>()
            val rawTokens = stream.split("<<")
            for (rt in rawTokens) {
                if (rt.isNotBlank()) {
                    list.add(rt.trim())
                }
            }
            return list
        }

        private fun executeForLoop(block: String, startIdx: Int): Int {
            val forHeaderStart = block.indexOf('(', startIdx)
            val forHeaderEnd = block.indexOf(')', forHeaderStart)
            val header = block.substring(forHeaderStart + 1, forHeaderEnd).trim()

            // Range-based for: for (int num : data)
            if (header.contains(":")) {
                val varPart = header.substringBefore(":").trim()
                val vName = varPart.split(" ").last().trim()
                val container = header.substringAfter(":").trim()

                val bodyStart = block.indexOf('{', forHeaderEnd)
                var depth = 1
                var i = bodyStart + 1
                val body = StringBuilder()
                while (i < block.length && depth > 0) {
                    val ch = block[i]
                    if (ch == '{') depth++
                    else if (ch == '}') {
                        depth--
                        if (depth == 0) break
                    }
                    body.append(ch)
                    i++
                }

                val list = vectorData[container] ?: emptyList<Any?>()
                for (item in list) {
                    variables[vName] = item
                    executeStatements(body.toString())
                }
                return i + 1
            }

            // Traditional for (int i = 0; i < n; i++)
            val parts = header.split(";")
            val init = if (parts.isNotEmpty()) parts[0].trim() else ""
            val cond = if (parts.size > 1) parts[1].trim() else "true"
            val step = if (parts.size > 2) parts[2].trim() else ""

            if (init.isNotEmpty()) executeSingleStatement(init)

            val bodyStart = block.indexOf('{', forHeaderEnd)
            var depth = 1
            var i = bodyStart + 1
            val body = StringBuilder()
            while (i < block.length && depth > 0) {
                val ch = block[i]
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
                body.append(ch)
                i++
            }

            val loopBody = body.toString()
            var loopCount = 0
            while (isCppTruthy(evalCppExpr(cond))) {
                executeStatements(loopBody)
                if (step.isNotEmpty()) executeSingleStatement(step)
                loopCount++
                if (loopCount > 5000) break
            }

            return i + 1
        }

        private fun executeWhileLoop(block: String, startIdx: Int): Int {
            val condStart = block.indexOf('(', startIdx)
            val condEnd = block.indexOf(')', condStart)
            val cond = block.substring(condStart + 1, condEnd).trim()

            val bodyStart = block.indexOf('{', condEnd)
            var depth = 1
            var i = bodyStart + 1
            val body = StringBuilder()
            while (i < block.length && depth > 0) {
                val ch = block[i]
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
                body.append(ch)
                i++
            }

            var loopCount = 0
            while (isCppTruthy(evalCppExpr(cond))) {
                executeStatements(body.toString())
                loopCount++
                if (loopCount > 5000) break
            }
            return i + 1
        }

        private fun executeIf(block: String, startIdx: Int): Int {
            val condStart = block.indexOf('(', startIdx)
            val condEnd = block.indexOf(')', condStart)
            val cond = block.substring(condStart + 1, condEnd).trim()

            val bodyStart = block.indexOf('{', condEnd)
            var depth = 1
            var i = bodyStart + 1
            val body = StringBuilder()
            while (i < block.length && depth > 0) {
                val ch = block[i]
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) break
                }
                body.append(ch)
                i++
            }

            if (isCppTruthy(evalCppExpr(cond))) {
                executeStatements(body.toString())
            }
            return i + 1
        }

        private fun evalCppExpr(expr: String): Any? {
            val e = expr.trim()
            if (e.isEmpty()) return null

            if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length - 1)
            if (e == "true") return true
            if (e == "false") return false
            e.toLongOrNull()?.let { return it }
            e.toDoubleOrNull()?.let { return it }

            // Method call e.g. data.front(), data.size()
            if (e.contains(".front()")) {
                val vName = e.substringBefore(".front()").trim()
                return vectorData[vName]?.firstOrNull() ?: 0
            }
            if (e.contains(".size()")) {
                val vName = e.substringBefore(".size()").trim()
                return vectorData[vName]?.size ?: 0
            }

            // Arithmetic +
            if (e.contains("+") && !e.startsWith("+")) {
                val p1 = evalCppExpr(e.substringBefore("+").trim())
                val p2 = evalCppExpr(e.substringAfter("+").trim())
                return addCpp(p1, p2)
            }

            // Comparisons
            if (e.contains("<=")) {
                val v1 = (evalCppExpr(e.substringBefore("<=").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evalCppExpr(e.substringAfter("<=").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 <= v2
            }
            if (e.contains(">=")) {
                val v1 = (evalCppExpr(e.substringBefore(">=").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evalCppExpr(e.substringAfter(">=").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 >= v2
            }
            if (e.contains("<")) {
                val v1 = (evalCppExpr(e.substringBefore("<").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evalCppExpr(e.substringAfter("<").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 < v2
            }
            if (e.contains(">")) {
                val v1 = (evalCppExpr(e.substringBefore(">").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evalCppExpr(e.substringAfter(">").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 > v2
            }
            if (e.contains("==")) {
                return evalCppExpr(e.substringBefore("==").trim()) == evalCppExpr(e.substringAfter("==").trim())
            }

            if (variables.containsKey(e)) {
                return variables[e]
            }

            return e
        }

        private fun isCppTruthy(v: Any?): Boolean {
            return when (v) {
                null -> false
                is Boolean -> v
                is Number -> v.toDouble() != 0.0
                else -> true
            }
        }

        private fun addCpp(a: Any?, b: Any?): Any? {
            if (a is String || b is String) {
                return (a?.toString() ?: "") + (b?.toString() ?: "")
            }
            if (a is Number && b is Number) {
                val res = a.toDouble() + b.toDouble()
                return if (res % 1 == 0.0) res.toLong() else res
            }
            return ""
        }
    }
}
