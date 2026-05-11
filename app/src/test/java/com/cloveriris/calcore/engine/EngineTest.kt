package com.cloveriris.calcore.engine

import com.cloveriris.calcore.engine.evaluator.Evaluator
import com.cloveriris.calcore.engine.parser.Lexer
import com.cloveriris.calcore.engine.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class EngineTest {

    private fun evaluate(expression: String): Double {
        val lexer = Lexer(expression)
        val tokens = lexer.tokenize()
        val parser = Parser(tokens)
        val ast = parser.parse() ?: throw IllegalStateException("Parse failed")
        return Evaluator.evaluate(ast)
    }

    @Test
    fun `addition`() {
        assertEquals(5.0, evaluate("2 + 3"), 0.0001)
    }

    @Test
    fun `subtraction`() {
        assertEquals(3.0, evaluate("7 - 4"), 0.0001)
    }

    @Test
    fun `multiplication`() {
        assertEquals(12.0, evaluate("3 × 4"), 0.0001)
        assertEquals(12.0, evaluate("3 * 4"), 0.0001)
    }

    @Test
    fun `division`() {
        assertEquals(2.5, evaluate("5 ÷ 2"), 0.0001)
        assertEquals(2.5, evaluate("5 / 2"), 0.0001)
    }

    @Test
    fun `operator precedence`() {
        assertEquals(14.0, evaluate("2 + 3 × 4"), 0.0001)
        assertEquals(20.0, evaluate("(2 + 3) × 4"), 0.0001)
    }

    @Test
    fun `power`() {
        assertEquals(8.0, evaluate("2 ^ 3"), 0.0001)
        assertEquals(9.0, evaluate("3 ^ 2"), 0.0001)
    }

    @Test
    fun `negative number`() {
        assertEquals(-5.0, evaluate("-5"), 0.0001)
        assertEquals(-3.0, evaluate("2 + -5"), 0.0001)
    }

    @Test
    fun `decimal numbers`() {
        assertEquals(3.14, evaluate("1.14 + 2"), 0.0001)
        assertEquals(0.3, evaluate("0.1 + 0.2"), 0.0001)
    }

    @Test
    fun `nested parentheses`() {
        assertEquals(21.0, evaluate("(1 + 2) × (3 + 4)"), 0.0001)
    }

    @Test
    fun `pi constant`() {
        assertEquals(PI, evaluate("π"), 0.0001)
        assertEquals(PI, evaluate("pi"), 0.0001)
    }

    @Test
    fun `e constant`() {
        assertEquals(kotlin.math.E, evaluate("e"), 0.0001)
    }

    @Test
    fun `sin function`() {
        assertEquals(1.0, evaluate("sin(π / 2)"), 0.0001)
        assertEquals(0.0, evaluate("sin(0)"), 0.0001)
    }

    @Test
    fun `cos function`() {
        assertEquals(1.0, evaluate("cos(0)"), 0.0001)
    }

    @Test
    fun `sqrt function`() {
        assertEquals(3.0, evaluate("sqrt(9)"), 0.0001)
    }

    @Test
    fun `log function`() {
        assertEquals(2.0, evaluate("log(100)"), 0.0001)
    }

    @Test
    fun `ln function`() {
        assertEquals(1.0, evaluate("ln(e)"), 0.0001)
    }

    @Test
    fun `complex expression`() {
        assertEquals(10.0, evaluate("2 × 3 + 4"), 0.0001)
        assertEquals(14.0, evaluate("2 × (3 + 4)"), 0.0001)
    }

    @Test
    fun `function with constant`() {
        assertEquals(0.0, evaluate("sin π"), 0.0001)
    }
}
