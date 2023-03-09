package com.rfid.sum.activities.ui.identify

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.github.sumimakito.awesomeqr.RenderResult
import com.google.android.material.slider.Slider
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.RFIDLogsModel
import com.rfid.sum.databinding.ActivityIdentifyBinding
import com.rfid.sum.databinding.BeanRfidLogsBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.QR
import com.rfid.sum.utils.Wait
import com.rfid.sum.widget.ModalBottomSheet
import kotlinx.coroutines.*


class IdentifyActivity : BaseBinding<ActivityIdentifyBinding>(), UHFScanning,
    Slider.OnSliderTouchListener {
    override fun getViewBinding() = ActivityIdentifyBinding.inflate(layoutInflater)
    override fun useReader() = true
    private var selectedItem: BatchModel? = null
    private val logAdapter = LogsAdapter()
    private var isOpen = true
    private var qrDecode = true
    private val modalBottomSheet = ModalBottomSheet()

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    if (isOpen) {
                        isOpen = false
                        val tid = msg.obj as String

                        if (selectedItem != null && selectedItem?.tid == tid) {
                            bind.scanBtn.hideProgress(R.string.btn_scan)
                            isOpen = true
                            return
                        }

                        loadData(tid)
                    }
                }
            }
        }
    }

    private val corScope = CoroutineScope(Job() + Dispatchers.Main)
    private fun startJob() {
        println("job start")
        corScope.launch {
            while (qrDecode) {
                delay(300)
                Logger.info("GENERATING QR")
                generateQr()
            }
            loadLogs()
        }
    }

    private val rfidLogs = mutableListOf<RFIDLogsModel>()
    private fun loadLogs() {
        volley.getDataById(Url.RFID_LOGS, selectedItem?.tid!!, object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        val list = castListModel<RFIDLogsModel>()
                        if (list.isNotEmpty()) {
                            rfidLogs.clear()
                            rfidLogs.addAll(list)
                            logAdapter.notifyDataSetChanged()
                        }
                        wait.hide()
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initId()
    }

    private fun initId() {
        UHFReader.listener = this
        UHFReader.setAntennaPower(20)
        wait = Wait(this, bind.root)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        with(bind) {
            identifyRv.layoutManager = LinearLayoutManager(this@IdentifyActivity)
            identifyRv.adapter = logAdapter

            scanBtn.setOnClickListener {
                scanBtn.showProgress {
                    progressColorRes = R.color.white
                    buttonTextRes = R.string.btn_stop
                }

                Handler(Looper.myLooper()!!).postDelayed({
                    if (isOpen) {
                        bind.scanBtn.hideProgress(R.string.btn_scan)
                    }
                }, 2000)

                Logger.info("WORKING")
                UHFReader.startSingleScan()
            }
            settingBtn.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
        }
    }

    private fun loadData(tid: String) {
        if (tid.isEmpty()) return

        qrDecode = true
        bind.rfidTv.text = tid
        val url = "${Url.ITEMS}?tid=$tid"
        wait.show()
        volley.getData(url, object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        if (data != null) {
                            val model = castModel<BatchModel>()!!
                            selectedItem = model
                            setDataItem(model)
                        }
                    } else {
                        wait.hide()
                        selectedItem = BatchModel(tid = tid)
                        setDataItem(selectedItem!!)
                        if (errorMsg == "item not found") tos.error("RFID tidak terdaftar")
                        else tos.error(errorMsg)
                        rfidLogs.clear()
                        logAdapter.notifyDataSetChanged()
                    }
                    isOpen = true
                    bind.scanBtn.hideProgress(R.string.btn_scan)
                }
            }
        }, session.getUser()?.token)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setDataItem(model: BatchModel?) {


        with(bind) {


            if (model != null) {
                batchNoTv.text = model.batchNo ?: ""
                batchInQtyTv.text = (model.batchInQty ?: 0).toString()
                uomCodeTv.text = model.uomCode ?: ""
                prodCodeTv.text = model.prodCode ?: ""
                prodNameTv.text = model.prodName ?: ""
                cusColor.text = model.cusColor
                "${getString(R.string.cust_color_hint)}${model.cusColor}".also {
                    colorTv.text = it
                }
                "LOT ${model.lot ?: ""}".also { lotTv.text = it }
                if (model.batchNo == null) tvWaitResult.gone(true)
                else tvWaitResult.gone(model.startDate != null && model.endDate != null)
                startJob()
            } else {
                batchNoTv.text = ""
                batchInQtyTv.text = ""
                uomCodeTv.text = ""
                prodCodeTv.text = ""
                prodNameTv.text = ""
                cusColor.text = ""
                lotTv.text = ""
                getString(R.string.cust_color_hint).also {
                    colorTv.text = it
                }
                rfidLogs.clear()
                logAdapter.notifyDataSetChanged()

            }

        }
        wait.hide()

    }


    private fun generateQr() {
        try {
            QR.generate(selectedItem?.batchNo!!, object : QR.Generated {
                override fun onGenerated(result: RenderResult?, e: Exception?) {
                    if (e == null) {
                        qrDecode = false
                        if (result?.bitmap != null) {
                            bind.qrCodeImg.setImageBitmap(result.bitmap)
//                        QR.generateBarcode(selectedItem?.batchNo!!, object: QR.Generated{
//                            override fun onBarcodeGenerated(b: Bitmap?, e: Exception?) {
//                                if (b != null) {
//                                    bind.bCodeImg.setImageBitmap(b)
//                                } else {
//                                    Logger.error("Error While Barcode: ${e?.message!!}", true)
//                                }
//                            }
//                        })
                        } else {
                            Logger.error("Can't render QR")
                        }
                    } else {
                        Logger.error("Error While QR: ${e.message!!}", true)
                    }
                }
            })
        } catch (e: Exception) {
            qrDecode = false
            e.printStackTrace()
        }

    }

    override fun outPutEpc(epc: EPCModel) {
        if (isOpen) {
            val msg = handler.obtainMessage(0)
            msg.obj = epc._TID
            handler.sendMessage(msg)
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Key Event">
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (Key.KEY_CODE_LIST.contains(keyCode)) {
            //UHFReader.startSingleScan()
            bind.scanBtn.performClick()
        }
        return super.onKeyUp(keyCode, event)
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Lifecycle">
    override fun onResume() {
        super.onResume()
        UHFReader.connectReader(this)
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class LogsAdapter : RecyclerView.Adapter<LogsAdapter.Holder>() {
        inner class Holder(val b: BeanRfidLogsBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = rfidLogs.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanRfidLogsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val log = rfidLogs[position]

            with(holder.b) {
                topSpace.gone(position > 0)
                lineConnector.gone(position == (rfidLogs.size - 1))

                noteTv.text = log.note
                dateTimeTv.text = log.createdAt
                userTv.text = log.user?.name
            }
        }
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Slider Event">
    override fun onStartTrackingTouch(slider: Slider) {
        /* no-op */
    }

    override fun onStopTrackingTouch(slider: Slider) {
        val power = slider.value
        modalBottomSheet.currentPower = power
        UHFReader.setAntennaPower(power.toInt())
    }
// </editor-fold>
}