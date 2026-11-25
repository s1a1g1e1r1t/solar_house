package com.example.model

import kotlin.math.roundToInt

enum class MainDestination(val route: String, val label: String) {
    CONFIGURATOR("configurator", "Проєкт"),
    BLUEPRINTS("blueprints", "Креслення"),
    SPECIFICATION("specification", "Специфікація"),
    INSTRUCTIONS("instructions", "Інструкція")
}

enum class StationTier(
    val title: String,
    val badge: String,
    val subtitle: String,
    val targetUse: String
) {
    DIRECT_HEATER(
        title = "Сонячний камін (Без АКБ)",
        badge = "Тільки ФЕМ + Камін",
        subtitle = "Мінімальна схема: панелі напряму гріють електрокамін без АКБ та інвертора",
        targetUse = "Найпростіший варіант денного обігріву кімнати від сонця"
    ),
    MINI(
        title = "Міні (Дача / Балкон)",
        badge = "12В • 0.8 кВт",
        subtitle = "Освітлення, роутер, ноутбуки, зарядки, малий холодильник",
        targetUse = "Базова автономність без складного електромонтажу"
    ),
    BASIC(
        title = "Базовий (Квартира / Малий дім)",
        badge = "24В • 3 кВт",
        subtitle = "Холодильник, газовий котел, освітлення, ТБ, роутер, ПК",
        targetUse = "Надійний резерв і денна автономність критичних приладів"
    ),
    OPTIMAL(
        title = "Оптимальний (Приватний дім)",
        badge = "48В • 5 кВт",
        subtitle = "Повний комфорт: насос, пральна машина, мікрохвильовка, котел",
        targetUse = "Збалансована автономна станція для щоденного життя"
    ),
    MAXI(
        title = "Максі (Повна автономія)",
        badge = "48В • 10 кВт",
        subtitle = "Бойлер, кондиціонер, електроплита, майстерня + резерв генератора",
        targetUse = "Висока потужність, 2 MPPT трекери, тривала автономність"
    )
}

enum class CityLocation(
    val cityName: String,
    val latitude: Float,
    val summerPsh: Float,
    val winterPsh: Float
) {
    KYIV("Київ", 50.45f, 4.9f, 1.15f),
    LVIV("Львів", 49.84f, 4.5f, 1.10f),
    ODESA("Одеса", 46.48f, 5.5f, 1.55f),
    KHARKIV("Харків", 49.99f, 4.9f, 1.20f),
    DNIPRO("Дніпро", 48.46f, 5.1f, 1.35f),
    ZAPORIZHZHIA("Запоріжжя", 47.84f, 5.2f, 1.40f),
    VINNYTSIA("Вінниця", 49.23f, 4.8f, 1.20f),
    POLTAVA("Полтава", 49.59f, 4.9f, 1.22f),
    IVANO_FRANKIVSK("Івано-Франківськ", 48.92f, 4.6f, 1.18f),
    UZHHOROD("Ужгород", 48.62f, 4.8f, 1.25f),
    CHERNIHIV("Чернігів", 51.49f, 4.6f, 1.05f),
    SUMY("Суми", 50.91f, 4.7f, 1.08f),
    MYKOLAIV("Миколаїв", 46.97f, 5.4f, 1.50f),
    CHERKASY("Черкаси", 49.44f, 5.0f, 1.25f),
    ZHYTOMYR("Житомир", 50.25f, 4.8f, 1.12f),
    TERNOPIL("Тернопіль", 49.55f, 4.6f, 1.14f),
    CHERNIVTSI("Чернівці", 48.29f, 4.8f, 1.25f);

    fun recommendedAngle(season: PanelTilt): Int {
        return when (season) {
            PanelTilt.SUMMER_25 -> (latitude - 15f).roundToInt().coerceIn(15, 65)
            PanelTilt.YEAR_ROUND_35 -> (latitude - 10f).roundToInt().coerceIn(15, 65)
            PanelTilt.WINTER_55 -> (latitude + 10f).roundToInt().coerceIn(15, 65)
        }
    }
}

enum class RoomInsulation(val label: String, val wattsPerSqMeter: Int) {
    GOOD("Добре утеплена (70 Вт/м²)", 70),
    AVERAGE("Стандартна (100 Вт/м²)", 100),
    POOR("Слабке / дача (130 Вт/м²)", 130)
}

enum class SystemVoltage(val volts: Int, val label: String, val cellsSeries: Int) {
    V12(12, "12 В", 4),
    V24(24, "24 В", 8),
    V48(48, "48 В", 16)
}

enum class BatteryChemistry(
    val title: String,
    val shortName: String,
    val dodPercent: Int,
    val cycleLife: String,
    val chargeVoltsPer12V: Float,
    val floatVoltsPer12V: Float,
    val cutoffVoltsPer12V: Float
) {
    LIFEPO4(
        title = "LiFePO4 (Літій-залізо-фосфат)",
        shortName = "LiFePO4",
        dodPercent = 85,
        cycleLife = "4000–6000 циклів",
        chargeVoltsPer12V = 14.4f,
        floatVoltsPer12V = 13.6f,
        cutoffVoltsPer12V = 11.2f
    ),
    AGM_GEL(
        title = "AGM / GEL (Свинцево-кислотний)",
        shortName = "AGM/GEL",
        dodPercent = 50,
        cycleLife = "400–700 циклів",
        chargeVoltsPer12V = 14.4f,
        floatVoltsPer12V = 13.5f,
        cutoffVoltsPer12V = 11.8f
    )
}

enum class ControllerType(val title: String, val shortName: String, val efficiency: Float) {
    MPPT("MPPT (Макс. точка потужності)", "MPPT", 0.96f),
    PWM("PWM (ШІМ — бюджетний для 12/24В)", "PWM", 0.74f)
}

