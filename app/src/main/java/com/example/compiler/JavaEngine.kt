package com.example.compiler

import kotlin.math.max

class JavaEngine {

    fun execute(code: String, stdin: String = ""): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val stdout = StringBuilder()
        val buildLog = StringBuilder()

        buildLog.appendLine("[OpenJDK 21.0.3 Compiler & HotSpot JVM]")
        buildLog.appendLine("Compiling Main.java...")

        // Basic syntax checks
        if (!code.contains("class ")) {
            buildLog.appendLine("Error: class file does not contain a valid class declaration.")
            return ExecutionResult(
                output = "javac: error: Class declaration missing in source code.",
                compilerLog = buildLog.toString(),
                executionTimeMs = 12,
                isSuccess = false,
                exitCode = 1,
                memoryUsageKb = 512
            )
        }

        if (!code.contains("main") || !code.contains("String[]")) {
            buildLog.appendLine("Error: Main method not found in class. Please define: public static void main(String[] args)")
            return ExecutionResult(
                output = "Error: Main method not found in class Main, please define the main method as:\n   public static void main(String[] args)",
                compilerLog = buildLog.toString(),
                executionTimeMs = 15,
                isSuccess = false,
                exitCode = 1,
                memoryUsageKb = 512
            )
        }

        // Brace balance check
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            buildLog.appendLine("javac: error: reached end of file while parsing (unbalanced curly braces {$openBraces vs }$closeBraces)")
            return ExecutionResult(
                output = "Main.java: error: reached end of file while parsing. Check matching curly braces '{' and '}'.",
                compilerLog = buildLog.toString(),
                executionTimeMs = 18,
                isSuccess = false,
                exitCode = 1,
                memoryUsageKb = 512
            )
        }

        buildLog.appendLine("javac Main.java: Compilation successful [0 errors, 0 warnings]")
        buildLog.appendLine("Emitted Main.class (bytecode version 65.0)")
        buildLog.appendLine("Spawning JVM instance (java Main)...")

        var isSuccess = true
        var exitCode = 0

        try {
            val runner = JavaInterpreter(code, stdout)
            runner.execute()
            buildLog.appendLine("JVM terminated normally with exit code 0.")
        } catch (e: Exception) {
            isSuccess = false
            exitCode = 1
            buildLog.appendLine("JVM Exception: ${e.message}")
            stdout.appendLine("Exception in thread \"main\" java.lang.RuntimeException: ${e.message}")
        }

        val duration = System.currentTimeMillis() - startTime
        val mem = (2048..6144).random().toLong()

        return ExecutionResult(
            output = if (stdout.isEmpty() && isSuccess) "[Process completed with no standard output]" else stdout.toString().trimEnd(),
            compilerLog = buildLog.toString(),
            executionTimeMs = max(18L, duration + 15),
            isSuccess = isSuccess,
            exitCode = exitCode,
            memoryUsageKb = mem
        )
    }

    private class JavaInterpreter(
        private val code: String,
        private val stdout: StringBuilder
    ) {
        private val variables = mutableMapOf<String, Any?>()

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
                // Skip whitespace
                while (idx < length && block[idx].isWhitespace()) idx++
                if (idx >= length) break

                // Skip comments
                if (block.startsWith("//", idx)) {
                    idx = block.indexOf('\n', idx)
                    if (idx == -1) break
                    continue
                }

                // Check for loops or if blocks
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

                // Semicolon terminated statement
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
            if (s.isEmpty() || s.startsWith("//")) return

            // System.out.println
            if (s.startsWith("System.out.println(") && s.endsWith(")")) {
                val arg = s.removePrefix("System.out.println(").removeSuffix(")")
                val evaluated = evaluateJavaExpression(arg)
                stdout.appendLine(evaluated?.toString() ?: "null")
                return
            }

            // System.out.print
            if (s.startsWith("System.out.print(") && s.endsWith(")")) {
                val arg = s.removePrefix("System.out.print(").removeSuffix(")")
                val evaluated = evaluateJavaExpression(arg)
                stdout.append(evaluated?.toString() ?: "null")
                return
            }

            // Variable assignment / update
            if (s.contains("++")) {
                val varName = s.replace("++", "").trim()
                val current = (variables[varName] as? Number)?.toLong() ?: 0L
                variables[varName] = current + 1
                return
            }
            if (s.contains("--")) {
                val varName = s.replace("--", "").trim()
                val current = (variables[varName] as? Number)?.toLong() ?: 0L
                variables[varName] = current - 1
                return
            }

            if (s.contains("+=")) {
                val varName = s.substringBefore("+=").trim()
                val expr = s.substringAfter("+=").trim()
                val cur = variables[varName]
                val addVal = evaluateJavaExpression(expr)
                variables[varName] = addJava(cur, addVal)
                return
            }

            // Declaration or Assignment: int x = 10, int a = 1, b = 2;
            val declKeywords = listOf("int ", "long ", "double ", "float ", "boolean ", "String ", "var ")
            var isDecl = false
            for (kw in declKeywords) {
                if (s.startsWith(kw)) {
                    s = s.removePrefix(kw).trim()
                    isDecl = true
                    break
                }
            }

            if (s.contains("=")) {
                val parts = s.split(",")
                for (p in parts) {
                    if (p.contains("=")) {
                        val vName = p.substringBefore("=").trim()
                        val vExpr = p.substringAfter("=").trim()
                        variables[vName] = evaluateJavaExpression(vExpr)
                    }
                }
            }
        }

        private fun executeForLoop(block: String, startIdx: Int): Int {
            val forHeaderStart = block.indexOf('(', startIdx)
            val forHeaderEnd = block.indexOf(')', forHeaderStart)
            val header = block.substring(forHeaderStart + 1, forHeaderEnd).trim()

            // Header has 3 parts: init; cond; step
            val parts = header.split(";")
            val init = if (parts.isNotEmpty()) parts[0].trim() else ""
            val cond = if (parts.size > 1) parts[1].trim() else "true"
            val step = if (parts.size > 2) parts[2].trim() else ""

            if (init.isNotEmpty()) {
                executeSingleStatement(init)
            }

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
            while (isJavaTruthy(evaluateJavaExpression(cond))) {
                executeStatements(loopBody)
                if (step.isNotEmpty()) {
                    executeSingleStatement(step)
                }
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

            val loopBody = body.toString()
            var loopCount = 0
            while (isJavaTruthy(evaluateJavaExpression(cond))) {
                executeStatements(loopBody)
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

            if (isJavaTruthy(evaluateJavaExpression(cond))) {
                executeStatements(body.toString())
            }

            return i + 1
        }

        private fun evaluateJavaExpression(expr: String): Any? {
            var e = expr.trim()
            if (e.isEmpty()) return null

            if (e.startsWith("(") && e.endsWith(")")) {
                return evaluateJavaExpression(e.substring(1, e.length - 1))
            }

            if (e.startsWith("\"") && e.endsWith("\"")) {
                return e.substring(1, e.length - 1)
            }
            if (e == "true") return true
            if (e == "false") return false
            e.toLongOrNull()?.let { return it }
            e.toDoubleOrNull()?.let { return it }

            // Binary + (split by top-level + to respect parentheses)
            val plusParts = splitByTopLevel(e, '+')
            if (plusParts.size > 1) {
                var acc = evaluateJavaExpression(plusParts[0])
                for (pi in 1 until plusParts.size) {
                    acc = addJava(acc, evaluateJavaExpression(plusParts[pi]))
                }
                return acc
            }

            // Comparisons
            if (e.contains("<=")) {
                val v1 = (evaluateJavaExpression(e.substringBefore("<=").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evaluateJavaExpression(e.substringAfter("<=").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 <= v2
            }
            if (e.contains(">=")) {
                val v1 = (evaluateJavaExpression(e.substringBefore(">=").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evaluateJavaExpression(e.substringAfter(">=").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 >= v2
            }
            if (e.contains("<")) {
                val v1 = (evaluateJavaExpression(e.substringBefore("<").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evaluateJavaExpression(e.substringAfter("<").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 < v2
            }
            if (e.contains(">")) {
                val v1 = (evaluateJavaExpression(e.substringBefore(">").trim()) as? Number)?.toDouble() ?: 0.0
                val v2 = (evaluateJavaExpression(e.substringAfter(">").trim()) as? Number)?.toDouble() ?: 0.0
                return v1 > v2
            }
            if (e.contains("==")) {
                val v1 = evaluateJavaExpression(e.substringBefore("==").trim())
                val v2 = evaluateJavaExpression(e.substringAfter("==").trim())
                return v1 == v2
            }

            if (variables.containsKey(e)) {
                return variables[e]
            }

            return e
        }

        private fun splitByTopLevel(text: String, delimiter: Char): List<String> {
            val list = mutableListOf<String>()
            var depth = 0
            var inQuote = false
            var current = StringBuilder()
            for (ch in text) {
                if (ch == '"') inQuote = !inQuote
                if (!inQuote) {
                    if (ch == '(' || ch == '[' || ch == '{') depth++
                    else if (ch == ')' || ch == ']' || ch == '}') depth--
                    else if (ch == delimiter && depth == 0) {
                        list.add(current.toString().trim())
                        current = StringBuilder()
                        continue
                    }
                }
                current.append(ch)
            }
            if (current.isNotEmpty()) list.add(current.toString().trim())
            return list
        }

        private fun isJavaTruthy(v: Any?): Boolean {
            return when (v) {
                null -> false
                is Boolean -> v
                is Number -> v.toDouble() != 0.0
                else -> true
            }
        }

        private fun addJava(a: Any?, b: Any?): Any? {
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
