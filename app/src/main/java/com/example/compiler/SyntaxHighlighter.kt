package com.example.compiler

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.model.CodeLanguage

object SyntaxHighlighter {

    // Hacker / Dev Editor Syntax Colors
    private val KeywordColor = Color(0xFFFF79C6)       // Pink / Magenta
    private val TypeColor = Color(0xFF8BE9FD)          // Cyan
    private val FunctionColor = Color(0xFF50FA7B)      // Bright Green
    private val StringColor = Color(0xFFF1FA8C)        // Soft Yellow
    private val NumberColor = Color(0xFFBD93F9)        // Soft Purple
    private val CommentColor = Color(0xFF6272A4)       // Muted Slate Blue
    private val PreprocessorColor = Color(0xFFFFB86C)  // Orange
    private val OperatorColor = Color(0xFFFF5555)      // Coral / Red

    private val pythonKeywords = setOf(
        "def", "return", "if", "elif", "else", "for", "while", "in", "import", "from",
        "as", "class", "try", "except", "finally", "with", "lambda", "yield", "pass",
        "break", "continue", "global", "nonlocal", "assert", "del", "raise", "True", "False", "None"
    )

    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null", "var"
    )

    private val cppKeywords = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else",
        "enum", "extern", "float", "for", "goto", "if", "int", "long", "register", "return",
        "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned",
        "void", "volatile", "while", "class", "namespace", "using", "public", "private", "protected",
        "template", "typename", "try", "catch", "throw", "new", "delete", "nullptr", "bool", "true", "false"
    )

    fun highlight(code: String, language: CodeLanguage): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            val keywords = when (language) {
                CodeLanguage.PYTHON -> pythonKeywords
                CodeLanguage.JAVA -> javaKeywords
                CodeLanguage.CPP -> cppKeywords
            }

            var i = 0
            val len = code.length

            while (i < len) {
                val ch = code[i]

                // Line comment in Python (#...)
                if (language == CodeLanguage.PYTHON && ch == '#') {
                    val end = code.indexOf('\n', i).let { if (it == -1) len else it }
                    safeAddStyle(SpanStyle(color = CommentColor, fontWeight = FontWeight.Normal), i, end, len)
                    i = end
                    continue
                }

                // C++ / Java single-line comment (//...)
                if ((language == CodeLanguage.JAVA || language == CodeLanguage.CPP) && ch == '/' && i + 1 < len && code[i + 1] == '/') {
                    val end = code.indexOf('\n', i).let { if (it == -1) len else it }
                    safeAddStyle(SpanStyle(color = CommentColor), i, end, len)
                    i = end
                    continue
                }

                // C++ / Java multi-line comment (/*...*/)
                if ((language == CodeLanguage.JAVA || language == CodeLanguage.CPP) && ch == '/' && i + 1 < len && code[i + 1] == '*') {
                    val end = code.indexOf("*/", i + 2).let { if (it == -1) len else (it + 2).coerceAtMost(len) }
                    safeAddStyle(SpanStyle(color = CommentColor), i, end, len)
                    i = end
                    continue
                }

                // C++ Preprocessor (#include, #define)
                if (language == CodeLanguage.CPP && ch == '#') {
                    val end = code.indexOf('\n', i).let { if (it == -1) len else it }
                    safeAddStyle(SpanStyle(color = PreprocessorColor, fontWeight = FontWeight.SemiBold), i, end, len)
                    i = end
                    continue
                }

                // String literals ("..." or '...')
                if (ch == '"' || ch == '\'') {
                    val quote = ch
                    var end = i + 1
                    while (end < len) {
                        if (code[end] == quote && code[end - 1] != '\\') {
                            end++
                            break
                        }
                        if (code[end] == '\n') break
                        end++
                    }
                    safeAddStyle(SpanStyle(color = StringColor), i, end, len)
                    i = end
                    continue
                }

                // Number literals
                if (ch.isDigit()) {
                    var end = i + 1
                    while (end < len && (code[end].isDigit() || code[end] == '.' || code[end] == 'x' || code[end] == 'X' || code[end] == 'f' || code[end] == 'L')) {
                        end++
                    }
                    safeAddStyle(SpanStyle(color = NumberColor, fontWeight = FontWeight.Medium), i, end, len)
                    i = end
                    continue
                }

                // Identifiers & Keywords
                if (ch.isLetter() || ch == '_') {
                    var end = i + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) {
                        end++
                    }
                    val word = code.substring(i, end)

                    if (keywords.contains(word)) {
                        safeAddStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold), i, end, len)
                    } else if (word.first().isUpperCase()) {
                        safeAddStyle(SpanStyle(color = TypeColor, fontWeight = FontWeight.Medium), i, end, len)
                    } else if (end < len && code[end] == '(') {
                        safeAddStyle(SpanStyle(color = FunctionColor), i, end, len)
                    }
                    i = end
                    continue
                }

                // Operators
                if (ch in setOf('+', '-', '*', '/', '%', '=', '!', '<', '>', '&', '|', '^', '~', '?', ':')) {
                    safeAddStyle(SpanStyle(color = OperatorColor), i, (i + 1).coerceAtMost(len), len)
                }

                i++
            }
        }
    }

    private fun AnnotatedString.Builder.safeAddStyle(style: SpanStyle, start: Int, end: Int, len: Int) {
        if (start in 0 until end && end <= len) {
            addStyle(style, start, end)
        }
    }
}
