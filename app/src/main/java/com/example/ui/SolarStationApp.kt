package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MainDestination
import com.example.ui.screens.BlueprintsScreen
import com.example.ui.screens.ConfiguratorScreen
import com.example.ui.screens.InstructionsScreen
import com.example.ui.screens.SpecificationScreen
import com.example.ui.viewmodel.SolarStationViewModel

@Composable
fun SolarStationApp(
    viewModel: SolarStationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.activeTab != MainDestination.CONFIGURATOR && uiState.inspectedNode == null) {
        BackHandler {
            viewModel.selectTab(MainDestination.CONFIGURATOR)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        MainDestination.entries.forEach { dest ->
                            val selected = uiState.activeTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = iconFor(dest, selected),
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MainDestination.entries.forEach { dest ->
                            val selected = uiState.activeTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = iconFor(dest, selected),
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 900.dp)

                    when (uiState.activeTab) {
                        MainDestination.CONFIGURATOR -> ConfiguratorScreen(
                            uiState = uiState,
                            onApplyPreset = viewModel::applyPreset,
                            onCityChange = viewModel::updateCity,
                            onUseCityAutoTiltChange = viewModel::updateUseCityAutoTilt,
                            onManualTiltChange = viewModel::updateManualTiltDegrees,
                            onRoomAreaChange = viewModel::updateRoomArea,
                            onRoomInsulationChange = viewModel::updateRoomInsulation,
                            onVoltageChange = viewModel::updateSystemVoltage,
                            onInverterTypeChange = viewModel::updateInverterType,
                            onControllerTypeChange = viewModel::updateControllerType,
                            onInverterWattsChange = viewModel::updateInverterWatts,
                            onPanelWattageChange = viewModel::updatePanelWattage,
                            onSeriesChange = viewModel::updateSeriesCount,
                            onParallelChange = viewModel::updateParallelStrings,
                            onTiltChange = viewModel::updatePanelTilt,
                            onOrientationChange = viewModel::updatePanelOrientation,
                            onChemistryChange = viewModel::updateBatteryChemistry,
                            onCapacityChange = viewModel::updateBatteryCapacity,
                            onToggleSpd = viewModel::toggleSpdProtection,
                            onToggleAts = viewModel::toggleGeneratorAts,
                            onToggleSmartBms = viewModel::toggleSmartBms,
                            onToggleAppliance = viewModel::toggleAppliance,
                            onOpenBlueprints = { viewModel.selectTab(MainDestination.BLUEPRINTS) },
                            modifier = contentModifier
                        )

                        MainDestination.BLUEPRINTS -> BlueprintsScreen(
                            uiState = uiState,
                            onSelectBlueprint = viewModel::selectBlueprint,
                            onInspectNode = viewModel::inspectNode,
                            modifier = contentModifier
                        )

                        MainDestination.SPECIFICATION -> SpecificationScreen(
                            uiState = uiState,
                            onToggleBomChecked = viewModel::toggleBomChecked,
                            modifier = contentModifier
                        )

                        MainDestination.INSTRUCTIONS -> InstructionsScreen(
                            uiState = uiState,
                            onToggleStepCompleted = viewModel::toggleStepCompleted,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(destination: MainDestination, selected: Boolean): ImageVector {
    return when (destination) {
        MainDestination.CONFIGURATOR -> if (selected) Icons.Filled.Tune else Icons.Outlined.Tune
        MainDestination.BLUEPRINTS -> if (selected) Icons.Filled.Architecture else Icons.Outlined.Architecture
        MainDestination.SPECIFICATION -> if (selected) Icons.Filled.Checklist else Icons.Outlined.Checklist
        MainDestination.INSTRUCTIONS -> if (selected) Icons.Filled.MenuBook else Icons.Outlined.MenuBook
    }
}
