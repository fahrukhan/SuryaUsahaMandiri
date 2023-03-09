package com.rfid.sum.activities.ui.toti

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.activities.ui.opname.SoResponseModel
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.ItemModel
import com.rfid.sum.data.model.UserRoleModel
import com.rfid.sum.data.model.WarehouseModel
import com.rfid.sum.databinding.ActivityTotiBinding
import com.rfid.sum.databinding.BeanItemRvTotiBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Beep
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject

class TotiActivity : BaseBinding<ActivityTotiBinding>(), UHFScanning, Slider.OnSliderTouchListener {
    override fun getViewBinding() = ActivityTotiBinding.inflate(layoutInflater)
    override fun useReader() = true

    private lateinit var pop: Pop
    private var destinationSelected: WarehouseModel? = null
    private val scannedList = mutableListOf<String>()
    private val itemList = mutableListOf<BatchModel>()
    private val adapter = ItemAdapter()
    private val modalBottomSheet = ModalBottomSheet()

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    val tid = msg.obj as String
                    if (!scannedList.contains(tid)) {
                        scannedList.add(tid)
                        getCpByRFID(tid)
                    }
                }
            }
        }
    }

    private fun getCpByRFID(tid: String) {

        if (tid.isEmpty()){
            return
        }

        volley.getDataById(Url.ITEMS_BY_RFID, tid, object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {

                    if (success) {
                        val model = castModel<BatchModel>()!!
                        itemList.add(model)
                        Beep.playSound(1)
                        adapter.notifyDataSetChanged()
                        buttonSet()
                    } else {
                        Logger.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initToti()
    }

    private fun initToti() {
        pop = Pop(this)
        UHFReader.listener = this
        Beep.initSound(this)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        with(bind) {
            cpRv.layoutManager = LinearLayoutManager(this@TotiActivity)
            cpRv.adapter = adapter

//            originBtn.setOnClickListener {
//                pop.warehouseListPop(root, object : Pop.PopListener{
//                    override fun onWarehouseSelected(wh: WarehouseModel) {
//                        originSelected = wh
//                        originBtn.text = wh.wrhsName
//                    }
//                })
//            }
            destinationBtn.setOnClickListener {
                pop.warehouseListPop(root, object : Pop.PopListener {
                    override fun onWarehouseSelected(wh: WarehouseModel) {
                        destinationSelected = wh
                        destinationBtn.text = wh.wrhsName
                    }
                })
            }
            scanBtn.setOnClickListener {
                doScan()
            }
            settingBtn.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }

            saveBtn.setOnClickListener {
                if(destinationSelected == null) {
                    tos.warning("Please select destination!")
                    return@setOnClickListener
                }

                val tidList = mutableListOf<String>()
                itemList.forEach { tidList.add(it.tid!!) }
                val param = mapOf(
                    "tid_list" to tidList,
                    "wrhscode" to destinationSelected?.wrhsCode
                )
                val gson = Gson().toJson(param)
                postRelocation(gson)
            }
        }
    }

    private fun doScan(){
        if (UHFReader.isScanning) {
            bind.scanBtn.hideProgress(R.string.btn_scan)
        } else {
            bind.scanBtn.showProgress {
                progressColor = Color.WHITE
                buttonTextRes = R.string.btn_stop
            }
        }
        UHFReader.startScan()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSuccessDialog(m: TransferResponseModel) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Transfer").setMessage(
            "Transfer result successfully saved!\n" +
                    "\n${m.receiptNumber}\n${m.createdAt}"
        )
        dialog.setPositiveButton("OK") { d, _ ->
//            itemList.clear()
//            adapter.notifyDataSetChanged()
//            buttonSet()
            d.dismiss()
        }
        dialog.show()
    }

    private fun postRelocation(gson: String?) {
        wait.show()
        volley.nciPostData(Url.RELOCATION, JSONObject(gson!!), object: NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        itemList.forEach {
                            it.wrhsCode = destinationSelected?.wrhsCode
                            it.warehouse = destinationSelected
                        }
                        adapter.notifyDataSetChanged()

                        val m = castModel<TransferResponseModel>()
                        if (m != null) {
                            showSuccessDialog(m)
                        } else {
                            tos.error("Error, please check web for result!")
                        }

                        //tos.success("Success")
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    fun buttonSet(){
        (itemList.size > 0).let { b ->
            with(bind){
                if (b && saveBtn.visibility != View.VISIBLE){
                    YoYo.with(Techniques.FadeInRight).duration(200)
                        .onStart { saveBtn.invisible(false) }
                        .playOn(saveBtn)
                }
                if (!b && saveBtn.visibility == View.VISIBLE){
                    YoYo.with(Techniques.FadeOutRight).duration(200)
                        .onEnd { saveBtn.invisible(true) }
                        .playOn(saveBtn)
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(0)
        msg.obj = epc._TID
        handler.sendMessage(msg)
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

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.Holder>() {
        inner class Holder(val b: BeanItemRvTotiBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanItemRvTotiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = itemList[position]

            with(holder.b) {
                prodName.text = item.prodName
                batchNo.text = item.batchNo
                warehouseName.text = item.warehouse?.wrhsName
                removeBtn.setOnClickListener {
                    scannedList.remove(item.tid)
                    itemList.removeIf { it.tid == item.tid }
                    notifyDataSetChanged()
                    buttonSet()
                }
            }
        }

        override fun getItemCount() = itemList.size
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Physic Button">
    private var isPressed = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isPressed){
            isPressed = true
            if(UHFReader.validButton(keyCode)){
                doScan()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPressed){
            isPressed = false
            if(UHFReader.validButton(keyCode)){
                doScan()
            }
        }
        return super.onKeyUp(keyCode, event)
    }
    // </editor-fold>

    override fun onResume() {
        super.onResume()
        connectReader()
    }

    override fun onPause() {
        super.onPause()
        Beep.freeSound()
    }

    data class TransferResponseModel(
        @field:SerializedName("id")
        val id: Int? = null,

        @field:SerializedName("updated_at")
        val updatedAt: String? = null,

        @field:SerializedName("user_id")
        val userId: Int? = null,

        @field:SerializedName("created_at")
        val createdAt: String? = null,

        @field:SerializedName("receipt_number")
        val receiptNumber: String? = null,

        @field:SerializedName("at")
        val date: String? = null,

        @field:SerializedName("wrhscode")
        val wrhsCode: String? = null,

        @field:SerializedName("token")
        var token: String? = null
    )
}