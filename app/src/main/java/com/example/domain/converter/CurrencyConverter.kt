package com.example.domain.converter

import com.example.domain.calculator.ExpressionParser

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val rateToUsd: Double // How many of this currency for 1 USD
)

object CurrencyConverter {

    val defaultCurrencies = listOf(
        Currency("USD", "US Dollar", "$", 1.0),
        Currency("EUR", "Euro", "€", 0.92),
        Currency("GBP", "British Pound", "£", 0.79),
        Currency("TZS", "Tanzanian Shilling", "TSh", 2680.0),
        Currency("KES", "Kenyan Shilling", "KSh", 129.5),
        Currency("UGX", "Ugandan Shilling", "USh", 3710.0),
        Currency("ZAR", "South African Rand", "R", 18.2),
        Currency("NGN", "Nigerian Naira", "₦", 1540.0),
        Currency("GHS", "Ghanaian Cedi", "GH₵", 15.6),
        Currency("INR", "Indian Rupee", "₹", 83.9),
        Currency("JPY", "Japanese Yen", "¥", 147.2),
        Currency("CNY", "Chinese Yuan", "¥", 7.17),
        Currency("CAD", "Canadian Dollar", "C$", 1.37),
        Currency("AUD", "Australian Dollar", "A$", 1.52),
        Currency("CHF", "Swiss Franc", "CHF", 0.86)
    )

    fun convert(amount: Double, from: Currency, to: Currency): Double {
        if (from.code == to.code) return amount
        val inUsd = amount / from.rateToUsd
        return inUsd * to.rateToUsd
    }

    fun convertFormatted(amount: Double, from: Currency, to: Currency): String {
        val result = convert(amount, from, to)
        return ExpressionParser.formatNumber(result)
    }
}
