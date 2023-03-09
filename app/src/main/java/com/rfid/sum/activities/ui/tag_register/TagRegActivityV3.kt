package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
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
import com.rfid.sum.databinding.ActivityTagRegV3Binding
import com.rfid.sum.databinding.ActivityTagRegisterBinding
import com.rfid.sum.databinding.BeanCpBatchChooserRvBinding
import com.rfid.sum.databinding.BeanCpBatchRvBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.utils.*
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject

class TagRegActivityV3 : BaseBinding<ActivityTagRegV3Binding>(), IRScanning, UHFScanning,
    Slider.OnSliderTouchListener, Pop.PopListener {
    override fun getViewBinding() = ActivityTagRegV3Binding.inflate(layoutInflater)
    override fun useReader() = true
    private val scanner = IRScanner
    private var isScannerReady = false
    private var isCpScan = true
    private val cpAdapter = Adapter()
    private val modalBottomSheet = ModalBottomSheet()
    private val batchMap = mutableMapOf<String, String>()

    private val singleScanList = mutableListOf<String>()
    //private val cpBatchList = mutableListOf<BatchModel>()
    private val cpBatchListScanned = mutableListOf<BatchModel>()
    private var regResult: TagRegisterAdvanced.RegResultModel? = null
    private var showResult = false


    private val handler = object: Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            val data = msg.obj as String
            var batch = ""
            data.split("|").let {
                if (it.size == 7) {
                    batch = it[0]
                    if (!batchMap.containsKey(batch)){
                        batchMap[batch] = data.trim()
                    }
                }
                else {
                    tos.info("Format QR tidak valid")
                    return
                }
            }

            //return
            when(msg.what){
                0 -> {
                    //val code = msg.obj as String
                    //val cp = code.trim()
                    batch.trim().let {
                        when (it.split(".").size) {
                            3 -> {
                                bind.tvCpNumber.text = it
                                bind.switchButton.isChecked = false
                            }
                            4 -> {
                                val qr = Format.getCPNumberFromBatch(it)
                                if (qr != null){
                                    bind.tvCpNumber.text = qr
                                    bind.switchButton.isChecked = false
                                }
                            }
                            else -> {
                                bind.tvCpNumber.text = getString(R.string.minus)
                                tos.error("CP Number tidak valid!")
                            }
                        }
                    }

                }
                1 -> {
                    singleScanList.clear()
                    //val code = msg.obj as String

                    val cp = bind.tvCpNumber.text.toString()
                    val bCp = Format.getCPNumberFromBatch(batch.trim())
                    if(cp != bCp){
                        tos.error("Batch tidak sesuai dengan CP Number!", gravity = Gravity.CENTER_VERTICAL)
                        return
                    }

                    if(cpBatchListScanned.any{ it.batchNo == batch.trim()}){
                        tos.error("Sudah ada! ${batch.trim()}")
                        return
                    }
                    UHFReader.startSingleScan()
                    wait.show("Dekatkan RFID Tag!")
                    Handler(Looper.myLooper()!!).postDelayed({scannedRFIDCheck(batch.trim())}, 1000)
                }
                2 -> {
                    val code = msg.obj as String

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTagReg()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initTagReg() {
        UHFReader.listener = this
        UHFReader.setAntennaPower(12)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        if (scanner.init(applicationContext)) {
            isScannerReady = true
            scanner.listen = this
        }
        with(bind){
            switchButton.isChecked = true
            rvBatches.layoutManager = LinearLayoutManager(this@TagRegActivityV3)
            rvBatches.adapter = cpAdapter

            switchButton.setOnCheckedChangeListener { _, isChecked ->
                isCpScan = isChecked
            }

            btnReset.setOnClickListener {
                val alert = AlertDialog.Builder(this@TagRegActivityV3)
                alert.setTitle("Reset Data").setMessage("Semua data akan dibersihkan, Yakin?")
                    .setPositiveButton("OK"){ d, _ ->
                        cpBatchListScanned.clear()
                        cpAdapter.notifyDataSetChanged()
                        updateTotalCpScanned()
                        showResult = false
                        d.dismiss()
                    }
                    .setNegativeButton("CANCEL"){ d, _ -> d.dismiss()}
                    .show()
            }

            scanBtn.setOnClickListener { doIRScan() }
            saveBtn.setOnClickListener {

                val list = cpBatchListScanned.filter { !it.tid.isNullOrEmpty() }
                val dataList = mutableListOf<TagRegisterAdvanced.PostRegModel>()

                list.forEach {
                    val data = batchMap[it.batchNo] ?: ""
                    if (data.isEmpty()) return@setOnClickListener
                    else dataList.add(TagRegisterAdvanced.PostRegModel(it.tid!!, data))
                }

                val params = mapOf("data" to dataList)
                val p = Gson().toJson(params)
                Logger.info(p)
                postReg(p)
            }
            btnSetting.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
            btnHistory.setOnClickListener {
                Ui.intent<TagRegisterHistory>(this@TagRegActivityV3)
            }
        }
    }

    private fun postReg(p: String) {
        wait.show()
        volley.nciPostData(Url.ITEMS, JSONObject(p!!), object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: com.rfid.sum.rest.model.Response, errorMsg: String, key: Int?) {
                with(response) {
                    wait.hide()
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
                    }
                    else tos.error(errorMsg)
                }
                println(response)
            }
        }, session.getUser()?.token)
    }

    private fun scannedRFIDCheck(code: String) {
        wait.hide()

        if (code.isEmpty()) return

        if (singleScanList.isEmpty()){
            tos.error("Tag RFID tidak terdeteksi, coba lagi.", gravity = Gravity.CENTER_VERTICAL)
        }else{
            if (singleScanList.size > 1){
                tos.error("Lebih dari 1 Tag RFID terdeteksi, Harap memberikan jarak dengan Tag RFID lainya!", Tos.LONG_TOA, gravity = Gravity.CENTER_VERTICAL)
            }else{
                val rfid = singleScanList.first()
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

        if (cpBatchListScanned.any { it.batchNo == batch.batchNo }){
            tos.error("batch sudah ada", gravity = Gravity.CENTER_VERTICAL)
            return
        }
        cpBatchListScanned.add(batch)
        cpAdapter.notifyDataSetChanged()
        updateTotalCpScanned()

    }

    private fun updateTotalCpScanned(){
        bind.tvTotalCpScanned.text = cpBatchListScanned.size.toString()
    }

    private fun doIRScan() {
        if (!isScannerReady) {
            tos.error("Scanner not ready")
            return
        }

        scanner.trigger()
    }

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

    // <editor-fold defaultstate="collapsed" desc="IRScanner">
    override fun outPutScanner(id: String?) {
        wait.hide()
        val obt = if (isCpScan) 0 else 1
        val msg = handler.obtainMessage(obt)
        msg.obj = id
        handler.sendMessage(msg)
    }

    override fun stopScan() {
        wait.hide()
    }

    override fun scanLoad() {
        wait.show()
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Adapter">
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

                removeBtn.setOnClickListener {
                    cpBatchListScanned.remove(gcp)
                    notifyItemRemoved(position)
                    updateTotalCpScanned()
                }

                //show reg result
                if (showResult && rfid.text.toString() != getString(R.string.minus)) {
                    regResult?.success?.apply {
                        icAfterRfid.apply {
                            icAfterRfid.gone(!contains(gcp.batchNo))
                            if (contains(gcp.batchNo)) {
                                chipMsg.gone(true)
                            } else {
                                chipMsg.text = getString(R.string.already_registered_warning_id)
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

    override fun onResume() {
        super.onResume()
        connectReader()
    }

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        if (!singleScanList.contains(epc._TID)){
            Logger.info(epc._TID)
            singleScanList.add(epc._TID)
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Slider">
    override fun onStartTrackingTouch(slider: Slider) {
        /* no-op */
    }

    override fun onStopTrackingTouch(slider: Slider) {
        wait.show("Set Power : ${slider.value.toInt()}")
        val power = slider.value
        modalBottomSheet.currentPower = power
        UHFReader.setAntennaPower(power.toInt())
        Handler(Looper.myLooper()!!).postDelayed({ wait.hide() },600)
        modalBottomSheet.dismiss()
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Pop">
    override fun onCpSelected(cp: String) {
        Logger.info(cp)
    }
    // </editor-fold>
}