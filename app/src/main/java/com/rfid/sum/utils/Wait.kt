package com.rfid.sum.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rfid.sum.R

class Wait(val context: Context, private val layout: ViewGroup) {

    //private val progressBar = ProgressBar(context, attr, R.attr.progressBarStyleLarge)
    @SuppressLint("InflateParams")
    private var view: View =
        (context as AppCompatActivity).layoutInflater.inflate(R.layout.wait, null)
    private var runMsg: TextView = view.findViewById(R.id.run_message)
    private val param = RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT
    )

    fun show(msg: String = "Please Wait...") {
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }

        runMsg.text = msg
        param.addRule(RelativeLayout.CENTER_IN_PARENT)
        view.elevation = 10F
        layout.addView(view, param)

        Ui.disableEvent((context as Activity))
        Ui.show(view)
    }

    fun updateMsg(msg: String) {
        runMsg.text = msg
    }

    fun hide() {
        Ui.enableEvent((context as Activity))
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }
        Ui.gone(view)
    }

}