package com.realdrum.simpledrumsrock.drumpadmachine.utils.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class CustomSeekbarSplash : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var w = 0f
    private var rectF = RectF()
    private var path = Path()
    private var paintBg: Paint
    private var paintProgress: Paint
    var progress = 0
    private var max = 100
    private var sizeThumb = 0f
    private var sizeBg = 0f
    private var sizePos = 0f
    private var isFirstShader = false
    private var radius = 0f
    private var isCreate = true

    private var colorBg = intArrayOf(Color.WHITE,  Color.parseColor("#D9D9D9"))
    private var colorPr = intArrayOf(
        Color.parseColor("#6644B8"),
        Color.parseColor("#6644B8"),
    )

    var onProgress: ICallBackProgress? = null

    init {
        w = resources.displayMetrics.widthPixels / 100f
        sizeBg = 3.33f * w
        sizePos = 2.22f * w
        radius = 3.33f * w

        paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isCreate) {
            isCreate = false
            rectF.set(radius * 2f, (height - radius) / 2f, width - radius * 2f, (height + radius) / 2f)
            path.addRoundRect(rectF, radius / 2f, radius / 2f, Path.Direction.CW)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        canvas.clipPath(path)

        paintBg.apply {
            color = colorBg[0]
            strokeWidth = sizeBg
        }
        canvas.drawLine(sizeThumb / 2 + radius, height / 2f, width - sizeThumb / 2 - radius, height / 2f, paintBg)
        paintBg.apply {
            color = colorBg[1]
            strokeWidth = 2 * sizeBg / 3
        }
        canvas.drawLine(sizeThumb / 2 + radius, height / 2f, width - sizeThumb / 2 - radius, height / 2f, paintBg)

        if (!isFirstShader) {
            paintProgress.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colorPr, null, Shader.TileMode.CLAMP)
            isFirstShader = true
        }
        paintProgress.strokeWidth = sizePos
        val p = (width - sizeThumb - 2 * radius) * progress / max + sizeThumb / 2f + radius
        canvas.drawLine(sizeThumb / 2f + radius, height / 2f, p, height / 2f, paintProgress)

        if (progress < 99) {
            progress++
            onProgress?.onProgress(progress)
            postInvalidateDelayed(25)
        }
    }

    fun setColorProgress(colors: IntArray) {
        this.colorPr = colors
        invalidate()
    }

    fun setColorBg(colors: IntArray) {
        this.colorBg = colors

        invalidate()
    }

    fun setProgressCur(progress: Int) {
        this.progress = progress

        invalidate()
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }
}