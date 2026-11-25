package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private data class HitTarget(
    val rect: Rect,
    val info: SchematicNodeInfo
)

@Composable
fun InteractiveBlueprintCanvas(
    blueprintType: BlueprintType,
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics,
    zoom: Float,
    pan: Offset,
    onTransform: (zoomChange: Float, panChange: Offset) -> Unit,
    selectedNodeId: String?,
    onNodeTapped: (SchematicNodeInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hitTargets = remember { mutableStateListOf<HitTarget>() }
    val solarColors = LocalSolarColors.current

    // Logical canvas coordinate space: 1000 x 720
    val logicalW = 1000f
    val logicalH = 720f

    Box(
        modifier = modifier
            .background(solarColors.blueprintBg)
            .clipToBounds()
            .pointerInput(blueprintType) {
                detectTransformGestures { _, panDelta, zoomDelta, _ ->
                    onTransform(zoomDelta, panDelta)
                }
            }
            .pointerInput(blueprintType, config, zoom, pan) {
                detectTapGestures { tapOffset ->
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val baseScale = min(canvasW / logicalW, canvasH / logicalH) * 0.96f
                    val totalScale = baseScale * zoom
                    val originX = (canvasW - logicalW * totalScale) / 2f + pan.x
                    val originY = (canvasH - logicalH * totalScale) / 2f + pan.y

                    val lx = (tapOffset.x - originX) / totalScale
                    val ly = (tapOffset.y - originY) / totalScale
                    val hit = hitTargets.lastOrNull { it.rect.contains(Offset(lx, ly)) }
                    onNodeTapped(hit?.info)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseScale = min(size.width / logicalW, size.height / logicalH) * 0.96f
            val totalScale = baseScale * zoom
            val originX = (size.width - logicalW * totalScale) / 2f + pan.x
            val originY = (size.height - logicalH * totalScale) / 2f + pan.y

            // Draw Blueprint Grid across full viewport
            drawBlueprintGrid(
                gridColor = solarColors.blueprintGrid,
                step = 32f * zoom.coerceIn(0.75f, 2.0f)
            )

            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(originX, originY)
                canvas.scale(totalScale, totalScale)

                hitTargets.clear()
                when (blueprintType) {
                    BlueprintType.WIRING_SCHEMATIC -> drawWiringSchematic(
                        config = config,
                        metrics = metrics,
                        selectedNodeId = selectedNodeId,
                        hitTargets = hitTargets
                    )

                    BlueprintType.ARRAY_AND_BATTERY -> drawArrayAndBatteryBlueprint(
                        config = config,
                        metrics = metrics,
                        selectedNodeId = selectedNodeId,
                        hitTargets = hitTargets
                    )

                    BlueprintType.PHYSICAL_MOUNTING -> drawMountingBlueprint(
                        config = config,
                        metrics = metrics,
                        selectedNodeId = selectedNodeId,
                        hitTargets = hitTargets
                    )

                    BlueprintType.ENERGY_BALANCE -> drawEnergyBalanceChart(
                        config = config,
                        metrics = metrics
                    )
                }

                // CAD Border & Title Block
                drawTitleBlock(config, metrics, blueprintType)

                canvas.restore()
            }
        }
    }
}

private fun DrawScope.drawBlueprintGrid(gridColor: Color, step: Float) {
    val w = size.width
    val h = size.height
    var x = 0f
    var idx = 0
    while (x <= w) {
        val alpha = if (idx % 4 == 0) 0.55f else 0.22f
        val stroke = if (idx % 4 == 0) 1.3f else 0.7f
        drawLine(
            color = gridColor.copy(alpha = alpha),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = stroke
        )
        x += step
        idx++
    }
    var y = 0f
    idx = 0
    while (y <= h) {
        val alpha = if (idx % 4 == 0) 0.55f else 0.22f
        val stroke = if (idx % 4 == 0) 1.3f else 0.7f
        drawLine(
            color = gridColor.copy(alpha = alpha),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = stroke
        )
        y += step
        idx++
    }
}

private fun DrawScope.drawWiringSchematic(
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics,
    selectedNodeId: String?,
    hitTargets: MutableList<HitTarget>
) {
    // Outer frame
    drawRect(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
        topLeft = Offset(10f, 10f),
        size = Size(980f, 700f),
        style = Stroke(width = 2f)
    )

    // 1. PV ARRAY (Top Left)
    val pvRect = Rect(36f, 45f, 256f, 195f)
    drawComponentBox(
        rect = pvRect,
        title = "СОНЯЧНЕ ПОЛЕ (ФЕМ)",
        line1 = "${config.totalPanels} × ${config.panelWattage} Вт (${metrics.totalPanelWatts} Вт)",
        line2 = "Схема: ${config.seriesCount}S × ${config.parallelStrings}P",
        line3 = "Voc: ${metrics.arrayVocWinterMinus20.roundToInt()}В • Imp: ${"%.1f".format(metrics.arrayImp)}А",
        accent = SolarAmber,
        isSelected = selectedNodeId == "pv",
        badge = "PV"
    )
    hitTargets += HitTarget(
        pvRect,
        SchematicNodeInfo(
            id = "pv",
            title = "Сонячні панелі (${config.totalPanels} шт.)",
            primarySpec = "${metrics.totalPanelWatts} Вт • Vmp ${metrics.arrayVmp.roundToInt()} В • Voc(зима) ${metrics.arrayVocWinterMinus20.roundToInt()} В",
            wiringSpec = "Сонячний кабель PV1-F ${metrics.recommendedSolarCableMm2} мм² (подвійна ізоляція, УФ-стійкий)",
            safetyNote = "Збирайте масив у похмуру погоду або накривайте панелі картоном. Перевіряйте полярність мультиметром перед підключенням."
        )
    )

    // 2. PV DC PROTECTION + SPD (Top Center-Left)
    val pvProtRect = Rect(316f, 55f, 516f, 185f)
    val spdText = if (config.includeSpdProtection) "+ УЗІП PV 1000В" else "Без УЗІП (реком.)"
    drawComponentBox(
        rect = pvProtRect,
        title = "DC ЗАХИСТ PV",
        line1 = "DC Автомат ${metrics.recommendedSolarBreakerA}А (2P)",
        line2 = spdText,
        line3 = "Кабель PV: ${metrics.recommendedSolarCableMm2} мм²",
        accent = Color(0xFFFB7185),
        isSelected = selectedNodeId == "pv_prot",
        badge = "DC"
    )
    hitTargets += HitTarget(
        pvProtRect,
        SchematicNodeInfo(
            id = "pv_prot",
            title = "Щит захисту сонячної лінії (PV)",
            primarySpec = "Двополюсний DC-автомат ${metrics.recommendedSolarBreakerA} А" + if (config.includeSpdProtection) " + DC УЗІП Тип II" else "",
            wiringSpec = "Підключення до шини заземлення PE кабелем 10 мм²",
            safetyNote = "ЗАБОРОНЕНО ставити звичайний AC-автомат на постійний струм PV — виникає незатухаюча дуга!"
        )
    )

    // Wire PV -> PV Protection
    drawLabeledWire(Offset(256f, 95f), Offset(316f, 95f), WireDcPositive, "+PV", Offset(286f, 83f))
    drawLabeledWire(Offset(256f, 145f), Offset(316f, 145f), WireDcNegative, "-PV", Offset(286f, 165f))

    // 3. POWER ELECTRONICS (Center)
    val isHybrid = config.inverterType == InverterType.HYBRID_MPPT
    val invRect: Rect
    if (isHybrid) {
        invRect = Rect(575f, 55f, 800f, 340f)
        drawComponentBox(
            rect = invRect,
            title = "ГІБРИДНИЙ ІНВЕРТОР",
            line1 = "${config.inverterWatts} Вт • Чистий синус",
            line2 = "MPPT: ${metrics.recommendedMpptCurrentA}А (до ${mMaxVoc(metrics)}В)",
            line3 = "Шина АКБ: ${config.systemVoltage.label}",
            accent = BlueprintCyanBright,
            isSelected = selectedNodeId == "inverter",
            badge = "INV"
        )
        hitTargets += HitTarget(
            invRect,
            SchematicNodeInfo(
                id = "inverter",
                title = "Гібридний інвертор ${(config.inverterWatts / 1000f)} кВт",
                primarySpec = "Чистий синус 230В • Вбудований MPPT ${metrics.recommendedMpptCurrentA}А / ${mMaxVoc(metrics)}В",
                wiringSpec = "АКБ підключається кабелем ${metrics.recommendedBatteryCableMm2} мм², PV вхід ${metrics.recommendedSolarCableMm2} мм²",
                safetyNote = "Зазор для охолодження не менше 30 см зверху і знизу. Увімкнення ТІЛЬКИ після підключення АКБ!"
            )
        )

        // Wires PV Prot -> Hybrid Inverter
        drawLabeledWire(Offset(516f, 95f), Offset(575f, 95f), WireDcPositive, "${metrics.recommendedSolarCableMm2}мм²", Offset(545f, 82f))
        drawLabeledWire(Offset(516f, 145f), Offset(575f, 145f), WireDcNegative, "", Offset.Zero)
    } else {
        // Standalone MPPT + Off-grid Inverter
        val mpptRect = Rect(565f, 45f, 755f, 185f)
        drawComponentBox(
            rect = mpptRect,
            title = "${config.controllerType.shortName} КОНТРОЛЕР",
            line1 = "Струм: ${metrics.recommendedMpptCurrentA} А",
            line2 = "Макс PV: ${mMaxVoc(metrics)} В",
            line3 = "Вихід: ${config.systemVoltage.label}",
            accent = EmeraldOk,
            isSelected = selectedNodeId == "mppt",
            badge = "MPPT"
        )
        hitTargets += HitTarget(
            mpptRect,
            SchematicNodeInfo(
                id = "mppt",
                title = "Контролер заряду ${config.controllerType.shortName} ${metrics.recommendedMpptCurrentA}А",
                primarySpec = "Шина ${config.systemVoltage.label} • Макс. напруга PV ≥ ${mMaxVoc(metrics)} В",
                wiringSpec = "Вхід PV ${metrics.recommendedSolarCableMm2} мм², вихід на АКБ ${max(10, metrics.recommendedBatteryCableMm2 / 2)} мм²",
                safetyNote = "Завжди підключайте АКБ до контролера ПЕРШИМ, а сонячні панелі — ДРУГИМ."
            )
        )
        drawLabeledWire(Offset(516f, 95f), Offset(565f, 95f), WireDcPositive, "", Offset.Zero)
        drawLabeledWire(Offset(516f, 145f), Offset(565f, 145f), WireDcNegative, "", Offset.Zero)

        invRect = Rect(565f, 235f, 775f, 385f)
        drawComponentBox(
            rect = invRect,
            title = "ІНВЕРТОР 230В",
            line1 = "${config.inverterWatts} Вт (Чист. синус)",
            line2 = "Вхід: ${config.systemVoltage.label} DC",
            line3 = "Вихід: 230В 50Гц",
            accent = BlueprintCyanBright,
            isSelected = selectedNodeId == "inverter",
            badge = "INV"
        )
        hitTargets += HitTarget(
            invRect,
            SchematicNodeInfo(
                id = "inverter",
                title = "Автономний інвертор ${config.inverterWatts} Вт",
                primarySpec = "Чистий синус 230В • Піковий струм по DC: ${metrics.maxDcBatteryCurrentA} А",
                wiringSpec = "Силовий кабель до АКБ ${metrics.recommendedBatteryCableMm2} мм² (довжина ≤ 1.5 м)",
                safetyNote = "Не перевантажуйте вище ${config.inverterWatts} Вт; для двигунів враховуйте пускові струми."
            )
        )
    }

    // 4. BATTERY BANK + FUSE + BMS (Bottom Center-Left)
    val fuseRect = Rect(335f, 380f, 515f, 495f)
    drawComponentBox(
        rect = fuseRect,
        title = "ЗАПОБІЖНИК АКБ",
        line1 = "${metrics.recommendedBatteryFuseA} А (MEGA / NH00)",
        line2 = "Плюсова шина (+)",
        line3 = "Кабель: ${metrics.recommendedBatteryCableMm2} мм²",
        accent = WireDcPositive,
        isSelected = selectedNodeId == "dc_fuse",
        badge = "FUSE"
    )
    hitTargets += HitTarget(
        fuseRect,
        SchematicNodeInfo(
            id = "dc_fuse",
            title = "Силовий запобіжник АКБ ${metrics.recommendedBatteryFuseA} А",
            primarySpec = "Встановлюється на (+) провід якомога ближче до клеми акумулятора",
            wiringSpec = "Мідний кабель ${metrics.recommendedBatteryCableMm2} мм² з лудженими наконечниками M8",
            safetyNote = "Захищає проводку від пожежі при КЗ. Без запобіжника LiFePO4 видає >1500 А при замиканні!"
        )
    )

    val batRect = Rect(36f, 370f, 276f, 540f)
    val bmsLabel = if (config.batteryChemistry == BatteryChemistry.LIFEPO4 && config.includeSmartBms) "Smart BMS ${config.systemVoltage.cellsSeries}S" else "Без BMS (${config.batteryChemistry.shortName})"
    drawComponentBox(
        rect = batRect,
        title = "АКУМУЛЯТОР (${config.batteryChemistry.shortName})",
        line1 = "${config.systemVoltage.label} • ${config.batteryCapacityAh} А·год",
        line2 = "Корисних: ${"%.1f".format(metrics.usableBatteryWh / 1000f)} кВт·год",
        line3 = bmsLabel,
        accent = EmeraldOk,
        isSelected = selectedNodeId == "battery",
        badge = "BAT"
    )
    hitTargets += HitTarget(
        batRect,
        SchematicNodeInfo(
            id = "battery",
            title = "Акумулятор ${config.batteryChemistry.shortName} ${config.systemVoltage.label} ${config.batteryCapacityAh} А·год",
            primarySpec = "Загальна ємність ${"%.1f".format(metrics.batteryNominalWh / 1000f)} кВт·год • Автономність ~${"%.1f".format(metrics.autonomyHours)} год",
            wiringSpec = "Макс. струм розряду ${metrics.maxDcBatteryCurrentA} А • Переріз ${metrics.recommendedBatteryCableMm2} мм²",
            safetyNote = if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                "Не заряджати при t < 0°C! Використовуйте передзаряд резистором при підключенні інвертора."
            else
                "Не розряджайте нижче 50% ємності для збереження ресурсу."
        )
    )

    // Battery <-> Fuse <-> Inverter/MPPT DC Bus Wires
    drawLabeledWire(Offset(276f, 420f), Offset(335f, 420f), WireDcPositive, "+", Offset(305f, 408f), strokeW = 5f)
    drawPathWire(
        points = listOf(Offset(515f, 420f), Offset(640f, 420f), Offset(640f, if (isHybrid) 340f else 385f)),
        color = WireDcPositive,
        strokeW = 5f
    )
    drawWireLabel(
        text = "+DC ${metrics.recommendedBatteryCableMm2}мм² (${metrics.maxDcBatteryCurrentA}А)",
        pos = Offset(580f, 406f),
        color = WireDcPositive
    )
    drawPathWire(
        points = listOf(Offset(276f, 495f), Offset(680f, 495f), Offset(680f, if (isHybrid) 340f else 385f)),
        color = WireDcNegative,
        strokeW = 5f
    )
    drawWireLabel(
        text = "-DC ${metrics.recommendedBatteryCableMm2}мм²",
        pos = Offset(460f, 514f),
        color = WireDcNegative
    )
    if (!isHybrid) {
        // Connect MPPT to DC Bus
        drawPathWire(points = listOf(Offset(640f, 185f), Offset(640f, 215f), Offset(545f, 215f), Offset(545f, 420f)), color = WireDcPositive, strokeW = 3.5f)
        drawPathWire(points = listOf(Offset(680f, 185f), Offset(680f, 222f), Offset(532f, 222f), Offset(532f, 495f)), color = WireDcNegative, strokeW = 3.5f)
    }

    // 5. AC BREAKER PANEL (Right Top)
    val acRect = Rect(828f, 65f, 972f, 285f)
    drawComponentBox(
        rect = acRect,
        title = "AC ЩИТ 230В",
        line1 = "Автомат ${metrics.recommendedAcBreakerA}А",
        line2 = "ПЗВ (УЗО) 30мА",
        line3 = "Реле напруги",
        accent = WireAcPhase,
        isSelected = selectedNodeId == "ac_panel",
        badge = "AC"
    )
    hitTargets += HitTarget(
        acRect,
        SchematicNodeInfo(
            id = "ac_panel",
            title = "Захисний AC-щиток 230 В",
            primarySpec = "Автомат ${metrics.recommendedAcBreakerA} А + ПЗВ тип A (30 мА) + Реле напруги",
            wiringSpec = "Кабель ВВГнг 3×2.5 мм² (до 4 кВт) або 3×4/6 мм² (від 5 кВт)",
            safetyNote = "Встановлюйте перекидний рубильник I-0-II для безпечного розриву міської мережі та інвертора."
        )
    )

    // Wire Inverter -> AC Panel
    val invRightX = invRect.right
    drawLabeledWire(Offset(invRightX, 130f), Offset(828f, 130f), WireAcPhase, "L 230В", Offset((invRightX + 828f) / 2f, 118f), strokeW = 3.5f)
    drawLabeledWire(Offset(invRightX, 170f), Offset(828f, 170f), WireAcNeutral, "N", Offset((invRightX + 828f) / 2f, 188f), strokeW = 3.5f)

    // 6. HOME LOADS (Right Bottom)
    val loadRect = Rect(828f, 345f, 972f, 495f)
    drawComponentBox(
        rect = loadRect,
        title = "СПОЖИВАЧІ",
        line1 = "Пік: ${metrics.peakLoadWatts} Вт",
        line2 = "${"%.1f".format(metrics.dailyLoadKwh)} кВт·год/д",
        line3 = "Пуск: ${metrics.surgeLoadWatts} Вт",
        accent = Color(0xFFA78BFA),
        isSelected = selectedNodeId == "loads",
        badge = "LOAD"
    )
    hitTargets += HitTarget(
        loadRect,
        SchematicNodeInfo(
            id = "loads",
            title = "Домашнє навантаження",
            primarySpec = "Розрахункове добове споживання: ${"%.1f".format(metrics.dailyLoadKwh)} кВт·год • Автономність ~${"%.1f".format(metrics.autonomyHours)} год",
            wiringSpec = "Потужні прилади (бойлер, пралка) підключайте на окремі лінії через автомати 16 А",
            safetyNote = "Уникайте одночасного ввімкнення приладів сумою вище ${config.inverterWatts} Вт."
        )
    )
    drawLabeledWire(Offset(900f, 285f), Offset(900f, 345f), WireAcPhase, "230В", Offset(926f, 318f), strokeW = 3.5f)

    // Optional Generator / ATS block
    if (config.includeGeneratorAts) {
        val genRect = Rect(735f, 520f, 972f, 610f)
        drawComponentBox(
            rect = genRect,
            title = "АВР + ГЕНЕРАТОР",
            line1 = "Перекидний рубильник I-0-II",
            line2 = "Аварійна підзарядка АКБ",
            line3 = "",
            accent = SolarAmber,
            isSelected = selectedNodeId == "gen",
            badge = "GEN"
        )
        hitTargets += HitTarget(
            genRect,
            SchematicNodeInfo(
                id = "gen",
                title = "Резервне введення (Мережа / Генератор)",
                primarySpec = "Перекидний рубильник I-0-II 40–63А перед входом AC IN",
                wiringSpec = "Механічне блокування унеможливлює зустрічну напругу",
                safetyNote = "При роботі від генератора обмежте струм AC-заряду в інверторі, щоб не перевантажити генератор."
            )
        )
        drawPathWire(listOf(Offset(735f, 565f), Offset(710f, 565f), Offset(710f, invRect.bottom)), WireAcPhase, strokeW = 2.8f, dashed = true)
    }

    // 7. EARTHING BUS (PE)
    val peRect = Rect(36f, 575f, 276f, 650f)
    drawComponentBox(
        rect = peRect,
        title = "ЗАЗЕМЛЕННЯ (PE ≤ 4 Ом)",
        line1 = "Рами ФЕМ + УЗІП + Інвертор",
        line2 = "Провід ПВ-3 10–16 мм²",
        line3 = "",
        accent = WireGround,
        isSelected = selectedNodeId == "pe",
        badge = "PE"
    )
    hitTargets += HitTarget(
        peRect,
        SchematicNodeInfo(
            id = "pe",
            title = "Контур захисного заземлення (PE)",
            primarySpec = "Опір заземлення ≤ 4 Ом (оцинковані або мідні стержні в ґрунт)",
            wiringSpec = "Жовто-зелений дріт 10–16 мм² до УЗІП, рам панелей та корпусу інвертора",
            safetyNote = "Якісне заземлення необхідне для коректної роботи ПЗВ та відведення грозових наведень УЗІП."
        )
    )
    // Green ground dashed links
    drawPathWire(listOf(Offset(416f, 185f), Offset(416f, 345f), Offset(156f, 345f), Offset(156f, 575f)), WireGround, strokeW = 2.2f, dashed = true)
    drawPathWire(listOf(Offset(invRect.left + 40f, invRect.bottom), Offset(invRect.left + 40f, 610f), Offset(276f, 610f)), WireGround, strokeW = 2.2f, dashed = true)
}

private fun DrawScope.drawArrayAndBatteryBlueprint(
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics,
    selectedNodeId: String?,
    hitTargets: MutableList<HitTarget>
) {
    drawRect(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
        topLeft = Offset(10f, 10f),
        size = Size(980f, 700f),
        style = Stroke(width = 2f)
    )

    // Top Section: Solar Array S x P wiring diagram
    val arrayZone = Rect(26f, 26f, 974f, 330f)
    drawRoundRect(
        color = Color(0xFF0F223D).copy(alpha = 0.55f),
        topLeft = arrayZone.topLeft,
        size = arrayZone.size,
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = SolarAmber.copy(alpha = 0.5f),
        topLeft = arrayZone.topLeft,
        size = arrayZone.size,
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(1.5f)
    )

    drawBlueprintText(
        "СХЕМА КОМУТАЦІЇ СОНЯЧНИХ ПАНЕЛЕЙ: ${config.seriesCount}S × ${config.parallelStrings}P (Усього ${config.totalPanels} шт. × ${config.panelWattage} Вт = ${metrics.totalPanelWatts} Вт)",
        Offset(44f, 52f),
        color = SolarAmber,
        sizePx = 15f,
        bold = true
    )
    drawBlueprintText(
        "Робоча напруга Vmp: ${metrics.arrayVmp.roundToInt()} В  |  Холостий хід Voc (-20°C): ${metrics.arrayVocWinterMinus20.roundToInt()} В  |  Струм Imp: ${"%.1f".format(metrics.arrayImp)} А",
        Offset(44f, 74f),
        color = Color(0xFFCBD5E1),
        sizePx = 12.5f
    )

    val maxCols = config.seriesCount.coerceAtMost(10)
    val rows = config.parallelStrings
    val availW = 790f
    val availH = 210f
    val panelW = min(84f, (availW - (maxCols - 1) * 18f) / maxCols)
    val panelH = min(78f, (availH - (rows - 1) * 20f) / rows)
    val startX = 48f
    val startY = 96f

    for (r in 0 until rows) {
        val py = startY + r * (panelH + 22f)
        for (c in 0 until config.seriesCount) {
            // Wrap visually if >10 in series, or compress gracefully
            val px = startX + c * (panelW + 16f)
            if (px + panelW > 870f) continue

            // Panel body
            drawRoundRect(
                color = Color(0xFF172554),
                topLeft = Offset(px, py),
                size = Size(panelW, panelH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF60A5FA),
                topLeft = Offset(px, py),
                size = Size(panelW, panelH),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(1.8f)
            )
            // Cell grid lines
            drawLine(Color(0xFF3B82F6).copy(alpha = 0.45f), Offset(px + panelW / 2f, py), Offset(px + panelW / 2f, py + panelH), 1f)
            drawLine(Color(0xFF3B82F6).copy(alpha = 0.45f), Offset(px, py + panelH / 2f), Offset(px + panelW, py + panelH / 2f), 1f)
            drawBlueprintText("${r + 1}.${c + 1}", Offset(px + 6f, py + 16f), Color.White, 10.5f, bold = true)
            drawBlueprintText("+", Offset(px + 6f, py + panelH - 6f), WireDcPositive, 12f, bold = true)
            drawBlueprintText("-", Offset(px + panelW - 14f, py + panelH - 6f), WireDcNegative, 12f, bold = true)

            // Series jumper to next panel
            if (c < config.seriesCount - 1 && px + panelW + 16f + panelW <= 870f) {
                val jY = py + panelH * 0.72f
                drawLine(
                    color = SolarAmber,
                    start = Offset(px + panelW, jY),
                    end = Offset(px + panelW + 16f, jY),
                    strokeWidth = 2.6f
                )
            }
        }

        // String output wires to combiner bus on right
        val lastPanelX = min(startX + (config.seriesCount - 1) * (panelW + 16f) + panelW, 855f)
        drawLine(WireDcPositive, Offset(startX, py + panelH * 0.25f), Offset(startX - 14f, py + panelH * 0.25f), 2.5f)
        drawLine(WireDcNegative, Offset(lastPanelX, py + panelH * 0.72f), Offset(885f, py + panelH * 0.72f), 2.5f)
    }

    // Combiner / Output Box on Right of Array
    val combRect = Rect(872f, 96f, 960f, 305f)
    drawComponentBox(
        rect = combRect,
        title = if (config.parallelStrings > 1) "КОМБАЙНЕР" else "ВИХІД PV",
        line1 = "+${metrics.arrayVmp.roundToInt()}В",
        line2 = "${"%.1f".format(metrics.arrayImp)}А DC",
        line3 = "${metrics.recommendedSolarCableMm2}мм²",
        accent = WireDcPositive,
        isSelected = selectedNodeId == "array_out",
        badge = "MC4"
    )
    hitTargets += HitTarget(
        arrayZone,
        SchematicNodeInfo(
            id = "array_out",
            title = "Послідовно-паралельне з'єднання ФЕМ",
            primarySpec = "${config.seriesCount} панелей послідовно (додається напруга) × ${config.parallelStrings} стрінгів паралельно (додається струм)",
            wiringSpec = "При послідовному з'єднанні з'єднуйте (-) однієї панелі з (+) наступної. На 1 стрінг використовуйте кабелі ${metrics.recommendedSolarCableMm2} мм².",
            safetyNote = if (config.parallelStrings >= 3) "При ≥3 паралельних стрінгах обов'язково ставте запобіжники 15А–20А на кожен стрінг!" else "Усі панелі в одному стрінгу мають бути однакової потужності та орієнтації."
        )
    )

    // Bottom Section: Battery Bank Cell Configuration
    val batZone = Rect(26f, 348f, 974f, 635f)
    drawRoundRect(
        color = Color(0xFF0A2126).copy(alpha = 0.55f),
        topLeft = batZone.topLeft,
        size = batZone.size,
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = EmeraldOk.copy(alpha = 0.55f),
        topLeft = batZone.topLeft,
        size = batZone.size,
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(1.5f)
    )

    val cellCount = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) config.systemVoltage.cellsSeries else config.systemVoltage.volts / 12
    val unitLabel = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) "${config.systemVoltage.cellsSeries}S LiFePO4 (по 3.2В)" else "${cellCount} × 12В ${config.batteryChemistry.shortName} послідовно"
    drawBlueprintText(
        "ЗБІРКА АКУМУЛЯТОРНОГО БАНКУ: $unitLabel • ${config.batteryCapacityAh} А·год (${"%.1f".format(metrics.batteryNominalWh / 1000f)} кВт·год)",
        Offset(44f, 374f),
        color = EmeraldOk,
        sizePx = 15f,
        bold = true
    )

    val colsPerRow = if (cellCount > 8) 8 else cellCount
    val cellRows = if (cellCount > 8) 2 else 1
    val cellW = min(72f, (660f - (colsPerRow - 1) * 14f) / colsPerRow)
    val cellH = if (cellRows == 2) 86f else 135f
    val cStartX = 48f
    val cStartY = 405f

    for (i in 0 until cellCount) {
        val row = i / colsPerRow
        val col = if (row % 2 == 0) i % colsPerRow else (colsPerRow - 1 - (i % colsPerRow))
        val cx = cStartX + col * (cellW + 14f)
        val cy = cStartY + row * (cellH + 22f)

        drawRoundRect(
            color = Color(0xFF064E3B),
            topLeft = Offset(cx, cy),
            size = Size(cellW, cellH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = EmeraldOk,
            topLeft = Offset(cx, cy),
            size = Size(cellW, cellH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(1.6f)
        )
        val voltsStr = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) "3.2В" else "12В"
        drawBlueprintText("#${i + 1}", Offset(cx + 8f, cy + 20f), Color.White, 12f, bold = true)
        drawBlueprintText(voltsStr, Offset(cx + 8f, cy + 38f), Color(0xFFA7F3D0), 11f)

        // Terminals
        drawCircle(WireDcPositive, 5f, Offset(cx + cellW * 0.25f, cy + 8f))
        drawCircle(WireDcNegative, 5f, Offset(cx + cellW * 0.75f, cy + 8f))

        // Copper busbar to next cell
        if (i < cellCount - 1) {
            val nextRow = (i + 1) / colsPerRow
            val nextCol = if (nextRow % 2 == 0) (i + 1) % colsPerRow else (colsPerRow - 1 - ((i + 1) % colsPerRow))
            val nx = cStartX + nextCol * (cellW + 14f)
            val ny = cStartY + nextRow * (cellH + 22f)
            drawLine(
                color = SolarAmber,
                start = Offset(cx + cellW * 0.75f, cy + 8f),
                end = Offset(nx + cellW * 0.25f, ny + 8f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
        }
    }

    // BMS / Output Bus Block on right of battery zone
    val bmsRect = Rect(740f, 405f, 954f, 610f)
    drawComponentBox(
        rect = bmsRect,
        title = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) "SMART BMS + ШИНА" else "ВИХІД БАНКУ АКБ",
        line1 = "Вихід: ${config.systemVoltage.label}",
        line2 = "Запобіжник: ${metrics.recommendedBatteryFuseA}А",
        line3 = "Кабель: ${metrics.recommendedBatteryCableMm2}мм²",
        accent = WireDcPositive,
        isSelected = selectedNodeId == "bat_bank",
        badge = if (config.batteryChemistry == BatteryChemistry.LIFEPO4) "BMS" else "DC"
    )
    hitTargets += HitTarget(
        batZone,
        SchematicNodeInfo(
            id = "bat_bank",
            title = "Внутрішня комутація АКБ ($unitLabel)",
            primarySpec = "Послідовне з'єднання дає ${config.systemVoltage.label} при ємності ${config.batteryCapacityAh} А·год",
            wiringSpec = if (config.batteryChemistry == BatteryChemistry.LIFEPO4)
                "Балансувальні дроти BMS паяються/прикручуються послідовно (B0 на загальний мінус, B1..B${config.systemVoltage.cellsSeries} на плюси комірок). Головний (-) через BMS (B- → P-)."
            else
                "Усі перемички мають бути однакового перерізу (${metrics.recommendedBatteryCableMm2} мм²) і однакової довжини.",
            safetyNote = "Затягуйте мідні перемички динамометричним ключем; перед складанням LiFePO4 обов'язково вирівняйте напругу всіх комірок!"
        )
    )
}

private fun DrawScope.drawMountingBlueprint(
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics,
    selectedNodeId: String?,
    hitTargets: MutableList<HitTarget>
) {
    drawRect(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
        topLeft = Offset(10f, 10f),
        size = Size(980f, 700f),
        style = Stroke(width = 2f)
    )

    drawBlueprintText(
        "АРХІТЕКТУРНО-МОНТАЖНЕ КРЕСЛЕННЯ: ДАХ (${config.panelTilt.degrees}°) ТА ТЕХНІЧНЕ ПРИМІЩЕННЯ",
        Offset(32f, 44f),
        color = BlueprintCyanBright,
        sizePx = 15.5f,
        bold = true
    )

    // Ground line
    drawLine(Color(0xFF475569), Offset(28f, 600f), Offset(970f, 600f), strokeWidth = 3f)

    // --- LEFT: HOUSE & ROOF ---
    val houseRect = Rect(60f, 340f, 410f, 600f)
    drawRect(Color(0xFF1E293B), houseRect.topLeft, houseRect.size)
    drawRect(Color(0xFF64748B), houseRect.topLeft, houseRect.size, style = Stroke(2f))

    // Roof triangle
    val roofPath = Path().apply {
        moveTo(35f, 340f)
        lineTo(235f, 200f)
        lineTo(435f, 340f)
        close()
    }
    drawPath(roofPath, Color(0xFF172554))
    drawPath(roofPath, Color(0xFF94A3B8), style = Stroke(2.2f))

    // Tilted Solar Panel on Roof according to config.panelTilt.degrees
    val rad = (config.panelTilt.degrees * PI / 180.0).toFloat()
    val panelLen = 175f
    val anchorX = 95f
    val anchorY = 300f
    val endX = anchorX + panelLen * cos(rad)
    val endY = anchorY - panelLen * sin(rad)
    // Gap mount posts (12 cm ventilation clearance)
    drawLine(Color(0xFFCBD5E1), Offset(anchorX + 20f, anchorY - 12f), Offset(anchorX + 20f, anchorY + 6f), 4f)
    drawLine(Color(0xFFCBD5E1), Offset(endX - 20f, endY + 12f), Offset(endX - 20f, anchorY - 90f), 4f)

    // Panel bar
    drawLine(Color(0xFF38BDF8), Offset(anchorX, anchorY - 12f), Offset(endX, endY - 12f), 11f, StrokeCap.Square)
    drawLine(SolarAmber, Offset(anchorX, anchorY - 17f), Offset(endX, endY - 17f), 2.5f)

    // Sun rays
    val sunCenter = Offset(85f, 115f)
    drawCircle(SolarAmber, 22f, sunCenter)
    for (a in 0 until 8) {
        val ang = a * (PI / 4.0).toFloat()
        drawLine(
            SolarAmber,
            Offset(sunCenter.x + 28f * cos(ang), sunCenter.y + 28f * sin(ang)),
            Offset(sunCenter.x + 40f * cos(ang), sunCenter.y + 40f * sin(ang)),
            2.5f
        )
    }
    drawPathWire(listOf(Offset(115f, 145f), Offset(165f, 215f)), SolarAmber, strokeW = 2f, dashed = true)
    drawBlueprintText("Кут ${config.panelTilt.degrees}° (${config.panelOrientation.label})", Offset(55f, 185f), SolarAmber, 12.5f, bold = true)
    drawBlueprintText("Вент. зазор ≥ 12 см під ФЕМ", Offset(185f, 240f), Color(0xFF7DD3FC), 11.5f)

    // Earthing rod outside left wall
    drawPathWire(listOf(Offset(60f, 440f), Offset(40f, 440f), Offset(40f, 650f)), WireGround, strokeW = 3f)
    drawLine(WireGround, Offset(25f, 650f), Offset(55f, 650f), 3f)
    drawLine(WireGround, Offset(30f, 658f), Offset(50f, 658f), 2.5f)
    drawLine(WireGround, Offset(35f, 666f), Offset(45f, 666f), 2f)
    drawBlueprintText("Контур PE ≤ 4 Ом", Offset(64f, 658f), WireGround, 12f, bold = true)

    hitTargets += HitTarget(
        Rect(35f, 180f, 435f, 345f),
        SchematicNodeInfo(
            id = "roof_mount",
            title = "Кріплення на даху (${config.panelTilt.degrees}°)",
            primarySpec = "${config.totalPanels} панелей • Профіль 40×40 мм • Вентиляційний зазор 10–15 см",
            wiringSpec = "Кабель PV1-F ${metrics.recommendedSolarCableMm2} мм² заводиться в дім через гермовід (PG-сальник) у УФ-стійкій гофрі",
            safetyNote = "Залишайте зазор 10–15 см під панелями для охолодження влітку — перегрів знижує потужність ФЕМ на 15–20%."
        )
    )

    // --- RIGHT: EQUIPMENT WALL SECTION ---
    val roomRect = Rect(480f, 105f, 955f, 600f)
    drawRect(Color(0xFF0F1E36), roomRect.topLeft, roomRect.size)
    drawRect(Color(0xFF475569), roomRect.topLeft, roomRect.size, style = Stroke(2f))
    drawBlueprintText("ТЕХНІЧНЕ ПРИМІЩЕННЯ (+5…+30°C, НЕГОРЮЧА СТІНА: ЦЕГЛА / БЕТОН / ЦСП)", Offset(495f, 128f), Color(0xFF94A3B8), 11.5f, bold = true)

    // Inverter Clearance Box (dashed 30cm zone)
    val clearRect = Rect(520f, 165f, 740f, 385f)
    drawRect(
        color = SolarAmber.copy(alpha = 0.45f),
        topLeft = clearRect.topLeft,
        size = clearRect.size,
        style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    )
    drawBlueprintText("Вільний зазор ≥ 30 см", Offset(570f, 158f), SolarAmber, 11.5f, bold = true)

    val wallInvRect = Rect(560f, 205f, 700f, 345f)
    drawComponentBox(
        rect = wallInvRect,
        title = "ІНВЕРТОР",
        line1 = "${config.inverterWatts} Вт",
        line2 = "${config.systemVoltage.label}",
        line3 = "Висота ~1.5 м",
        accent = BlueprintCyanBright,
        isSelected = selectedNodeId == "wall_inv",
        badge = "INV"
    )
    hitTargets += HitTarget(
        wallInvRect,
        SchematicNodeInfo(
            id = "wall_inv",
            title = "Настінний монтаж інвертора",
            primarySpec = "Висота розміщення ~1.4–1.6 м для зручного доступу до екрану та вентиляції",
            wiringSpec = "Обов'язково зазор ≥ 30 см зверху та знизу, ≥ 20 см з боків",
            safetyNote = "Монтуйте виключно на негорючу основу (бетон, цегла, ЦСП/ГВЛ)."
        )
    )

    // AC/DC Shield beside inverter
    val shieldRect = Rect(765f, 205f, 925f, 355f)
    drawComponentBox(
        rect = shieldRect,
        title = "ЩИТ ЗАХИСТУ",
        line1 = "PV DC ${metrics.recommendedSolarBreakerA}А + УЗІП",
        line2 = "AC ${metrics.recommendedAcBreakerA}А + ПЗВ",
        line3 = "Рубильник I-0-II",
        accent = WireAcPhase,
        isSelected = selectedNodeId == "wall_shield",
        badge = "BOX"
    )
    hitTargets += HitTarget(
        shieldRect,
        SchematicNodeInfo(
            id = "wall_shield",
            title = "Монтажний електрощит (DC + AC)",
            primarySpec = "Роздільні дин-рейки для високовольтного PV DC (з УЗІП) та виходу AC 230В",
            wiringSpec = "Автомати підписані; вхід PV не змішувати в одному кабель-каналі з AC",
            safetyNote = "Перед щитом має бути вільний доступ не менше 80 см для безпечного вимкнення."
        )
    )

    // Battery Rack on Floor
    val rackRect = Rect(535f, 445f, 765f, 585f)
    drawComponentBox(
        rect = rackRect,
        title = "СТЕЛАЖ АКБ (${config.batteryChemistry.shortName})",
        line1 = "${config.systemVoltage.label} ${config.batteryCapacityAh} А·год",
        line2 = "Запобіжник ${metrics.recommendedBatteryFuseA}А на (+)",
        line3 = "Кабель ≤ 1.5 м (${metrics.recommendedBatteryCableMm2}мм²)",
        accent = EmeraldOk,
        isSelected = selectedNodeId == "wall_bat",
        badge = "BAT"
    )
    hitTargets += HitTarget(
        rackRect,
        SchematicNodeInfo(
            id = "wall_bat",
            title = "Розміщення акумуляторного блоку",
            primarySpec = "Встановлюється на підлогу або посилену негорючу полицю безпосередньо під інвертором",
            wiringSpec = "Довжина кабелів ${metrics.recommendedBatteryCableMm2} мм² не більше 1.5 м для мінімального падіння напруги",
            safetyNote = "Тримайте подалі від котла/батарей опалення (ідеально +15…+25°C)."
        )
    )

    // Short thick cable arrows between inverter and battery rack
    drawLabeledWire(Offset(610f, 345f), Offset(610f, 445f), WireDcPositive, "+ ${metrics.recommendedBatteryCableMm2}мм²", Offset(550f, 405f), strokeW = 5f)
    drawLabeledWire(Offset(655f, 345f), Offset(655f, 445f), WireDcNegative, "- ≤1.5м", Offset(695f, 405f), strokeW = 5f)
    // Conduit from roof to shield
    drawPathWire(listOf(Offset(410f, 280f), Offset(480f, 280f), Offset(480f, 180f), Offset(845f, 180f), Offset(845f, 205f)), SolarAmber, strokeW = 3f)
}

private fun DrawScope.drawEnergyBalanceChart(
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics
) {
    drawRect(
        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
        topLeft = Offset(10f, 10f),
        size = Size(980f, 700f),
        style = Stroke(width = 2f)
    )

    drawBlueprintText(
        "ДОБОВИЙ ЕНЕРГОБАЛАНС СТАНЦІЇ (24 ГОДИНИ) • ПОЛЕ ${metrics.totalPanelWatts} ВТ / АКБ ${"%.1f".format(metrics.usableBatteryWh / 1000f)} КВТ·ГОД",
        Offset(36f, 46f),
        color = BlueprintCyanBright,
        sizePx = 15f,
        bold = true
    )

    // Summary legend pills at top
    drawBlueprintText("● Літня генерація: ${"%.1f".format(metrics.estimatedSummerDailyKwh)} кВт·год/д", Offset(36f, 76f), SolarAmber, 13f, bold = true)
    drawBlueprintText("● Зимова генерація: ${"%.1f".format(metrics.estimatedWinterDailyKwh)} кВт·год/д", Offset(315f, 76f), BlueprintCyanBright, 13f, bold = true)
    drawBlueprintText("● Споживання дому: ${"%.1f".format(metrics.dailyLoadKwh)} кВт·год/д", Offset(595f, 76f), Color(0xFFFB7185), 13f, bold = true)
    drawBlueprintText("— Заряд АКБ (SoC %)", Offset(825f, 76f), EmeraldOk, 13f, bold = true)

    val chartLeft = 80f
    val chartTop = 105f
    val chartRight = 925f
    val chartBottom = 565f
    val chartW = chartRight - chartLeft
    val chartH = chartBottom - chartTop

    // Axes
    drawLine(Color(0xFF64748B), Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom), 2f)
    drawLine(Color(0xFF64748B), Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 2f)
    drawLine(EmeraldOk.copy(alpha = 0.6f), Offset(chartRight, chartTop), Offset(chartRight, chartBottom), 1.5f)

    val points = metrics.hourlyCurve
    val maxPower = max(
        500f,
        max(
            points.maxOfOrNull { it.summerSolarW } ?: 500f,
            points.maxOfOrNull { it.loadW } ?: 500f
        ) * 1.15f
    )

    // Horizontal Y-grid & labels
    for (step in 0..4) {
        val frac = step / 4f
        val y = chartBottom - frac * chartH
        drawLine(Color(0xFF334155), Offset(chartLeft, y), Offset(chartRight, y), 1f)
        val wattsLabel = "${(maxPower * frac).roundToInt()} Вт"
        drawBlueprintText(wattsLabel, Offset(18f, y + 4f), Color(0xFF94A3B8), 11.5f)
        val socLabel = "${(frac * 100).roundToInt()}%"
        drawBlueprintText(socLabel, Offset(chartRight + 10f, y + 4f), EmeraldOk, 11.5f)
    }

    if (points.isEmpty()) return
    val dx = chartW / (points.size - 1).coerceAtLeast(1)

    // Load bars
    val barW = dx * 0.45f
    points.forEachIndexed { idx, pt ->
        val x = chartLeft + idx * dx - barW / 2f
        val h = (pt.loadW / maxPower) * chartH
        drawRect(
            color = Color(0xFFFB7185).copy(alpha = 0.42f),
            topLeft = Offset(x, chartBottom - h),
            size = Size(barW, h)
        )
    }

    // Summer solar filled area
    val summerFill = Path().apply {
        moveTo(chartLeft, chartBottom)
        points.forEachIndexed { idx, pt ->
            val x = chartLeft + idx * dx
            val y = chartBottom - (pt.summerSolarW / maxPower) * chartH
            lineTo(x, y)
        }
        lineTo(chartRight, chartBottom)
        close()
    }
    drawPath(summerFill, SolarAmber.copy(alpha = 0.22f))

    val summerLine = Path()
    val winterLine = Path()
    val socLine = Path()
    points.forEachIndexed { idx, pt ->
        val x = chartLeft + idx * dx
        val sy = chartBottom - (pt.summerSolarW / maxPower) * chartH
        val wy = chartBottom - (pt.winterSolarW / maxPower) * chartH
        val socY = chartBottom - (pt.batterySocPercent / 100f) * chartH
        if (idx == 0) {
            summerLine.moveTo(x, sy)
            winterLine.moveTo(x, wy)
            socLine.moveTo(x, socY)
        } else {
            summerLine.lineTo(x, sy)
            winterLine.lineTo(x, wy)
            socLine.lineTo(x, socY)
        }
        if (idx % 3 == 0) {
            drawBlueprintText(String.format("%02d:00", pt.hour), Offset(x - 16f, chartBottom + 22f), Color(0xFFCBD5E1), 11.5f)
        }
    }

    drawPath(summerLine, SolarAmber, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    drawPath(winterLine, BlueprintCyanBright, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
    drawPath(socLine, EmeraldOk, style = Stroke(width = 3f, cap = StrokeCap.Round))

    // Bottom info note
    drawBlueprintText(
        "Орієнтовна автономність від АКБ без сонця: ~${"%.1f".format(metrics.autonomyHours)} год (за середнього навантаження) • Кут ФЕМ: ${config.panelTilt.degrees}°",
        Offset(36f, 622f),
        Color(0xFFE2E8F0),
        13f,
        bold = true
    )
}

private fun DrawScope.drawComponentBox(
    rect: Rect,
    title: String,
    line1: String,
    line2: String,
    line3: String,
    accent: Color,
    isSelected: Boolean,
    badge: String
) {
    val bgColor = if (isSelected) accent.copy(alpha = 0.22f) else Color(0xFF0F1E36).copy(alpha = 0.92f)
    val borderColor = if (isSelected) Color.White else accent
    val borderW = if (isSelected) 3.2f else 2f

    drawRoundRect(
        color = bgColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = borderW)
    )

    // Header band
    drawRoundRect(
        color = accent.copy(alpha = 0.25f),
        topLeft = rect.topLeft,
        size = Size(rect.width, 28f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    drawBlueprintText(badge, Offset(rect.left + 8f, rect.top + 19f), accent, 10.5f, bold = true)
    drawBlueprintText(title, Offset(rect.left + 44f, rect.top + 19f), Color.White, 11.5f, bold = true)
    if (line1.isNotEmpty()) drawBlueprintText(line1, Offset(rect.left + 10f, rect.top + 52f), Color(0xFFF8FAFC), 12.5f, bold = true)
    if (line2.isNotEmpty()) drawBlueprintText(line2, Offset(rect.left + 10f, rect.top + 74f), Color(0xFFCBD5E1), 11.5f)
    if (line3.isNotEmpty()) drawBlueprintText(line3, Offset(rect.left + 10f, rect.top + 96f), accent, 11.5f, bold = true)
}

private fun DrawScope.drawLabeledWire(
    start: Offset,
    end: Offset,
    color: Color,
    label: String,
    labelPos: Offset,
    strokeW: Float = 3f
) {
    drawLine(color = color, start = start, end = end, strokeWidth = strokeW, cap = StrokeCap.Round)
    if (label.isNotEmpty()) {
        drawWireLabel(label, labelPos, color)
    }
}

private fun DrawScope.drawPathWire(
    points: List<Offset>,
    color: Color,
    strokeW: Float = 3f,
    dashed: Boolean = false
) {
    if (points.size < 2) return
    val p = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = p,
        color = color,
        style = Stroke(
            width = strokeW,
            cap = StrokeCap.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
        )
    )
}

private fun DrawScope.drawWireLabel(text: String, pos: Offset, color: Color) {
    drawBlueprintText(text, pos, color, 10.5f, bold = true)
}

private fun DrawScope.drawTitleBlock(
    config: SolarStationConfig,
    metrics: CalculatedStationMetrics,
    blueprintType: BlueprintType
) {
    val block = Rect(645f, 648f, 982f, 704f)
    drawRect(Color(0xFF0A1426), block.topLeft, block.size)
    drawRect(Color(0xFF38BDF8).copy(alpha = 0.65f), block.topLeft, block.size, style = Stroke(1.5f))
    drawLine(Color(0xFF38BDF8).copy(alpha = 0.45f), Offset(645f, 675f), Offset(982f, 675f), 1f)
    drawBlueprintText("ПРОЄКТ СЕС: ${config.tier.title.uppercase()}", Offset(655f, 666f), SolarAmber, 10.5f, bold = true)
    drawBlueprintText(
        "${blueprintType.shortTitle} | ${metrics.totalPanelWatts}Вт / ${config.systemVoltage.label} / ${config.inverterWatts}Вт",
        Offset(655f, 694f),
        Color(0xFFE2E8F0),
        10.5f
    )
}

private fun DrawScope.drawBlueprintText(
    text: String,
    pos: Offset,
    color: Color,
    sizePx: Float,
    bold: Boolean = false
) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color.toArgb()
            this.textSize = sizePx
            this.isAntiAlias = true
            this.typeface = if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.nativeCanvas.drawText(text, pos.x, pos.y, paint)
    }
}

private fun mMaxVoc(metrics: CalculatedStationMetrics): Int = metrics.requiredMpptMaxVoc
