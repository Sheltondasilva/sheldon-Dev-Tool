package com.example

import com.example.compiler.CppEngine
import com.example.compiler.JavaEngine
import com.example.compiler.PythonEngine
import com.example.compiler.SyntaxHighlighter
import com.example.model.CodeLanguage
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPythonEngineExecution() {
        val engine = PythonEngine()
        val code = """
x = 10
y = 20
total = x + y
print("Sum is:", total)
""".trimIndent()
        val result = engine.execute(code)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Sum is: 30"))
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testJavaEngineExecution() {
        val engine = JavaEngine()
        val code = """
public class Main {
    public static void main(String[] args) {
        int a = 5;
        int b = 15;
        System.out.println("Result: " + (a + b));
    }
}
""".trimIndent()
        val result = engine.execute(code)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Result: 20"))
    }

    @Test
    fun testCppEngineExecution() {
        val engine = CppEngine()
        val code = """
#include <iostream>
#include <vector>
using namespace std;

int main() {
    vector<int> nums = {1, 2, 3};
    cout << "Vector elements size: " << nums.size() << endl;
    return 0;
}
""".trimIndent()
        val result = engine.execute(code)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Vector elements size: 3"))
    }

    @Test
    fun testSyntaxHighlighter() {
        val code = "def test_func():\n    return 42"
        val highlighted = SyntaxHighlighter.highlight(code, CodeLanguage.PYTHON)
        assertNotNull(highlighted)
        assertEquals(code, highlighted.text)
    }

    @Test
    fun testLanguageLocalization() {
        val es = com.example.model.LanguageManager.getStrings(com.example.model.AppLanguage.SPANISH)
        assertEquals("Ajustes", es.tabSettings)
        val ja = com.example.model.LanguageManager.getStrings(com.example.model.AppLanguage.JAPANESE)
        assertEquals("設定", ja.tabSettings)
        val en = com.example.model.LanguageManager.getStrings(com.example.model.AppLanguage.ENGLISH)
        assertEquals("Settings", en.tabSettings)
    }

    @Test
    fun testPerformanceMetricsDefaults() {
        val metrics = com.example.ui.monitor.PerformanceMetrics(
            cpuUsagePct = 25,
            ramUsedMb = 2048,
            ramTotalMb = 4096,
            ramUsagePct = 50,
            batteryPct = 85,
            batteryTempC = 28.5f
        )
        assertEquals(25, metrics.cpuUsagePct)
        assertEquals(50, metrics.ramUsagePct)
        assertEquals(85, metrics.batteryPct)
        assertEquals(28.5f, metrics.batteryTempC, 0.01f)
    }

    @Test
    fun testPerformanceCsvFormatting() {
        val record = com.example.ui.monitor.PerformanceLogRecord(
            timestamp = 1700000000000L,
            timeFormatted = "2026-10-04 12:00:00",
            cpuUsagePct = 34,
            cpuCores = 8,
            activeThreads = 15,
            ramUsedMb = 2048,
            ramTotalMb = 4096,
            ramUsagePct = 50,
            jvmHeapUsedMb = 32,
            batteryPct = 90,
            batteryVoltageMv = 4150,
            batteryTempC = 27.5f,
            batteryStatus = "Discharging",
            batteryPlugType = "Unplugged",
            rxSpeedKbps = 120.5f,
            txSpeedKbps = 15.0f
        )
        val csvHeader = "Timestamp,DateTime,CPU_Usage_Percent,CPU_Cores,Active_Threads,RAM_Used_MB,RAM_Total_MB,RAM_Usage_Percent,JVM_Heap_Used_MB,Battery_Percent,Battery_Voltage_mV,Battery_Temp_C,Battery_Status,Battery_Plug_Type,Network_Rx_KBps,Network_Tx_KBps"
        val csvRow = "${record.timestamp},\"${record.timeFormatted}\",${record.cpuUsagePct},${record.cpuCores},${record.activeThreads},${record.ramUsedMb},${record.ramTotalMb},${record.ramUsagePct},${record.jvmHeapUsedMb},${record.batteryPct},${record.batteryVoltageMv},${record.batteryTempC},\"${record.batteryStatus}\",\"${record.batteryPlugType}\",${record.rxSpeedKbps},${record.txSpeedKbps}"
        
        assertTrue(csvHeader.contains("CPU_Usage_Percent"))
        assertTrue(csvHeader.contains("Battery_Temp_C"))
        assertTrue(csvRow.contains("2026-10-04 12:00:00"))
        assertTrue(csvRow.contains("34,8,15,2048"))
    }
}
