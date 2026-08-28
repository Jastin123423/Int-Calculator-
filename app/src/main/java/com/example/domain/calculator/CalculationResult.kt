package com.example.domain.calculator

sealed class CalculationResult {
    data class Success(val value: Double, val formattedValue: String) : CalculationResult()
    data class Error(val message: String) : CalculationResult()
}
