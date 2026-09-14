package ru.balalaiki.kipiacalculator

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConvertersTest {
    @Test
    fun pressureConversionIsCorrect() {
        val result = UnitConverters.convert(0.6, "МПа", "бар", UnitConverters.pressureUnits)
        assertEquals(6.0, result, 0.000001)
    }

    @Test
    fun currentScalingIsCorrect() {
        assertEquals(50.0, UnitConverters.currentToValue(12.0, 0.0, 100.0), 0.000001)
        assertEquals(12.0, UnitConverters.valueToCurrent(50.0, 0.0, 100.0), 0.000001)
    }
}
