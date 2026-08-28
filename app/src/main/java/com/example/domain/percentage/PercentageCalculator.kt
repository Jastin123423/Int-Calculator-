package com.example.domain.percentage

import com.example.domain.calculator.ExpressionParser
import kotlin.math.abs

object PercentageCalculator {

    fun percentageOf(percent: Double, total: Double): Double {
        return total * (percent / 100.0)
    }

    fun whatPercentage(part: Double, total: Double): Double {
        if (total == 0.0) return 0.0
        return (part / total) * 100.0
    }

    fun increaseByPercent(value: Double, percent: Double): Double {
        return value * (1.0 + percent / 100.0)
    }

    fun decreaseByPercent(value: Double, percent: Double): Double {
        return value * (1.0 - percent / 100.0)
    }

    fun percentageDifference(v1: Double, v2: Double): Double {
        val avg = (v1 + v2) / 2.0
        if (avg == 0.0) return 0.0
        return (abs(v1 - v2) / avg) * 100.0
    }

    fun percentageChange(oldVal: Double, newVal: Double): Double {
        if (oldVal == 0.0) return 0.0
        return ((newVal - oldVal) / abs(oldVal)) * 100.0
    }

    fun format(valDouble: Double): String {
        return ExpressionParser.formatNumber(valDouble)
    }
}
