package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.ScanMode
import com.rfid.sum.data.constant.ScanMode.*
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.databinding.ActivityTagRegisterAdvancedBinding
import com.rfid.sum.databinding.BeanCpBatchRvBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import com.rfid.sum.utils.Ui
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject


class TagRegisterAdvanced : BaseBinding<ActivityTagRegisterAdvancedBinding>(), UHFScanning,
    Slider.OnSliderTouchListener {
    override fun getViewBinding() = ActivityTagRegisterAdvancedBinding.inflate(layoutInflater)
    override fun useReader() = true
    private val cpBatchList = mutableListOf<BatchModel>()
    private val cpAdapter = Adapter()
    private var isAuto = false
    private var scanMode = SINGLE
    private val scannedList = mutableListOf<String>()
    private var beanIndexClick = 0
    private var isSingleBeanOpen = true
    private lateinit var pop: Pop
    private val modalBottomSheet = ModalBottomSheet()
    private var regResult: RegResultModel? = null
    private var showResult = false

    private val handler = object : Handler(Looper.myLooper()!!) {
        @SuppressLint("NotifyDataSetChanged")
        override fun handleMessage(msg: Message) {
            Logger.info("Mode: $scanMode")
            when (msg.what) {
                0 -> {
                    val tid = msg.obj as String
                    when (scanMode) {
                        SINGLE -> {
                            Logger.info(tid)
                            val list = cpBatchList.filter { it.tid.isNullOrEmpty() }
                            if (list.isNotEmpty()) {
                                if (cpBatchList.any { it.tid == tid }) {
                                    tos.error("RFID: $tid already taken.")
                                } else {
                                    cpBatchList.find { it.batchNo == list[0].batchNo }?.tid = tid
                                }
                            }
                            cpAdapter.notifyDataSetChanged()
                            Handler(Looper.myLooper()!!).postDelayed({ isPressed = false }, 500)
                        }
                        SINGLE_BEAN -> {
                            if (isSingleBeanOpen) {
                                isSingleBeanOpen = false
                                if (cpBatchList.find { it.tid == tid } != null) {
                                    tos.info("RFID already taken.")
                                    return
                                }
                                cpBatchList[beanIndexClick].tid = tid
                                cpAdapter.notifyDataSetChanged()
                            }
                            Handler(Looper.myLooper()!!).postDelayed(
                                { isSingleBeanOpen = true },
                                500
                            )
                        }
                        CONTINUOUS -> {
                            Logger.info("Continuous Scan Started")
                            if (!scannedList.contains(tid)) {
                                scannedList.add(tid)
                                val list = cpBatchList.filter { it.tid.isNullOrEmpty() }
                                if (list.isNotEmpty()) {
                                    if (cpBatchList.any { it.tid == tid }) {
                                        tos.error("RFID: $tid already taken.")
                                    } else {
                                        cpBatchList.find { it.batchNo == list[0].batchNo }?.tid =
                                            tid
                                    }
                                }
                                cpAdapter.notifyDataSetChanged()
                            }

                            if (cpBatchList.none { it.tid.isNullOrEmpty() }) {
                                UHFReader.stop()
                                bind.scanBtn.text = getString(R.string.btn_scan)
                                scanMode = SINGLE
                                cpAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initAdv()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initAdv() {
        UHFReader.listener = this
        pop = Pop(this)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        if (intent.hasExtra(Key.INTENT_DATA)) {
            val data = intent.getStringExtra(Key.INTENT_DATA)
            val list = Gson().fromJson(data, List::class.java)
            if (list is List<*>) {
                list.forEach {
                    val gson = Gson().toJson(it)
                    try {
                        val m = Gson().fromJson(gson, BatchModel::class.java)
                        cpBatchList.add(m)
                    } catch (e: Exception) {
                        Logger.error(e.message!!)
                    }
                }
            }
            cpAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(this, "Error load data", Toast.LENGTH_SHORT).show()
        }

        with(bind) {
            batchRv.apply {
                layoutManager = LinearLayoutManager(this@TagRegisterAdvanced)
                adapter = cpAdapter
            }
            autoSw.setOnCheckedChangeListener { _, isChecked ->
                isAuto = isChecked
                cpAdapter.notifyDataSetChanged()
            }
            scanBtn.setOnClickListener {
                if (isAuto) doScan(CONTINUOUS)
                else tos.info("Auto Scan disabled.")
            }
            clearBtn.setOnClickListener {
                fun reset() {
                    cpBatchList.forEach {
                        it.tid = null
                    }
                    showResult = false
                    regResult = null
                    cpAdapter.notifyDataSetChanged()
                }
                pop.confirmDialog("RESET", "Delete all rfid detected?", ::reset)
            }
            saveBtn.setOnClickListener {
                val list = cpBatchList.filter { !it.tid.isNullOrEmpty() }
                val dataList = mutableListOf<PostRegModel>()

                list.forEach {
                    dataList.add(PostRegModel(it.tid!!, it.batchNo!!))
                }
                val params = mapOf("data" to dataList)
                val p = Gson().toJson(params)

                postReg(p)
            }
            settingBtn.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
        }
    }

    private fun postReg(p: String?) {
        wait.show()
        volley.nciPostData(Url.ITEMS, JSONObject(p!!), object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        castModel<RegResultModel>().let {
                            if (it != null) {
                                regResult = it
                                showResult = true
                                cpAdapter.notifyDataSetChanged()
                                //setRegResult(it)
                            }
                        }

                        tos.success("Success")
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

    @SuppressLint("NotifyDataSetChanged")
    private fun doScan(mode: ScanMode) {
        scanMode = mode
        if (!isUhfReady) {
            tos.error(getString(R.string.warning_hh_not_ready))
            return
        }

        when (mode) {
            SINGLE, SINGLE_BEAN -> UHFReader.startSingleScan()
            CONTINUOUS -> {
                if (UHFReader.isScanning) {
                    scanMode = SINGLE
                    bind.scanBtn.text = getString(R.string.btn_scan)
                } else {
                    scannedList.clear()
                    bind.scanBtn.text = getString(R.string.btn_stop)
                }

                cpAdapter.notifyDataSetChanged()
                UHFReader.startScan()
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Physic Button">
//    private var isPressed = false
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (!isPressed){
//            if (UHFReader.validButton(keyCode)) {
//                Logger.info("WORKING")
//                isPressed = true
//                if (isAuto) doScan(CONTINUOUS)
//                else doScan(SINGLE)
//            }
//        }
//        return super.onKeyDown(keyCode, event)
//    }
    private var isPressed = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isPressed) {
            isPressed = true
            if (UHFReader.validButton(keyCode)) {
                if (isAuto) doScan(CONTINUOUS)
                else doScan(SINGLE)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPressed) {
            if (UHFReader.validButton(keyCode)) {
                if (scanMode == CONTINUOUS) {
                    isPressed = false
                    doScan(CONTINUOUS)
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
    // </editor-fold>

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
            val gcp = cpBatchList[position]

            with(holder.b) {
                finishLine.gone(position != (itemCount - 1))
                batchNo.text = gcp.batchNo
                prodName.text = gcp.prodName
                rfid.text = gcp.tid ?: getString(R.string.minus)
                if (scanMode == CONTINUOUS) {
                    if ((scannedList.size - 1) >= position) {
                        scanBtn.hideProgress(getString(R.string.btn_scan))
                    } else {
                        scanBtn.showProgress {
                            buttonText = "scanning"
                            progressColor = Color.WHITE
                        }
                    }
                } else {
                    scanBtn.hideProgress(getString(R.string.btn_scan))
                }

                if (gcp.tid.isNullOrEmpty()) {
                    Ui.show(scanBtn)
                    scanBtn.isEnabled = !isAuto
                    Ui.gone(clearBtn)
                } else {
                    Ui.show(clearBtn)
                    Ui.gone(scanBtn)
                }

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

                scanBtn.setOnClickListener {
                    beanIndexClick = holder.adapterPosition
                    doScan(SINGLE_BEAN)
                }
                clearBtn.setOnClickListener {
                    if (UHFReader.isScanning) {
                        tos.error("Scanning in progress!")
                        return@setOnClickListener
                    }


                    if (scannedList.contains(gcp.tid)) scannedList.remove(gcp.tid)
                    gcp.tid = null
                    notifyItemChanged(holder.adapterPosition)
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

        override fun getItemCount() = cpBatchList.size
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Lifecycle">
    override fun onResume() {
        super.onResume()
        connectReader()
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(0)
        msg.obj = epc._TID
        handler.sendMessage(msg)
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Slider Listener">
    override fun onStartTrackingTouch(slider: Slider) {
        /* no-op */
    }

    override fun onStopTrackingTouch(slider: Slider) {
        val power = slider.value
        modalBottomSheet.currentPower = power
        UHFReader.setAntennaPower(power.toInt())
    }
// </editor-fold>

    data class PostRegModel(
        val tid: String,
        val qr_data: String
    )

    data class RegResultModel(
        val success: MutableList<String> = mutableListOf(),
        val failed: MutableList<String> = mutableListOf()
    )
}

fun main() {
    val list = listOf<GetCPBatchModel>(
        GetCPBatchModel(tid = "ABC"),
        GetCPBatchModel(tid = "BCD"),
        GetCPBatchModel(tid = "CDF")
    )

    val a = !list.any { it.tid == "ABC" }
    println(a)
}