package ru.kipia.calculator

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher

class MainActivity : Activity() {
    private data class Palette(
        val background: Int,
        val header: Int,
        val surface: Int,
        val surface2: Int,
        val text: Int,
        val muted: Int,
        val line: Int,
        val blue: Int,
        val green: Int,
        val orange: Int
    )

    private sealed class Screen {
        object Home : Screen()
        object Calculations : Screen()
        object Reference : Screen()
        object Settings : Screen()
        data class Calculator(val kind: String) : Screen()
        data class Help(val kind: String) : Screen()
        data class Category(val id: String) : Screen()
        data class Article(val id: String, var scrollY: Int = 0) : Screen()
        data class Term(val key: String) : Screen()
        data class Diagram(val type: String, val title: String) : Screen()
    }

    private lateinit var palette: Palette
    private var dark = true
    private var current: Screen = Screen.Home
    private val history = mutableListOf<Screen>()
    private var currentScroll: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark", true)
        applyPalette()
        render()
    }

    @Deprecated("Совместимость с Android 10")
    override fun onBackPressed() {
        if (history.isNotEmpty()) {
            current = history.removeAt(history.lastIndex)
            render()
        } else if (current !is Screen.Home) {
            current = Screen.Home
            render()
        } else {
            super.onBackPressed()
        }
    }

    private fun applyPalette() {
        palette = if (dark) Palette(
            Color.rgb(7, 23, 33), Color.rgb(9, 29, 42), Color.rgb(18, 42, 58),
            Color.rgb(25, 52, 70), Color.rgb(242, 247, 251), Color.rgb(180, 199, 212),
            Color.rgb(55, 88, 109), Color.rgb(29, 157, 241), Color.rgb(29, 181, 99), Color.rgb(255, 159, 44)
        ) else Palette(
            Color.rgb(238, 244, 248), Color.rgb(18, 53, 77), Color.WHITE,
            Color.rgb(225, 236, 243), Color.rgb(18, 35, 48), Color.rgb(84, 105, 119),
            Color.rgb(178, 199, 212), Color.rgb(20, 131, 218), Color.rgb(19, 146, 78), Color.rgb(230, 126, 24)
        )
        window.statusBarColor = palette.header
        window.navigationBarColor = palette.header
    }

    private fun navigate(next: Screen) {
        saveArticleScroll()
        history.add(current)
        current = next
        render()
    }

    private fun root(next: Screen) {
        history.clear()
        current = next
        render()
    }

    private fun saveArticleScroll() {
        (current as? Screen.Article)?.scrollY = currentScroll?.scrollY ?: 0
    }

    private fun render() {
        currentScroll = null
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.background)
        }
        val content = FrameLayout(this)
        val screen = when (val s = current) {
            Screen.Home -> homeScreen()
            Screen.Calculations -> calculationsScreen()
            Screen.Reference -> referenceScreen()
            Screen.Settings -> settingsScreen()
            is Screen.Calculator -> calculatorScreen(s.kind)
            is Screen.Help -> helpScreen(s.kind)
            is Screen.Category -> categoryScreen(s.id)
            is Screen.Article -> articleScreen(s)
            is Screen.Term -> termScreen(s.key)
            is Screen.Diagram -> diagramScreen(s.type, s.title)
        }
        content.addView(screen, FrameLayout.LayoutParams(-1, -1))
        shell.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        shell.addView(bottomNavigation(), LinearLayout.LayoutParams(-1, dp(64)))
        setContentView(shell)
    }

    private fun homeScreen(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(palette.background) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
            background = verticalGradient(palette.header, if (dark) Color.rgb(13, 59, 80) else Color.rgb(31, 91, 126))
        }
        header.addView(title("Помощник КИПиА", 28f, Color.WHITE))
        header.addView(text("Расчёты • Приборы • Справочник", 15f, Color.rgb(210, 229, 241)).apply { setPadding(0, dp(4), 0, 0) })
        header.addView(text("Знания и расчёты всегда под рукой", 13f, Color.rgb(157, 194, 215)).apply { setPadding(0, dp(8), 0, 0) })
        page.addView(header)
        val body = vertical(dp(14))
        val cards = listOf(
            arrayOf("◴", "Давление", "Конвертер единиц", "pressure", "#0B5C91"),
            arrayOf("♨", "Температура", "°C ↔ °F ↔ K", "temperature", "#873A26"),
            arrayOf("▥", "Длина", "Конвертер единиц", "length", "#08745A"),
            arrayOf("⌁", "Сигнал 4–20 мА", "Расчёт и проверка", "signal", "#41458D"),
            arrayOf("ϟ", "Электрический калькулятор", "U • I • R • P", "electrical", "#8A6A08"),
            arrayOf("◉", "Цена деления шкалы", "Шкальные приборы", "division", "#46535D"),
            arrayOf("10ⁿ", "Приставки СИ", "10⁻³⁰ … 10³⁰", "si", "#087786"),
            arrayOf("▣", "Справочник КИПиА", "Приборы, схемы, теория", "reference", "#774637")
        )
        cards.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { c ->
                val card = homeCard(c[0], c[1], c[2], Color.parseColor(c[4])) {
                    if (c[3] == "reference") root(Screen.Reference) else navigate(Screen.Calculator(c[3]))
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(126), 1f).apply { setMargins(dp(5), dp(5), dp(5), dp(5)) })
            }
            body.addView(row)
        }
        val scroll = ScrollView(this).apply { addView(body); isFillViewport = true }
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun calculationsScreen(): View = page("Расчёты", "calculations") { body ->
        body.addView(text("Все инженерные инструменты работают без интернета.", 15f, palette.muted).margins(bottom = 14))
        listOf(
            Triple("Давление", "Па, кПа, МПа, бар, кгс/см² и другие", "pressure"),
            Triple("Температура", "Перевод °C, °F и K", "temperature"),
            Triple("Длина", "мкм, мм, см, дм, м, км", "length"),
            Triple("Сигнал 4–20 мА", "Показание, ток и процент диапазона", "signal"),
            Triple("Электрический калькулятор", "Напряжение, ток, сопротивление, мощность", "electrical"),
            Triple("Цена деления шкалы", "Манометры, термометры и другие шкалы", "division"),
            Triple("Приставки СИ", "Полный диапазон приставок", "si")
        ).forEach { (name, sub, kind) -> body.addView(listCard(name, sub) { navigate(Screen.Calculator(kind)) }) }
    }

    private fun calculatorScreen(kind: String): View = when (kind) {
        "pressure" -> converterScreen("Давление", kind, Engineering.pressureUnits, 2, 4)
        "length" -> converterScreen("Длина", kind, Engineering.lengthUnits, 1, 4)
        "temperature" -> temperatureScreen()
        "signal" -> signalScreen()
        "electrical" -> electricalScreen()
        "division" -> divisionScreen()
        "si" -> siScreen()
        else -> calculationsScreen()
    }

    private fun converterScreen(title: String, kind: String, units: List<Engineering.UnitDef>, fromIndex: Int, toIndex: Int): View =
        page(title, kind) { body ->
            body.addView(sectionTabs())
            val input = numberInput(if (kind == "pressure") "0,6" else "1500")
            val from = spinner(units.map { it.title }, fromIndex)
            val to = spinner(units.map { it.title }, toIndex)
            body.addView(label("Значение")); body.addView(inputSurface(input))
            val unitRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            unitRow.addView(labeledSpinner("Из единицы", from), LinearLayout.LayoutParams(0, -2, 1f))
            val swap = Button(this).apply {
                text = "⇄"; textSize = 24f; setTextColor(palette.text); background = rounded(palette.surface2, 12)
                setOnClickListener { val old = from.selectedItemPosition; from.setSelection(to.selectedItemPosition); to.setSelection(old) }
            }
            unitRow.addView(swap, LinearLayout.LayoutParams(dp(48), dp(52)).apply { setMargins(dp(8), dp(24), dp(8), 0) })
            unitRow.addView(labeledSpinner("В единицу", to), LinearLayout.LayoutParams(0, -2, 1f))
            body.addView(unitRow)
            val result = resultPanel()
            body.addView(primaryButton("▦  Рассчитать") {
                val value = Engineering.parseNumber(input.text.toString()) ?: return@primaryButton invalid()
                val answer = Engineering.convertLinear(value, units[from.selectedItemPosition], units[to.selectedItemPosition])
                result.text = "Результат:\n${Engineering.format(answer)} ${units[to.selectedItemPosition].title}"
            })
            body.addView(result)
            if (kind == "pressure") body.addView(infoCard("1 МПа ≈ 10 кгс/см²\nТочно: 0,6 МПа ≈ 6,1183 кгс/см²"))
            else body.addView(infoCard("Пример: 1500 мм = 1,5 м"))
        }

    private fun temperatureScreen(): View = page("Температура", "temperature") { body ->
        body.addView(sectionTabs())
        val units = listOf("°C", "°F", "K")
        val input = numberInput("100")
        val from = spinner(units, 0); val to = spinner(units, 1)
        body.addView(label("Значение")); body.addView(inputSurface(input))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(labeledSpinner("Из единицы", from), LinearLayout.LayoutParams(0, -2, 1f))
        val swap = Button(this).apply {
            text = "⇄"; textSize = 24f; setTextColor(palette.text); background = rounded(palette.surface2, 12)
            setOnClickListener { val old = from.selectedItemPosition; from.setSelection(to.selectedItemPosition); to.setSelection(old) }
        }
        row.addView(swap, LinearLayout.LayoutParams(dp(48), dp(52)).apply { setMargins(dp(8), dp(24), dp(8), 0) })
        row.addView(labeledSpinner("В единицу", to), LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(row)
        val result = resultPanel()
        body.addView(primaryButton("▦  Рассчитать") {
            val value = Engineering.parseNumber(input.text.toString()) ?: return@primaryButton invalid()
            val answer = Engineering.convertTemperature(value, units[from.selectedItemPosition], units[to.selectedItemPosition])
            result.text = "Результат:\n${Engineering.format(answer)} ${units[to.selectedItemPosition]}"
        })
        body.addView(result)
        body.addView(infoCard("0 °C = 32 °F = 273,15 K"))
    }

    private fun signalScreen(): View = page("Сигнал 4–20 мА", "signal") { body ->
        body.addView(sectionTabs())
        val low = numberInput("0"); val high = numberInput("1,6"); val unit = textInput("МПа")
        body.addView(label("Нижний предел (4 мА)")); body.addView(inputSurface(low))
        body.addView(label("Верхний предел (20 мА)")); body.addView(inputSurface(high))
        body.addView(label("Единица измеряемой величины")); body.addView(inputSurface(unit))
        val dynamic = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val dirButtons = mutableListOf<Button>()
        val dir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var currentDirection = true
        fun buildDirection() {
            dynamic.removeAllViews()
            val input = numberInput(if (currentDirection) "12" else "0,8")
            dynamic.addView(label(if (currentDirection) "Ток на входе, мА" else "Показание прибора"))
            dynamic.addView(inputSurface(input))
            val result = resultPanel()
            val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000; progressTintList = ColorStateList.valueOf(palette.green); progressBackgroundTintList = ColorStateList.valueOf(palette.surface2)
            }
            dynamic.addView(primaryButton("▦  Рассчитать") {
                val a = Engineering.parseNumber(low.text.toString()); val b = Engineering.parseNumber(high.text.toString())
                val x = Engineering.parseNumber(input.text.toString())
                if (a == null || b == null || x == null || a == b) return@primaryButton invalid()
                val suffix = unit.text.toString().trim().ifBlank { "ед." }
                if (currentDirection) {
                    val value = Engineering.currentToValue(x, a, b); val percent = Engineering.signalPercent(x)
                    result.text = "Результат:\n${Engineering.format(value)} $suffix\n${Engineering.format(percent)} % диапазона" +
                        if (x !in 4.0..20.0) "\n⚠ Ток вне диапазона 4–20 мА" else ""
                    progress.progress = (percent.coerceIn(0.0, 100.0) * 10).toInt()
                } else {
                    val ma = Engineering.valueToCurrent(x, a, b); val percent = Engineering.signalPercent(ma)
                    result.text = "Результат:\n${Engineering.format(ma)} мА\n${Engineering.format(percent)} % диапазона"
                    progress.progress = (percent.coerceIn(0.0, 100.0) * 10).toInt()
                }
            })
            dynamic.addView(result)
            dynamic.addView(progress, LinearLayout.LayoutParams(-1, dp(22)).apply { setMargins(dp(8), dp(8), dp(8), 0) })
            dynamic.addView(text("4 мА                 12 мА                 20 мА", 12f, palette.muted).apply { gravity = Gravity.CENTER })
        }
        listOf("мА → Показание", "Показание → мА").forEachIndexed { index, caption ->
            val b = segmentButton(caption, index == 0) {
                currentDirection = index == 0
                dirButtons.forEachIndexed { i, button -> styleSegment(button, i == index) }
                buildDirection()
            }
            dirButtons.add(b); dir.addView(b, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(3), dp(8), dp(3), dp(12)) })
        }
        body.addView(dir); body.addView(dynamic); buildDirection()
        body.addView(TechnicalDiagramView(this, "signal420", dark).apply { background = rounded(palette.surface, 14) }, LinearLayout.LayoutParams(-1, dp(170)).apply { setMargins(0, dp(14), 0, 0) })
    }

    private fun divisionScreen(): View = page("Цена деления шкалы", "division") { body ->
        body.addView(sectionTabs())
        val low = numberInput("0"); val high = numberInput("1,6"); val count = numberInput("80", integer = true); val unit = textInput("МПа")
        body.addView(label("Начало шкалы")); body.addView(inputSurface(low))
        body.addView(label("Конец шкалы")); body.addView(inputSurface(high))
        body.addView(label("Количество промежутков (делений)")); body.addView(inputSurface(count))
        body.addView(label("Единица измерения")); body.addView(inputSurface(unit))
        val result = resultPanel()
        body.addView(primaryButton("▦  Рассчитать") {
            val a = Engineering.parseNumber(low.text.toString()); val b = Engineering.parseNumber(high.text.toString())
            val n = count.text.toString().trim().toIntOrNull()
            if (a == null || b == null || n == null || n <= 0) return@primaryButton invalid()
            result.text = "Результат:\n1 деление = ${Engineering.format(Engineering.divisionValue(a, b, n))} ${unit.text.toString().trim()}"
        })
        body.addView(result)
        body.addView(infoCard("Штрих — нарисованная линия. Деление — промежуток между соседними штрихами. Считать нужно промежутки."))
    }

    private fun electricalScreen(): View = page("Электрический калькулятор", "electrical") { body ->
        body.addView(sectionTabs())
        body.addView(label("Что найти?"))
        val targets = listOf("U", "I", "R", "P")
        val targetNames = mapOf("U" to "Напряжение", "I" to "Ток", "R" to "Сопротивление", "P" to "Мощность")
        var target = "U"
        val targetButtons = mutableListOf<Button>()
        val targetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun pairs(t: String): List<Pair<String, String>> = when (t) {
            "U" -> listOf("I" to "R", "P" to "I", "P" to "R")
            "I" -> listOf("U" to "R", "P" to "U", "P" to "R")
            "R" -> listOf("U" to "I", "U" to "P", "P" to "I")
            else -> listOf("U" to "I", "U" to "R", "I" to "R")
        }
        fun units(name: String) = when (name) {
            "U" -> Engineering.voltageUnits
            "I" -> Engineering.currentUnits
            "R" -> Engineering.resistanceUnits
            else -> Engineering.powerUnits
        }
        fun name(name: String) = "$name — ${targetNames[name]}"
        fun rebuildForm() {
            form.removeAllViews()
            val modes = pairs(target)
            val modeSpinner = spinner(modes.map { "${name(it.first)} + ${name(it.second)}" }, 0)
            form.addView(label("Известные значения")); form.addView(spinnerSurface(modeSpinner))
            val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            form.addView(fields)
            val outputUnit = spinner(units(target).map { it.title }, if (units(target).size > 1) 1 else 0)
            form.addView(labeledSpinner("Единица результата", outputUnit))
            val result = resultPanel()
            var input1: EditText? = null; var input2: EditText? = null; var unit1: Spinner? = null; var unit2: Spinner? = null
            fun rebuildFields() {
                fields.removeAllViews()
                val pair = modes[modeSpinner.selectedItemPosition]
                input1 = numberInput("Введите ${pair.first}"); input2 = numberInput("Введите ${pair.second}")
                unit1 = spinner(units(pair.first).map { it.title }, if (units(pair.first).size > 1) 1 else 0)
                unit2 = spinner(units(pair.second).map { it.title }, if (units(pair.second).size > 1) 1 else 0)
                fields.addView(label(name(pair.first))); fields.addView(valueWithUnit(input1!!, unit1!!))
                fields.addView(label(name(pair.second))); fields.addView(valueWithUnit(input2!!, unit2!!))
            }
            modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = rebuildFields()
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
            rebuildFields()
            form.addView(primaryButton("▦  Рассчитать") {
                val pair = modes[modeSpinner.selectedItemPosition]
                val x = Engineering.parseNumber(input1?.text.toString()); val y = Engineering.parseNumber(input2?.text.toString())
                if (x == null || y == null) return@primaryButton invalid()
                try {
                    val a = x * units(pair.first)[unit1!!.selectedItemPosition].factorToBase
                    val b = y * units(pair.second)[unit2!!.selectedItemPosition].factorToBase
                    val r = Engineering.electrical(pair.first, a, pair.second, b)
                    val targetBase = when (target) { "U" -> r.voltage; "I" -> r.current; "R" -> r.resistance; else -> r.power }
                    val out = units(target)[outputUnit.selectedItemPosition]
                    val formula = formulaFor(pair.first, pair.second, target)
                    result.text = "Результат:\n$target = ${Engineering.format(targetBase / out.factorToBase)} ${out.title}\n\n" +
                        "U = ${Engineering.format(r.voltage)} В   •   I = ${Engineering.format(r.current)} А\n" +
                        "R = ${Engineering.format(r.resistance)} Ом   •   P = ${Engineering.format(r.power)} Вт\n\nФормула: $formula"
                } catch (_: IllegalArgumentException) { invalid() }
            })
            form.addView(result)
        }
        targets.forEachIndexed { index, t ->
            val b = segmentButton("$t\n${targetNames[t]}", index == 0) {
                target = t; targetButtons.forEach { styleSegment(it, it.tag == t) }; rebuildForm()
            }.apply { tag = t }
            targetButtons.add(b); targetRow.addView(b, LinearLayout.LayoutParams(0, dp(70), 1f).apply { setMargins(dp(3), dp(6), dp(3), dp(10)) })
        }
        body.addView(targetRow); body.addView(form); rebuildForm()
    }

    private fun formulaFor(a: String, b: String, target: String): String {
        val pair = setOf(a, b)
        return when (target) {
            "U" -> when { pair == setOf("I", "R") -> "U = I × R"; pair == setOf("P", "I") -> "U = P / I"; else -> "U = √(P × R)" }
            "I" -> when { pair == setOf("U", "R") -> "I = U / R"; pair == setOf("P", "U") -> "I = P / U"; else -> "I = √(P / R)" }
            "R" -> when { pair == setOf("U", "I") -> "R = U / I"; pair == setOf("U", "P") -> "R = U² / P"; else -> "R = P / I²" }
            else -> when { pair == setOf("U", "I") -> "P = U × I"; pair == setOf("U", "R") -> "P = U² / R"; else -> "P = I² × R" }
        }
    }

    private fun siScreen(): View = page("Приставки СИ", "si") { body ->
        body.addView(infoCard("Полный современный диапазон: от кветто 10⁻³⁰ до кветта 10³⁰."))
        val prefixes = listOf(
            Triple("кветта", "Q", "10³⁰"), Triple("ронна", "R", "10²⁷"), Triple("иотта", "Y", "10²⁴"),
            Triple("зетта", "Z", "10²¹"), Triple("экса", "E", "10¹⁸"), Triple("пета", "P", "10¹⁵"),
            Triple("тера", "T", "10¹²"), Triple("гига", "G", "10⁹"), Triple("мега", "M", "10⁶"),
            Triple("кило", "k", "10³"), Triple("гекто", "h", "10²"), Triple("дека", "da", "10¹"),
            Triple("без приставки", "—", "10⁰"), Triple("деци", "d", "10⁻¹"), Triple("санти", "c", "10⁻²"),
            Triple("милли", "m", "10⁻³"), Triple("микро", "µ", "10⁻⁶"), Triple("нано", "n", "10⁻⁹"),
            Triple("пико", "p", "10⁻¹²"), Triple("фемто", "f", "10⁻¹⁵"), Triple("атто", "a", "10⁻¹⁸"),
            Triple("зепто", "z", "10⁻²¹"), Triple("иокто", "y", "10⁻²⁴"), Triple("ронто", "r", "10⁻²⁷"),
            Triple("кветто", "q", "10⁻³⁰")
        )
        prefixes.forEach { (name, symbol, power) ->
            val frequent = name in listOf("микро", "милли", "кило", "мега")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(11), dp(14), dp(11))
                background = rounded(if (frequent) palette.surface2 else palette.surface, 10, if (frequent) palette.blue else palette.line)
            }
            row.addView(text(name, 16f, if (frequent) palette.blue else palette.text).bold(), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text(symbol, 17f, palette.text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(54), -2))
            row.addView(text(power, 16f, palette.muted).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(dp(90), -2))
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(6)) })
        }
        body.addView(infoCard("1 мА = 0,001 А\n1 кОм = 1000 Ом\n1 МПа = 1 000 000 Па"))
    }

    private fun referenceScreen(): View = page("Справочник КИПиА", "reference", showBack = false) { body ->
        val search = textInput("Поиск по разделам и статьям…")
        body.addView(inputSurface(search))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun renderList(query: String) {
            results.removeAllViews()
            if (query.isBlank()) {
                ReferenceData.categories.forEach { cat ->
                    results.addView(categoryCard(cat) { navigate(Screen.Category(cat.id)) })
                }
            } else {
                val q = query.trim().lowercase()
                val found = ReferenceData.articles.filter { article ->
                    article.title.lowercase().contains(q) || article.summary.lowercase().contains(q) ||
                        article.sections.any { it.text.lowercase().contains(q) }
                }
                results.addView(text("Найдено: ${found.size}", 13f, palette.muted).margins(bottom = 8))
                found.forEach { article -> results.addView(listCard(article.title, article.summary) { navigate(Screen.Article(article.id)) }) }
                if (found.isEmpty()) results.addView(infoCard("Ничего не найдено. Попробуйте другое слово: манометр, давление, поверка, реле…"))
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderList(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        body.addView(results); renderList("")
    }

    private fun categoryScreen(id: String): View {
        val cat = ReferenceData.category(id) ?: return referenceScreen()
        return page(cat.title, "reference") { body ->
            body.addView(text(cat.subtitle, 15f, palette.muted).margins(bottom = 14))
            ReferenceData.articlesIn(id).forEach { article -> body.addView(listCard(article.title, article.summary) { navigate(Screen.Article(article.id)) }) }
            if (ReferenceData.articlesIn(id).isEmpty()) body.addView(infoCard("Материалы раздела готовятся."))
        }
    }

    private fun articleScreen(screen: Screen.Article): View {
        val article = ReferenceData.article(screen.id) ?: return referenceScreen()
        return page(article.title, "article") { body ->
            body.addView(text(article.title, 27f, palette.text).bold().margins(bottom = 6))
            body.addView(linkedText(article.summary, 17f).margins(bottom = 14))
            article.diagram?.let { type ->
                val diagram = TechnicalDiagramView(this, type, dark).apply {
                    background = rounded(palette.surface, 14, palette.line)
                    isClickable = true
                    setOnClickListener { navigate(Screen.Diagram(type, "Схема: ${article.title}")) }
                }
                body.addView(diagram, LinearLayout.LayoutParams(-1, dp(320)).apply { setMargins(0, dp(4), 0, dp(8)) })
                body.addView(text("Нажмите на схему, чтобы открыть на весь экран", 12f, palette.muted).apply { gravity = Gravity.CENTER }.margins(bottom = 14))
            }
            article.sections.forEach { section ->
                body.addView(text(section.title, 20f, palette.text).bold().margins(top = 12, bottom = 6))
                body.addView(linkedText(section.text, 16f))
            }
            body.addView(infoCard("Выделенные голубым технические термины можно нажимать."))
        }.also { currentScroll?.post { currentScroll?.scrollTo(0, screen.scrollY) } }
    }

    private fun termScreen(key: String): View {
        val term = ReferenceData.terms[key.lowercase()]
        return page(term?.title ?: key, "reference") { body ->
            body.addView(text(term?.title ?: key, 27f, palette.text).bold().margins(bottom = 12))
            body.addView(infoCard(term?.definition ?: "Определение будет добавлено в следующем обновлении."))
            term?.articleId?.let { id -> body.addView(primaryButton("Подробнее") { navigate(Screen.Article(id)) }) }
            body.addView(secondaryButton("← Назад к статье") { onBackPressed() })
        }
    }

    private fun diagramScreen(type: String, title: String): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(palette.background) }
        root.addView(toolbar(title, null, true))
        root.addView(TechnicalDiagramView(this, type, dark, zoomable = true), LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(text("Разведите два пальца, чтобы увеличить схему", 14f, palette.muted).apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(10), dp(10), dp(10)) })
        return root
    }

    private fun settingsScreen(): View = page("Настройки", "settings", showBack = false) { body ->
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = rounded(palette.surface, 14, palette.line)
        }
        val words = vertical(0).apply {
            addView(text("Тёмная тема", 17f, palette.text).bold())
            addView(text("Удобна при слабом освещении", 13f, palette.muted))
        }
        val toggle = Switch(this).apply {
            isChecked = dark
            setOnCheckedChangeListener { _, checked ->
                dark = checked; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("dark", dark).apply(); applyPalette(); render()
            }
        }
        themeRow.addView(words, LinearLayout.LayoutParams(0, -2, 1f)); themeRow.addView(toggle)
        body.addView(themeRow)
        body.addView(infoCard("Помощник КИПиА\nВерсия 1.0.0\n\nВсе калькуляторы, статьи и технические схемы работают без интернета. Регистрация не требуется."))
        body.addView(infoCard("Расчёты служат рабочей подсказкой. Ответственные измерения, настройку защит и электрические работы выполняйте по документации оборудования и правилам безопасности."))
    }

    private fun helpScreen(kind: String): View {
        val (title, content) = when (kind) {
            "pressure" -> "Давление" to "Что это: сила на единицу площади.\n\nДля чего: контроль трубопроводов, насосов, котлов и сосудов.\n\nОбозначение: P.\n\nФормула: P = F / S.\n\nПример: 0,6 МПа ≈ 6,1183 кгс/см²; для быстрой прикидки 1 МПа ≈ 10 кгс/см²."
            "length" -> "Длина" to "Конвертер переводит одну единицу длины в другую.\n\nОбозначение: l.\n\nПример: 1500 мм = 1,5 м. При переходе от миллиметров к метрам делим на 1000."
            "temperature" -> "Температура" to "Температура характеризует тепловое состояние.\n\nОбозначение: T или t.\n\nФормулы: °F = °C × 9/5 + 32; K = °C + 273,15.\n\nПример: 0 °C = 32 °F = 273,15 K."
            "signal" -> "Сигнал 4–20 мА" to "Стандартный токовый сигнал датчиков. 4 мА соответствует нижнему пределу, 20 мА — верхнему.\n\nФормула: X = Xmin + (I − 4) / 16 × (Xmax − Xmin).\n\nПример: диапазон 0–1,6 МПа, 12 мА = 0,8 МПа = 50 %."
            "division" -> "Цена деления" to "Цена деления показывает, сколько единиц приходится на один промежуток шкалы.\n\nФормула: (верхний предел − нижний предел) / число промежутков.\n\nШтрих — линия, деление — промежуток.\n\nПример: (1,6 − 0) / 80 = 0,02 МПа."
            "electrical" -> "Электрический калькулятор" to "Связывает напряжение U, ток I, сопротивление R и мощность P.\n\nU = I × R; I = U / R; R = U / I; P = U × I.\n\nПример: I = 0,25 А и R = 200 Ом → U = 50 В, P = 12,5 Вт."
            "si" -> "Приставки СИ" to "Приставка изменяет масштаб единицы.\n\n10³ = кило, 10⁶ = мега, 10⁻³ = милли, 10⁻⁶ = микро.\n\nПример: 1 мА = 0,001 А, 1 МПа = 1 000 000 Па."
            "reference" -> "Справочник" to "Офлайн-материалы для начинающего работника КИПиА. Используйте поиск или категории. Голубые термины нажимаются и открывают короткое объяснение."
            else -> "О разделе" to "Раздел приложения «Помощник КИПиА». Все основные данные хранятся на телефоне и доступны без интернета."
        }
        return page(title, null) { body ->
            body.addView(text(title, 28f, palette.text).bold().margins(bottom = 14))
            body.addView(infoCard(content))
        }
    }

    private fun page(title: String, help: String?, showBack: Boolean = true, build: (LinearLayout) -> Unit): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(palette.background) }
        root.addView(toolbar(title, help, showBack && (history.isNotEmpty() || current !is Screen.Home)))
        val body = vertical(dp(14)); build(body)
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(body) }
        currentScroll = scroll
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }

    private fun toolbar(title: String, help: String?, back: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(10), dp(6)); setBackgroundColor(palette.header)
        if (back) addView(Button(this@MainActivity).apply {
            text = "‹"; textSize = 32f; setTextColor(Color.WHITE); backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT); setOnClickListener { onBackPressed() }
        }, LinearLayout.LayoutParams(dp(54), dp(54)))
        addView(text(title, 20f, Color.WHITE).bold(), LinearLayout.LayoutParams(0, -2, 1f))
        if (help != null) addView(Button(this@MainActivity).apply {
            text = "?"; textSize = 18f; setTextColor(Color.WHITE); background = rounded(Color.rgb(41, 77, 100), 28); setOnClickListener { navigate(Screen.Help(help)) }
        }, LinearLayout.LayoutParams(dp(46), dp(46)))
    }

    private fun bottomNavigation(): View {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(palette.header); setPadding(dp(4), dp(3), dp(4), dp(3)) }
        val items = listOf("⌂\nГлавная" to Screen.Home, "▦\nРасчёты" to Screen.Calculations, "▣\nСправочник" to Screen.Reference, "⚙\nНастройки" to Screen.Settings)
        items.forEach { (label, target) ->
            val selected = when (target) {
                Screen.Home -> current is Screen.Home
                Screen.Calculations -> current is Screen.Calculations || current is Screen.Calculator
                Screen.Reference -> current is Screen.Reference || current is Screen.Category || current is Screen.Article || current is Screen.Term
                else -> current is Screen.Settings
            }
            val b = Button(this).apply {
                text = label; textSize = 11f; isAllCaps = false; setTextColor(if (selected) Color.rgb(105, 204, 255) else Color.rgb(180, 204, 218))
                backgroundTintList = ColorStateList.valueOf(if (selected) Color.rgb(17, 55, 79) else Color.TRANSPARENT)
                setOnClickListener { root(target) }
            }
            bar.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return bar
    }

    private fun sectionTabs(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; background = rounded(palette.surface2, 10, palette.line); setPadding(dp(2), dp(2), dp(2), dp(2))
        addView(text("Расчёт", 14f, Color.WHITE).apply { gravity = Gravity.CENTER; background = rounded(palette.blue, 9) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        addView(text("О разделе — нажмите ?", 13f, palette.muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(42), 1f))
    }

    private fun homeCard(icon: String, name: String, sub: String, color: Int, click: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(8), dp(8), dp(8)); background = rounded(color, 14, lighten(color)); elevation = dp(2).toFloat(); isClickable = true
        addView(text(icon, 31f, Color.rgb(116, 218, 255)).apply { gravity = Gravity.CENTER })
        addView(text(name, 16f, Color.WHITE).bold().apply { gravity = Gravity.CENTER })
        addView(text(sub, 11f, Color.rgb(218, 232, 239)).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
        setOnClickListener { click() }
    }

    private fun categoryCard(cat: ReferenceCategory, click: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(13), dp(11), dp(13), dp(11)); background = rounded(palette.surface, 11, palette.line); isClickable = true
        addView(text(cat.icon, 22f, palette.blue).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(40), -2))
        val words = vertical(0).apply { addView(text(cat.title, 16f, palette.text).bold()); addView(text(cat.subtitle, 12f, palette.muted)) }
        addView(words, LinearLayout.LayoutParams(0, -2, 1f)); addView(text("›", 27f, palette.muted))
        setOnClickListener { click() }
    }.margins(bottom = 7)

    private fun listCard(name: String, sub: String, click: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(15), dp(13), dp(12), dp(13)); background = rounded(palette.surface, 12, palette.line); isClickable = true
        val words = vertical(0).apply { addView(text(name, 17f, palette.text).bold()); addView(text(sub, 13f, palette.muted).apply { setPadding(0, dp(3), 0, 0) }) }
        addView(words, LinearLayout.LayoutParams(0, -2, 1f)); addView(text("›", 26f, palette.blue))
        setOnClickListener { click() }
    }.margins(bottom = 8)

    private fun linkedText(value: String, size: Float): TextView {
        val span = SpannableString(value)
        val occupied = BooleanArray(value.length)
        val lower = value.lowercase()
        ReferenceData.terms.values.sortedByDescending { it.title.length }.forEach { term ->
            var start = lower.indexOf(term.title.lowercase())
            while (start >= 0) {
                val end = start + term.title.length
                if ((start until end).none { occupied[it] }) {
                    (start until end).forEach { occupied[it] = true }
                    span.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) { navigate(Screen.Term(term.title.lowercase())) }
                        override fun updateDrawState(ds: TextPaint) { ds.color = palette.blue; ds.isUnderlineText = false; ds.isFakeBoldText = true }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                start = lower.indexOf(term.title.lowercase(), start + 1)
            }
        }
        return text("", size, palette.text).apply { text = span; movementMethod = LinkMovementMethod.getInstance(); highlightColor = Color.TRANSPARENT; setLineSpacing(0f, 1.18f) }
    }

    private fun resultPanel() = text("Результат появится здесь", 21f, palette.text).bold().apply {
        setPadding(dp(16), dp(15), dp(16), dp(15)); background = rounded(if (dark) Color.rgb(11, 79, 57) else Color.rgb(215, 245, 226), 12, palette.green); setTextIsSelectable(true)
    }.margins(top = 10, bottom = 10)

    private fun infoCard(value: String) = text(value, 15f, palette.text).apply {
        setPadding(dp(15), dp(13), dp(15), dp(13)); setLineSpacing(0f, 1.15f); background = rounded(palette.surface, 11, palette.line)
    }.margins(top = 10, bottom = 6)

    private fun primaryButton(caption: String, click: () -> Unit) = Button(this).apply {
        text = caption; textSize = 17f; isAllCaps = false; setTextColor(Color.WHITE); background = rounded(palette.blue, 11); setOnClickListener { click() }
    }.margins(top = 14, bottom = 4, height = 56)

    private fun secondaryButton(caption: String, click: () -> Unit) = Button(this).apply {
        text = caption; textSize = 16f; isAllCaps = false; setTextColor(palette.text); background = rounded(palette.surface2, 11, palette.line); setOnClickListener { click() }
    }.margins(top = 12, bottom = 4, height = 54)

    private fun segmentButton(caption: String, selected: Boolean, click: () -> Unit) = Button(this).apply {
        text = caption; textSize = 12f; isAllCaps = false; setOnClickListener { click() }; styleSegment(this, selected)
    }

    private fun styleSegment(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) Color.WHITE else palette.text)
        button.background = rounded(if (selected) palette.blue else palette.surface2, 10, if (selected) palette.blue else palette.line)
    }

    private fun numberInput(hint: String, integer: Boolean = false) = EditText(this).apply {
        this.hint = hint; textSize = 19f; setTextColor(palette.text); setHintTextColor(palette.muted); setSingleLine(true); background = null; setPadding(dp(12), 0, dp(12), 0)
        inputType = InputType.TYPE_CLASS_NUMBER or if (integer) 0 else InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    private fun textInput(hint: String) = EditText(this).apply {
        this.hint = hint; textSize = 17f; setTextColor(palette.text); setHintTextColor(palette.muted); setSingleLine(true); background = null; setPadding(dp(12), 0, dp(12), 0)
    }

    private fun inputSurface(input: EditText): View = FrameLayout(this).apply {
        background = rounded(palette.surface, 10, palette.line); addView(input, FrameLayout.LayoutParams(-1, dp(54)))
    }.margins(top = 4, bottom = 10)

    private fun valueWithUnit(input: EditText, unit: Spinner): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; background = rounded(palette.surface, 10, palette.line)
        addView(input, LinearLayout.LayoutParams(0, dp(54), 1f)); addView(unit, LinearLayout.LayoutParams(dp(112), dp(54)))
    }.margins(top = 4, bottom = 10)

    private fun spinner(items: List<String>, selected: Int = 0) = Spinner(this).apply {
        adapter = TextAdapter(this@MainActivity, items); setSelection(selected.coerceIn(0, items.lastIndex)); background = rounded(palette.surface, 10, palette.line)
    }

    private fun spinnerSurface(spinner: Spinner): View = FrameLayout(this).apply {
        addView(spinner, FrameLayout.LayoutParams(-1, dp(54)))
    }.margins(top = 4, bottom = 10)

    private fun labeledSpinner(caption: String, spinner: Spinner): View = vertical(0).apply {
        addView(label(caption)); addView(spinner, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, dp(4), 0, dp(10)) })
    }

    private inner class TextAdapter(context: Context, private val values: List<String>) : BaseAdapter() {
        private val inflaterContext = context
        override fun getCount() = values.size
        override fun getItem(position: Int) = values[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View = row(values[position], false)
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View = row(values[position], true)
        private fun row(value: String, dropdown: Boolean) = TextView(inflaterContext).apply {
            text = value; textSize = 16f; setTextColor(palette.text); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(12), 0); setBackgroundColor(palette.surface)
            minHeight = dp(if (dropdown) 50 else 48)
        }
    }

    private fun vertical(padding: Int) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(padding, padding, padding, dp(24)) }
    private fun title(value: String, size: Float, color: Int) = text(value, size, color).bold()
    private fun label(value: String) = text(value, 14f, palette.muted).bold().margins(top = 6)
    private fun text(value: String, size: Float, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color) }
    private fun TextView.bold() = apply { setTypeface(typeface, Typeface.BOLD) }
    private fun <T : View> T.margins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0, width: Int = -1, height: Int = -2): T = apply {
        layoutParams = LinearLayout.LayoutParams(width, height).apply { setMargins(dp(left), dp(top), dp(right), dp(bottom)) }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) }
    }

    private fun verticalGradient(top: Int, bottom: Int) = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom))
    private fun lighten(color: Int) = Color.rgb((Color.red(color) + 35).coerceAtMost(255), (Color.green(color) + 35).coerceAtMost(255), (Color.blue(color) + 35).coerceAtMost(255))
    private fun invalid() = Toast.makeText(this, "Проверьте введённые данные", Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
