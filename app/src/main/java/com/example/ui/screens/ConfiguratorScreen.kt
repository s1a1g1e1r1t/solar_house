package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SolarUiState
import kotlin.math.roundToInt

@Composable
fun ConfiguratorScreen(
    uiState: SolarUiState,
    onApplyPreset: (StationTier) -> Unit,
    onCityChange: (CityLocation) -> Unit,
    onUseCityAutoTiltChange: (Boolean) -> Unit,
    onManualTiltChange: (Int) -> Unit,
    onRoomAreaChange: (Int) -> Unit,
    onRoomInsulationChange: (RoomInsulation) -> Unit,
    onVoltageChange: (SystemVoltage) -> Unit,
    onInverterTypeChange: (InverterType) -> Unit,
    onControllerTypeChange: (ControllerType) -> Unit,
    onInverterWattsChange: (Int) -> Unit,
    onPanelWattageChange: (Int) -> Unit,
    onSeriesChange: (Int) -> Unit,
    onParallelChange: (Int) -> Unit,
    onTiltChange: (PanelTilt) -> Unit,
    onOrientationChange: (PanelOrientation) -> Unit,
    onChemistryChange: (BatteryChemistry) -> Unit,
    onCapacityChange: (Int) -> Unit,
    onToggleSpd: (Boolean) -> Unit,
    onToggleAts: (Boolean) -> Unit,
    onToggleSmartBms: (Boolean) -> Unit,
    onToggleAppliance: (String) -> Unit,
    onOpenBlueprints: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    val metrics = uiState.metrics
    val solarColors = LocalSolarColors.current

    var expandLoads by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(196.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_solar_home_hero),
                    contentDescription = "Автономна сонячна станція для дому",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BlueprintNavy.copy(alpha = 0.25f),
                                    BlueprintNavy.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = SolarAmber,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        val badgeText = if (config.isDirectSolarHeater) {
                            "ФЕМ ${metrics.totalPanelWatts} Вт → Камін ${config.roomAreaM2} м² • ${config.city.cityName} (${metrics.effectiveTiltDegrees}°)"
                        } else {
                            "${config.systemVoltage.label} • ${metrics.totalPanelWatts} Вт ФЕМ • ${config.inverterWatts} Вт • ${config.city.cityName} (${metrics.effectiveTiltDegrees}°)"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSolarAmber,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column {
                        Text(
                            text = if (config.isDirectSolarHeater) "Сонячний обігрів без АКБ" else "Конструктор СЕС для дому",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Оберіть рівень проєкту від прямого каміна до повної автономії дому",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // 2. Presets Carousel (Direct Heater -> Міні -> Максі)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Готові варіанти станції",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(StationTier.entries) { tier ->
                        val selected = config.tier == tier
                        PresetTierCard(
                            tier = tier,
                            selected = selected,
                            onClick = { onApplyPreset(tier) }
                        )
                    }
                }
            }
        }

        // 3. Live KPI Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiMiniCard(
                    label = "Панелі",
                    value = "${metrics.totalPanelWatts} Вт",
                    sub = "${config.totalPanels} шт (${config.seriesCount}S×${config.parallelStrings}P)",
                    icon = Icons.Filled.WbSunny,
                    accent = SolarAmberDark,
                    modifier = Modifier.weight(1f)
                )
                if (config.isDirectSolarHeater) {
                    KpiMiniCard(
                        label = "Обігрів",
                        value = "${config.roomAreaM2} м²",
                        sub = "Потреба ~${metrics.recommendedHeaterWattsForRoom} Вт",
                        icon = Icons.Filled.Fireplace,
                        accent = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMiniCard(
                        label = "Покриття",
                        value = "${metrics.heaterCoveragePercent}%",
                        sub = "Кут ${metrics.effectiveTiltDegrees}° (${config.city.cityName})",
                        icon = Icons.Filled.Explore,
                        accent = BlueprintCyan,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    KpiMiniCard(
                        label = "Корисн. АКБ",
                        value = "${"%.1f".format(metrics.usableBatteryWh / 1000f)} кВт·г",
                        sub = "${config.batteryChemistry.shortName} ${config.batteryCapacityAh}А·г",
                        icon = Icons.Filled.BatteryChargingFull,
                        accent = EmeraldOk,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMiniCard(
                        label = "Автономність",
                        value = "${"%.1f".format(metrics.autonomyHours)} год",
                        sub = "Кут ${metrics.effectiveTiltDegrees}° (${config.city.cityName})",
                        icon = Icons.Filled.Schedule,
                        accent = BlueprintCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Engineering Alerts
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.alerts.forEach { alert ->
                    val (bg, fg, icon) = when (alert.severity) {
                        AlertSeverity.CRITICAL -> Triple(solarColors.dangerContainer, solarColors.dangerText, Icons.Filled.Error)
                        AlertSeverity.WARNING -> Triple(solarColors.warningContainer, solarColors.warningText, Icons.Filled.Warning)
                        AlertSeverity.INFO -> Triple(solarColors.okContainer, solarColors.okText, Icons.Filled.CheckCircle)
                    }
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(alert.title, style = MaterialTheme.typography.titleSmall, color = fg)
                                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = fg)
                            }
                        }
                    }
                }
            }
        }

        // 5. Direct Solar Heater Room Area Card (only in DIRECT_HEATER mode)
        if (config.isDirectSolarHeater) {
            item {
                SectionCard(
                    title = "1. Електрокамін та площа кімнати",
                    icon = Icons.Filled.Fireplace
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Площа кімнати для обігріву:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${config.roomAreaM2} м²",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = config.roomAreaM2.toFloat(),
                        onValueChange = { onRoomAreaChange(it.roundToInt()) },
                        valueRange = 6f..50f,
                        modifier = Modifier.testTag("room_area_slider")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(10, 15, 20, 25, 35, 45)) { area ->
                            FilterChip(
                                selected = config.roomAreaM2 == area,
                                onClick = { onRoomAreaChange(area) },
                                label = { Text("$area м²") },
                                modifier = Modifier.testTag("room_area_chip_$area")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Рівень утеплення приміщення:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoomInsulation.entries.forEach { ins ->
                            SelectableOptionRow(
                                title = ins.label,
                                selected = config.roomInsulation == ins,
                                onClick = { onRoomInsulationChange(ins) }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Розрахункова потужність каміна: ~${metrics.recommendedHeaterWattsForRoom} Вт • Панелі дають до ${metrics.totalPanelWatts} Вт (${metrics.heaterCoveragePercent}% в ясну погоду, взимку ~${"%.1f".format(metrics.estimatedWinterDailyKwh)} кВт·год/день).",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // 6. Location (City) & Optimal Tilt Angle Card (for ALL modes!)
        item {
            SectionCard(
                title = if (config.isDirectSolarHeater) "2. Місто будинку та кут нахилу ФЕМ" else "1. Місто будинку та кут нахилу ФЕМ",
                icon = Icons.Filled.LocationOn
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Місто розташування будинку:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${config.city.cityName} (${"%.1f".format(config.city.latitude)}°)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CityLocation.entries) { city ->
                        FilterChip(
                            selected = config.city == city,
                            onClick = { onCityChange(city) },
                            label = { Text(city.cityName) },
                            modifier = Modifier.testTag("city_${city.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Сезонний пріоритет генерації:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelTilt.entries.forEach { t ->
                        val angleForCity = config.city.recommendedAngle(t)
                        FilterChip(
                            selected = config.panelTilt == t,
                            onClick = { onTiltChange(t) },
                            label = { Text("${t.shortLabel} ($angleForCity°)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("season_tilt_${t.name.lowercase()}")
                        )
                    }
                }

                SwitchRowItem(
                    title = "Автоматичний кут за широтою м. ${config.city.cityName}",
                    subtitle = "Оптимум зараз: ${config.city.recommendedAngle(config.panelTilt)}° (${config.panelTilt.hint})",
                    checked = config.useCityAutoTilt,
                    onCheckedChange = onUseCityAutoTiltChange
                )

                AnimatedVisibility(visible = !config.useCityAutoTilt) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ручний кут даху/каркасу:", style = MaterialTheme.typography.labelLarge)
                            Text("${config.manualTiltDegrees}°", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = config.manualTiltDegrees.toFloat(),
                            onValueChange = { onManualTiltChange(it.roundToInt()) },
                            valueRange = 15f..65f
                        )
                    }
                }

                Text("Орієнтація панелей:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelOrientation.entries.forEach { o ->
                        FilterChip(
                            selected = config.panelOrientation == o,
                            onClick = { onOrientationChange(o) },
                            label = { Text(o.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 7. Section: Solar Array (for ALL modes!)
        item {
            SectionCard(
                title = if (config.isDirectSolarHeater) "3. Сонячні панелі та комутація" else "2. Сонячне поле (ФЕМ)",
                icon = Icons.Filled.SolarPower
            ) {
                Text("Потужність 1 панелі:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(330, 410, 450, 550).forEach { w ->
                        FilterChip(
                            selected = config.panelWattage == w,
                            onClick = { onPanelWattageChange(w) },
                            label = { Text("$w Вт") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepperControl(
                        label = "Послідовно (S)",
                        valueText = "${config.seriesCount}S",
                        onMinus = { onSeriesChange(config.seriesCount - 1) },
                        onPlus = { onSeriesChange(config.seriesCount + 1) },
                        modifier = Modifier.weight(1f),
                        tagPrefix = "series"
                    )
                    StepperControl(
                        label = "Стрінгів (P)",
                        valueText = "${config.parallelStrings}P",
                        onMinus = { onParallelChange(config.parallelStrings - 1) },
                        onPlus = { onParallelChange(config.parallelStrings + 1) },
                        modifier = Modifier.weight(1f),
                        tagPrefix = "parallel"
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Поле: ${config.totalPanels} шт = ${metrics.totalPanelWatts} Вт • Робоча Vmp ${metrics.arrayVmp.roundToInt()}В • Хол. хід (-20°C) ${metrics.arrayVocWinterMinus20.roundToInt()}В • Струм ${"%.1f".format(metrics.arrayImp)}А",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 8. Sections ONLY for Battery-backed stations (hidden in Direct Solar Heater mode)
        if (!config.isDirectSolarHeater) {
            item {
                SectionCard(
                    title = "3. Системна напруга та інвертор",
                    icon = Icons.Filled.ElectricalServices
                ) {
                    Text("Номінальна напруга шини АКБ:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SystemVoltage.entries.forEach { v ->
                            FilterChip(
                                selected = config.systemVoltage == v,
                                onClick = { onVoltageChange(v) },
                                label = { Text(v.label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voltage_${v.volts}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Архітектура системи:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InverterType.entries.forEach { type ->
                            SelectableOptionRow(
                                title = type.title,
                                selected = config.inverterType == type,
                                onClick = { onInverterTypeChange(type) }
                            )
                        }
                    }

                    if (config.inverterType == InverterType.PURE_SINE_OFFGRID) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Тип контролера заряду:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ControllerType.entries.forEach { c ->
                                FilterChip(
                                    selected = config.controllerType == c,
                                    onClick = { onControllerTypeChange(c) },
                                    label = { Text(c.shortName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Потужність інвертора:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${config.inverterWatts} Вт (${"%.1f".format(config.inverterWatts / 1000f)} кВт)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val inverterSteps = listOf(800, 1500, 3000, 5000, 8000, 10000)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(inverterSteps) { w ->
                            FilterChip(
                                selected = config.inverterWatts == w,
                                onClick = { onInverterWattsChange(w) },
                                label = { Text(if (w >= 1000) "${w / 1000f} кВт".replace(".0", "") else "$w Вт") }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "4. Акумуляторний банк (АКБ)",
                    icon = Icons.Filled.BatteryStd
                ) {
                    Text("Тип хімії АКБ:", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BatteryChemistry.entries.forEach { chem ->
                            SelectableOptionRow(
                                title = "${chem.title} • ${chem.cycleLife} (DoD ${chem.dodPercent}%)",
                                selected = config.batteryChemistry == chem,
                                onClick = { onChemistryChange(chem) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ємність банку на шині ${config.systemVoltage.label}:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${config.batteryCapacityAh} А·год (${"%.1f".format(metrics.batteryNominalWh / 1000f)} кВт·год)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = config.batteryCapacityAh.toFloat(),
                        onValueChange = { onCapacityChange((it / 50f).roundToInt() * 50) },
                        valueRange = 50f..500f,
                        steps = 8,
                        modifier = Modifier.testTag("battery_capacity_slider")
                    )

                    if (config.batteryChemistry == BatteryChemistry.LIFEPO4) {
                        SwitchRowItem(
                            title = "Smart BMS з активним балансиром",
                            subtitle = "Захист комірок та моніторинг по Bluetooth",
                            checked = config.includeSmartBms,
                            onCheckedChange = onToggleSmartBms
                        )
                    }
                }
            }
        }

        // 9. Section: Protection
        item {
            SectionCard(
                title = if (config.isDirectSolarHeater) "4. Захист лінії постійного струму" else "5. Захист та резервне живлення",
                icon = Icons.Filled.Security
            ) {
                SwitchRowItem(
                    title = if (config.isDirectSolarHeater) "Грозозахист (DC УЗІП)" else "УЗІП (грозозахист) по DC і AC",
                    subtitle = "Захист від імпульсних грозових перенапруг на даху",
                    checked = config.includeSpdProtection,
                    onCheckedChange = onToggleSpd
                )
                if (!config.isDirectSolarHeater) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SwitchRowItem(
                        title = "Введення генератора / перекидний рубильник",
                        subtitle = "Аварійна зарядка АКБ у похмурі зимові тижні",
                        checked = config.includeGeneratorAts,
                        onCheckedChange = onToggleAts
                    )
                }
            }
        }

        // 10. Section: Appliances Load Calculator (only for battery systems)
        if (!config.isDirectSolarHeater) {
            item {
                SectionCard(
                    title = "6. Калькулятор навантаження дому",
                    icon = Icons.Filled.Devices
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { expandLoads = !expandLoads }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Споживання: ${"%.2f".format(metrics.dailyLoadKwh)} кВт·год/добу • Пік ${metrics.peakLoadWatts} Вт",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (expandLoads) "Натисніть, щоб згорнути список приладів" else "Натисніть, щоб обрати побутові прилади",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (expandLoads) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = expandLoads) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            config.appliances.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggleAppliance(app.id) }
                                        .minimumInteractiveComponentSize()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = app.selected,
                                        onCheckedChange = { onToggleAppliance(app.id) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${app.watts} Вт × ${app.dailyHours} год = ${(app.watts * app.dailyHours).roundToInt()} Вт·год/д",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 11. CTA to Blueprints
        item {
            Button(
                onClick = onOpenBlueprints,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp)
                    .testTag("open_blueprints_cta"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Architecture, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Відкрити креслення станції", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PresetTierCard(
    tier: StationTier,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .width(232.dp)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("preset_${tier.name.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = tier.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = tier.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tier.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KpiMiniCard(
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StepperControl(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("${tagPrefix}_minus")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Зменшити $label")
                }
                Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalIconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("${tagPrefix}_plus")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Збільшити $label")
                }
            }
        }
    }
}

@Composable
private fun SwitchRowItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .minimumInteractiveComponentSize()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
