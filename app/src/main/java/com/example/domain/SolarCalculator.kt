package com.example.domain

import com.example.model.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object SolarCalculator {

    fun defaultAppliances(): List<ApplianceLoad> = listOf(
        ApplianceLoad("router", "Wi-Fi роутер + ONU оптика", 20, 24f, 1.0f, "Зв'язок", true),
        ApplianceLoad("lights", "LED освітлення дому", 80, 6f, 1.0f, "Освітлення", true),
        ApplianceLoad("laptop", "Ноутбуки та зарядки", 90, 5f, 1.0f, "Офіс", true),
        ApplianceLoad("fridge", "Холодильник (клас A+)", 140, 8f, 4.0f, "Кухня", true),
        ApplianceLoad("boiler_pump", "Газовий котел + цирк. насос", 130, 10f, 2.0f, "Опалення", true),
        ApplianceLoad("tv", "Телевізор", 110, 4f, 1.0f, "Побут", false),
        ApplianceLoad("well_pump", "Свердловинний насос", 900, 0.8f, 3.0f, "Вода", false),
        ApplianceLoad("microwave", "Мікрохвильова піч", 1200, 0.3f, 1.2f, "Кухня", false),
        ApplianceLoad("washer", "Пральна машина (нагрів)", 2000, 1.0f, 1.5f, "Побут", false),
        ApplianceLoad("kettle", "Електрочайник", 2000, 0.25f, 1.0f, "Кухня", false),
        ApplianceLoad("ac_unit", "Кондиціонер інверторний", 1100, 4.0f, 1.5f, "Клімат", false),
        ApplianceLoad("water_heater", "Електробойлер (1.5 кВт)", 1500, 2.5f, 1.0f, "Вода", false)
    )

    fun presetConfig(tier: StationTier): SolarStationConfig {
        val baseAppliances = defaultAppliances()
        return when (tier) {
            StationTier.MINI -> SolarStationConfig(
                tier = StationTier.MINI,
                systemVoltage = SystemVoltage.V12,
                panelWattage = 330,
                seriesCount = 2,
                parallelStrings = 1,
                controllerType = ControllerType.MPPT,
                batteryChemistry = BatteryChemistry.LIFEPO4,
                batteryCapacityAh = 100,
                inverterWatts = 800,
                inverterType = InverterType.PURE_SINE_OFFGRID,
                panelTilt = PanelTilt.YEAR_ROUND_35,
                panelOrientation = PanelOrientation.SOUTH,
                includeSpdProtection = true,
                includeGeneratorAts = false,
                includeSmartBms = true,
                appliances = baseAppliances.map {
                    it.copy(selected = it.id in setOf("router", "lights", "laptop", "fridge"))
                }
            )

            StationTier.BASIC -> SolarStationConfig(
                tier = StationTier.BASIC,
                systemVoltage = SystemVoltage.V24,
                panelWattage = 410,
                seriesCount = 4,
                parallelStrings = 1,
                controllerType = ControllerType.MPPT,
                batteryChemistry = BatteryChemistry.LIFEPO4,
                batteryCapacityAh = 150,
                inverterWatts = 3000,
                inverterType = InverterType.HYBRID_MPPT,
                panelTilt = PanelTilt.YEAR_ROUND_35,
                panelOrientation = PanelOrientation.SOUTH,
                includeSpdProtection = true,
                includeGeneratorAts = false,
                includeSmartBms = true,
                appliances = baseAppliances.map {
                    it.copy(selected = it.id in setOf("router", "lights", "laptop", "fridge", "boiler_pump", "tv"))
                }
            )

            StationTier.OPTIMAL -> SolarStationConfig(
                tier = StationTier.OPTIMAL,
                systemVoltage = SystemVoltage.V48,
                panelWattage = 450,
                seriesCount = 8,
                parallelStrings = 1,
                controllerType = ControllerType.MPPT,
                batteryChemistry = BatteryChemistry.LIFEPO4,
                batteryCapacityAh = 150,
                inverterWatts = 5000,
                inverterType = InverterType.HYBRID_MPPT,
                panelTilt = PanelTilt.YEAR_ROUND_35,
                panelOrientation = PanelOrientation.SOUTH,
                includeSpdProtection = true,
                includeGeneratorAts = true,
                includeSmartBms = true,
                appliances = baseAppliances.map {
                    it.copy(selected = it.id in setOf("router", "lights", "laptop", "fridge", "boiler_pump", "tv", "well_pump", "microwave", "washer"))
                }
            )

            StationTier.MAXI -> SolarStationConfig(
                tier = StationTier.MAXI,
                systemVoltage = SystemVoltage.V48,
                panelWattage = 550,
                seriesCount = 9,
                parallelStrings = 2,
                controllerType = ControllerType.MPPT,
                batteryChemistry = BatteryChemistry.LIFEPO4,
                batteryCapacityAh = 300,
                inverterWatts = 10000,
                inverterType = InverterType.HYBRID_MPPT,
                panelTilt = PanelTilt.WINTER_55,
                panelOrientation = PanelOrientation.SOUTH,
                includeSpdProtection = true,
                includeGeneratorAts = true,
                includeSmartBms = true,
                appliances = baseAppliances.map { it.copy(selected = true) }
            )
        }
    }

    fun calculateMetrics(config: SolarStationConfig): CalculatedStationMetrics {
        val totalPanels = config.totalPanels
        val totalPanelWatts = totalPanels * config.panelWattage

        // Realistic electrical parameters by panel wattage class
        val (singleVoc, singleVmp) = when {
            config.panelWattage >= 480 -> 49.6f to 41.5f
            config.panelWattage >= 380 -> 37.8f to 31.6f
            else -> 40.2f to 33.4f
        }
        val singleImp = config.panelWattage / singleVmp

        val arrayVocNominal = singleVoc * config.seriesCount
        val arrayVocWinter = arrayVocNominal * 1.15f // +15% safety margin at -20°C
        val arrayVmp = singleVmp * config.seriesCount
        val arrayImp = singleImp * config.parallelStrings

        val rawMpptCurrent = (totalPanelWatts.toFloat() / config.systemVoltage.volts) * config.controllerType.efficiency
        val recommendedMpptCurrentA = listOf(30, 40, 60, 80, 100, 120, 150, 200)
            .firstOrNull { it >= ceil(rawMpptCurrent.toDouble()).toInt() } ?: 200

        val requiredMpptMaxVoc = when {
            arrayVocWinter <= 95f -> 100
            arrayVocWinter <= 145f -> 150
            arrayVocWinter <= 240f -> 250
            arrayVocWinter <= 430f -> 450
            else -> 500
        }

        val batteryNominalWh = config.systemVoltage.volts * config.batteryCapacityAh
        val usableBatteryWh = (batteryNominalWh * (config.batteryChemistry.dodPercent / 100f)).roundToInt()

        val maxDcBatteryCurrentA = (config.inverterWatts / (config.systemVoltage.volts * 0.90f)).roundToInt()
        val recommendedBatteryCableMm2 = when {
            maxDcBatteryCurrentA <= 35 -> 10
            maxDcBatteryCurrentA <= 60 -> 16
            maxDcBatteryCurrentA <= 95 -> 25
            maxDcBatteryCurrentA <= 130 -> 35
            maxDcBatteryCurrentA <= 175 -> 50
            maxDcBatteryCurrentA <= 230 -> 70
            else -> 95
        }

        val recommendedSolarCableMm2 = when {
            arrayImp <= 15f -> 4
            arrayImp <= 30f -> 6
            else -> 10
        }

        val rawBatteryFuse = (maxDcBatteryCurrentA * 1.25f).roundToInt()
        val recommendedBatteryFuseA = listOf(40, 60, 80, 100, 125, 150, 200, 250, 300, 350)
            .firstOrNull { it >= rawBatteryFuse } ?: 350

        val rawSolarBreaker = (arrayImp * 1.3f).roundToInt()
        val recommendedSolarBreakerA = listOf(16, 20, 25, 32, 40, 50, 63)
            .firstOrNull { it >= rawSolarBreaker } ?: 63

        val rawAcCurrent = (config.inverterWatts / 230f) * 1.2f
        val recommendedAcBreakerA = listOf(6, 10, 16, 20, 25, 32, 40, 50, 63)
            .firstOrNull { it >= ceil(rawAcCurrent.toDouble()).toInt() } ?: 63

        // Solar Yield Calculation (Ukraine average PSH: ~4.9h summer, ~1.25h winter)
        val (summerTiltFactor, winterTiltFactor) = when (config.panelTilt) {
            PanelTilt.SUMMER_25 -> 1.05f to 0.78f
            PanelTilt.YEAR_ROUND_35 -> 1.00f to 0.94f
            PanelTilt.WINTER_55 -> 0.85f to 1.15f
        }
        val systemDerate = config.controllerType.efficiency * 0.86f
        val estimatedSummerDailyKwh = (totalPanelWatts / 1000f) * 4.9f * summerTiltFactor * config.panelOrientation.summerFactor * systemDerate
        val estimatedWinterDailyKwh = (totalPanelWatts / 1000f) * 1.25f * winterTiltFactor * config.panelOrientation.winterFactor * systemDerate

        val selectedApps = config.appliances.filter { it.selected }
        val dailyLoadKwh = selectedApps.sumOf { (it.watts * it.dailyHours).toDouble() }.toFloat() / 1000f
        val peakLoadWatts = selectedApps.sumOf { it.watts }
        val maxSingleSurge = selectedApps.maxOfOrNull { (it.watts * it.surgeMultiplier).roundToInt() } ?: 0
        val surgeLoadWatts = max(peakLoadWatts, maxSingleSurge)

        val avgHourlyWatts = max(50f, (dailyLoadKwh * 1000f) / 24f)
        val autonomyHours = usableBatteryWh / avgHourlyWatts

        // Engineering Alerts & Safety Diagnostics
        val alerts = mutableListOf<EngineeringAlert>()

        if (config.systemVoltage == SystemVoltage.V12 && config.inverterWatts > 1500) {
            alerts += EngineeringAlert(
                title = "Надто високий струм для 12В системи",
                message = "При ${config.inverterWatts} Вт на шині 12В струм сягає $maxDcBatteryCurrentA А. Перейдіть на 24В або 48В для безпеки та тонших кабелів.",
                severity = AlertSeverity.CRITICAL
            )
        }
        if (config.systemVoltage == SystemVoltage.V24 && config.inverterWatts > 4000) {
            alerts += EngineeringAlert(
                title = "Рекомендовано 48В для потужності > 4 кВт",
                message = "Струм АКБ $maxDcBatteryCurrentA А потребує кабелю $recommendedBatteryCableMm2 мм². Система 48В зменшить струм удвічі.",
                severity = AlertSeverity.WARNING
            )
        }
        if (config.controllerType == ControllerType.PWM && config.seriesCount > 1) {
            alerts += EngineeringAlert(
                title = "PWM контролер не підтримує високу напругу",
                message = "При послідовному з'єднанні ${config.seriesCount}S напруга ${arrayVocNominal.roundToInt()} В спалить PWM контролер або втратить >50% потужності. Оберіть MPPT.",
                severity = AlertSeverity.CRITICAL
            )
        }
        if (arrayVocWinter > 450f && config.inverterType == InverterType.HYBRID_MPPT) {
            alerts += EngineeringAlert(
                title = "Перевірте ліміт напруги MPPT інвертора",
                message = "Зимова напруга холостого ходу при -20°C складе ${arrayVocWinter.roundToInt()} В. Стандартні гібридні інвертори мають межу 450–500 В (для 2 стрінгів краще 2 MPPT).",
                severity = AlertSeverity.WARNING
            )
        }
        if (config.inverterWatts > batteryNominalWh * 1.1f && config.batteryChemistry == BatteryChemistry.LIFEPO4) {
            alerts += EngineeringAlert(
                title = "Ємність АКБ замала для піку інвертора",
                message = "Інвертор ${config.inverterWatts} Вт може вимагати струм розряду вище 1C (${config.batteryCapacityAh} А). Переконайтеся, що BMS розрахована на $maxDcBatteryCurrentA А.",
                severity = AlertSeverity.WARNING
            )
        }
        if (config.batteryChemistry == BatteryChemistry.AGM_GEL && config.inverterWatts > batteryNominalWh * 0.5f) {
            alerts += EngineeringAlert(
                title = "Високе навантаження на AGM/GEL батарею",
                message = "Свинцеві АКБ швидко деградують при струмах вище 0.3C. Для тривалої роботи краще перейти на LiFePO4.",
                severity = AlertSeverity.WARNING
            )
        }
        if (dailyLoadKwh > estimatedSummerDailyKwh * 1.05f && selectedApps.isNotEmpty()) {
            alerts += EngineeringAlert(
                title = "Дефіцит сонячної генерації",
                message = "Добове споживання (${"%.1f".format(dailyLoadKwh)} кВт·год) перевищує літній виробіток (${"%.1f".format(estimatedSummerDailyKwh)} кВт·год). Додайте панелей або зменште навантаження.",
                severity = AlertSeverity.WARNING
            )
        }
        if (peakLoadWatts > config.inverterWatts) {
            alerts += EngineeringAlert(
                title = "Пікове навантаження перевищує інвертор",
                message = "Сума обраних приладів ($peakLoadWatts Вт) більша за номінал інвертора (${config.inverterWatts} Вт). Не вмикайте потужні прилади одночасно або оберіть більший інвертор.",
                severity = AlertSeverity.WARNING
            )
        }
        if (alerts.isEmpty()) {
            alerts += EngineeringAlert(
                title = "Конфігурація збалансована",
                message = "Напруга поля (${arrayVmp.roundToInt()} В), ємність АКБ (${"%.1f".format(usableBatteryWh / 1000f)} кВт·год корисних) та потужність інвертора узгоджені.",
                severity = AlertSeverity.INFO
            )
        }

        // 24h energy curve simulation (hour 0..23)
        val hourlyCurve = buildHourlyCurve(
            totalPanelWatts = totalPanelWatts,
            summerDailyKwh = estimatedSummerDailyKwh,
            winterDailyKwh = estimatedWinterDailyKwh,
            dailyLoadKwh = max(0.3f, dailyLoadKwh),
            usableBatteryWh = max(500, usableBatteryWh)
        )

        return CalculatedStationMetrics(
            totalPanelWatts = totalPanelWatts,
            singlePanelVoc = singleVoc,
            singlePanelVmp = singleVmp,
            singlePanelImp = singleImp,
            arrayVocNominal = arrayVocNominal,
            arrayVocWinterMinus20 = arrayVocWinter,
            arrayVmp = arrayVmp,
            arrayImp = arrayImp,
            recommendedMpptCurrentA = recommendedMpptCurrentA,
            requiredMpptMaxVoc = requiredMpptMaxVoc,
            batteryNominalWh = batteryNominalWh,
            usableBatteryWh = usableBatteryWh,
            maxDcBatteryCurrentA = maxDcBatteryCurrentA,
            recommendedBatteryCableMm2 = recommendedBatteryCableMm2,
            recommendedSolarCableMm2 = recommendedSolarCableMm2,
            recommendedBatteryFuseA = recommendedBatteryFuseA,
            recommendedSolarBreakerA = recommendedSolarBreakerA,
            recommendedAcBreakerA = recommendedAcBreakerA,
            estimatedSummerDailyKwh = estimatedSummerDailyKwh,
            estimatedWinterDailyKwh = estimatedWinterDailyKwh,
            dailyLoadKwh = dailyLoadKwh,
            peakLoadWatts = peakLoadWatts,
            surgeLoadWatts = surgeLoadWatts,
            autonomyHours = autonomyHours,
            alerts = alerts,
            hourlyCurve = hourlyCurve
        )
    }

    private fun buildHourlyCurve(
        totalPanelWatts: Int,
        summerDailyKwh: Float,
        winterDailyKwh: Float,
        dailyLoadKwh: Float,
        usableBatteryWh: Int
    ): List<HourlyEnergyPoint> {
        val rawSummerShape = (0..23).map { h ->
            when {
                h in 6..19 -> kotlin.math.sin(((h - 5.5) / 14.0) * Math.PI).toFloat().coerceAtLeast(0f)
                else -> 0f
            }
        }
        val rawWinterShape = (0..23).map { h ->
            when {
                h in 9..15 -> kotlin.math.sin(((h - 8.5) / 7.0) * Math.PI).toFloat().coerceAtLeast(0f)
                else -> 0f
            }
        }
        val summerSum = rawSummerShape.sum().coerceAtLeast(1f)
        val winterSum = rawWinterShape.sum().coerceAtLeast(1f)

        val loadWeights = (0..23).map { h ->
            when (h) {
                in 0..5 -> 0.45f
                in 6..9 -> 1.25f
                in 10..16 -> 0.90f
                in 17..22 -> 1.55f
                else -> 0.70f
            }
        }
        val loadSum = loadWeights.sum()

        var soc = 55f
        return (0..23).map { h ->
            val summerW = (rawSummerShape[h] / summerSum) * summerDailyKwh * 1000f
            val winterW = (rawWinterShape[h] / winterSum) * winterDailyKwh * 1000f
            val loadW = (loadWeights[h] / loadSum) * dailyLoadKwh * 1000f
            val netWh = summerW - loadW
            soc = (soc + (netWh / usableBatteryWh) * 100f).coerceIn(10f, 100f)
            HourlyEnergyPoint(
                hour = h,
                summerSolarW = summerW.coerceAtMost(totalPanelWatts.toFloat()),
                winterSolarW = winterW,
                loadW = loadW,
                batterySocPercent = soc
            )
        }
    }

    fun generateBom(config: SolarStationConfig, m: CalculatedStationMetrics): List<BomItem> {
        val items = mutableListOf<BomItem>()

        items += BomItem(
            id = "panels",
            category = "Генерація (ФЕМ)",
            title = "Сонячні панелі ${config.panelWattage} Вт (Mono Half-Cell)",
            specification = "Схема ${config.seriesCount}S × ${config.parallelStrings}P • Загалом ${m.totalPanelWatts} Вт • Voc(зима) ${m.arrayVocWinterMinus20.roundToInt()} В",
            quantity = "${config.totalPanels} шт.",
            rationale = "Забезпечує до ${"%.1f".format(m.estimatedSummerDailyKwh)} кВт·год/день влітку"
        )

        items += BomItem(
            id = "mounting",
            category = "Генерація (ФЕМ)",
            title = "Кріплення (алюм. профіль, притиски, гаки/гвинт-шурупи)",
            specification = "Кут нахилу ${config.panelTilt.degrees}° • Орієнтація: ${config.panelOrientation.label}",
            quantity = "На ${config.totalPanels} панелей",
            rationale = "Алюмінієвий профіль 40×40 мм + заземлення рам панелей"
        )

        items += BomItem(
            id = "solar_cable",
            category = "Кабелі та комутація",
            title = "Сонячний кабель PV1-F (подвійна ізоляція, УФ-стійкий)",
            specification = "Переріз ${m.recommendedSolarCableMm2} мм² (червоний + чорний)",
            quantity = "2 × 15–25 м",
            rationale = "Розраховано під робочий струм поля ${"%.1f".format(m.arrayImp)} А з мінімальними втратами"
        )

        items += BomItem(
            id = "mc4",
            category = "Кабелі та комутація",
            title = "Конектори MC4" + if (config.parallelStrings > 1) " + Y-розгалужувачі" else "",
            specification = "IP68, розраховані на 1000–1500 В DC, 30 А",
            quantity = if (config.parallelStrings > 1) "${config.parallelStrings * 2 + 2} пар + 1 пара Y" else "4 пари",
            rationale = "Обтиск лише спеціалізованим кримпером для MC4"
        )

        if (config.inverterType == InverterType.HYBRID_MPPT) {
            items += BomItem(
                id = "inverter",
                category = "Силова електроніка",
                title = "Гібридний інвертор ${(config.inverterWatts / 1000f)} кВт (${config.systemVoltage.label})",
                specification = "Чистий синус • Вбуд. MPPT до ${m.requiredMpptMaxVoc} В / ${m.recommendedMpptCurrentA} А",
                quantity = "1 шт.",
                rationale = "Сумісний з LiFePO4 профілями заряду та байпасом"
            )
        } else {
            items += BomItem(
                id = "mppt",
                category = "Силова електроніка",
                title = "Контролер заряду ${config.controllerType.shortName} ${m.recommendedMpptCurrentA} А",
                specification = "Шина ${config.systemVoltage.label} • Макс. вхід PV ≥ ${m.requiredMpptMaxVoc} В",
                quantity = "1 шт.",
                rationale = "Керує зарядом АКБ за оптимальною точкою потужності"
            )
            items += BomItem(
                id = "inverter_standalone",
                category = "Силова електроніка",
                title = "Автономний інвертор ${config.inverterWatts} Вт (Чистий синус)",
                specification = "Вхід ${config.systemVoltage.label} DC → Вихід 230 В 50 Гц",
                quantity = "1 шт.",
                rationale = "Чистий синус критичний для холодильників, насосів і котлів"
            )
        }

        items += BomItem(
            id = "battery",
            category = "Накопичення енергії",
            title = "Акумуляторний банк ${config.batteryChemistry.shortName} ${config.systemVoltage.label} ${config.batteryCapacityAh} А·год",
            specification = "Номінал ${"%.1f".format(m.batteryNominalWh / 1000f)} кВт·год (корисних ${"%.1f".format(m.usableBatteryWh / 1000f)} кВт·год) • ${config.systemVoltage.cellsSeries}S конфігурація",
            quantity = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) "${config.systemVoltage.cellsSeries} комірок або 1 блок" else "${config.systemVoltage.volts / 12} шт. по 12В",
            rationale = "Ресурс: ${config.batteryChemistry.cycleLife}"
        )

        if (config.includeSmartBms && config.batteryChemistry == BatteryChemistry.LIFEPO4) {
            val bmsRating = listOf(60, 100, 150, 200, 250).firstOrNull { it >= m.maxDcBatteryCurrentA } ?: 250
            items += BomItem(
                id = "bms",
                category = "Накопичення енергії",
                title = "Smart BMS + активний балансир (${config.systemVoltage.cellsSeries}S ${bmsRating} А)",
                specification = "Bluetooth моніторинг, захист від перезаряду, перерозряду та морозу (<0°C)",
                quantity = "1 шт.",
                rationale = "Обов'язково для LiFePO4! Витримує струм ${m.maxDcBatteryCurrentA} А"
            )
        }

        items += BomItem(
            id = "battery_cable",
            category = "Кабелі та комутація",
            title = "Силовий мідний кабель КГ / ПВ-3 (АКБ ↔ Інвертор)",
            specification = "Переріз ${m.recommendedBatteryCableMm2} мм² + мідні луджені наконечники під болт M8",
            quantity = "2 × 1.5 м (червоний/чорний)",
            rationale = "Тримає ${m.maxDcBatteryCurrentA} А без нагріву; мінімальна довжина"
        )

        items += BomItem(
            id = "dc_fuse",
            category = "Захист і автоматика",
            title = "Силовий запобіжник АКБ (MEGA / ANL / NH00) + тримач",
            specification = "Номінал ${m.recommendedBatteryFuseA} А DC (на плюсовий дріт біля клеми АКБ)",
            quantity = "1 компл.",
            rationale = "Рятує від пожежі при короткому замиканні АКБ (струм КЗ >1000 А)"
        )

        items += BomItem(
            id = "pv_breaker",
            category = "Захист і автоматика",
            title = "Двополюсний DC-автомат або роз'єднувач сонячного поля",
            specification = "Номінал ${m.recommendedSolarBreakerA} А, напруга DC ≥ 500–1000 В (не AC!)",
            quantity = "${config.parallelStrings} шт.",
            rationale = "Для безпечного відключення панелей при обслуговуванні"
        )

        if (config.includeSpdProtection) {
            items += BomItem(
                id = "spd",
                category = "Захист і автоматика",
                title = "УЗІП (обмежувач імпульсних перенапруг) DC PV + AC",
                specification = "DC SPD Тип II (500–1000В) + AC SPD Тип II (275В) + заземлення ПВ-3 10 мм²",
                quantity = "1 компл.",
                rationale = "Захист інвертора від грозових наведень на даху"
            )
        }

        items += BomItem(
            id = "ac_panel",
            category = "Захист і автоматика",
            title = "AC-щиток: Автомат ${m.recommendedAcBreakerA} А + ПЗВ (УЗО тип A) + Реле напруги",
            specification = "Вхід/вихід 230 В • Струм витоку ПЗВ 30 мА",
            quantity = "1 щит у зборі",
            rationale = "Захист людей від ураження струмом та побутової техніки"
        )

        if (config.includeGeneratorAts) {
            items += BomItem(
                id = "ats",
                category = "Захист і автоматика",
                title = "Перекидний рубильник (I-0-II) або АВР + ввід генератора",
                specification = "32–63 А для безпечного перемикання Мережа / СЕС / Генератор",
                quantity = "1 шт.",
                rationale = "Виключає зустрічну напругу при підключенні резервного генератора"
            )
        }

        items += BomItem(
            id = "earthing",
            category = "Захист і автоматика",
            title = "Комплект контуру заземлення",
            specification = "Оцинковані/мідні стержні (4.5–6 м) + кабель 10–16 мм² (PE ≤ 4 Ом)",
            quantity = "1 компл.",
            rationale = "Для заземлення рам панелей, корпусу інвертора та УЗІП"
        )

        return items
    }

    fun generateBuildSteps(config: SolarStationConfig, m: CalculatedStationMetrics): List<BuildStep> {
        val bulkV = "%.1f".format(config.batteryChemistry.chargeVoltsPer12V * (config.systemVoltage.volts / 12f))
        val floatV = "%.1f".format(config.batteryChemistry.floatVoltsPer12V * (config.systemVoltage.volts / 12f))
        val cutV = "%.1f".format(config.batteryChemistry.cutoffVoltsPer12V * (config.systemVoltage.volts / 12f))

        return listOf(
            BuildStep(
                number = 1,
                title = "Аудит навантажень та підготовка інструменту",
                subtitle = "Перевірка реальної потужності та безпечна комплектація",
                estimatedTime = "2–3 год",
                safetyLevel = StepSafetyLevel.STANDARD,
                summary = "Перед закупівлею перевірте ватметром реальне добове споживання (у вашому проєкті: ${"%.1f".format(m.dailyLoadKwh)} кВт·год/день) та пускові струми холодильника або насоса (до ${m.surgeLoadWatts} Вт).",
                checklist = listOf(
                    "Підготуйте мультиметр із вимірюванням DC до 600В",
                    "Підготуйте кримпер для MC4 та гідравлічний прес для силових наконечників ${m.recommendedBatteryCableMm2} мм²",
                    "Переконайтеся, що переріз проводки в будинку витримує потужність ${config.inverterWatts} Вт"
                ),
                dynamicRecommendation = "Для вашої станції (${config.systemVoltage.label}, ${config.inverterWatts} Вт) не використовуйте подовжувачі з тонким дротом — підключайте живлення у вхідний щиток через перекидний рубильник.",
                commonMistake = "Ігнорування пускового струму холодильника чи насоса. Асинхронний двигун на 150 Вт при старті бере 600–800 Вт на 1–2 секунди."
            ),
            BuildStep(
                number = 2,
                title = "Місце монтажу, вентиляція та пожежна безпека",
                subtitle = "Підготовка негорючої основи та температурного режиму",
                estimatedTime = "2–4 год",
                safetyLevel = StepSafetyLevel.IMPORTANT,
                summary = "Інвертор виділяє тепло і шумить вентиляторами, а акумулятори бояться морозу та перегріву. Монтуйте обладнання у сухому провітрюваному приміщенні (гараж, котельня, коридор).",
                checklist = listOf(
                    "Стіна для інвертора — бетон, цегла або лист ЦСП/ГВЛ (НЕ на дерево чи пінопласт!)",
                    "Залиште вільні зазори навколо інвертора: по 30 см зверху і знизу, по 20 см з боків",
                    "Розмістіть АКБ на негорючій полиці нижче інвертора, подалі від радіаторів опалення",
                    "Встановіть контур заземлення та підведіть шину PE (жовто-зелений 10–16 мм²)"
                ),
                dynamicRecommendation = if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                    "Увага: LiFePO4 АКБ КАТЕГОРИЧНО не можна заряджати при температурі нижче 0°C — це незворотно руйнує комірки. Температура в приміщенні має бути +5…+30°C."
                else
                    "Свинцеві AGM/GEL батареї при перезаряді можуть виділяти гази — забезпечте природну вентиляцію приміщення.",
                commonMistake = "Монтаж інвертора на дерев'яну вагонку або у закриту шафу без припливу повітря — призводить до перегріву та ризику займання."
            ),
            BuildStep(
                number = 3,
                title = "Монтаж сонячних панелей та траси PV",
                subtitle = "Кріплення на дах/каркас під кутом ${config.panelTilt.degrees}°",
                estimatedTime = "4–8 год",
                safetyLevel = StepSafetyLevel.CRITICAL,
                summary = "Встановіть ${config.totalPanels} панелей за схемою ${config.seriesCount}S × ${config.parallelStrings}P (орієнтація: ${config.panelOrientation.label}). Панелі на сонці завжди під високою напругою — накривайте їх картоном під час обтиску!",
                checklist = listOf(
                    "Закріпіть алюмінієвий профіль до крокв даху з урахуванням вітрового навантаження",
                    "Заземліть алюмінієві рами панелей та профіль на окрему шину PE (не на громовідвід!)",
                    "З'єднайте панелі за схемою ${config.seriesCount}S (плюс однієї в мінус наступної)",
                    "Прокладіть кабель PV1-F ${m.recommendedSolarCableMm2} мм² у гофротрубі, захищеній від УФ",
                    "Перевірте полярність і напругу холостого ходу мультиметром (очікувано ~${m.arrayVocNominal.roundToInt()} В DC)"
                ),
                dynamicRecommendation = "Використовуйте тільки спеціалізований DC-автомат ${m.recommendedSolarBreakerA} А на вході в приміщення" +
                    (if (config.includeSpdProtection) " разом з DC УЗІП перед інвертором." else "."),
                commonMistake = "Використання звичайного AC-автомата на сонячну лінію DC! Постійний струм не переходить через нуль — у звичайному AC-автоматі виникає електрична дуга і пожежа."
            ),
            BuildStep(
                number = 4,
                title = "Збірка та балансування акумуляторної батареї",
                subtitle = "${config.batteryChemistry.shortName} ${config.systemVoltage.label} ${config.batteryCapacityAh} А·год (${config.systemVoltage.cellsSeries}S)",
                estimatedTime = "2–3 год",
                safetyLevel = StepSafetyLevel.CRITICAL,
                summary = if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                    "Якщо ви збираєте батарею з призматичних комірок ${config.systemVoltage.cellsSeries}S, обов'язково проведіть вирівнювання напруги (Top Balancing) перед збіркою та правильно підключіть балансувальний шлейф BMS."
                else
                    "Для збірки з ${config.systemVoltage.volts / 12} шт. 12В батарей спершу зарядіть кожну окремо до однакового рівня, щоб уникнути розбалансу.",
                checklist = listOf(
                    "Очистіть контакти шин і клем, затягуйте болти динамометричним ключем (зазвичай 6–8 Н·м для M8)",
                    "Встановіть силовий запобіжник ${m.recommendedBatteryFuseA} А безпосередньо біля плюсової клеми (+)",
                    if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                        "Підключіть балансувальні дроти BMS послідовно від B- до B+ та перевірте розпіновку мультиметром перед підключенням плати BMS"
                    else
                        "З'єднайте батареї перемичками ${m.recommendedBatteryCableMm2} мм² однакової довжини",
                    "Перевірте сумарну напругу на виході блоку АКБ (~${config.systemVoltage.volts} В)"
                ),
                dynamicRecommendation = "При підключенні батареї до інвертора спершу подайте живлення через керамічний резистор 10–30 Ом (10 Вт) на 3–5 секунд для плавного заряду конденсаторів інвертора (анти-іскра).",
                commonMistake = "Пряме перше підключення потужної LiFePO4 АКБ без резистора передзаряду — потужна іскра пошкоджує клеми або може вибити ключі BMS."
            ),
            BuildStep(
                number = 5,
                title = "Силова DC-комутація та монтаж AC-щита",
                subtitle = "Підключення кабелями ${m.recommendedBatteryCableMm2} мм² та захист виходу 230В",
                estimatedTime = "2–3 год",
                safetyLevel = StepSafetyLevel.CRITICAL,
                summary = "Обтисніть наконечники кабелю ${m.recommendedBatteryCableMm2} мм² гідравлічним пресом і закрийте термоусадкою з клеєм. Зберіть AC-щиток із ПЗВ 30 мА, автоматом ${m.recommendedAcBreakerA} А та реле напруги.",
                checklist = listOf(
                    "Прокладіть короткі (до 1.5 м) кабелі однакової довжини від АКБ до інвертора",
                    "Підключіть вихід інвертора (AC OUT) через двополюсний автомат ${m.recommendedAcBreakerA} А та ПЗВ типу A (30 мА)",
                    "Встановіть перекидний трипозиційний рубильник I-0-II (Мережа / Нуль / Інвертор)",
                    "Ніколи не з'єднуйте нуль (N) виходу інвертора напряму з міським нулем без розриву!"
                ),
                dynamicRecommendation = if (config.includeGeneratorAts)
                    "Для генератора використовуйте окремий перекидний рубильник на вхід AC IN інвертора або встановіть реле пріоритету, щоб генератор ніколи не зустрівся з міською мережею."
                else
                    "Обов'язково підпишіть усі автомати на щитку (PV DC, АКБ, AC Вхід, AC Вихід, ПЗВ) — це збереже обладнання в екстреній ситуації.",
                commonMistake = "«Виделка-виделка» в розетку — смертельно небезпечно! Підключайте будинок ТІЛЬКИ через перекидний рубильник у щитку."
            ),
            BuildStep(
                number = 6,
                title = "Безпечний порядок першого запуску",
                subtitle = "Критична послідовність: АКБ → Інвертор → Сонце → Навантаження",
                estimatedTime = "30 хв",
                safetyLevel = StepSafetyLevel.CRITICAL,
                summary = "Порушення черговості ввімкнення — найчастіша причина виходу з ладу MPPT контролерів. Виконуйте пуск строго крок за кроком:",
                checklist = listOf(
                    "1. Усі автомати (PV DC, AC вхід/вихід) ВИМКНЕНІ",
                    "2. Зробіть передзаряд конденсаторів резистором і увімкніть запобіжник/рубильник АКБ",
                    "3. Увімкніть кнопку живлення інвертора — переконайтесь, що екран засвітився без помилок",
                    "4. Налаштуйте тип АКБ і напруги заряду (крок 7)",
                    "5. Увімкніть DC-автомат сонячних панелей — переконайтеся, що пішов заряд від сонця",
                    "6. Поступово увімкніть автомати AC навантаження (спочатку світло/роутер, потім холодильник)"
                ),
                dynamicRecommendation = "Для вимкнення станції дійте у ЗВОРОТНОМУ порядку: 1) Вимкнути навантаження AC → 2) Вимкнути PV DC панелі → 3) Вимкнути інвертор → 4) Відключити АКБ.",
                commonMistake = "Відключення акумулятора при підключених сонячних панелях! Контролер/інвертор без буфера АКБ отримує стрибок напруги і миттєво згорає."
            ),
            BuildStep(
                number = 7,
                title = "Налаштування профілю заряду та обслуговування",
                subtitle = "Точні параметри для ${config.batteryChemistry.shortName} ${config.systemVoltage.label}",
                estimatedTime = "1 год",
                safetyLevel = StepSafetyLevel.IMPORTANT,
                summary = "Виставте у меню інвертора/контролера ручні налаштування (режим User / USE), щоб уникнути перезаряду або недозаряду батареї.",
                checklist = listOf(
                    "Напруга основного заряду (Bulk / Absorption): $bulkV В",
                    "Напруга підтримання (Float): $floatV В",
                    "Поріг відключення розряду (Low DC Cut-off): $cutV В",
                    "Максимальний струм заряду (AC + PV): не більше ${(config.batteryCapacityAh * 0.5f).roundToInt()} А для довгого ресурсу",
                    "Через 1 тиждень роботи підтягніть усі гвинтові клеми та перевірте нагрів кабелів рукою або тепловізором"
                ),
                dynamicRecommendation = if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                    "Вимкніть функцію вирівнювального заряду (Equalization) у меню інвертора! Для LiFePO4 високовольтна еквілізація небезпечна."
                else
                    "Для AGM/GEL не розряджайте батарею нижче 50% ($cutV В під помірним навантаженням), інакше за один сезон ємність впаде вдвічі.",
                commonMistake = "Залишення заводських налаштувань під свинцевий АКБ для LiFePO4 батареї з увімкненою функцією десульфатації (15.5В / 31В / 62В)."
            )
        )
    }
}
