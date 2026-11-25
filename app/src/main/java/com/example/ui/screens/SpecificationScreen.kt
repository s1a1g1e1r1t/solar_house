package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BomItem
import com.example.ui.viewmodel.SolarUiState
import kotlin.math.roundToInt

@Composable
fun SpecificationScreen(
    uiState: SolarUiState,
    onToggleBomChecked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    val metrics = uiState.metrics
    val groupedBom = uiState.bomItems.groupBy { it.category }
    val checkedCount = uiState.bomItems.count { it.id in uiState.checkedBomIds }
    val totalCount = uiState.bomItems.size.coerceAtLeast(1)

    val bulkV = "%.1f".format(config.batteryChemistry.chargeVoltsPer12V * (config.systemVoltage.volts / 12f))
    val floatV = "%.1f".format(config.batteryChemistry.floatVoltsPer12V * (config.systemVoltage.volts / 12f))
    val cutV = "%.1f".format(config.batteryChemistry.cutoffVoltsPer12V * (config.systemVoltage.volts / 12f))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Специфікація та комплектація", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (config.isDirectSolarHeater)
                        "Мінімальний набір для прямого обігріву кімнати ${config.roomAreaM2} м² (без АКБ та інвертора)."
                    else
                        "Розрахований перелік обладнання, кабелів і захисної автоматики для вашого проєкту.",
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Підготовка комплектуючих", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$checkedCount з $totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { checkedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50.dp))
                    )
                    Text(
                        "Відмічайте позиції, які ви вже придбали або підготували до монтажу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Розрахункові параметри", style = MaterialTheme.typography.titleSmall)
                    }
                    if (config.isDirectSolarHeater) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Площа кімнати", "${config.roomAreaM2} м²", Modifier.weight(1f))
                            SpecPill("Потреба тепла", "${metrics.recommendedHeaterWattsForRoom} Вт", Modifier.weight(1f))
                            SpecPill("Покриття", "${metrics.heaterCoveragePercent}%", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Струм поля Imp", "${"%.1f".format(metrics.arrayImp)} А", Modifier.weight(1f))
                            SpecPill("Кабель PV1-F", "${metrics.recommendedSolarCableMm2} мм²", Modifier.weight(1f))
                            SpecPill("DC Автомат", "${metrics.recommendedSolarBreakerA} А", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Кут (${config.city.cityName})", "${metrics.effectiveTiltDegrees}°", Modifier.weight(1f))
                            SpecPill("Зима тепло", "${"%.1f".format(metrics.estimatedWinterDailyKwh)} кВт·г/д", Modifier.weight(1f))
                            SpecPill("Літо тепло", "${"%.1f".format(metrics.estimatedSummerDailyKwh)} кВт·г/д", Modifier.weight(1f))
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Струм АКБ макс.", "${metrics.maxDcBatteryCurrentA} А", Modifier.weight(1f))
                            SpecPill("Кабель АКБ", "${metrics.recommendedBatteryCableMm2} мм²", Modifier.weight(1f))
                            SpecPill("Запобіжник АКБ", "${metrics.recommendedBatteryFuseA} А", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Bulk заряд", "$bulkV В", Modifier.weight(1f))
                            SpecPill("Float буфер", "$floatV В", Modifier.weight(1f))
                            SpecPill("Відключення", "$cutV В", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecPill("Кут (${config.city.cityName})", "${metrics.effectiveTiltDegrees}°", Modifier.weight(1f))
                            SpecPill("Літо", "${"%.1f".format(metrics.estimatedSummerDailyKwh)} кВт·г/д", Modifier.weight(1f))
                            SpecPill("Зима", "${"%.1f".format(metrics.estimatedWinterDailyKwh)} кВт·г/д", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        groupedBom.forEach { (category, itemsInCat) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(itemsInCat, key = { it.id }) { bom ->
                val isChecked = bom.id in uiState.checkedBomIds
                BomItemCard(
                    item = bom,
                    checked = isChecked,
                    onToggle = { onToggleBomChecked(bom.id) }
                )
            }
        }
    }
}

@Composable
private fun SpecPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BomItemCard(
    item: BomItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .testTag("bom_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .minimumInteractiveComponentSize(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = item.quantity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = item.specification,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
