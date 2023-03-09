package com.rfid.sum.utils

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.rfid.sum.R
import es.dmoral.toasty.Toasty

class Tos(private val context: Context) {

    init {
        Toasty.Config.getInstance()
            .setGravity(Gravity.BOTTOM)
            .setToastTypeface(Typeface.MONOSPACE)
            .setTextSize(12)
            .apply()
    }

    fun success(msg: String, duration: Int = SHORT_TOA, withIcon: Boolean = true){
        Toasty.custom(
            context,
            msg,
            R.drawable.ic_check,
            R.color.green_400,
            duration,
            withIcon,
            true
        ).show()
    }

    fun info(msg: String, duration: Int = SHORT_TOA, isDark: Boolean = false, withIcon: Boolean = true ){
        //Toasty.info(context, msg, duration).show(
        var textColor = ContextCompat.getColor(context, R.color.white)
        var bgColor = ContextCompat.getColor(context, R.color.primaryColor)

        if (isDark) {
            textColor = ContextCompat.getColor(context, R.color.primaryColor)
            bgColor = ContextCompat.getColor(context, R.color.white)
        }

        else ContextCompat.getColor(context, R.color.white)
        Toasty.custom(
            context,
            msg,
            ContextCompat.getDrawable(context, R.drawable.ic_info),
            bgColor, //bgColor
            textColor, //textColor
            duration,
            withIcon,
            true
        ).show()
    }

    fun warning(msg: String, duration: Int = SHORT_TOA, withIcon: Boolean = true){

        val t = Toasty.custom(
            context,
            msg,
            R.drawable.ic_warning,
            R.color.warningColor,
            duration,
            withIcon,
            true
        ).show()

    }

    fun error(msg: String, duration: Int = SHORT_TOA, withIcon: Boolean = true, gravity: Int = Gravity.BOTTOM){
        var dur = duration
        if (msg.length > 80) {
            dur = LONG_TOA
        }

        val t = Toasty.custom(
            context,
            msg,
            R.drawable.ic_dangerous,
            R.color.errorColor,
            dur,
            withIcon,
            true
        )

        t.setGravity(gravity,0,0)
        t.show()
    }

    companion object {
        const val LONG_TOA = Toast.LENGTH_LONG
        const val SHORT_TOA = Toast.LENGTH_SHORT
    }

}