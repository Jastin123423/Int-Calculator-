package com.example.domain.datetime

import java.util.Calendar
import java.util.concurrent.TimeUnit

data class DateDiffResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long
)

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long
)

object DateCalculator {

    fun calculateDateDifference(startMillis: Long, endMillis: Long): DateDiffResult {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        if (startCal.after(endCal)) {
            return calculateDateDifference(endMillis, startMillis)
        }

        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        var months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        var days = endCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months -= 1
            val tempCal = Calendar.getInstance().apply {
                timeInMillis = startMillis
                add(Calendar.MONTH, 1)
            }
            val maxDaysInPrevMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            days += maxDaysInPrevMonth
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        val totalDays = TimeUnit.MILLISECONDS.toDays(endMillis - startMillis)

        return DateDiffResult(years, months, days, totalDays)
    }

    fun addDays(startMillis: Long, days: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    fun calculateAge(birthdateMillis: Long, currentMillis: Long = System.currentTimeMillis()): AgeResult {
        val diff = calculateDateDifference(birthdateMillis, currentMillis)
        val millisDiff = currentMillis - birthdateMillis
        val totalHours = TimeUnit.MILLISECONDS.toHours(millisDiff)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millisDiff)

        return AgeResult(
            years = diff.years,
            months = diff.months,
            days = diff.days,
            totalDays = diff.totalDays,
            totalHours = totalHours,
            totalMinutes = totalMinutes
        )
    }
}
