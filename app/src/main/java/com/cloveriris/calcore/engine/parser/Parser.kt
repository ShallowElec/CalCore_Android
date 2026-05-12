package com.cloveriris.calcore.engine.parser

/**
 * 语法分析器（Parser）
 *
 * 将 Token 流转换为抽象语法树（AST）。
 * 使用递归下降解析，支持运算符优先级。
 */
class Parser(private val tokens: List<Token>) {

    private var position: Int = 0
    private val currentToken: Token
        get() = tokens.getOrElse(position) { Token.EOF }

    fun parse(): Expression? {
        return try {
            parseExpression()
        } catch (e: ParseException) {
            null
        }
    }

    /**
     * expression = term { ("+" | "-") term }
     */
    private fun parseExpression(): Expression {
        var left = parseTerm()
        while (currentToken is Token.Plus || currentToken is Token.Minus) {
            val op = when (currentToken) {
                is Token.Plus -> BinaryOperator.ADD
                is Token.Minus -> BinaryOperator.SUBTRACT
                else -> break
            }
            advance()
            val right = parseTerm()
            left = Expression.Binary(left, op, right)
        }
        return left
    }

    /**
     * term = factor { ("×" | "÷") factor }
     */
    private fun parseTerm(): Expression {
        var left = parseFactor()
        while (currentToken is Token.Multiply || currentToken is Token.Divide) {
            val op = when (currentToken) {
                is Token.Multiply -> BinaryOperator.MULTIPLY
                is Token.Divide -> BinaryOperator.DIVIDE
                else -> break
            }
            advance()
            val right = parseFactor()
            left = Expression.Binary(left, op, right)
        }
        return left
    }

    /**
     * factor = power { "^" power }
     */
    private fun parseFactor(): Expression {
        var left = parsePower()
        while (currentToken is Token.Power) {
            advance()
            val right = parsePower()
            left = Expression.Binary(left, BinaryOperator.POWER, right)
        }
        return left
    }

    /**
     * power = postfix
     */
    private fun parsePower(): Expression {
        return parsePostfix()
    }

    /**
     * postfix = unary { "!" }
     *
     * 阶乘是后缀一元运算符，优先级高于幂运算。
     */
    private fun parsePostfix(): Expression {
        var expr = parseUnary()
        while (currentToken is Token.Factorial) {
            advance()
            expr = Expression.Unary(UnaryOperator.FACTORIAL, expr)
        }
        return expr
    }

    /**
     * unary = ("-" | "%") primary | primary
     */
    private fun parseUnary(): Expression {
        return when (currentToken) {
            is Token.Minus -> {
                advance()
                Expression.Unary(UnaryOperator.NEGATE, parsePrimary())
            }
            is Token.Percent -> {
                advance()
                Expression.Unary(UnaryOperator.PERCENT, parsePrimary())
            }
            else -> parsePrimary()
        }
    }

    /**
     * primary = number | constant | function | "(" expression ")"
     */
    private fun parsePrimary(): Expression {
        return when (val token = currentToken) {
            is Token.Number -> {
                advance()
                Expression.NumberLiteral(token.value)
            }
            is Token.Constant -> {
                advance()
                Expression.ConstantRef(token.literal, token.value)
            }
            is Token.Variable -> {
                advance()
                Expression.VariableRef(token.literal)
            }
            is Token.Function -> {
                advance()
                if (currentToken is Token.LParen) {
                    advance() // consume "("
                    val arg = parseExpression()
                    expect(Token.RParen::class.java, "Expected ')' after function argument")
                    Expression.FunctionCall(token.literal, arg)
                } else {
                    // 无括号函数调用，如 sin π
                    val arg = parsePrimary()
                    Expression.FunctionCall(token.literal, arg)
                }
            }
            is Token.LParen -> {
                advance() // consume "("
                val expr = parseExpression()
                expect(Token.RParen::class.java, "Expected ')'")
                expr
            }
            is Token.Plus -> {
                // 一元正号，直接跳过
                advance()
                parsePrimary()
            }
            else -> {
                throw ParseException("Unexpected token: $token")
            }
        }
    }

    private fun advance() {
        if (position < tokens.size - 1) {
            position++
        }
    }

    private fun <T : Token> expect(tokenClass: Class<T>, message: String) {
        if (!tokenClass.isInstance(currentToken)) {
            throw ParseException(message)
        }
        advance()
    }

    class ParseException(message: String) : Exception(message)
}
