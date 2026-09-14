package ru.balalaiki.kipiacalculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object UnitConverters {
    val pressureUnits = linkedMapOf(
        "Па" to 1.0,
        "кПа" to 1_000.0,
        "МПа" to 1_000_000.0,
        "бар" to 100_000.0,
        "кгс/см²" to 98_066.5,
        "мм рт. ст." to 133.322368,
        "мм вод. ст." to 9.80665
    )

    val lengthUnits = linkedMapOf(
        "мкм" to 0.000001,
        "мм" to 0.001,
        "см" to 0.01,
        "м" to 1.0,
        "км" to 1_000.0
    )

    fun convert(value: Double, from: String, to: String, factors: Map<String, Double>): Double {
        val fromFactor = factors.getValue(from)
        val toFactor = factors.getValue(to)
        return value * fromFactor / toFactor
    }

    fun currentToValue(currentMa: Double, low: Double, high: Double): Double =
        low + (currentMa - 4.0) / 16.0 * (high - low)

    fun valueToCurrent(value: Double, low: Double, high: Double): Double =
        4.0 + (value - low) / (high - low) * 16.0

    fun format(value: Double): String {
        if (!value.isFinite()) return "—"
        val symbols = DecimalFormatSymbols(Locale("ru", "RU"))
        val pattern = if (value != 0.0 && (abs(value) < 0.000001 || abs(value) >= 1_000_000_000)) {
            "0.######E0"
        } else {
            "0.##########"
        }
        return DecimalFormat(pattern, symbols).format(value)
    }
}
