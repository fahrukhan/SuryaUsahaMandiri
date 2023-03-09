package com.rfid.sum.activities.ui.tag_update

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.animation.Animation
import androidx.core.os.postDelayed
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.medisafe.controller.IRScanner
import com.rfid.medisafe.controller.IRScanning
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.databinding.ActivityTagUpdateBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import com.rfid.sum.utils.Tos
import org.json.JSONObject
import java.net.URL
import kotlin.time.Duration

class TagUpdateActivity : BaseBinding<ActivityTagUpdateBinding>(), IRScanning, UHFScanning, NciApiListener {
    override fun getViewBinding() = ActivityTagUpdateBinding.inflate(layoutInflater)
    override fun useReader() = true

    private val scanner = IRScanner
    private var isScannerReady = false
    lateinit var pop: Pop
    private val singleScanList = mutableListOf<String>()
    private var batchFromQr: BatchModel? = null

    private val handler = object: Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0 -> {
                    val data = msg.obj as String
                    var batch = ""
                    data.split("|").let {
                        if (it.size == 7) {
                            batchFromQr = Format.qrToBatch(data)
                            batch = it[0]
                            wait.updateMsg("Load Batch Info")
                            batch.trim().let {
                                val url = "${Url.ITEMS}/batchno/$batch"
                                volley.getData(url, this@TagUpdateActivity, session.getUser()?.token, 0)
                            }
                        }else {
                            wait.hide()
                            tos.info("Format QR tidak valid")
                            return
                        }
                    }
                }
                1 -> {
//                    singleScanList.clear()
//                    //val code = msg.obj as String
//
//                    val cp = bind.tvCpNumber.text.toString()
//                    val bCp = Format.getCPNumberFromBatch(batch.trim())
//                    if(cp != bCp){
//                        tos.error("Batch tidak sesuai dengan CP Number!", gravity = Gravity.CENTER_VERTICAL)
//                        return
//                    }
//
//                    if(cpBatchListScanned.any{ it.batchNo == batch.trim()}){
//                        tos.error("Sudah ada! ${batch.trim()}")
//                        return
//                    }
//                    UHFReader.startSingleScan()
//                    wait.show("Dekatkan RFID Tag!")
//                    Handler(Looper.myLooper()!!).postDelayed({scannedRFIDCheck(batch.trim())}, 1000)
                }
                2 -> {
//                    val code = msg.obj as String

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUpd()
    }

    private fun initUpd() {
        pop = Pop(this)
        UHFReader.listener = this
        UHFReader.setAntennaPower(12)

        if (scanner.init(applicationContext)) {
            isScannerReady = true
            scanner.listen = this
        }

        YoYo.with(Techniques.FadeInDown).duration(1500).repeat(Animation.INFINITE)
            .playOn(bind.ivArrow)

        with(bind){

        }
    }

    private fun scannedRFIDCheck() {
        if (batchFromQr == null) return

        if (singleScanList.isEmpty()){
            tos.error("Tag RFID tidak terdeteksi, coba lagi.", gravity = Gravity.CENTER_VERTICAL)
            batchFromQr?.tid = null
            bind.tvNewRfid.text = getString(R.string.minus)
        }else{
            if (singleScanList.size > 1){
                tos.error("Lebih dari 1 Tag RFID terdeteksi, Harap memberikan jarak dengan Tag RFID lainya!", Tos.LONG_TOA, gravity = Gravity.CENTER_VERTICAL)
                batchFromQr?.tid = null
                bind.tvNewRfid.text = getString(R.string.minus)
            }else{
                val rfid = singleScanList.first()
                batchFromQr?.tid = rfid
                bind.tvNewRfid.text = rfid
            }
        }
    }

    private fun doIRScan() {
        if (!isScannerReady) {
            tos.error("Scanner not ready")
            return
        }
        scanner.trigger()
    }

    private fun showWarning(msg: String, isDanger: Boolean = false){
        with(bind){
            val color = ColorStateList.valueOf(if (isDanger) Color.RED else getColor(R.color.primaryColor))
            tvWarnMsg.setTextColor(color)
            ivWarnIc.imageTintList = color

            tvWarnMsg.text = msg
            llWarn.gone(false)
        }
    }

    private fun hideWarning(){
        bind.llWarn.gone(true)
    }