enum class InverterType(val title: String, val shortName: String) {
    PURE_SINE_OFFGRID("Автономний чистий синус + окремий MPPT", "Off-Grid"),
    HYBRID_MPPT("Гібридний інвертор (Все-в-одному MPPT)", "Гібрид")
}

enum class PanelTilt(val degrees: Int, val label: String, val shortLabel: String, val hint: String) {
    SUMMER_25(25, "Літо (Широта − 15°)", "Літо", "Максимум генерації з квітня по вересень"),
    YEAR_ROUND_35(35, "Річний (Широта − 10°)", "Річний", "Оптимальний баланс для України на весь рік"),
    WINTER_55(55, "Зима / Обігрів (Широта + 10°)", "Зима", "Краще ловить низьке зимове сонце, менше снігу")
}

enum class PanelOrientation(val label: String, val summerFactor: Float, val winterFactor: Float) {
    SOUTH("Південь (180°)", 1.0f, 1.0f),
    EAST_WEST("Схід – Захід", 0.88f, 0.78f)
}

data class ApplianceLoad(
    val id: String,
    val name: String,
    val watts: Int,
    val dailyHours: Float,
    val surgeMultiplier: Float = 1.0f,
    val category: String,
    val selected: Boolean
)

data class SolarStationConfig(
    val tier: StationTier = StationTier.BASIC,
    val isDirectSolarHeater: Boolean = false,
    val roomAreaM2: Int = 16,
    val roomInsulation: RoomInsulation = RoomInsulation.AVERAGE,
    val heaterRatedWatts: Int = 1500,
    val city: CityLocation = CityLocation.KYIV,
    val useCityAutoTilt: Boolean = true,
    val manualTiltDegrees: Int = 40,
    val systemVoltage: SystemVoltage = SystemVoltage.V24,
    val panelWattage: Int = 410,
    val seriesCount: Int = 4,
    val parallelStrings: Int = 1,
    val controllerType: ControllerType = ControllerType.MPPT,
    val batteryChemistry: BatteryChemistry = BatteryChemistry.LIFEPO4,
    val batteryCapacityAh: Int = 150,
    val inverterWatts: Int = 3000,
    val inverterType: InverterType = InverterType.HYBRID_MPPT,
    val panelTilt: PanelTilt = PanelTilt.YEAR_ROUND_35,
    val panelOrientation: PanelOrientation = PanelOrientation.SOUTH,
    val includeSpdProtection: Boolean = true,
    val includeGeneratorAts: Boolean = false,
    val includeSmartBms: Boolean = true,
    val appliances: List<ApplianceLoad> = emptyList()
) {
    val totalPanels: Int get() = seriesCount * parallelStrings
    val effectiveTiltDegrees: Int
        get() = if (useCityAutoTilt) city.recommendedAngle(panelTilt) else manualTiltDegrees
}

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

data class EngineeringAlert(
    val title: String,
    val message: String,
    val severity: AlertSeverity
)

data class HourlyEnergyPoint(
    val hour: Int,
    val summerSolarW: Float,
    val winterSolarW: Float,
    val loadW: Float,
    val batterySocPercent: Float
)

data class CalculatedStationMetrics(
    val totalPanelWatts: Int,
    val singlePanelVoc: Float,
    val singlePanelVmp: Float,
    val singlePanelImp: Float,
    val arrayVocNominal: Float,
    val arrayVocWinterMinus20: Float,
    val arrayVmp: Float,
    val arrayImp: Float,
    val recommendedMpptCurrentA: Int,
    val requiredMpptMaxVoc: Int,
    val batteryNominalWh: Int,
    val usableBatteryWh: Int,
    val maxDcBatteryCurrentA: Int,
    val recommendedBatteryCableMm2: Int,
    val recommendedSolarCableMm2: Int,
    val recommendedBatteryFuseA: Int,
    val recommendedSolarBreakerA: Int,
    val recommendedAcBreakerA: Int,
    val estimatedSummerDailyKwh: Float,
    val estimatedWinterDailyKwh: Float,
    val dailyLoadKwh: Float,
    val peakLoadWatts: Int,
    val surgeLoadWatts: Int,
    val autonomyHours: Float,
    val recommendedHeaterWattsForRoom: Int,
    val heaterCoveragePercent: Int,
    val effectiveTiltDegrees: Int,
    val alerts: List<EngineeringAlert>,
    val hourlyCurve: List<HourlyEnergyPoint>
)

data class BomItem(
    val id: String,
    val category: String,
    val title: String,
    val specification: String,
    val quantity: String,
    val rationale: String,
    val isEssential: Boolean = true
)

enum class StepSafetyLevel(val label: String) {
    STANDARD("Стандарт"),
    IMPORTANT("Важливо"),
    CRITICAL("Критична безпека")
}

data class BuildStep(
    val number: Int,
    val title: String,
    val subtitle: String,
    val estimatedTime: String,
    val safetyLevel: StepSafetyLevel,
    val summary: String,
    val checklist: List<String>,
    val dynamicRecommendation: String,
    val commonMistake: String
)

enum class BlueprintType(val title: String, val shortTitle: String) {
    WIRING_SCHEMATIC("Принципова схема", "Електросхема"),
    ARRAY_AND_BATTERY("Панелі та навантаження", "Комутація"),
    PHYSICAL_MOUNTING("Монтаж і зазори", "Монтаж"),
    ENERGY_BALANCE("Енергобаланс 24г", "Графік 24г")
}

data class SchematicNodeInfo(
    val id: String,
    val title: String,
    val primarySpec: String,
    val wiringSpec: String,
    val safetyNote: String
)
