package com.rfid.sum.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.widget.TextView
import com.rfid.sum.data.model.BatchModel
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

object Format {

    fun calendarString(c: Calendar, pattern: String): String {
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(c.time)
    }

    fun setHighLightedText(tv: TextView, textToHighlight: String, color: Int = -0x100) {
        val tvt: String = tv.text.toString()
        var ofe = tvt.indexOf(textToHighlight, 0)
        val wordToSpan: Spannable = SpannableString(tv.text)
        var ofs = 0
        while (ofs < tvt.length && ofe != -1) {
            ofe = tvt.indexOf(textToHighlight, ofs)
            if (ofe == -1) break else {
                // set color here
                wordToSpan.setSpan(
                    BackgroundColorSpan(color),
                    ofe,
                    ofe + textToHighlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE)
            }
            ofs = ofe + 1
        }
    }

    fun attributeSingleLine(attrs: Array<String?>): String {
        val sbAttrs = StringBuilder()
        attrs.forEachIndexed { i, s ->
            if (i > 0){ if (s != null){ sbAttrs.append(", ").append(s) } }
            else{ if (s != null){ sbAttrs.append(s) } }
        }
        return sbAttrs.toString()
    }

    fun getCPNumberFromBatch(batch: String): String? {
        var arg: String? = null
        if (!batch.contains(".")) return null

        val arr = batch.split(".")
        if (arr.size == 4){
            arg = "${arr[0]}.${arr[1]}.${arr[2]}"
        }

        return arg
    }

    fun qrToBatch(qr: String): BatchModel? {
        //D22L0692.01.0101.005|B11A40487D0033|1616NEW40033DOUBLING|0033|1616NEW|30.0000|M
        val data = qr.trim().split("|")
        if (data.size != 7) return null

        return BatchModel(
            batchNo = data[0],
            prodCode = data[1],
            prodName = data[2],
            cusColor = data[3],
            labeljual = data[4],
            batchInQtyReg = data[5],
            uomCodeReg = data[6]
        )
    }

    private const val ATTRIBUTE_TYPE_DATE = "date"
    private const val ATTRIBUTE_TYPE_NUMBER = "number"
    const val ATTRIBUTE_TYPE_LIST = "list"
}