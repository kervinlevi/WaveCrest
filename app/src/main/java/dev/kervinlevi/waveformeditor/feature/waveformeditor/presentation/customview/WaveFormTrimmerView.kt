package dev.kervinlevi.waveformeditor.feature.waveformeditor.presentation.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import dev.kervinlevi.waveformeditor.R
import dev.kervinlevi.waveformeditor.feature.waveformeditor.domain.model.WavePair
import kotlin.math.ceil

class WaveFormTrimmerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waves = mutableListOf<WavePair>()
    private val wavePath = Path()
    private val selectedWavePath = Path()
    private var viewRect = RectF()
    private var waveRect = RectF()

    private var lastTouchEvent = PointF(0f, 0f)
    private var xIncrement = 0f
    private val wavePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.wave_unselected_color)
            style = Paint.Style.FILL
        }
    }
    private val selectedWavePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.wave_color)
            style = Paint.Style.FILL
        }
    }
    private val backgroundPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.wave_bg_color)
            style = Paint.Style.FILL
        }
    }
    private val markerPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.picker_inactive_color)
            style = Paint.Style.FILL
            strokeWidth = markerThickness
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val activeMarkerPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.picker_active_color)
            style = Paint.Style.FILL
            strokeWidth = markerThickness
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val knobSize by lazy {
        resources.getDimensionPixelSize(R.dimen.editor_marker_knob_size).toFloat()
    }
    private val markerThickness by lazy {
        resources.getDimensionPixelSize(R.dimen.editor_marker_thickness).toFloat()
    }

    private var markerA = Marker()
    private var markerB = Marker()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        waveRect = RectF(0f, knobSize / 2f, w.toFloat(), h.toFloat() - knobSize / 2f)
        computeWavePath()

        markerA.bound.top = 0f
        markerA.bound.bottom = h.toFloat()
        markerB.bound.top = 0f
        markerB.bound.bottom = h.toFloat()
        computeMarkerPath()
        computeSelectedWavePath()

        disableBackGesture()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawRect(waveRect, backgroundPaint)
            drawPath(wavePath, wavePaint)
            drawPath(selectedWavePath, selectedWavePaint)

            if (showMarkers()) {
                drawMarkers()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || !showMarkers()) {
            selectMarker(markerASelected = false, markerBSelected = false)
            postInvalidate()
            return false
        }
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (markerA.containsTouch(x, y) && markerB.containsTouch(x, y)) {
                    val upper = y < viewRect.centerY()
                    selectMarker(markerASelected = upper, markerBSelected = !upper)
                } else if (markerA.containsTouch(x, y)) {
                    selectMarker(markerASelected = true, markerBSelected = false)
                } else if (markerB.containsTouch(x, y)) {
                    selectMarker(markerASelected = false, markerBSelected = true)
                } else {
                    selectMarker(markerASelected = false, markerBSelected = false)
                }
            }

            MotionEvent.ACTION_UP -> {
                selectMarker(markerASelected = false, markerBSelected = false)
            }

            MotionEvent.ACTION_MOVE -> {
                if (markerA.isSelected) {
                    markerA.index = (x / xIncrement).toInt().coerceIn(0, markerB.index - 1)
                    markerA.index = minOf(markerB.index - 1, markerA.index)
                    disallowScrollViewInterception()

                }
                if (markerB.isSelected) {
                    markerB.index = ceil(x / xIncrement).toInt()
                    markerB.index = markerB.index.coerceIn(markerA.index + 1, waves.lastIndex)
                    disallowScrollViewInterception()
                }
            }
        }
        lastTouchEvent.x = event.x
        lastTouchEvent.y = event.y

        computeMarkerPath()
        computeSelectedWavePath()
        postInvalidate()
        return true
    }

    fun setWaves(waves: List<WavePair>, markerAIndex: Int, markerBIndex: Int) {
        if (this.waves == waves && markerAIndex == markerA.index && markerBIndex == markerB.index) {
            return
        }

        this.waves.clear()
        this.waves.addAll(waves)
        markerA.index = markerAIndex
        markerB.index = markerBIndex

        computeWavePath()
        computeMarkerPath()
        computeSelectedWavePath()
        postInvalidate()
    }

    fun getMarkers(): Pair<Int, Int> {
        return markerA.index to markerB.index
    }

    fun isEmpty(): Boolean {
        return waves.isEmpty()
    }

    private fun computeWavePath() {
        val upperWavePath = Path()
        val lowerWavePath = Path()

        xIncrement = width / (waves.size - 1).toFloat()
        val centerY = viewRect.centerY()
        val maxWaveLength = waveRect.centerY() - waveRect.top

        wavePath.reset()
        upperWavePath.moveTo(0f, centerY)
        lowerWavePath.moveTo(0f, centerY)

        var x = 0f
        waves.forEachIndexed { index, pair ->
            val upperY = centerY - maxWaveLength * (pair.second / 1.0).toFloat()
            val lowerY = centerY - maxWaveLength * (pair.first / 1.0).toFloat()

            if (index == waves.lastIndex) {
                x = width.toFloat()
            }

            upperWavePath.lineTo(x, upperY)
            lowerWavePath.lineTo(x, lowerY)

            if (index != waves.lastIndex) {
                x += xIncrement
            }
        }

        upperWavePath.lineTo(x, centerY)
        lowerWavePath.lineTo(x, centerY)

        wavePath.apply {
            addPath(upperWavePath)
            addPath(lowerWavePath)
        }
    }

    private fun computeMarkerPath() {
        markerA.bound.left = markerA.index * xIncrement
        markerA.bound.right = markerA.bound.left + knobSize

        markerB.bound.right = markerB.index * xIncrement
        markerB.bound.left = markerB.bound.right - knobSize

        markerA.knobPath.apply {
            reset()
            moveTo(markerA.bound.left, 0f)
            arcTo(
                markerA.bound.left, 0f, markerA.bound.left + knobSize, knobSize, 270f, 180f, false
            )
            lineTo(markerA.bound.left, knobSize)
        }

        markerB.knobPath.apply {
            reset()
            moveTo(markerB.bound.right, viewRect.bottom)
            arcTo(
                markerB.bound.right - knobSize,
                viewRect.bottom - knobSize,
                markerB.bound.right,
                viewRect.bottom,
                90f,
                180f,
                false
            )
            lineTo(markerB.bound.right, viewRect.bottom - knobSize)
        }
    }

    private fun computeSelectedWavePath() {
        selectedWavePath.reset()

        selectedWavePath.moveTo(markerA.bound.left, markerA.bound.top)
        selectedWavePath.lineTo(markerB.bound.right, markerB.bound.top)
        selectedWavePath.lineTo(markerB.bound.right, markerB.bound.bottom)
        selectedWavePath.lineTo(markerA.bound.left, markerA.bound.bottom)
        selectedWavePath.op(wavePath, Path.Op.INTERSECT)
    }

    private fun Canvas.drawMarkers() {
        val offset = knobSize / 3f
        if (!markerA.isSelected) {
            drawLine(
                markerA.bound.left, 0f, markerA.bound.left, viewRect.bottom - offset, markerPaint
            )
            drawPath(markerA.knobPath, markerPaint)
        }

        val paintB = if (markerB.isSelected) activeMarkerPaint else markerPaint
        drawLine(markerB.bound.right, offset, markerB.bound.right, viewRect.bottom, paintB)
        drawPath(markerB.knobPath, paintB)

        if (markerA.isSelected) {
            drawLine(
                markerA.bound.left,
                0f,
                markerA.bound.left,
                viewRect.bottom - offset,
                activeMarkerPaint
            )
            drawPath(markerA.knobPath, activeMarkerPaint)
        }
    }

    private fun showMarkers(): Boolean {
        return markerA.index >= 0 && markerA.index < waves.size && markerB.index >= 0 && markerB.index < waves.size
    }

    private fun selectMarker(markerASelected: Boolean, markerBSelected: Boolean) {
        markerA.isSelected = markerASelected
        markerB.isSelected = markerBSelected
    }

    private fun disallowScrollViewInterception() {
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun disableBackGesture() { // disable back swipe gesture for the view bounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(
                viewRect.toRect()
            )
        }
    }

    data class Marker(
        var index: Int = -1,
        val knobPath: Path = Path(),
        var isSelected: Boolean = false,
        val bound: RectF = RectF()
    ) {
        fun containsTouch(x: Float, y: Float): Boolean {
            return bound.contains(x , y)
        }
    }
}
