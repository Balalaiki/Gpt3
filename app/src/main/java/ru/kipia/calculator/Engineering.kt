package ru.kipia.calculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/** Вся расчётная логика отделена от Android UI и проверяется unit-тестами. */
object Engineering {
    data class UnitDef(val title: String, val factorToBase: Double)

    val pressureUnits = listOf(
        UnitDef("Па", 1.0),
        UnitDef("кПа", 1_000.0),
        UnitDef("МПа", 1_000_000.0),
        UnitDef("бар", 100_000.0),
        UnitDef("кгс/см²", 98_066.5),
        UnitDef("мм рт. ст.", 133.322368421),
        UnitDef("мм вод. ст.", 9.80665),
        UnitDef("атм", 101_325.0)
    )

    val lengthUnits = listOf(
        UnitDef("мкм", 0.000001),
        UnitDef("мм", 0.001),
        UnitDef("см", 0.01),
        UnitDef("дм", 0.1),
        UnitDef("м", 1.0),
        UnitDef("км", 1_000.0)
    )

    val currentUnits = listOf(UnitDef("мкА", 0.000001), UnitDef("мА", 0.001), UnitDef("А", 1.0))
    val voltageUnits = listOf(UnitDef("мВ", 0.001), UnitDef("В", 1.0), UnitDef("кВ", 1_000.0))
    val resistanceUnits = listOf(UnitDef("Ом", 1.0), UnitDef("кОм", 1_000.0), UnitDef("МОм", 1_000_000.0))
    val powerUnits = listOf(UnitDef("мВт", 0.001), UnitDef("Вт", 1.0), UnitDef("кВт", 1_000.0))

    fun convertLinear(value: Double, from: UnitDef, to: UnitDef): Double =
        value * from.factorToBase / to.factorToBase

    fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> value
        }
        return when (to) {
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            "K" -> celsius + 273.15
            else -> celsius
        }
    }

    fun divisionValue(min: Double, max: Double, divisions: Int): Double {
        require(divisions > 0) { "Количество делений должно быть больше нуля" }
        return (max - min) / divisions
    }

    fun currentToValue(currentMa: Double, min: Double, max: Double): Double =
        min + (currentMa - 4.0) / 16.0 * (max - min)

    fun valueToCurrent(value: Double, min: Double, max: Double): Double {
        require(max != min) { "Диапазон не может быть нулевым" }
        return 4.0 + (value - min) / (max - min) * 16.0
    }

    fun signalPercent(currentMa: Double): Double = (currentMa - 4.0) / 16.0 * 100.0

    data class ElectricalResult(val voltage: Double, val current: Double, val resistance: Double, val power: Double)

    /**
     * Принимает две величины в основных единицах СИ: В, А, Ом, Вт.
     * Поддерживаемые пары: U/I, U/R, I/R, P/U, P/I, P/R.
     */
    fun electrical(firstName: String, first: Double, secondName: String, second: Double): ElectricalResult {
        val values = mapOf(firstName to first, secondName to second)
        val u: Double
        val i: Double
        val r: Double
        val p: Double
        when {
            values.containsKey("U") && values.containsKey("I") -> {
                u = values.getValue("U"); i = values.getValue("I")
                require(i != 0.0); r = u / i; p = u * i
            }
            values.containsKey("U") && values.containsKey("R") -> {
                u = values.getValue("U"); r = values.getValue("R")
                require(r != 0.0); i = u / r; p = u * i
            }
            values.containsKey("I") && values.containsKey("R") -> {
                i = values.getValue("I"); r = values.getValue("R")
                u = i * r; p = u * i
            }
            values.containsKey("P") && values.containsKey("U") -> {
                p = values.getValue("P"); u = values.getValue("U")
                require(u != 0.0); i = p / u; require(i != 0.0); r = u / i
            }
            values.containsKey("P") && values.containsKey("I") -> {
                p = values.getValue("P"); i = values.getValue("I")
                require(i != 0.0); u = p / i; r = u / i
            }
            values.containsKey("P") && values.containsKey("R") -> {
                p = values.getValue("P"); r = values.getValue("R")
                require(p >= 0.0 && r > 0.0); u = sqrt(p * r); i = sqrt(p / r)
            }
            else -> error("Неподдерживаемая пара величин")
        }
        require(listOf(u, i, r, p).all { it.isFinite() })
        return ElectricalResult(u, i, r, p)
    }

    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && abs(it) < 1e100 }

    fun format(value: Double): String {
        if (!value.isFinite()) return "—"
        val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
            decimalSeparator = ','
            groupingSeparator = ' '
        }
        val magnitude = abs(value)
        val pattern = when {
            magnitude != 0.0 && magnitude < 0.0000001 -> "0.######E0"
            magnitude >= 1_000_000_000.0 -> "0.######E0"
            magnitude >= 1_000_000.0 -> "#,##0.###"
            magnitude >= 1.0 -> "#,##0.######"
            else -> "0.#########"
        }
        return DecimalFormat(pattern, symbols).format(value)
    }
}
