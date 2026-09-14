package ru.balalaiki.kipiacalculator

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class MainActivity : Activity() {
    private val navy = Color.rgb(16, 24, 32)
    private val orange = Color.rgb(255, 159, 28)
    private val paper = Color.rgb(243, 246, 248)
    private val ink = Color.rgb(23, 33, 43)
    private var homeVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    @Deprecated("Handled for compatibility with Android 10")
    override fun onBackPressed() {
        if (homeVisible) super.onBackPressed() else showHome()
    }

    private fun showHome() {
        homeVisible = true
        val root = pageRoot()
        root.addView(header("КИПиА Калькулятор", false))
        root.addView(text("Быстрые расчёты и подсказки без интернета", 17f, ink).apply {
            setPadding(dp(18), dp(20), dp(18), dp(8))
        })
        root.addView(text("Выберите нужный инструмент", 14f, Color.DKGRAY).apply {
            setPadding(dp(18), 0, dp(18), dp(14))
        })

        val tools = listOf(
            "Давление" to { showPressure() },
            "Сигнал 4–20 мА" to { showSignal() },
            "Длина" to { showLength() },
            "Температура" to { showTemperature() },
            "Закон Ома" to { showOhm() },
            "Приставки СИ" to { showSi() }
        )
        tools.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(5), dp(12), dp(5))
            }
            pair.forEach { (label, action) ->
                row.addView(menuButton(label, action), LinearLayout.LayoutParams(0, dp(88), 1f).apply {
                    setMargins(dp(5), 0, dp(5), 0)
                })
            }
            root.addView(row)
        }

        root.addView(text("Версия 0.1 • рассчитано для работы и обучения", 12f, Color.GRAY).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(24), dp(12), dp(24))
        })
        display(root)
    }

    private fun showPressure() = showToolPage(
        "Конвертер давления",
        "Введите значение и единицу — получите результат сразу во всех единицах."
    ) { content ->
        val input = numberInput("Например: 0,6")
        val spinner = unitSpinner(UnitConverters.pressureUnits.keys.toList(), 2)
        val result = resultText()
        content.addView(fieldLabel("Значение"))
        content.addView(input)
        content.addView(fieldLabel("Исходная единица"))
        content.addView(spinner)
        content.addView(actionButton("ПЕРЕВЕСТИ") {
            val value = input.valueOrNull() ?: return@actionButton showInputError()
            val from = spinner.selectedItem.toString()
            result.text = UnitConverters.pressureUnits.keys.joinToString("\n") { unit ->
                val converted = UnitConverters.convert(value, from, unit, UnitConverters.pressureUnits)
                "${UnitConverters.format(converted)} $unit"
            }
        })
        content.addView(result)
        content.addView(note("Для рабочих прикидок: 1 МПа ≈ 10 кгс/см². Точнее: 1 МПа = 10,1972 кгс/см²."))
    }

    private fun showLength() = showToolPage(
        "Конвертер длины",
        "Микрометры, миллиметры, сантиметры, метры и километры."
    ) { content ->
        val input = numberInput("Например: 250")
        val spinner = unitSpinner(UnitConverters.lengthUnits.keys.toList(), 1)
        val result = resultText()
        content.addView(fieldLabel("Значение"))
        content.addView(input)
        content.addView(fieldLabel("Исходная единица"))
        content.addView(spinner)
        content.addView(actionButton("ПЕРЕВЕСТИ") {
            val value = input.valueOrNull() ?: return@actionButton showInputError()
            val from = spinner.selectedItem.toString()
            result.text = UnitConverters.lengthUnits.keys.joinToString("\n") { unit ->
                val converted = UnitConverters.convert(value, from, unit, UnitConverters.lengthUnits)
                "${UnitConverters.format(converted)} $unit"
            }
        })
        content.addView(result)
    }

    private fun showSignal() = showToolPage(
        "Сигнал 4–20 мА",
        "Пересчёт тока в показание прибора и обратно. Пример диапазона: 0…10 бар."
    ) { content ->
        val low = numberInput("Нижний предел, например 0")
        val high = numberInput("Верхний предел, например 10")
        val current = numberInput("Ток, мА — например 12")
        val physical = numberInput("Показание — например 5")
        val result = resultText()

        content.addView(fieldLabel("Диапазон измерения"))
        content.addView(low)
        content.addView(high)
        content.addView(fieldLabel("Из тока в показание"))
        content.addView(current)
        content.addView(actionButton("мА → ЗНАЧЕНИЕ") {
            val lo = low.valueOrNull()
            val hi = high.valueOrNull()
            val ma = current.valueOrNull()
            if (lo == null || hi == null || ma == null || hi == lo) return@actionButton showInputError()
            val value = UnitConverters.currentToValue(ma, lo, hi)
            val percent = (ma - 4.0) / 16.0 * 100.0
            result.text = "${UnitConverters.format(value)}\n${UnitConverters.format(percent)} % диапазона" +
                if (ma !in 4.0..20.0) "\nВнимание: ток вне диапазона 4–20 мА" else ""
        })
        content.addView(fieldLabel("Из показания в ток"))
        content.addView(physical)
        content.addView(actionButton("ЗНАЧЕНИЕ → мА") {
            val lo = low.valueOrNull()
            val hi = high.valueOrNull()
            val value = physical.valueOrNull()
            if (lo == null || hi == null || value == null || hi == lo) return@actionButton showInputError()
            result.text = "${UnitConverters.format(UnitConverters.valueToCurrent(value, lo, hi))} мА"
        })
        content.addView(result)
        content.addView(note("4 мА = 0 % диапазона, 12 мА = 50 %, 20 мА = 100 %."))
    }

    private fun showTemperature() = showToolPage(
        "Температура",
        "Перевод между градусами Цельсия, Фаренгейта и Кельвинами."
    ) { content ->
        val input = numberInput("Например: 100")
        val spinner = unitSpinner(listOf("°C", "°F", "K"), 0)
        val result = resultText()
        content.addView(fieldLabel("Температура"))
        content.addView(input)
        content.addView(fieldLabel("Исходная единица"))
        content.addView(spinner)
        content.addView(actionButton("ПЕРЕВЕСТИ") {
            val value = input.valueOrNull() ?: return@actionButton showInputError()
            val celsius = when (spinner.selectedItem.toString()) {
                "°F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            val fahrenheit = celsius * 9.0 / 5.0 + 32.0
            val kelvin = celsius + 273.15
            result.text = "${UnitConverters.format(celsius)} °C\n" +
                "${UnitConverters.format(fahrenheit)} °F\n" +
                "${UnitConverters.format(kelvin)} K"
        })
        content.addView(result)
    }

    private fun showOhm() = showToolPage(
        "Закон Ома",
        "Выберите две известные величины. Приложение вычислит остальные."
    ) { content ->
        val modes = listOf("Напряжение U и ток I", "Напряжение U и сопротивление R", "Ток I и сопротивление R")
        val mode = unitSpinner(modes, 0)
        val first = numberInput("Первая величина")
        val second = numberInput("Вторая величина")
        val result = resultText()
        content.addView(fieldLabel("Что известно"))
        content.addView(mode)
        content.addView(first)
        content.addView(second)
        content.addView(actionButton("РАССЧИТАТЬ") {
            val a = first.valueOrNull()
            val b = second.valueOrNull()
            if (a == null || b == null) return@actionButton showInputError()
            val values = when (mode.selectedItemPosition) {
                0 -> if (b != 0.0) doubleArrayOf(a, b, a / b, a * b) else null
                1 -> if (b != 0.0) doubleArrayOf(a, a / b, b, a * a / b) else null
                else -> doubleArrayOf(a * b, a, b, a * a * b)
            } ?: return@actionButton showInputError()
            result.text = "U = ${UnitConverters.format(values[0])} В\n" +
                "I = ${UnitConverters.format(values[1])} А\n" +
                "R = ${UnitConverters.format(values[2])} Ом\n" +
                "P = ${UnitConverters.format(values[3])} Вт"
        })
        content.addView(result)
        content.addView(note("Главная формула: U = I × R. Мощность: P = U × I."))
    }

    private fun showSi() = showToolPage(
        "Приставки СИ",
        "Приставка показывает, во сколько раз единица больше или меньше основной."
    ) { content ->
        val rows = listOf(
            Triple("гига", "Г", "10⁹ = 1 000 000 000"),
            Triple("мега", "М", "10⁶ = 1 000 000"),
            Triple("кило", "к", "10³ = 1 000"),
            Triple("гекто", "г", "10² = 100"),
            Triple("дека", "да", "10¹ = 10"),
            Triple("деци", "д", "10⁻¹ = 0,1"),
            Triple("санти", "с", "10⁻² = 0,01"),
            Triple("милли", "м", "10⁻³ = 0,001"),
            Triple("микро", "мк", "10⁻⁶ = 0,000001"),
            Triple("нано", "н", "10⁻⁹ = 0,000000001")
        )
        rows.forEachIndexed { index, (name, symbol, value) ->
            content.addView(text("$name ($symbol)   $value", 16f, ink).apply {
                setPadding(dp(14), dp(13), dp(14), dp(13))
                setBackgroundColor(if (index % 2 == 0) Color.WHITE else Color.rgb(229, 235, 239))
            })
        }
        content.addView(note("Примеры: 1 кПа = 1 000 Па; 1 МПа = 1 000 кПа; 1 мА = 0,001 А."))
    }

    private fun showToolPage(title: String, intro: String, build: (LinearLayout) -> Unit) {
        homeVisible = false
        val root = pageRoot()
        root.addView(header(title, true))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }
        content.addView(text(intro, 15f, Color.DKGRAY).apply { setPadding(0, 0, 0, dp(14)) })
        build(content)
        root.addView(content)
        display(root)
    }

    private fun pageRoot() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(paper)
    }

    private fun display(root: LinearLayout) {
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun header(title: String, back: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(10), dp(16), dp(10))
        setBackgroundColor(navy)
        if (back) {
            addView(Button(this@MainActivity).apply {
                text = "‹"
                textSize = 30f
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(navy)
                setOnClickListener { showHome() }
            }, LinearLayout.LayoutParams(dp(54), dp(54)))
        }
        addView(text(title, 21f, Color.WHITE).apply {
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(58), 1f))
    }

    private fun menuButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        isAllCaps = false
        backgroundTintList = ColorStateList.valueOf(navy)
        setOnClickListener { action() }
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(navy)
        backgroundTintList = ColorStateList.valueOf(orange)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply {
            setMargins(0, dp(12), 0, dp(12))
        }
    }

    private fun numberInput(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 17f
        setTextColor(ink)
        setHintTextColor(Color.GRAY)
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
            InputType.TYPE_NUMBER_FLAG_SIGNED
        setPadding(dp(12), dp(10), dp(12), dp(10))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply {
            setMargins(0, dp(4), 0, dp(8))
        }
    }

    private fun unitSpinner(items: List<String>, selected: Int) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        setSelection(selected)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
    }

    private fun resultText() = text("Результат появится здесь", 19f, ink).apply {
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextIsSelectable(true)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
    }

    private fun fieldLabel(value: String) = text(value, 14f, ink).apply {
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(8), 0, 0)
    }

    private fun note(value: String) = text(value, 14f, Color.DKGRAY).apply {
        setPadding(dp(2), dp(10), dp(2), dp(10))
    }

    private fun text(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun EditText.valueOrNull(): Double? = text.toString()
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && abs(it) < 1e100 }

    private fun showInputError() {
        Toast.makeText(this, "Проверьте введённые числа", Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
