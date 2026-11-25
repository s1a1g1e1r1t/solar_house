package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BuildStep
import com.example.model.StepSafetyLevel
import com.example.ui.theme.LocalSolarColors
import com.example.ui.viewmodel.SolarUiState

@Composable
fun InstructionsScreen(
    uiState: SolarUiState,
    onToggleStepCompleted: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = uiState.buildSteps
    val completedCount = steps.count { it.number in uiState.completedStepNumbers }
    val totalSteps = steps.size.coerceAtLeast(1)
    var expandedStep by remember { mutableIntStateOf(1) }
    val solarColors = LocalSolarColors.current
    val config = uiState.config

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Покрокова інструкція зі збірки", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (config.isDirectSolarHeater)
                        "Адаптована під схему прямого обігріву: ${uiState.metrics.totalPanelWatts} Вт → Електрокамін (${config.roomAreaM2} м²), м. ${config.city.cityName} (${uiState.metrics.effectiveTiltDegrees}°)."
                    else
                        "Адаптована під ваш проєкт (${config.systemVoltage.label}, ${config.batteryChemistry.shortName}, ${config.inverterWatts} Вт, м. ${config.city.cityName}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Прогрес створення станції", style = MaterialTheme.typography.titleSmall)
                        Text("$completedCount / $totalSteps етапів", style = MaterialTheme.typography.labelSmall)
                    }
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50.dp))
                    )
                }
            }
        }

        item {
            Surface(
                color = solarColors.dangerContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = solarColors.dangerText)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (config.isDirectSolarHeater)
                                "Золоте правило прямого підключення до ТЕНа"
                            else
                                "Золоте правило першого запуску",
                            style = MaterialTheme.typography.titleSmall,
                            color = solarColors.dangerText
                        )
                        Text(
                            if (config.isDirectSolarHeater)
                                "Використовуйте ТІЛЬКИ чисто резистивний нагрівач без електроніки та встановіть DC-автомат ${uiState.metrics.recommendedSolarBreakerA} А. Звичайний AC-вимикач або вилка в розетці призводять до електричної дуги!"
                            else
                                "Порядок увімкнення: 1) Акумулятор → 2) Інвертор → 3) Сонячні панелі → 4) Домашнє навантаження. Ніколи не від'єднуйте АКБ, поки підключені панелі!",
                            style = MaterialTheme.typography.bodySmall,
                            color = solarColors.dangerText
                        )
                    }
                }
            }
        }

        items(steps, key = { it.number }) { step ->
            val isCompleted = step.number in uiState.completedStepNumbers
            val isExpanded = expandedStep == step.number
            BuildStepCard(
                step = step,
                isCompleted = isCompleted,
                isExpanded = isExpanded,
                onToggleExpand = { expandedStep = if (isExpanded) -1 else step.number },
                onToggleCompleted = { onToggleStepCompleted(step.number) }
            )
        }
    }
}

@Composable
private fun BuildStepCard(
    step: BuildStep,
    isCompleted: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val solarColors = LocalSolarColors.current
    val (badgeBg, badgeFg) = when (step.safetyLevel) {
        StepSafetyLevel.CRITICAL -> solarColors.dangerContainer to solarColors.dangerText
        StepSafetyLevel.IMPORTANT -> solarColors.warningContainer to solarColors.warningText
        StepSafetyLevel.STANDARD -> solarColors.okContainer to solarColors.okText
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("build_step_${step.number}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleExpand)
                    .minimumInteractiveComponentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        .clickable(onClick = onToggleCompleted),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Filled.Check, contentDescription = "Етап виконано", tint = MaterialTheme.colorScheme.onTertiary)
                    } else {
                        Text(
                            "${step.number}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = badgeBg, shape = RoundedCornerShape(50.dp)) {
                            Text(
                                text = step.safetyLevel.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "• ${step.estimatedTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(step.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        step.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()
                    Text(step.summary, style = MaterialTheme.typography.bodyMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Контрольний список:", style = MaterialTheme.typography.labelLarge)
                        step.checklist.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Text(item, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Порада під ваш проєкт", style = MaterialTheme.typography.labelLarge)
                                Text(step.dynamicRecommendation, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Surface(
                        color = solarColors.warningContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = solarColors.warningText)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Типова помилка", style = MaterialTheme.typography.labelLarge, color = solarColors.warningText)
                                Text(step.commonMistake, style = MaterialTheme.typography.bodySmall, color = solarColors.warningText)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_step_btn_${step.number}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (isCompleted) Icons.Filled.Undo else Icons.Filled.Done,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCompleted) "Позначити як невиконаний" else "Позначити етап виконаним")
                    }
                }
            }
        }
    }
}