    private fun loadBatchDat(old: BatchModel?) {
        if (batchFromQr == null){
            tos.error("Error data QR, mohon lakukan scan ulang!")
            return
        }
        val new = batchFromQr!!

        if (old == null){
            tos.error("Error data server, coba scan ulang!")
            return
        }
        if (old.prodName?.replace(" ", "") != new.prodName){
            tos.error("Error saat mencocokan data label dengan data server!")
            return
        }

        val isResultWithNewDifferent = (old.batchInQty != new.batchInQtyReg || old.uomCode != new.uomCodeReg)
        val isOldAndNewDifferent = (old.batchInQtyReg != new.batchInQtyReg || old.uomCodeReg != new.uomCodeReg)

        with(bind){

            if (isResultWithNewDifferent){
                showWarning("Label baru berbeda dengan data result!", true)
                llNewData.gone(false)
                llWarn.gone(false)
            }else if(!isOldAndNewDifferent){
                showWarning("Tidak ada perbedaan data pada label baru.")
                llNewData.gone(true)
                llWarn.gone(false)
            }else if(isOldAndNewDifferent){
                //showWarning("Pastikan data pada tampilan sesuai dengan label.")
                llNewData.gone(false)
                llWarn.gone(true)
            }else{
                llNewData.gone(true)
                llWarn.gone(true)
            }

            tvRfid.text = old.tid
            tvQty.text = old.batchInQtyReg
            tvUom.text = old.uomCodeReg
            "LOT ${old.lot}".also { tvLot.text = it }
            tvCustColor.text = old.cusColor
            tvBatchNo.text = old.batchNo
            tvProdCode.text = old.prodCode
            tvProdName.text = old.prodName
            old.cusColor.also { tvCustColor.text = it }


            tvNewQty.text = new.batchInQtyReg
            tvNewUom.text = new.uomCodeReg
            "LOT ${old.lot}".also { tvNewLot.text = it }
            tvNewCustColor.text = new.cusColor
            tvNewBatchNo.text = new.batchNo
            tvNewProdCode.text = new.prodCode
            tvNewProdName.text = old.prodName
            new.cusColor.also { tvNewCustColor.text = it }

            btnResultVp.setOnClickListener {
                pop.cpDetailsPop2(old.batchNo!!, root, old)
            }
            btnSave.setOnClickListener {
                if (new.tid == null){
                    tos.error("RFID tidak terdeteksi, coba lakukan scan ulang!")
                    return@setOnClickListener
                }

                fun saveData(){
                    wait.show("Update Batch...")
                    val param = mutableMapOf<String, String>()
                    param["batchinqty_registration"] = new.batchInQtyReg!!
                    param["uomcode_registration"] = new.uomCodeReg!!
                    param["tid"] = new.tid!!

                    val gson = Gson().toJson(param)
                    volley.putData(Url.PUT_BATCH, new.batchNo!!, JSONObject(gson), this@TagUpdateActivity,
                        session.getUser()?.token, 1
                    )
                }

                if (isResultWithNewDifferent){
                    val alert = AlertDialog.Builder(this@TagUpdateActivity)
                    alert.setTitle("PERINGATAN")
                        .setMessage("Label baru memiliki data yang berbeda dengan result verpacking, Tetap simpan?")
                        .setPositiveButton("UPDATE"){ d, _ ->
                            saveData()
                            d.dismiss()
                        }
                        .setNegativeButton("CANCEL"){ d, _ ->
                            d.dismiss()
                            return@setNegativeButton
                        }
                        .show()

                }else{
                    saveData()
                }
            }
            singleScanList.clear()
            UHFReader.startSingleScan()
            Handler(Looper.myLooper()!!).postDelayed({scannedRFIDCheck()}, 100)
        }
    }

    // <editor-fold defaultstate="collapsed" desc="IR Scanning">
    override fun outPutScanner(id: String?) {
        val msg = handler.obtainMessage(0)
        msg.obj = id
        handler.sendMessage(msg)
    }

    override fun stopScan() {
        /* no-op */
    }

    override fun scanLoad() {
        wait.show()
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UHF Scanning">
    override fun outPutEpc(epc: EPCModel) {
        if (!singleScanList.contains(epc._TID)){
            Logger.info(epc._TID)
            singleScanList.add(epc._TID)
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="NCI Api">
    override fun onResponse(response: Response, errorMsg: String, key: Int?) {
        with(response){
            wait.hide()
            if (success){
                when(key){
                    0 -> {
                        val model = castModel<BatchModel>()
                        loadBatchDat(model)
                    }
                    1 -> {
                        tos.success("UPDATE SUCCESS")
                        Handler(Looper.myLooper()!!).postDelayed(
                            { finish() },500
                        )
                    }
                    else -> {
                        Logger.info(data.toString())
                    }
                }
            }else{
                tos.error(errorMsg)
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Physic Button">
    private var isPressed = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (UHFReader.validButton(keyCode)) {
            if (!isPressed) {
                isPressed = true
                doIRScan()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (UHFReader.validButton(keyCode)) {
            isPressed = false
        }
        return super.onKeyUp(keyCode, event)
    }
    // </editor-fold>
}