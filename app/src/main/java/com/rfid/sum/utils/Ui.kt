package com.rfid.sum.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rfid.sum.data.constant.Key
import java.lang.Exception

object Ui {

    fun invisible(v: View){
        v.visibility = View.INVISIBLE
    }

    fun gone(view: View){
        view.visibility = View.GONE
    }

    fun gone(views: Array<View>){
        views.forEach {
            gone(it)
        }
    }

    fun show(view: View){
        view.visibility = View.VISIBLE
    }

    fun show(views: Array<View>){
        views.forEach {
            show(it)
        }
    }

    inline fun <reified T> intent(activity: Activity, data: String? = null ){
        val i = Intent(activity, T::class.java)
        if (data != null) i.putExtra(Key.INTENT_DATA, data)
        activity.startActivity(i)
    }

    fun disableEvent(activity: Activity){
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun enableEvent(activity: Activity){
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    class ColorTransparentUtils{
        // This default color int
        private val defaultColorID = android.R.color.black
        private val defaultColor = "000000"

        /**
         * This method convert numver into hexa number or we can say transparent code
         *
         * @param trans number of transparency you want
         * @return it return hex decimal number or transparency code
         */
        private fun convert(trans: Int): String {
            val hexString = Integer.toHexString(Math.round((255 * trans / 100).toFloat()))
            return (if (hexString.length < 2) "0" else "") + hexString
        }

        fun transparentColor(colorCode: Int, trans: Int): String? {
            return convertIntoColor(colorCode, trans)
        }

        /**
         * Convert color code into transparent color code
         *
         * @param colorCode color code
         * @param transCode transparent number
         * @return transparent color code
         */
        private fun convertIntoColor(colorCode: Int, transCode: Int): String? {
            // convert color code into hexa string and remove starting 2 digit
            var color = defaultColor
            try {
                color = Integer.toHexString(colorCode).uppercase().substring(2)
            } catch (ignored: Exception) {
            }
            return if (color.isNotEmpty() && transCode < 100) {
                if (color.trim { it <= ' ' }.length == 6) {
                    "#" + convert(transCode) + color
                } else {
                    //Log.d(TAG, "Color is already with transparency")
                    convert(transCode) + color
                }
            } else "#" + Integer.toHexString(defaultColorID).uppercase().substring(2)
            // if color is empty or any other problem occur then we return deafult color;
        }
    }

    fun hideFabByRv(rv: RecyclerView, fab: FloatingActionButton){
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if(dy > 0){
                    fab.hide()
                } else{
                    fab.show()
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    fun hideViewByRv(rv: RecyclerView, view: View, root: ViewGroup){
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val transition = Slide(Gravity.BOTTOM)
                transition.duration = 400
                transition.addTarget(view)

                TransitionManager.beginDelayedTransition(root, transition)
                if(dy > 0){
                    gone(view)
//                    show = false
                } else{
                    show(view)
                    //show = true
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    fun hideFabByRv(rv: ListView, fab: FloatingActionButton){
        rv.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                val btnPosY = fab.scrollY
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    fab.animate().cancel();
                    fab.animate().translationYBy(150F);
                } else {
                    fab.animate().cancel();
                    fab.animate().translationY(btnPosY.toFloat());
                }
            }

            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }
        })
    }


    /* fun showKeyboard(context: Context) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    } */

    fun closeKeyboard(activity: Activity) {
        activity.currentFocus.let { view ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    fun checkFields(tis: List<TextInputEditText>): Int {
        var errorCount = 0
        tis.forEach { tied ->
            if (tied.text?.isEmpty()!!) {
                errorCount++
                val til = tied.parent.parent as TextInputLayout

                til.error = "*Can't be empty!"
                tied.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        til.isErrorEnabled = false
                    }

                    override fun afterTextChanged(p0: Editable?) {
                    }
                })
            }
        }
        return errorCount
    }



}