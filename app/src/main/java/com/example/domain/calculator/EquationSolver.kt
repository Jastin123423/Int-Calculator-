package com.example.domain.calculator

import kotlin.math.sqrt

data class EquationSolution(
    val equation: String,
    val solutions: List<String>,
    val steps: List<String>,
    val isSolved: Boolean,
    val errorMessage: String? = null
)

object EquationSolver {

    fun solve(rawEquation: String): EquationSolution {
        val eq = rawEquation.replace(" ", "").lowercase()
        if (!eq.contains("=")) {
            return EquationSolution(
                equation = rawEquation,
                solutions = emptyList(),
                steps = emptyList(),
                isSolved = false,
                errorMessage = "Equation must contain an '=' sign (e.g. 2x + 5 = 15)"
            )
        }

        val sides = eq.split("=")
        if (sides.size != 2 || sides[0].isBlank() || sides[1].isBlank()) {
            return EquationSolution(
                equation = rawEquation,
                solutions = emptyList(),
                steps = emptyList(),
                isSolved = false,
                errorMessage = "Invalid equation format"
            )
        }

        val leftStr = sides[0]
        val rightStr = sides[1]

        // Check if quadratic x^2 or x2
        return if (leftStr.contains("x^2") || leftStr.contains("x²") || rightStr.contains("x^2") || rightStr.contains("x²")) {
            solveQuadratic(leftStr, rightStr, rawEquation)
        } else if (leftStr.contains("x") || rightStr.contains("x")) {
            solveLinear(leftStr, rightStr, rawEquation)
        } else {
            EquationSolution(
                equation = rawEquation,
                solutions = emptyList(),
                steps = emptyList(),
                isSolved = false,
                errorMessage = "No variable 'x' found in equation"
            )
        }
    }

    private fun solveLinear(leftStr: String, rightStr: String, rawEquation: String): EquationSolution {
        val steps = mutableListOf<String>()

        try {
            val (leftCoeff, leftConst) = parseLinearSide(leftStr)
            val (rightCoeff, rightConst) = parseLinearSide(rightStr)

            steps.add("Original equation: $leftStr = $rightStr")

            val netCoeff = leftCoeff - rightCoeff
            val netConst = rightConst - leftConst

            steps.add("Group x terms on left, constants on right: (${netCoeff})x = $netConst")

            if (netCoeff == 0.0) {
                if (netConst == 0.0) {
                    return EquationSolution(
                        equation = rawEquation,
                        solutions = listOf("Infinite solutions (x ∈ ℝ)"),
                        steps = steps,
                        isSolved = true
                    )
                } else {
                    return EquationSolution(
                        equation = rawEquation,
                        solutions = emptyList(),
                        steps = steps,
                        isSolved = false,
                        errorMessage = "No solution (0 = $netConst is false)"
                    )
                }
            }

            val xVal = netConst / netCoeff
            val formattedVal = ExpressionParser.formatNumber(xVal)
            steps.add("Divide by $netCoeff: x = $formattedVal")

            return EquationSolution(
                equation = rawEquation,
                solutions = listOf("x = $formattedVal"),
                steps = steps,
                isSolved = true
            )
        } catch (e: Exception) {
            return EquationSolution(
                equation = rawEquation,
                solutions = emptyList(),
                steps = emptyList(),
                isSolved = false,
                errorMessage = "Could not parse linear equation. Try format like: 2x + 5 = 15"
            )
        }
    }

    private fun parseLinearSide(str: String): Pair<Double, Double> {
        var s = str.replace("-", "+-")
        if (s.startsWith("+")) s = s.substring(1)
        val tokens = s.split("+").filter { it.isNotBlank() }

        var coeff = 0.0
        var constant = 0.0

        for (token in tokens) {
            if (token.contains("x")) {
                val numPart = token.replace("x", "")
                coeff += when (numPart) {
                    "" -> 1.0
                    "-" -> -1.0
                    else -> numPart.toDoubleOrNull() ?: 1.0
                }
            } else {
                constant += token.toDoubleOrNull() ?: 0.0
            }
        }

        return Pair(coeff, constant)
    }

    private fun solveQuadratic(leftStr: String, rightStr: String, rawEquation: String): EquationSolution {
        val steps = mutableListOf<String>()

        try {
            // Simplified ax^2 + bx + c = 0 parser
            // Form: x^2 - 4 = 0 or 2x^2 + 4x - 6 = 0
            steps.add("Original equation: $leftStr = $rightStr")
            steps.add("Rearrange into standard form ax² + bx + c = 0")

            // Simple pattern extraction
            var a = 1.0
            var b = 0.0
            var c = 0.0

            if (leftStr.contains("x^2") || leftStr.contains("x²")) {
                val clean = leftStr.replace("x²", "x^2")
                val parts = clean.split("x^2")
                val aPart = parts[0]
                a = when (aPart) {
                    "" -> 1.0
                    "-" -> -1.0
                    else -> aPart.toDoubleOrNull() ?: 1.0
                }
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    val rem = parts[1]
                    val (bCoeff, cConst) = parseLinearSide(rem)
                    b = bCoeff
                    c = cConst
                }
            }

            val rightVal = rightStr.toDoubleOrNull() ?: 0.0
            c -= rightVal

            steps.add("Coefficients: a = $a, b = $b, c = $c")

            val discriminant = b * b - 4 * a * c
            steps.add("Discriminant Δ = b² - 4ac = ${b}² - 4($a)($c) = $discriminant")

            if (discriminant < 0) {
                val realPart = -b / (2 * a)
                val imagPart = sqrt(-discriminant) / (2 * a)
                val realStr = ExpressionParser.formatNumber(realPart)
                val imagStr = ExpressionParser.formatNumber(imagPart)
                val sol1 = "$realStr + ${imagStr}i"
                val sol2 = "$realStr - ${imagStr}i"
                steps.add("Δ < 0: Complex roots")

                return EquationSolution(
                    equation = rawEquation,
                    solutions = listOf("x₁ = $sol1", "x₂ = $sol2"),
                    steps = steps,
                    isSolved = true
                )
            } else if (discriminant == 0.0) {
                val x = -b / (2 * a)
                val formattedX = ExpressionParser.formatNumber(x)
                steps.add("Δ = 0: Single double root x = -b / (2a)")

                return EquationSolution(
                    equation = rawEquation,
                    solutions = listOf("x = $formattedX"),
                    steps = steps,
                    isSolved = true
                )
            } else {
                val x1 = (-b + sqrt(discriminant)) / (2 * a)
                val x2 = (-b - sqrt(discriminant)) / (2 * a)
                val str1 = ExpressionParser.formatNumber(x1)
                val str2 = ExpressionParser.formatNumber(x2)
                steps.add("Δ > 0: Two real roots x = (-b ± √Δ) / 2a")

                return EquationSolution(
                    equation = rawEquation,
                    solutions = listOf("x₁ = $str1", "x₂ = $str2"),
                    steps = steps,
                    isSolved = true
                )
            }
        } catch (e: Exception) {
            return EquationSolution(
                equation = rawEquation,
                solutions = emptyList(),
                steps = emptyList(),
                isSolved = false,
                errorMessage = "Could not solve quadratic equation. Try format: x² - 4 = 0"
            )
        }
    }
}
