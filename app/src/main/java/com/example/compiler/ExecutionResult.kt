package com.example.compiler

data class ExecutionResult(
    val output: String,
    val compilerLog: String,
    val executionTimeMs: Long,
    val isSuccess: Boolean,
    val exitCode: Int,
    val memoryUsageKb: Long
)
