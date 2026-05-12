package com.cloveriris.calcore.domain.usecase

import com.cloveriris.calcore.engine.evaluator.Evaluator
import com.cloveriris.calcore.engine.parser.Expression
import com.cloveriris.calcore.engine.parser.Lexer
import com.cloveriris.calcore.engine.parser.Parser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluateUseCase @Inject constructor() {

    fun evaluate(expression: String, variables: Map<String, Double> = emptyMap()): Result<Double> {
        return try {
            val lexer = Lexer(expression)
            val tokens = lexer.tokenize()

            val parser = Parser(tokens)
            val ast = parser.parse()
                ?: return Result.failure(IllegalArgumentException("Failed to parse expression"))

            val result = Evaluator.evaluate(ast, variables)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parse(expression: String): Result<Expression> {
        return try {
            val lexer = Lexer(expression)
            val tokens = lexer.tokenize()

            val parser = Parser(tokens)
            val ast = parser.parse()
                ?: return Result.failure(IllegalArgumentException("Failed to parse expression"))

            Result.success(ast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
