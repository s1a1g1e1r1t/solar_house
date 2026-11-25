package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.SolarCalculator
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SolarUiState(
    val activeTab: MainDestination = MainDestination.CONFIGURATOR,
    val config: SolarStationConfig = SolarCalculator.presetConfig(StationTier.BASIC),
    val metrics: CalculatedStationMetrics = SolarCalculator.calculateMetrics(SolarCalculator.presetConfig(StationTier.BASIC)),
    val bomItems: List<BomItem> = emptyList(),
    val buildSteps: List<BuildStep> = emptyList(),
    val checkedBomIds: Set<String> = emptySet(),
    val completedStepNumbers: Set<Int> = emptySet(),
    val activeBlueprint: BlueprintType = BlueprintType.WIRING_SCHEMATIC,
    val inspectedNode: SchematicNodeInfo? = null,
    val showPresetsSheet: Boolean = false
) {
    init {
        // Initialized via ViewModel
    }
}

class SolarStationViewModel : ViewModel() {

    private val initialConfig = SolarCalculator.presetConfig(StationTier.BASIC)
    private val initialMetrics = SolarCalculator.calculateMetrics(initialConfig)

    private val _uiState = MutableStateFlow(
        SolarUiState(
            config = initialConfig,
            metrics = initialMetrics,
            bomItems = SolarCalculator.generateBom(initialConfig, initialMetrics),
            buildSteps = SolarCalculator.generateBuildSteps(initialConfig, initialMetrics)
        )
    )
    val uiState: StateFlow<SolarUiState> = _uiState.asStateFlow()

    fun selectTab(destination: MainDestination) {
        _uiState.update { it.copy(activeTab = destination) }
    }

    fun applyPreset(tier: StationTier) {
        val currentApps = _uiState.value.config.appliances
        val preset = SolarCalculator.presetConfig(tier)
        // Keep user's appliance list structure, apply preset selections
        val updatedConfig = if (currentApps.isEmpty()) preset else preset
        recalculate(updatedConfig)
    }

    fun updateSystemVoltage(voltage: SystemVoltage) {
        val cfg = _uiState.value.config
        val adjustedInverter = when (voltage) {
            SystemVoltage.V12 -> cfg.inverterWatts.coerceAtMost(1500)
            SystemVoltage.V24 -> cfg.inverterWatts.coerceIn(1000, 4000)
            SystemVoltage.V48 -> cfg.inverterWatts.coerceAtLeast(3000)
        }
        recalculate(cfg.copy(systemVoltage = voltage, inverterWatts = adjustedInverter))
    }

    fun updatePanelWattage(watts: Int) {
        recalculate(_uiState.value.config.copy(panelWattage = watts))
    }

    fun updateSeriesCount(series: Int) {
        recalculate(_uiState.value.config.copy(seriesCount = series.coerceIn(1, 14)))
    }

    fun updateParallelStrings(parallel: Int) {
        recalculate(_uiState.value.config.copy(parallelStrings = parallel.coerceIn(1, 4)))
    }

    fun updateControllerType(type: ControllerType) {
        val cfg = _uiState.value.config
        val adjustedInverterType = if (type == ControllerType.PWM) InverterType.PURE_SINE_OFFGRID else cfg.inverterType
        recalculate(cfg.copy(controllerType = type, inverterType = adjustedInverterType))
    }

    fun updateBatteryChemistry(chemistry: BatteryChemistry) {
        val cfg = _uiState.value.config
        recalculate(
            cfg.copy(
                batteryChemistry = chemistry,
                includeSmartBms = chemistry == BatteryChemistry.LIFEPO4
            )
        )
    }

    fun updateBatteryCapacity(ah: Int) {
        recalculate(_uiState.value.config.copy(batteryCapacityAh = ah.coerceIn(50, 600)))
    }

    fun updateInverterWatts(watts: Int) {
        recalculate(_uiState.value.config.copy(inverterWatts = watts.coerceIn(500, 12000)))
    }

    fun updateInverterType(type: InverterType) {
        val cfg = _uiState.value.config
        val controller = if (type == InverterType.HYBRID_MPPT) ControllerType.MPPT else cfg.controllerType
        recalculate(cfg.copy(inverterType = type, controllerType = controller))
    }

    fun updatePanelTilt(tilt: PanelTilt) {
        recalculate(_uiState.value.config.copy(panelTilt = tilt))
    }

    fun updatePanelOrientation(orientation: PanelOrientation) {
        recalculate(_uiState.value.config.copy(panelOrientation = orientation))
    }

    fun toggleSpdProtection(enabled: Boolean) {
        recalculate(_uiState.value.config.copy(includeSpdProtection = enabled))
    }

    fun toggleGeneratorAts(enabled: Boolean) {
        recalculate(_uiState.value.config.copy(includeGeneratorAts = enabled))
    }

    fun toggleSmartBms(enabled: Boolean) {
        recalculate(_uiState.value.config.copy(includeSmartBms = enabled))
    }

    fun toggleAppliance(id: String) {
        val cfg = _uiState.value.config
        val updatedApps = cfg.appliances.map {
            if (it.id == id) it.copy(selected = !it.selected) else it
        }
        recalculate(cfg.copy(appliances = updatedApps))
    }

    fun selectBlueprint(type: BlueprintType) {
        _uiState.update { it.copy(activeBlueprint = type, inspectedNode = null) }
    }

    fun inspectNode(node: SchematicNodeInfo?) {
        _uiState.update { it.copy(inspectedNode = node) }
    }

    fun toggleBomChecked(id: String) {
        _uiState.update { state ->
            val next = if (id in state.checkedBomIds) {
                state.checkedBomIds - id
            } else {
                state.checkedBomIds + id
            }
            state.copy(checkedBomIds = next)
        }
    }

    fun toggleStepCompleted(stepNumber: Int) {
        _uiState.update { state ->
            val next = if (stepNumber in state.completedStepNumbers) {
                state.completedStepNumbers - stepNumber
            } else {
                state.completedStepNumbers + stepNumber
            }
            state.copy(completedStepNumbers = next)
        }
    }

    private fun recalculate(newConfig: SolarStationConfig) {
        val newMetrics = SolarCalculator.calculateMetrics(newConfig)
        val newBom = SolarCalculator.generateBom(newConfig, newMetrics)
        val newSteps = SolarCalculator.generateBuildSteps(newConfig, newMetrics)
        _uiState.update {
            it.copy(
                config = newConfig,
                metrics = newMetrics,
                bomItems = newBom,
                buildSteps = newSteps
            )
        }
    }
}
