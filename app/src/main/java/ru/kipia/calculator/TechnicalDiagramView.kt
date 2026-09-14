package ru.kipia.calculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Локальные векторные технические схемы: работают офлайн и не теряют качество при увеличении. */
class TechnicalDiagramView(
    context: Context,
    private val diagram: String,
    private val dark: Boolean,
    private val zoomable: Boolean = false
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scale = 1f
    private val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            scale = (scale * d.scaleFactor).coerceIn(1f, 4f)
            invalidate()
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!zoomable) return super.onTouchEvent(event)
        detector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(if (dark) Color.rgb(19, 39, 54) else Color.rgb(235, 242, 247))
        canvas.save()
        canvas.scale(scale, scale, width / 2f, height / 2f)
        when (diagram) {
            "thermocouple" -> drawThermocouple(canvas)
            "signal420" -> drawSignal(canvas)
            else -> drawManometer(canvas)
        }
        canvas.restore()
    }

    private fun drawManometer(c: Canvas) {
        val cx = width * .5f
        val cy = height * .44f
        val r = minOf(width, height) * .30f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(244, 246, 242)
        c.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * .08f
        paint.color = Color.rgb(190, 132, 55)
        c.drawCircle(cx, cy, r * 1.06f, paint)

        paint.strokeWidth = 3f
        paint.color = Color.DKGRAY
        for (n in 0..10) {
            val angle = Math.toRadians((135.0 + n * 27.0))
            val x1 = cx + cos(angle).toFloat() * r * .72f
            val y1 = cy + sin(angle).toFloat() * r * .72f
            val x2 = cx + cos(angle).toFloat() * r * .88f
            val y2 = cy + sin(angle).toFloat() * r * .88f
            c.drawLine(x1, y1, x2, y2, paint)
        }
        paint.strokeWidth = 7f
        paint.color = Color.rgb(29, 126, 220)
        val a = Math.toRadians(315.0)
        c.drawLine(cx, cy, cx + cos(a).toFloat() * r * .68f, cy + sin(a).toFloat() * r * .68f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.DKGRAY
        c.drawCircle(cx, cy, r * .09f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * .10f
        paint.color = Color.rgb(219, 151, 55)
        val tube = RectF(cx - r * .48f, cy - r * .42f, cx + r * .48f, cy + r * .48f)
        c.drawArc(tube, 60f, 245f, false, paint)
        paint.color = Color.rgb(62, 74, 82)
        paint.strokeWidth = r * .05f
        c.drawLine(cx, cy, cx - r * .35f, cy + r * .38f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(190, 132, 55)
        c.drawRect(cx - r * .16f, cy + r * 1.02f, cx + r * .16f, cy + r * 1.42f, paint)
        label(c, "Стрелка", cx + r * .35f, cy - r * .44f, Color.rgb(61, 138, 231))
        label(c, "Трубка Бурдона", cx - r * 1.15f, cy + r * .55f, Color.rgb(245, 170, 44))
        label(c, "Штуцер", cx + r * .28f, cy + r * 1.22f, Color.rgb(231, 99, 78))
        label(c, "Шкала", cx - r * 1.05f, cy - r * .65f, Color.rgb(137, 145, 151))
    }

    private fun drawThermocouple(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 16f
        paint.color = Color.rgb(231, 99, 78)
        val p1 = Path().apply { moveTo(w * .15f, h * .72f); cubicTo(w * .42f, h * .68f, w * .43f, h * .35f, w * .5f, h * .30f) }
        c.drawPath(p1, paint)
        paint.color = Color.rgb(61, 138, 231)
        val p2 = Path().apply { moveTo(w * .85f, h * .72f); cubicTo(w * .58f, h * .68f, w * .57f, h * .35f, w * .5f, h * .30f) }
        c.drawPath(p2, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(245, 170, 44)
        c.drawCircle(w * .5f, h * .29f, 20f, paint)
        label(c, "Горячий спай", w * .36f, h * .14f, Color.rgb(245, 170, 44))
        label(c, "Проводник A (+)", w * .07f, h * .82f, Color.rgb(231, 99, 78))
        label(c, "Проводник B (−)", w * .58f, h * .82f, Color.rgb(61, 138, 231))
    }

    private fun drawSignal(c: Canvas) {
        val left = width * .12f; val right = width * .88f; val y = height * .48f
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 22f; paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(45, 84, 109); c.drawLine(left, y, right, y, paint)
        paint.color = Color.rgb(35, 190, 99); c.drawLine(left, y, width * .50f, y, paint)
        paint.style = Paint.Style.FILL
        listOf(left to "4 мА\n0 %", width * .5f to "12 мА\n50 %", right to "20 мА\n100 %").forEach { (x, t) ->
            paint.color = Color.WHITE; c.drawCircle(x, y, 17f, paint)
            label(c, t, x - 52f, y + 48f, Color.rgb(35, 190, 99))
        }
    }

    private fun label(c: Canvas, value: String, x: Float, y: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.textSize = minOf(width, height) * .045f
        paint.color = color
        value.split("\n").forEachIndexed { index, line -> c.drawText(line, x, y + index * paint.textSize * 1.2f, paint) }
    }
}
