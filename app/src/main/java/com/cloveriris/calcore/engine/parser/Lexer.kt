package com.cloveriris.calcore.engine.parser

/**
 * 词法分析器（Lexer）
 *
 * 将输入字符串转换为 Token 流。
 */
class Lexer(private val input: String) {

    private var position: Int = 0
    private var currentChar: Char? = input.getOrNull(position)

    private val constants = mapOf(
        "pi" to kotlin.math.PI,
        "π" to kotlin.math.PI,
        "e" to kotlin.math.E
    )

    private val functions = setOf(
        "sin", "cos", "tan",
        "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "log", "ln",
        "sqrt", "abs",
        "floor", "ceil", "round"
    )

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (currentChar != null) {
            val token = nextToken()
            if (token !is Token.EOF) {
                tokens.add(token)
            }
        }
        tokens.add(Token.EOF)
        return tokens
    }

    private fun nextToken(): Token {
        skipWhitespace()

        return when {
            currentChar == null -> Token.EOF
            currentChar!!.isDigit() || currentChar == '.' -> readNumber()
            currentChar!!.isLetter() -> readIdentifier()
            currentChar == '+' -> {
                advance()
                Token.Plus()
            }
            currentChar == '-' -> {
                advance()
                Token.Minus()
            }
            currentChar == '*' || currentChar == '×' -> {
                advance()
                Token.Multiply()
            }
            currentChar == '/' || currentChar == '÷' -> {
                advance()
                Token.Divide()
            }
            currentChar == '^' -> {
                advance()
                Token.Power()
            }
            currentChar == '%' -> {
                advance()
                Token.Percent()
            }
            currentChar == '(' -> {
                advance()
                Token.LParen()
            }
            currentChar == ')' -> {
                advance()
                Token.RParen()
            }
            else -> {
                val ch = currentChar.toString()
                advance()
                Token.Illegal(ch)
            }
        }
    }

    private fun readNumber(): Token.Number {
        val start = position
        var hasDot = false

        while (currentChar != null && (currentChar!!.isDigit() || currentChar == '.')) {
            if (currentChar == '.') {
                if (hasDot) break // 第二个小数点，停止
                hasDot = true
            }
            advance()
        }

        val literal = input.substring(start, position)
        val value = literal.toDoubleOrNull() ?: 0.0
        return Token.Number(literal, value)
    }

    private fun readIdentifier(): Token {
        val start = position
        while (currentChar != null && currentChar!!.isLetter()) {
            advance()
        }

        val literal = input.substring(start, position).lowercase()

        return when {
            literal in constants -> Token.Constant(literal, constants[literal]!!)
            literal in functions -> Token.Function(literal)
            else -> Token.Variable(literal)
        }
    }

    private fun skipWhitespace() {
        while (currentChar != null && currentChar!!.isWhitespace()) {
            advance()
        }
    }

    private fun advance() {
        position++
        currentChar = input.getOrNull(position)
    }
}
