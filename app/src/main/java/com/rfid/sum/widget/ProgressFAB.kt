package com.rfid.sum.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rfid.sum.R


class ProgressFAB : FloatingActionButton {

    private var margin = 0.0f
    private var arcProportion = 0.5f
    private var animationDuration = 2000
    private var startAngel = 180f
    private var progressBarColor = Color.WHITE
    private var progressBarMargin = 5f
    private var progressBarWidth = 5f
    private var isDrawProgress = false
    private var showEditModePreview = false
    private lateinit var mainPaint: Paint
    private lateinit var rectangle: RectF
    private lateinit var lastImageDrawable: Drawable


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(attrs)

    }


    private fun init(attrs: AttributeSet?) {
        attrs?.let {
            val attr =
                context.obtainStyledAttributes(attrs, R.styleable.ProgressFAB)
            progressBarColor =
                attr.getColor(
                    R.styleable.ProgressFAB_pfab_color,
                    progressBarColor
                )
            startAngel = attr.getFloat(
                R.styleable.ProgressFAB_pfab_startAngel,
                startAngel
            )
            animationDuration = attr.getInt(
                R.styleable.ProgressFAB_pfab_animationDuration,
                animationDuration
            )
            progressBarMargin =
                attr.getDimension(
                    R.styleable.ProgressFAB_pfab_margin,
                    progressBarMargin
                )
            progressBarWidth =
                attr.getDimension(
                    R.styleable.ProgressFAB_pfab_width,
                    progressBarWidth
                )
            arcProportion = attr.getFloat(
                R.styleable.ProgressFAB_pfab_arcProportion,
                arcProportion
            )
            showEditModePreview = attr.getBoolean(
                R.styleable.ProgressFAB_pfab_showEditModePreview,
                showEditModePreview
            )
            attr.recycle()
        }

        lastImageDrawable = drawable
        mainPaint = Paint()
        mainPaint.isAntiAlias = true
        mainPaint.color = progressBarColor
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = dp2px(resources, progressBarWidth).toFloat()
        margin = dp2px(resources, progressBarMargin).toFloat()
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        rectangle = RectF(
            0f + margin,
            0f + margin,
            measuredWidth.toFloat() - margin,
            measuredHeight.toFloat() - margin
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when {
            (isInEditMode && showEditModePreview) ->
                drawProgressBar(canvas)
            (!isInEditMode && isDrawProgress) ->
                drawProgressBar(canvas)
        }

    }

    private fun drawProgressBar(canvas: Canvas) {
        canvas.drawArc(rectangle, startAngel, arcProportion * 360, false, mainPaint)
    }

    fun showLoadingAnimation(isStartAnimation: Boolean) {
        if (isStartAnimation) {
            setImageResource(R.drawable.ic_read)
            isDrawProgress = true
            ValueAnimator.ofFloat(startAngel, 360f + startAngel).apply {
                repeatCount = ValueAnimator.INFINITE
                interpolator = null
                duration = animationDuration.toLong()
                addUpdateListener { animator ->
                    this@ProgressFAB.startAngel = animator.animatedValue as Float
                    this@ProgressFAB.invalidate()
                }
                start()
            }
            return
        }
        setImageDrawable(lastImageDrawable)
        isDrawProgress = false

    }

    fun isShowingLoadingAnimation() = isDrawProgress


    private fun dp2px(resource: Resources, dp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resource.displayMetrics).toInt()

}