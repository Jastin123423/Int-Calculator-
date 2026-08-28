package com.example.domain.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

object ExpressionParser {

    fun evaluate(expression: String, angleUnit: AngleUnit = AngleUnit.DEG): CalculationResult {
        if (expression.isBlank()) {
            return CalculationResult.Success(0.0, "0")
        }

        val cleaned = sanitizeExpression(expression)
        if (cleaned.isEmpty()) {
            return CalculationResult.Error("Invalid expression")
        }

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) {
                return CalculationResult.Error("Invalid expression")
            }
            val rpn = infixToRPN(tokens)
            val rawResult = evaluateRPN(rpn, angleUnit)

            if (rawResult.isNaN()) {
                CalculationResult.Error("Invalid input")
            } else if (rawResult.isInfinite()) {
                CalculationResult.Error("Result is too large")
            } else {
                val formatted = formatNumber(rawResult)
                CalculationResult.Success(rawResult, formatted)
            }
        } catch (e: ArithmeticException) {
            CalculationResult.Error(e.message ?: "Calculation error")
        } catch (e: Exception) {
            CalculationResult.Error("Invalid expression")
        }
    }

    private fun sanitizeExpression(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "pi")
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace(" ", "")
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        // Avoid floating point precision glitches like 0.30000000000000004
        val bd = try {
            BigDecimal(value, MathContext.DECIMAL128)
                .setScale(12, RoundingMode.HALF_UP)
                .stripTrailingZeros()
        } catch (e: Exception) {
            BigDecimal.valueOf(value)
        }

        val doubleVal = bd.toDouble()

        // Scientific notation for extremely large or tiny numbers
        if (abs(doubleVal) >= 1e12 || (abs(doubleVal) > 0 && abs(doubleVal) < 1e-7)) {
            val df = DecimalFormat("0.######E0", DecimalFormatSymbols(Locale.US))
            return df.format(doubleVal)
        }

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val df = DecimalFormat("#,##0.###########", symbols)
        return df.format(bd)
    }

    private sealed class Token {
        data class NumberToken(val value: Double) : Token()
        data class OperatorToken(val op: String, val precedence: Int, val rightAssociative: Boolean) : Token()
        data class FunctionToken(val func: String) : Token()
        object LeftParen : Token()
        object RightParen : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        fun addImplicitMultiplyIfNeeded() {
            if (tokens.isNotEmpty()) {
                val last = tokens.last()
                if (last is Token.NumberToken || last is Token.RightParen) {
                    tokens.add(Token.OperatorToken("*", 2, false))
                }
            }
        }

        while (i < len) {
            val ch = expr[i]

            when {
                ch.isDigit() || ch == '.' -> {
                    val sb = StringBuilder()
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    val num = sb.toString().toDoubleOrNull() ?: 0.0
                    tokens.add(Token.NumberToken(num))
                    continue
                }

                ch == '(' -> {
                    addImplicitMultiplyIfNeeded()
                    tokens.add(Token.LeftParen)
                    i++
                }

                ch == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }

                ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '%' || ch == '^' || ch == '!' -> {
                    val isUnary = (ch == '+' || ch == '-') &&
                            (tokens.isEmpty() || tokens.last() is Token.LeftParen || tokens.last() is Token.OperatorToken)

                    if (isUnary) {
                        if (ch == '-') {
                            // Represent unary minus as a function 'neg'
                            addImplicitMultiplyIfNeeded()
                            tokens.add(Token.FunctionToken("neg"))
                        }
                    } else {
                        val precedence = when (ch) {
                            '!' -> 4
                            '^' -> 3
                            '*', '/', '%' -> 2
                            '+', '-' -> 1
                            else -> 1
                        }
                        val rightAssoc = ch == '^'
                        tokens.add(Token.OperatorToken(ch.toString(), precedence, rightAssoc))
                    }
                    i++
                }

                expr.startsWith("mod", i, ignoreCase = true) -> {
                    tokens.add(Token.OperatorToken("mod", 2, false))
                    i += 3
                }

                expr.startsWith("pi", i, ignoreCase = true) -> {
                    addImplicitMultiplyIfNeeded()
                    tokens.add(Token.NumberToken(Math.PI))
                    i += 2
                }

                ch == 'e' && (i + 1 == len || !expr[i + 1].isLetter()) -> {
                    addImplicitMultiplyIfNeeded()
                    tokens.add(Token.NumberToken(Math.E))
                    i++
                }

                ch.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < len && expr[i].isLetter()) {
                        sb.append(expr[i])
                        i++
                    }
                    val func = sb.toString().lowercase()
                    addImplicitMultiplyIfNeeded()
                    tokens.add(Token.FunctionToken(func))
                }

                else -> i++
            }
        }

        return tokens
    }

    private fun infixToRPN(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = mutableListOf<Token>()

        for (token in tokens) {
            when (token) {
                is Token.NumberToken -> output.add(token)
                is Token.FunctionToken -> stack.add(token)
                is Token.OperatorToken -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.OperatorToken) {
                            if ((!token.rightAssociative && token.precedence <= top.precedence) ||
                                (token.rightAssociative && token.precedence < top.precedence)
                            ) {
                                output.add(stack.removeAt(stack.size - 1))
                            } else break
                        } else if (top is Token.FunctionToken) {
                            output.add(stack.removeAt(stack.size - 1))
                        } else break
                    }
                    stack.add(token)
                }
                is Token.LeftParen -> stack.add(token)
                is Token.RightParen -> {
                    var foundLeftParen = false
                    while (stack.isNotEmpty()) {
                        val top = stack.removeAt(stack.size - 1)
                        if (top is Token.LeftParen) {
                            foundLeftParen = true
                            break
                        } else {
                            output.add(top)
                        }
                    }
                    if (stack.isNotEmpty() && stack.last() is Token.FunctionToken) {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeAt(stack.size - 1)
            if (top !is Token.LeftParen && top !is Token.RightParen) {
                output.add(top)
            }
        }

        return output
    }

    private fun evaluateRPN(tokens: List<Token>, angleUnit: AngleUnit): Double {
        val stack = mutableListOf<Double>()

        fun toRad(valInUnit: Double): Double {
            return when (angleUnit) {
                AngleUnit.DEG -> Math.toRadians(valInUnit)
                AngleUnit.RAD -> valInUnit
                AngleUnit.GRAD -> valInUnit * Math.PI / 200.0
            }
        }

        fun fromRad(valInRad: Double): Double {
            return when (angleUnit) {
                AngleUnit.DEG -> Math.toDegrees(valInRad)
                AngleUnit.RAD -> valInRad
                AngleUnit.GRAD -> valInRad * 200.0 / Math.PI
            }
        }

        fun factorial(n: Double): Double {
            if (n < 0) throw ArithmeticException("Negative factorial")
            val intN = n.toLong()
            if (n != intN.toDouble()) throw ArithmeticException("Non-integer factorial")
            if (intN > 170) throw ArithmeticException("Overflow")
            var res = 1.0
            for (i in 2..intN) {
                res *= i
            }
            return res
        }

        for (token in tokens) {
            when (token) {
                is Token.NumberToken -> stack.add(token.value)
                is Token.FunctionToken -> {
                    if (stack.isEmpty()) throw ArithmeticException("Invalid function argument")
                    val a = stack.removeAt(stack.size - 1)
                    val result = when (token.func) {
                        "neg" -> -a
                        "sin" -> sin(toRad(a))
                        "cos" -> cos(toRad(a))
                        "tan" -> {
                            val r = tan(toRad(a))
                            if (abs(r) > 1e15) throw ArithmeticException("Undefined tan")
                            r
                        }
                        "asin" -> fromRad(asin(a))
                        "acos" -> fromRad(acos(a))
                        "atan" -> fromRad(atan(a))
                        "sinh" -> sinh(a)
                        "cosh" -> cosh(a)
                        "tanh" -> tanh(a)
                        "log", "log10" -> log10(a)
                        "ln" -> ln(a)
                        "sqrt" -> {
                            if (a < 0) throw ArithmeticException("Cannot calculate square root of negative number")
                            sqrt(a)
                        }
                        "cbrt" -> cbrt(a)
                        "abs" -> abs(a)
                        "fact" -> factorial(a)
                        "exp" -> exp(a)
                        else -> throw ArithmeticException("Unknown function: ${token.func}")
                    }
                    stack.add(result)
                }
                is Token.OperatorToken -> {
                    if (token.op == "!") {
                        if (stack.isEmpty()) throw ArithmeticException("Invalid operator")
                        val a = stack.removeAt(stack.size - 1)
                        stack.add(factorial(a))
                        continue
                    }
                    if (stack.size < 2) throw ArithmeticException("Invalid expression")
                    val b = stack.removeAt(stack.size - 1)
                    val a = stack.removeAt(stack.size - 1)
                    val result = when (token.op) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> {
                            if (b == 0.0) throw ArithmeticException("Cannot divide by zero")
                            a / b
                        }
                        "%" -> {
                            // Percentage: e.g., 50 + 10% = 50 + 5, or 20% = 0.2
                            a * (b / 100.0)
                        }
                        "mod" -> a % b
                        "^" -> a.pow(b)
                        else -> throw ArithmeticException("Unknown operator: ${token.op}")
                    }
                    stack.add(result)
                }
                else -> {}
            }
        }

        if (stack.size != 1) {
            throw ArithmeticException("Invalid expression")
        }

        return stack.first()
    }
}
