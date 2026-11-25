package com.example

import com.example.domain.SolarCalculator
import com.example.model.AlertSeverity
import com.example.model.ControllerType
import com.example.model.StationTier
import com.example.model.SystemVoltage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarCalculatorTest {

    @Test
    fun `presets scale from MINI to MAXI correctly`() {
        val mini = SolarCalculator.calculateMetrics(SolarCalculator.presetConfig(StationTier.MINI))
        val basic = SolarCalculator.calculateMetrics(SolarCalculator.presetConfig(StationTier.BASIC))
        val optimal = SolarCalculator.calculateMetrics(SolarCalculator.presetConfig(StationTier.OPTIMAL))
        val maxi = SolarCalculator.calculateMetrics(SolarCalculator.presetConfig(StationTier.MAXI))

        assertTrue(mini.totalPanelWatts < basic.totalPanelWatts)
        assertTrue(basic.totalPanelWatts < optimal.totalPanelWatts)
        assertTrue(optimal.totalPanelWatts < maxi.totalPanelWatts)
        assertTrue(mini.usableBatteryWh < maxi.usableBatteryWh)
    }

    @Test
    fun `high inverter power on 12V triggers critical current alert`() {
        val config = SolarCalculator.presetConfig(StationTier.MINI).copy(
            systemVoltage = SystemVoltage.V12,
            inverterWatts = 3000
        )
        val metrics = SolarCalculator.calculateMetrics(config)
        assertTrue(metrics.alerts.any { it.severity == AlertSeverity.CRITICAL })
        assertTrue(metrics.recommendedBatteryCableMm2 >= 70)
    }

    @Test
    fun `PWM controller with series panels triggers critical warning`() {
        val config = SolarCalculator.presetConfig(StationTier.BASIC).copy(
            controllerType = ControllerType.PWM,
            seriesCount = 4
        )
        val metrics = SolarCalculator.calculateMetrics(config)
        assertTrue(metrics.alerts.any { it.severity == AlertSeverity.CRITICAL })
    }

    @Test
    fun `BOM and 7 build steps generated for any configuration`() {
        val config = SolarCalculator.presetConfig(StationTier.OPTIMAL)
        val metrics = SolarCalculator.calculateMetrics(config)
        val bom = SolarCalculator.generateBom(config, metrics)
        val steps = SolarCalculator.generateBuildSteps(config, metrics)

        assertTrue(bom.isNotEmpty())
        assertEquals(7, steps.size)
    }
}
