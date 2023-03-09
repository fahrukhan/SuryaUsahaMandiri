package com.rfid.sum.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.button.MaterialButton
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.data.support.Preference
import com.rfid.sum.data.support.Session
import com.rfid.sum.data.support.TokenMgt
import com.rfid.sum.rest.ParamBuilder
import com.rfid.sum.rest.Volley
import com.rfid.sum.utils.Tos
import com.rfid.sum.utils.Wait

abstract class BaseBinding<B : ViewBinding> : AppCompatActivity() {
    lateinit var bind: B
    lateinit var tos: Tos
    lateinit var pref: Preference
    lateinit var session: Session
    lateinit var volley: Volley
    lateinit var wait: Wait
    lateinit var tokenMgt: TokenMgt
    lateinit var paramBuilder: ParamBuilder
    private var useReader: Boolean = false
    var isUhfReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = getViewBinding()
        useReader = useReader()
        setContentView(bind.root)

        initComponent()
    }

    abstract fun getViewBinding(): B
    abstract fun useReader(): Boolean

    private fun initComponent() {
        if (useReader) {
            connectReader()
        }

        tos = Tos(this)
        volley = Volley(this)
        session = Session(this)
        tokenMgt = TokenMgt(this)
        paramBuilder = ParamBuilder(this)
        wait = Wait(this, bind.root as ViewGroup)
    }

    protected fun connectReader() {
        if (!isUhfReady) {
            val c = UHFReader.connectReader(this)
            isUhfReady = if (!c.success) {
                tos.error(c.message)
                false
            } else {
                UHFReader.setAntennaPower(12)
                true
            }
        }
    }

    protected fun MaterialButton.hideBtn(b: Boolean){
        if (b && this.visibility != View.VISIBLE){
            YoYo.with(Techniques.FadeInRight).duration(200)
                .onStart { this.invisible(false) }
                .playOn(this)
        }
        if (!b && this.visibility == View.VISIBLE){
            YoYo.with(Techniques.FadeOutRight).duration(200)
                .onEnd { this.invisible(true) }
                .playOn(this)
        }
    }

    protected fun MaterialButton.hideBtn2(b: Boolean){
        if (b && this.visibility != View.VISIBLE){
            YoYo.with(Techniques.FadeInRight).duration(200)
                .onStart { this.gone(true) }
                .playOn(this)
        }
        if (!b && this.visibility == View.VISIBLE){
            YoYo.with(Techniques.FadeOutRight).duration(200)
                .onEnd { this.gone(false) }
                .playOn(this)
        }
    }

    protected fun View.gone(b: Boolean) {
        this.visibility = if (b) View.GONE else View.VISIBLE
    }

    protected fun View.invisible(b: Boolean) {
        this.visibility = if (b) View.INVISIBLE else View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        if (isUhfReady){
            isUhfReady = false
            UHFReader.dispose()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isUhfReady){
            isUhfReady = false
            UHFReader.dispose()
        }
    }
}