package com.example

import com.example.domain.SolarCalculator
import com.example.model.*
import org.junit.Assert.*
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
    fun `direct solar heater preset has no battery or inverter and computes room wattage`() {
        val config = SolarCalculator.presetConfig(StationTier.DIRECT_HEATER).copy(
            roomAreaM2 = 20,
            roomInsulation = RoomInsulation.AVERAGE
        )
        val metrics = SolarCalculator.calculateMetrics(config)
        val bom = SolarCalculator.generateBom(config, metrics)
        val steps = SolarCalculator.generateBuildSteps(config, metrics)

        assertTrue(config.isDirectSolarHeater)
        assertEquals(0, metrics.batteryNominalWh)
        assertEquals(2000, metrics.recommendedHeaterWattsForRoom)
        assertTrue(bom.none { it.id == "battery" || it.id == "inverter" })
        assertTrue(bom.any { it.id == "heater" })
        assertEquals(5, steps.size)
    }

    @Test
    fun `city location adjusts optimal tilt angle and solar yield`() {
        val odesaConfig = SolarCalculator.presetConfig(StationTier.BASIC).copy(
            city = CityLocation.ODESA,
            panelTilt = PanelTilt.WINTER_55,
            useCityAutoTilt = true
        )
        val chernihivConfig = odesaConfig.copy(city = CityLocation.CHERNIHIV)

        val odesaMetrics = SolarCalculator.calculateMetrics(odesaConfig)
        val chernihivMetrics = SolarCalculator.calculateMetrics(chernihivConfig)

        assertTrue(odesaMetrics.effectiveTiltDegrees < chernihivMetrics.effectiveTiltDegrees)
        assertTrue(odesaMetrics.estimatedWinterDailyKwh > chernihivMetrics.estimatedWinterDailyKwh)
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
    fun `BOM and 7 build steps generated for battery configuration`() {
        val config = SolarCalculator.presetConfig(StationTier.OPTIMAL)
        val metrics = SolarCalculator.calculateMetrics(config)
        val bom = SolarCalculator.generateBom(config, metrics)
        val steps = SolarCalculator.generateBuildSteps(config, metrics)

        assertTrue(bom.isNotEmpty())
        assertEquals(7, steps.size)
    }
}
