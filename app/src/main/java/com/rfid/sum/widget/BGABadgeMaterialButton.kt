package com.rfid.sum.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import cn.bingoogolapple.badgeview.BGABadgeViewHelper
import cn.bingoogolapple.badgeview.BGABadgeable
import cn.bingoogolapple.badgeview.BGADragDismissDelegate
import com.google.android.material.button.MaterialButton

class BGABadgeMaterialButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : MaterialButton(context, attrs), BGABadgeable {

    private val mBadgeViewHelper: BGABadgeViewHelper =
        BGABadgeViewHelper(this, context, attrs, BGABadgeViewHelper.BadgeGravity.RightTop)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mBadgeViewHelper.onTouchEvent(event)
    }

    override fun callSuperOnTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mBadgeViewHelper.drawBadge(canvas)
    }

    override fun showCirclePointBadge() {
        mBadgeViewHelper.showCirclePointBadge()
    }

    override fun showTextBadge(badgeText: String) {
        mBadgeViewHelper.showTextBadge(badgeText)
    }

    override fun hiddenBadge() {
        mBadgeViewHelper.hiddenBadge()
    }

    override fun showDrawableBadge(bitmap: Bitmap) {
        mBadgeViewHelper.showDrawable(bitmap)
    }

    override fun setDragDismissDelegate(delegate: BGADragDismissDelegate) {
        mBadgeViewHelper.setDragDismissDelegate(delegate)
    }

    override fun isShowBadge(): Boolean {
        return mBadgeViewHelper.isShowBadge
    }

    override fun isDraggable(): Boolean {
        return mBadgeViewHelper.isDraggable
    }

    override fun isDragging(): Boolean {
        return mBadgeViewHelper.isDragging
    }

    override fun getBadgeViewHelper(): BGABadgeViewHelper {
        return mBadgeViewHelper
    }
}