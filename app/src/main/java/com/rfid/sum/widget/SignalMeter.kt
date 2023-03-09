package com.rfid.sum.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.rfid.sum.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@SuppressLint("CustomViewStyleable")
class SignalMeter
@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
): View(context, attrs, defStyleAttr) {

    init {
        init(context.obtainStyledAttributes(attrs, R.styleable.SearchMeter))
    }

    private var defaultSize = 0
    private var arcDistance = 0 // ArcPadding

    private var mMiddleArcPaint: Paint? = null
    private var mWarningMiddleArcPaint: Paint? = null
    private var mOuterArcPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mCurrentTextPaint: Paint? = null
    private var mArcProgressPaint: Paint? = null
    private var radius = 0f
    private var mMiddleRect: RectF? = null
    private var mOuterRect: RectF? = null
    private var mMiddleProgressRect: RectF? = null
    private val mMinNum = 0
    private val mMaxNum = 100 // TODO
    private var mCurrentNum = 0
    private var mCurrentAngle = 0f
    private val mMaxAngle = 280f // TODO
    private var bitmap: Bitmap? = null
    lateinit var pos: FloatArray
    lateinit var tan: FloatArray

    //private val matrix: Matrix? = null
    private var mBitmapPaint: Paint? = null
    private var mNeedle: Paint? = null
    var progress = 0f // TODO private

    private var mShowText = true
    private var textColor = Color.WHITE

    private fun init(attrs: TypedArray) {
        mShowText = attrs.getBoolean(R.styleable.SearchMeter_show_text, mShowText)
        textColor = attrs.getInt(R.styleable.SearchMeter_text_color, textColor)
        defaultSize = dp2px(250)
        arcDistance = dp2px(12)

        mMiddleArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mMiddleArcPaint!!) {
            strokeWidth = 18f
            color = ContextCompat.getColor(context, R.color.primaryColor)
            style = Paint.Style.STROKE
            alpha = 30
        }

        mOuterArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mOuterArcPaint!!) {
            strokeWidth = 3f
            color = ContextCompat.getColor(context, R.color.primaryColor)
            style = Paint.Style.STROKE
            alpha = 230
        }

        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mTextPaint!!) {
            color = Color.parseColor("#99ffffff") // TODO
            textAlign = Paint.Align.CENTER
            textSize = 11f
//            setTypeface(Typeface
//                .create("sans-serif-condensed", Typeface.NORMAL)
//            )
        }

        //middle text
        mCurrentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mCurrentTextPaint!!) {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = 8f
//            typeface = Typeface.create("sans-serif-condensed",
//                Typeface.NORMAL)
        }

        mArcProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mArcProgressPaint!!) {
            strokeWidth = 18f
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            alpha = 150
        }

        //main speed needle
        mNeedle = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mNeedle!!) {
            strokeWidth = 18f
            color = ContextCompat.getColor(context, R.color.primaryColor)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }


        //mNeedle.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));
        mBitmapPaint = Paint()
        with(mBitmapPaint!!){
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_circle) // TODO

        pos = FloatArray(2)
        tan = FloatArray(2)
        matrix to Matrix()
        mWarningMiddleArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        with(mWarningMiddleArcPaint!!){
            strokeWidth = 18f
            // TODO Allow color parameter
            color = Color.parseColor("#eef3797b")
            style = Paint.Style.STROKE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveMeasure(widthMeasureSpec, defaultSize),
        resolveMeasure(heightMeasureSpec, defaultSize))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutParams = ViewGroup.LayoutParams(w, h)
        radius = (width / 2).toFloat()

        mOuterRect = RectF(
            DEFAULT_PADDING.toFloat(), DEFAULT_PADDING.toFloat(),
            (width - DEFAULT_PADDING).toFloat(),
            (height - DEFAULT_PADDING).toFloat())

        mMiddleRect = RectF(
            (DEFAULT_PADDING + arcDistance).toFloat(),
            (DEFAULT_PADDING + arcDistance).toFloat(),
            (width - DEFAULT_PADDING - arcDistance).toFloat(),
            (height - DEFAULT_PADDING - arcDistance).toFloat())

        mMiddleProgressRect = RectF(
            (DEFAULT_PADDING + arcDistance).toFloat(),
            (DEFAULT_PADDING + arcDistance).toFloat(),
            (width - DEFAULT_PADDING - arcDistance).toFloat(),
            (height - DEFAULT_PADDING - arcDistance).toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        with(canvas!!){
            drawMiddleArc(this)
            drawRingProgress(this)
            drawCenterText(this)
        }
    }

    /**
     * @param canvas
     */
    private fun drawRingProgress(canvas: Canvas) {
        val path = Path()
        path.addArc(mMiddleProgressRect!!, START_ANGLE, mCurrentAngle)
        val pathMeasure = PathMeasure(path, false)
        pathMeasure.getPosTan(pathMeasure.length * 1, pos, tan)
        matrix.reset()
        matrix.postTranslate(pos[0] - bitmap!!.width / 2, pos[1] - bitmap!!.height / 2)
        //canvas.drawPath(path, mArcProgressPaint); // TODO
        if (mCurrentAngle == 0f) return
        //canvas.drawBitmap(bitmap!!, matrix, mBitmapPaint)
        mBitmapPaint!!.color = ContextCompat.getColor(context, R.color.primaryColor)
        mBitmapPaint!!.alpha = 200
        canvas.drawCircle(pos[0], pos[1], 8f, mBitmapPaint!!)
    }

    /**
     * @param canvas
     */
    private fun drawCenterText(canvas: Canvas) {
        // TODO measureText for aligning

        //mTextPaint.setTextSize(dp2px(12)); // FIXME
        //mTextPaint.setStyle(Paint.Style.STROKE);
        //canvas.drawText(String.valueOf(mMinNum), dp2px(60), height - dp2px(38), mTextPaint); // FIXME

        //mTextPaint.setTextSize(dp2px(12)); // FIXME
        //canvas.drawText(String.valueOf(mMaxNum), width - dp2px(65), height - dp2px(38), mTextPaint); // FIXME
        if (mShowText) {
            mCurrentTextPaint!!.textSize = dp2px(24).toFloat() // FIXME
            canvas.drawText(mCurrentNum.toString(), radius, (height - arcDistance - 24).toFloat(),
                mCurrentTextPaint!!)
        }
    }

    /**
     * @param canvas
     */
    private fun drawMiddleArc(canvas: Canvas) {
        val r = radius - DEFAULT_PADDING - arcDistance - arcDistance
        val toX =
            width / 2 + Math.cos(Math.toRadians((mCurrentAngle + START_ANGLE).toDouble()))
                .toFloat() * r
        val toY =
            width / 2 + Math.sin(Math.toRadians((mCurrentAngle + START_ANGLE).toDouble()))
                .toFloat() * r
        val centerX = width / 2
        val centerY = height / 2
        val margin = 0
        canvas.drawLine(centerX.toFloat(), (centerY + margin).toFloat(), toX, toY, mNeedle!!)

        //canvas.drawArc(mOuterRect, START_ANGLE, END_ANGLE, false, mOuterArcPaint);
        canvas.drawArc(mMiddleRect!!, START_ANGLE, END_ANGLE, false,
            mMiddleArcPaint!!)
        // TODO Allow warning range parameters
        //canvas.drawArc(mMiddleRect, START_ANGLE + 35, END_ANGLE - 70, false, mMiddleArcPaint);
        //canvas.drawArc(mMiddleRect, START_ANGLE, 35, false, mWarningMiddleArcPaint);
        //canvas.drawArc(mMiddleRect, END_ANGLE + 95, 35, false, mWarningMiddleArcPaint);
    }

    private fun resolveMeasure(measureSpec: Int, defaultSize: Int): Int{
        val result: Int
        val specSize = MeasureSpec.getSize(measureSpec)
        result = when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.UNSPECIFIED -> defaultSize
            MeasureSpec.AT_MOST -> Math.min(specSize, defaultSize)
            MeasureSpec.EXACTLY -> specSize
            else -> defaultSize
        }
        return result
    }

    //@JvmName("setProgress1")
    internal fun setProgress(progress: Float) {
        this.progress = progress
        mCurrentNum = (progress * mMaxNum).toInt()
        mCurrentAngle = mMaxAngle * progress
        postInvalidate()
    }

    /**
     * dp2px
     *
     * @param values
     * @return
     */
    private fun dp2px(values: Int): Int {
        val density = resources.displayMetrics.density
        return (values * density + 0.5f).toInt()
    }

    companion object {
        private const val DEFAULT_PADDING = 80
        private const val START_ANGLE = 130f // TODO auto-adjust
        private const val END_ANGLE = 280f // TODO
    }

}