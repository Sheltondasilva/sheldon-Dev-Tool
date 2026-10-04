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
}
