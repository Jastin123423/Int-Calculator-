package com.example.domain.calculator

data class CalculationStep(
    val stepNumber: Int,
    val description: String,
    val subExpression: String,
    val intermediateResult: String
)

object StepByStepSolver {

    fun generateSteps(expression: String, angleUnit: AngleUnit = AngleUnit.DEG): List<CalculationStep> {
        val steps = mutableListOf<CalculationStep>()
        if (expression.isBlank()) return steps

        val calcResult = ExpressionParser.evaluate(expression, angleUnit)
        if (calcResult !is CalculationResult.Success) {
            return steps
        }

        // Generate high-level steps breakdown
        val parts = expression.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 3) {
            var stepNum = 1
            // Simple order of operations breakdown illustration
            steps.add(
                CalculationStep(
                    stepNumber = stepNum++,
                    description = "Evaluate primary operations (multiplication, division, functions)",
                    subExpression = expression,
                    intermediateResult = "Parsed expression"
                )
            )
            steps.add(
                CalculationStep(
                    stepNumber = stepNum,
                    description = "Compute final result following PEMDAS rules",
                    subExpression = expression,
                    intermediateResult = calcResult.formattedValue
                )
            )
        } else {
            steps.add(
                CalculationStep(
                    stepNumber = 1,
                    description = "Evaluate expression",
                    subExpression = expression,
                    intermediateResult = calcResult.formattedValue
                )
            )
        }

        return steps
    }
}
