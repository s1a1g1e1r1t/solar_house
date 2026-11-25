package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BlueprintType
import com.example.model.SchematicNodeInfo
import com.example.ui.components.InteractiveBlueprintCanvas
import com.example.ui.theme.*
import com.example.ui.viewmodel.SolarUiState
import kotlin.math.roundToInt

@Composable
fun BlueprintsScreen(
    uiState: SolarUiState,
    onSelectBlueprint: (BlueprintType) -> Unit,
    onInspectNode: (SchematicNodeInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember(uiState.activeBlueprint) { mutableFloatStateOf(1f) }
    var pan by remember(uiState.activeBlueprint) { mutableStateOf(Offset.Zero) }

    if (uiState.inspectedNode != null) {
        BackHandler {
            onInspectNode(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Blueprint Type Switcher
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Інженерні креслення СЕС",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Схеми автоматично адаптуються до ваших параметрів (${uiState.metrics.totalPanelWatts} Вт, ${uiState.config.systemVoltage.label}, ${uiState.config.inverterWatts} Вт).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BlueprintType.entries) { type ->
                FilterChip(
                    selected = uiState.activeBlueprint == type,
                    onClick = { onSelectBlueprint(type) },
                    label = { Text(type.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (type) {
                                BlueprintType.WIRING_SCHEMATIC -> Icons.Filled.AccountTree
                                BlueprintType.ARRAY_AND_BATTERY -> Icons.Filled.GridView
                                BlueprintType.PHYSICAL_MOUNTING -> Icons.Filled.HomeWork
                                BlueprintType.ENERGY_BALANCE -> Icons.Filled.ShowChart
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("blueprint_tab_${type.name.lowercase()}")
                )
            }
        }

        // Blueprint Interactive Viewport Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 340.dp, max = 460.dp)
            ) {
                InteractiveBlueprintCanvas(
                    blueprintType = uiState.activeBlueprint,
                    config = uiState.config,
                    metrics = uiState.metrics,
                    zoom = zoom,
                    pan = pan,
                    onTransform = { zoomDelta, panDelta ->
                        zoom = (zoom * zoomDelta).coerceIn(0.8f, 3.2f)
                        pan += panDelta
                    },
                    selectedNodeId = uiState.inspectedNode?.id,
                    onNodeTapped = onInspectNode,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Zoom Controls (Top Right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BlueprintNavy.copy(alpha = 0.85f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Зменшити масштаб", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            zoom = 1f
                            pan = Offset.Zero
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Скинути масштаб", tint = SolarAmber)
                    }
                    IconButton(
                        onClick = { zoom = (zoom + 0.25f).coerceAtMost(3.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Збільшити масштаб", tint = Color.White)
                    }
                }

                // Interaction hint badge
                Surface(
                    color = BlueprintNavy.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (uiState.activeBlueprint == BlueprintType.ENERGY_BALANCE)
                            "Жести: масштабування та перетягування"
                        else
                            "Натисніть на вузол схеми для деталей",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Wire Legend Bar
        if (uiState.activeBlueprint != BlueprintType.ENERGY_BALANCE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendDot(WireDcPositive, "+DC (${uiState.metrics.recommendedBatteryCableMm2}мм²)")
                LegendDot(WireDcNegative, "-DC (Мінус)")
                LegendDot(WireAcPhase, "AC 230В")
                LegendDot(WireGround, "PE Заземлення")
            }
        }

        // Inspected Component Detail Card
        AnimatedVisibility(visible = uiState.inspectedNode != null) {
            uiState.inspectedNode?.let { node ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(node.title, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { onInspectNode(null) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Закрити деталі вузла")
                            }
                        }
                        Text(node.primarySpec, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Підключення: ${node.wiringSpec}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = node.safetyNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Technical Quick-Readout Summary Card below blueprint
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Ключові номінали на кресленні", style = MaterialTheme.typography.titleSmall)
                BlueprintRow("Схема ФЕМ", "${uiState.config.seriesCount}S × ${uiState.config.parallelStrings}P (${uiState.metrics.totalPanelWatts} Вт)")
                BlueprintRow("Напруга PV (зима -20°C)", "${uiState.metrics.arrayVocWinterMinus20.roundToInt()} В (MPPT ≥ ${uiState.metrics.requiredMpptMaxVoc} В)")
                BlueprintRow("Кабель PV / Автомат PV", "${uiState.metrics.recommendedSolarCableMm2} мм² / DC ${uiState.metrics.recommendedSolarBreakerA} А")
                BlueprintRow("Кабель АКБ / Запобіжник", "${uiState.metrics.recommendedBatteryCableMm2} мм² / ${uiState.metrics.recommendedBatteryFuseA} А (макс. ${uiState.metrics.maxDcBatteryCurrentA} А)")
                BlueprintRow("Захист виходу AC 230В", "Автомат ${uiState.metrics.recommendedAcBreakerA} А + ПЗВ 30 мА")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BlueprintRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
