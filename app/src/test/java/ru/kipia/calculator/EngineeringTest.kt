package ru.kipia.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class EngineeringTest {
    @Test fun pressureExample() {
        assertEquals(6.118297, Engineering.convertLinear(0.6, Engineering.pressureUnits[2], Engineering.pressureUnits[4]), 0.000001)
    }

    @Test fun lengthExample() {
        assertEquals(1.5, Engineering.convertLinear(1500.0, Engineering.lengthUnits[1], Engineering.lengthUnits[4]), 0.000001)
    }

    @Test fun temperatureConversions() {
        assertEquals(32.0, Engineering.convertTemperature(0.0, "°C", "°F"), 0.000001)
        assertEquals(273.15, Engineering.convertTemperature(0.0, "°C", "K"), 0.000001)
    }

    @Test fun scaleDivisionExample() {
        assertEquals(0.02, Engineering.divisionValue(0.0, 1.6, 80), 0.000001)
    }

    @Test fun signalExamples() {
        assertEquals(0.8, Engineering.currentToValue(12.0, 0.0, 1.6), 0.000001)
        assertEquals(12.0, Engineering.valueToCurrent(0.8, 0.0, 1.6), 0.000001)
        assertEquals(50.0, Engineering.signalPercent(12.0), 0.000001)
    }

    @Test fun ohmsLawPairs() {
        val result = Engineering.electrical("I", 0.25, "R", 200.0)
        assertEquals(50.0, result.voltage, 0.000001)
        assertEquals(12.5, result.power, 0.000001)
    }
}
