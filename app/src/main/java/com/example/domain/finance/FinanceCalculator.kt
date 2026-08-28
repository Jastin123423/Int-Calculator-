package com.example.domain.finance

import com.example.domain.calculator.ExpressionParser
import kotlin.math.pow

data class LoanResult(
    val monthlyPayment: Double,
    val totalInterest: Double,
    val totalRepayment: Double
) {
    fun formattedMonthlyPayment() = ExpressionParser.formatNumber(monthlyPayment)
    fun formattedTotalInterest() = ExpressionParser.formatNumber(totalInterest)
    fun formattedTotalRepayment() = ExpressionParser.formatNumber(totalRepayment)
}

data class InterestResult(
    val principal: Double,
    val totalInterest: Double,
    val totalAmount: Double
) {
    fun formattedInterest() = ExpressionParser.formatNumber(totalInterest)
    fun formattedTotal() = ExpressionParser.formatNumber(totalAmount)
}

data class TipResult(
    val tipAmount: Double,
    val totalAmount: Double,
    val perPersonAmount: Double
) {
    fun formattedTip() = ExpressionParser.formatNumber(tipAmount)
    fun formattedTotal() = ExpressionParser.formatNumber(totalAmount)
    fun formattedPerPerson() = ExpressionParser.formatNumber(perPersonAmount)
}

data class DiscountResult(
    val discountAmount: Double,
    val finalPrice: Double
) {
    fun formattedDiscount() = ExpressionParser.formatNumber(discountAmount)
    fun formattedFinalPrice() = ExpressionParser.formatNumber(finalPrice)
}

data class TaxResult(
    val taxAmount: Double,
    val netAmount: Double,
    val grossAmount: Double
) {
    fun formattedTax() = ExpressionParser.formatNumber(taxAmount)
    fun formattedNet() = ExpressionParser.formatNumber(netAmount)
    fun formattedGross() = ExpressionParser.formatNumber(grossAmount)
}

object FinanceCalculator {

    fun calculateLoan(principal: Double, annualRatePercent: Double, years: Double): LoanResult {
        if (principal <= 0 || years <= 0) return LoanResult(0.0, 0.0, 0.0)
        val months = years * 12
        if (annualRatePercent == 0.0) {
            val monthly = principal / months
            return LoanResult(monthly, 0.0, principal)
        }
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        val monthlyPayment = principal * (monthlyRate * (1 + monthlyRate).pow(months)) /
                ((1 + monthlyRate).pow(months) - 1)
        val totalRepayment = monthlyPayment * months
        val totalInterest = totalRepayment - principal

        return LoanResult(monthlyPayment, totalInterest, totalRepayment)
    }

    fun calculateSimpleInterest(principal: Double, annualRatePercent: Double, years: Double): InterestResult {
        val interest = principal * (annualRatePercent / 100.0) * years
        return InterestResult(principal, interest, principal + interest)
    }

    fun calculateCompoundInterest(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        compoundsPerYear: Int = 12
    ): InterestResult {
        val r = annualRatePercent / 100.0
        val n = compoundsPerYear.toDouble()
        val totalAmount = principal * (1 + r / n).pow(n * years)
        val totalInterest = totalAmount - principal
        return InterestResult(principal, totalInterest, totalAmount)
    }

    fun calculateTip(billAmount: Double, tipPercent: Double, splitCount: Int = 1): TipResult {
        val count = if (splitCount < 1) 1 else splitCount
        val tip = billAmount * (tipPercent / 100.0)
        val total = billAmount + tip
        val perPerson = total / count
        return TipResult(tip, total, perPerson)
    }

    fun calculateDiscount(originalPrice: Double, discountPercent: Double): DiscountResult {
        val discount = originalPrice * (discountPercent / 100.0)
        val finalPrice = originalPrice - discount
        return DiscountResult(discount, finalPrice)
    }

    fun calculateTax(amount: Double, taxPercent: Double, isTaxInclusive: Boolean = false): TaxResult {
        return if (isTaxInclusive) {
            val net = amount / (1 + taxPercent / 100.0)
            val tax = amount - net
            TaxResult(tax, net, amount)
        } else {
            val tax = amount * (taxPercent / 100.0)
            val gross = amount + tax
            TaxResult(tax, amount, gross)
        }
    }
}
