package com.example.domain.converter

import com.example.domain.calculator.ExpressionParser

enum class UnitCategory(val displayName: String) {
    LENGTH("Length"),
    AREA("Area"),
    VOLUME("Volume"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    SPEED("Speed"),
    TIME("Time"),
    DATA("Data"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    POWER("Power"),
    FREQUENCY("Frequency"),
    ANGLE("Angle"),
    FUEL_ECONOMY("Fuel Economy")
}

data class UnitItem(val name: String, val symbol: String, val toBaseRatio: Double)

object UnitConverter {

    fun getUnitsForCategory(category: UnitCategory): List<UnitItem> {
        return when (category) {
            UnitCategory.LENGTH -> listOf(
                UnitItem("Meter", "m", 1.0),
                UnitItem("Kilometer", "km", 1000.0),
                UnitItem("Centimeter", "cm", 0.01),
                UnitItem("Millimeter", "mm", 0.001),
                UnitItem("Mile", "mi", 1609.344),
                UnitItem("Yard", "yd", 0.9144),
                UnitItem("Foot", "ft", 0.3048),
                UnitItem("Inch", "in", 0.0254)
            )
            UnitCategory.AREA -> listOf(
                UnitItem("Square Meter", "m²", 1.0),
                UnitItem("Square Kilometer", "km²", 1e6),
                UnitItem("Square Foot", "ft²", 0.092903),
                UnitItem("Acre", "ac", 4046.856),
                UnitItem("Hectare", "ha", 10000.0)
            )
            UnitCategory.VOLUME -> listOf(
                UnitItem("Liter", "L", 1.0),
                UnitItem("Milliliter", "mL", 0.001),
                UnitItem("Cubic Meter", "m³", 1000.0),
                UnitItem("Gallon (US)", "gal", 3.78541),
                UnitItem("Quart", "qt", 0.946353),
                UnitItem("Pint", "pt", 0.473176),
                UnitItem("Cup", "cup", 0.24)
            )
            UnitCategory.WEIGHT -> listOf(
                UnitItem("Kilogram", "kg", 1.0),
                UnitItem("Gram", "g", 0.001),
                UnitItem("Milligram", "mg", 0.000001),
                UnitItem("Metric Ton", "t", 1000.0),
                UnitItem("Pound", "lb", 0.453592),
                UnitItem("Ounce", "oz", 0.0283495)
            )
            UnitCategory.TEMPERATURE -> listOf(
                UnitItem("Celsius", "°C", 1.0),
                UnitItem("Fahrenheit", "°F", 1.0),
                UnitItem("Kelvin", "K", 1.0)
            )
            UnitCategory.SPEED -> listOf(
                UnitItem("Meters/second", "m/s", 1.0),
                UnitItem("Kilometers/hour", "km/h", 0.277778),
                UnitItem("Miles/hour", "mph", 0.44704),
                UnitItem("Knot", "kn", 0.514444)
            )
            UnitCategory.TIME -> listOf(
                UnitItem("Second", "s", 1.0),
                UnitItem("Minute", "min", 60.0),
                UnitItem("Hour", "h", 3600.0),
                UnitItem("Day", "d", 86400.0),
                UnitItem("Week", "wk", 604800.0),
                UnitItem("Year", "yr", 31536000.0)
            )
            UnitCategory.DATA -> listOf(
                UnitItem("Byte", "B", 1.0),
                UnitItem("Kilobyte", "KB", 1024.0),
                UnitItem("Megabyte", "MB", 1048576.0),
                UnitItem("Gigabyte", "GB", 1073741824.0),
                UnitItem("Terabyte", "TB", 1099511627776.0)
            )
            UnitCategory.PRESSURE -> listOf(
                UnitItem("Pascal", "Pa", 1.0),
                UnitItem("Kilopascal", "kPa", 1000.0),
                UnitItem("Bar", "bar", 100000.0),
                UnitItem("PSI", "psi", 6894.76),
                UnitItem("Atmosphere", "atm", 101325.0)
            )
            UnitCategory.ENERGY -> listOf(
                UnitItem("Joule", "J", 1.0),
                UnitItem("Kilojoule", "kJ", 1000.0),
                UnitItem("Calorie", "cal", 4.184),
                UnitItem("Kilocalorie", "kcal", 4184.0),
                UnitItem("Watt-hour", "Wh", 3600.0),
                UnitItem("Kilowatt-hour", "kWh", 3600000.0)
            )
            UnitCategory.POWER -> listOf(
                UnitItem("Watt", "W", 1.0),
                UnitItem("Kilowatt", "kW", 1000.0),
                UnitItem("Horsepower", "hp", 745.7)
            )
            UnitCategory.FREQUENCY -> listOf(
                UnitItem("Hertz", "Hz", 1.0),
                UnitItem("Kilohertz", "kHz", 1000.0),
                UnitItem("Megahertz", "MHz", 1e6),
                UnitItem("Gigahertz", "GHz", 1e9)
            )
            UnitCategory.ANGLE -> listOf(
                UnitItem("Degree", "°", 1.0),
                UnitItem("Radian", "rad", 57.2958),
                UnitItem("Gradian", "grad", 0.9)
            )
            UnitCategory.FUEL_ECONOMY -> listOf(
                UnitItem("Liters/100km", "L/100km", 1.0),
                UnitItem("MPG (US)", "mpg", 1.0)
            )
        }
    }

    fun convert(value: Double, from: UnitItem, to: UnitItem, category: UnitCategory): Double {
        if (from == to) return value

        if (category == UnitCategory.TEMPERATURE) {
            // Special temperature logic
            val celsius = when (from.symbol) {
                "°C" -> value
                "°F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (to.symbol) {
                "°C" -> celsius
                "°F" -> (celsius * 9.0 / 5.0) + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }

        if (category == UnitCategory.FUEL_ECONOMY) {
            if (from.symbol == "L/100km" && to.symbol == "mpg") {
                return if (value <= 0) 0.0 else 235.215 / value
            } else if (from.symbol == "mpg" && to.symbol == "L/100km") {
                return if (value <= 0) 0.0 else 235.215 / value
            }
        }

        // Standard ratio conversion: value * from.toBaseRatio / to.toBaseRatio
        val baseValue = value * from.toBaseRatio
        return baseValue / to.toBaseRatio
    }

    fun convertFormatted(value: Double, from: UnitItem, to: UnitItem, category: UnitCategory): String {
        val result = convert(value, from, to, category)
        return ExpressionParser.formatNumber(result)
    }
}
