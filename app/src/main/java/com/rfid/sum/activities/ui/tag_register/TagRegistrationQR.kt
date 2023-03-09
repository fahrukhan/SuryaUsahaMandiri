package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.marginLeft
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.medisafe.controller.IRScanner
import com.rfid.medisafe.controller.IRScanning
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.databinding.ActivityTagRegistrationQrBinding
import com.rfid.sum.databinding.BeanCpBatchRvBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Tos
import com.rfid.sum.utils.Ui
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject

class TagRegistrationQR : BaseBinding<ActivityTagRegistrationQrBinding>(), IRScanning, UHFScanning,
    Slider.OnSliderTouchListener {
    override fun getViewBinding()= ActivityTagRegistrationQrBinding.inflate(layoutInflater)
    override fun useReader() = true

    private val scanner = IRScanner
    private var isScannerReady = false
    private val singleScanList = mutableListOf<String>()
    private val cpBatchList = mutableListOf<BatchModel>()
    private val cpBatchListScanned = mutableListOf<BatchModel>()
    private val cpAdapter = Adapter()
    private val modalBottomSheet = ModalBottomSheet()
    private var regResult: TagRegisterAdvanced.RegResultModel? = null
    private var showResult = false

    private val handler = object: Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0 -> {
                    singleScanList.clear()
                    val code = msg.obj as String
                    //bind.tiBatchNumb.setText(code)
                    bind.tvLastQR.text = code
                    if(cpBatchListScanned.any{ it.batchNo == code.trim()}){
                        tos.error("Sudah ada! ${code.trim()}")
                        return
                    }
                    UHFReader.startSingleScan()
                    wait.show("Dekatkan RFID Tag!")
                    Handler(Looper.myLooper()!!).postDelayed({scannedRFIDCheck(code.trim())}, 1000)
                }
            }
        }
    }

    private fun scannedRFIDCheck(code: String) {
        wait.hide()

        if (singleScanList.isEmpty()){
            tos.info("Tag RFID tidak terdeteksi, coba lagi.")
        }else{
            if (singleScanList.size > 1){
                tos.error("Lebih dari 1 Tag RFID terdeteksi, Harap memberikan jarak dengan Tag RFID lainya!", Tos.LONG_TOA)
            }else{
                val rfid = singleScanList.first()
//                println(cpBatchList)
//                println(code)
//                val a = cpBatchList.find { it.batchNo!! == code.trim() }
//                println(a)
                addRfidToList(BatchModel( batchNo = code.trim(), tid = rfid.trim()))
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addRfidToList(batch: BatchModel) {


        cpBatchListScanned.find { it.tid == batch.tid }.let {
            if (it != null){
                tos.error("RFID Tag digunakan ${it.batchNo}")
                return
            }
        }

        cpBatchList.filter { it.batchNo == batch.batchNo }.let { b ->
            Logger.info("B ==> $b")
            if (b.isNotEmpty()){
                if (cpBatchListScanned.any { it.batchNo == b[0].batchNo }){
                    tos.error("batch sudah ada")
                    return
                }
                b[0].tid = batch.tid
                cpBatchListScanned.add(b[0])
                cpAdapter.notifyDataSetChanged()
                updateCpScannedTotal()
            }else{
                tos.error("CP Number tidak cocok")
            }
        }
    }

    private fun updateCpScannedTotal(){ bind.tvTotalCpScanned.text = cpBatchListScanned.size.toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initReg()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initReg() {
        UHFReader.listener = this
        UHFReader.setAntennaPower(12)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        if (scanner.init(applicationContext)) {
            isScannerReady = true
            scanner.listen = this
        }

        with(bind){
            rvBatches.layoutManager = LinearLayoutManager(this@TagRegistrationQR)
            rvBatches.adapter = cpAdapter

            btnSetting.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }

            saveBtn.setOnClickListener {
                val list = cpBatchListScanned.filter { !it.tid.isNullOrEmpty() }
                val dataList = mutableListOf<TagRegisterAdvanced.PostRegModel>()

                list.forEach {
                    dataList.add(TagRegisterAdvanced.PostRegModel(it.tid!!, it.batchNo!!))
                }
                val params = mapOf("data" to dataList)
                val p = Gson().toJson(params)

                postReg(p)
            }
            clearBtn.setOnClickListener {
                val alert = AlertDialog.Builder(this@TagRegistrationQR)
                alert.setTitle("Reset Data").setMessage("Semua data akan dibersihkan, Yakin?")
                    .setPositiveButton("OK"){ d, _ ->
                        cpBatchListScanned.clear()
                        cpAdapter.notifyDataSetChanged()
                        updateCpScannedTotal()
                        d.dismiss()
                    }
                    .setNegativeButton("CANCEL"){ d, _ -> d.dismiss()}
                    .show()
            }
            btnScanQr.setOnClickListener {
                doIRScan()
            }
            scanBtn.setOnClickListener { doIRScan() }
        }

        if (intent.hasExtra(Key.INTENT_DATA)) {
            val data = intent.getStringExtra(Key.INTENT_DATA)
            val list = Gson().fromJson(data, List::class.java)
            if (list is List<*>) {
                list.forEachIndexed { i, it ->
                    val gson = Gson().toJson(it)
                    try {
                        val m = Gson().fromJson(gson, BatchModel::class.java)
                        if (i == 0){
                            val bn = m.batchNo?.split(".")
                            if (bn is List<*>){
                                val cp = if (bn.size > 3) "${bn[0]}.${bn[1]}.${bn[2]}"
                                else getString(R.string.minus)
                                bind.tvCpNumber.text = cp
                            }
                        }
                        cpBatchList.add(m)
                    } catch (e: Exception) {
                        Logger.error(e.message!!)
                    }
                }
                if(list.isNotEmpty()){
                    bind.tvTotalCp.text = "${list.size}"
                }
            }
        } else {
            Toast.makeText(this, "Error load data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postReg(p: String?) {
        wait.show()
        volley.nciPostData(Url.ITEMS, JSONObject(p!!), object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: com.rfid.sum.rest.model.Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        castModel<TagRegisterAdvanced.RegResultModel>().let {
                            if (it != null) {
                                regResult = it
                                showResult = true
                                cpAdapter.notifyDataSetChanged()
                                //setRegResult(it)
                            }
                        }

                        tos.success("Selesai")
//                        val list = cpBatchList.filter { it.rfid.isNullOrEmpty() }
//                        cpBatchList.clear()
//                        cpBatchList.addAll(list)

                    } else tos.error(errorMsg)
                    wait.hide()
                }
                println(response)
            }
        }, session.getUser()?.token)
    }

    private fun doIRScan() {
        if (!isScannerReady) {
            tos.error("Scanner not ready")
            return
        }

        scanner.trigger()
    }

    // <editor-fold defaultstate="collapsed" desc="IR Interface">
    override fun stopScan() {
        wait.hide()
    }

    override fun scanLoad() {
        wait.show()
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        if (!singleScanList.contains(epc._TID)){
            Logger.info(epc._TID)
            singleScanList.add(epc._TID)
        }
    }
    // </editor-fold>

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

    override fun outPutScanner(id: String?) {
        wait.hide()
        val msg = handler.obtainMessage(0)
        msg.obj = id
        handler.sendMessage(msg)
    }

    // <editor-fold defaultstate="collapsed" desc="RV Adapter">
    inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val b: BeanCpBatchRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                BeanCpBatchRvBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val gcp = cpBatchListScanned[position]

            with(holder.b) {
                finishLine.gone(position != (itemCount - 1))
                batchNo.text = gcp.batchNo
                prodName.text = gcp.prodName
                rfid.text = gcp.tid ?: getString(R.string.minus)
                rfid.setTextColor(ColorStateList.valueOf(getColor(R.color.secondaryTextLight)))

                Ui.gone(scanBtn)
                Ui.gone(prodName)
                Ui.gone(imgRfidIc)

//                if (scanMode == ScanMode.CONTINUOUS) {
//                    if ((scannedList.size - 1) >= position) {
//                        scanBtn.hideProgress(getString(R.string.btn_scan))
//                    } else {
//                        scanBtn.showProgress {
//                            buttonText = "scanning"
//                            progressColor = Color.WHITE
//                        }
//                    }
//                } else {
//                    scanBtn.hideProgress(getString(R.string.btn_scan))
//                }

//                if (gcp.tid.isNullOrEmpty()) {
//                    Ui.show(scanBtn)
//                   //scanBtn.isEnabled = !isAuto
//                    Ui.gone(clearBtn)
//                } else {
//                    Ui.show(clearBtn)
//                    Ui.gone(scanBtn)
//                }

//                if (savingStatusMsg.containsKey(gcp.tmbBarcodeId)){
//                    savingStatusMsg[gcp.tmbBarcodeId].let {
//                        if (it == "Success"){
//                            imgRes.imageTintList = ColorStateList.valueOf(getColor(R.color.red_300))
//                            msgRes.setTextColor(getColor(R.color.red_300))
//                        }else{
//                            imgRes.imageTintList = ColorStateList.valueOf(getColor(R.color.red_300))
//                            msgRes.setTextColor(getColor(R.color.secondaryTextLight))
//                        }
//                        msgRes.text = it
//                    }
//                    Ui.show(msgLine)
//                }else{
//                    Ui.gone(msgLine)
//                }

//                scanBtn.setOnClickListener {
//                    beanIndexClick = holder.adapterPosition
//                    doScan(ScanMode.SINGLE_BEAN)
//                }
//                clearBtn.setOnClickListener {
//                    if (UHFReader.isScanning) {
//                        tos.error("Scanning in progress!")
//                        return@setOnClickListener
//                    }
//
//
//                    //if (scannedList.contains(gcp.tid)) scannedList.remove(gcp.tid)
//                    gcp.tid = null
//                    notifyItemChanged(holder.adapterPosition)
//
//                }

                removeBtn.setOnClickListener {
                    cpBatchListScanned.remove(gcp)
                    notifyItemRemoved(position)
                    updateCpScannedTotal()
                }

                //show reg result
                if (showResult && rfid.text.toString() != getString(R.string.minus)) {
                    regResult?.success?.apply {
                        icAfterRfid.apply {
                            icAfterRfid.gone(!contains(gcp.tid))
                            if (contains(gcp.tid)) {
                                chipMsg.gone(true)
                            } else {
                                chipMsg.text = getString(R.string.already_registered_warning)
                                chipMsg.gone(false)
                            }
                        }
                    }
                } else {
                    icAfterRfid.gone(true)
                    chipMsg.gone(true)
                }
            }
        }

        override fun getItemCount() = cpBatchListScanned.size
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Slider">
    override fun onStartTrackingTouch(slider: Slider) {
        /* no-op */
    }

    override fun onStopTrackingTouch(slider: Slider) {
        wait.show()
        val power = slider.value
        modalBottomSheet.currentPower = power
        UHFReader.setAntennaPower(power.toInt())
        Handler(Looper.myLooper()!!).postDelayed({ wait.hide() },500)
    }
    // </editor-fold>

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}