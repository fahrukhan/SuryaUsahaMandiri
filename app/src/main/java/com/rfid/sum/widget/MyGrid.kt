package com.rfid.sum.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

class MyGrid @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : GridView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val expandSpec = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2,
            MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, expandSpec)
    }
}