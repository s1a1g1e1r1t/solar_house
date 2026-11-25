package com.example.model

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
    MINI(
        title = "Міні (Дача / Балкон)",
        badge = "12В • 0.8 кВт",
        subtitle = "Освітлення, роутер, ноутбуки, зарядки, малий холодильник",
        targetUse = "Найпростіший старт без складного електромонтажу"
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

enum class PanelTilt(val degrees: Int, val label: String, val hint: String) {
    SUMMER_25(25, "25° (Літо)", "Максимум генерації з квітня по вересень"),
    YEAR_ROUND_35(35, "35° (Річний)", "Оптимальний баланс для України на весь рік"),
    WINTER_55(55, "55° (Зима / Автономія)", "Краще ловить низьке зимове сонце, менше снігу")
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
    ARRAY_AND_BATTERY("Панелі та АКБ", "Комутація"),
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
