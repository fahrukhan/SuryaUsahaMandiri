package com.rfid.sum.activities.ui.outbound

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.CustomerModel
import com.rfid.sum.data.model.ItemModel
import com.rfid.sum.data.model.OutboundModel
import com.rfid.sum.databinding.ActivityOutboundBinding
import com.rfid.sum.databinding.BeanPopWarehouseBinding
import com.rfid.sum.databinding.BeanSoRvBinding
import com.rfid.sum.databinding.PopListCpBatchBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Beep
import com.rfid.sum.utils.Logger
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject

class OutboundActivity : BaseBinding<ActivityOutboundBinding>(), UHFScanning,
    Slider.OnSliderTouchListener {
    override fun getViewBinding() = ActivityOutboundBinding.inflate(layoutInflater)
    override fun useReader() = true

    private val customerList = mutableListOf<CustomerModel>()
    private var selectedCustomer: CustomerModel? = null
    private val modalBottomSheet = ModalBottomSheet()
    private val scannedList = mutableListOf<String>()
    private val itemList = mutableListOf<BeanModel>()
    private val adapter = ItemAdapter()

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initOut()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initOut() {
        UHFReader.connectReader(this)
        UHFReader.listener = this
        UHFReader.setUpdateParam(20)
        Beep.initSound(this)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        with(bind) {
            cpRv.layoutManager = LinearLayoutManager(this@OutboundActivity)
            cpRv.adapter = adapter

            customerBtn.setOnClickListener { popCustomers() }
            saveBtn.setOnClickListener {

                if (UHFReader.isScanning){
                    tos.error("Please stop scan!")
                    return@setOnClickListener
                }

                if(itemList.size < 1){
                    tos.info("Nothing to save!")
                    return@setOnClickListener
                }

                if(selectedCustomer == null){
                    tos.info("Please select customer!")
                    return@setOnClickListener
                }

                saveBtn.isEnabled = false
                wait.show()
                val param = mutableMapOf<String, Any>()
                param["customer_id"] = selectedCustomer?.id!!
                val dataList = mutableListOf<Any>()
                itemList.forEach {
                    val data = mutableMapOf<String, Any>()
                    data["prdnmbr"] = it.prodNumber
                    val batchList = mutableListOf<Map<String, String>>()
                    it.listItem.forEach { im ->
                        batchList.add(mapOf(
                            "batchno" to im.batchNo!!,
                            "tid" to im.tid!!
                        ))
                    }
                    data["batch_list"] = batchList
                    dataList.add(data)
                }

                param["data"] = dataList
                val gson = Gson().toJson(param)
                volley.nciPostData(Url.OUTBOUNDS, JSONObject(gson), object: NciApiListener{
                    override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                        with(response){
                            if (success){
                                val m = castModel<OutboundModel>()
                                if (m != null){
                                    tos.success("Success")
                                    showSuccessDialog(m)
                                }
                                wait.hide()
                            }else{
                                wait.hide()
                                tos.error(errorMsg)
                            }
                            saveBtn.isEnabled = true
                        }
                    }
                }, session.getUser()?.token)
            }
            scanBtn.setOnClickListener {
                doScan()
            }
            historyIcon.setOnClickListener {
                startActivity(Intent(this@OutboundActivity, OutboundListActivity::class.java))
            }
            settingBtn.setOnClickListener {
                if (UHFReader.isScanning) {
                    tos.info("Scanning in progress.")
                    return@setOnClickListener
                }

                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
            clearBtn.setOnClickListener {
                if (UHFReader.isScanning) {
                    tos.info("Scanning in progress.")
                    return@setOnClickListener
                }

                fun clear(){
                    itemList.clear()
                    scannedList.clear()
                    bind.totalDetected.text = "0"
                    adapter.notifyDataSetChanged()
                }

                val dialog = AlertDialog.Builder(this@OutboundActivity)
                    .setTitle("RESET").setMessage("Clear all detected items?")
                    .setPositiveButton("OK"){ _, _ -> clear() }
                    .setNegativeButton("CANCEL"){ d, _ -> d.dismiss()}
                dialog.show()
            }
        }
        loadCustomer()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSuccessDialog(m: OutboundModel) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Outbound Success")
            .setMessage("\nOutbound Numb: ${m.receiptNumber}\nDatetime: ${m.createdAt}")
            .setNegativeButton("History"){d,_ ->
                d.dismiss()
                startActivity(Intent(this, OutboundListActivity::class.java))
            }
            .setPositiveButton("OK"){ d, _ ->
                d.dismiss()
            }
        dialog.setOnDismissListener {
            scannedList.clear()
            itemList.clear()
            adapter.notifyDataSetChanged()
//            startActivity(Intent(this, OutboundListActivity::class.java))
        }
        dialog.show()
    }

    private fun loadCustomer() {
        wait.show("Load customer")
        volley.getData(Url.CUSTOMER, object : NciApiListener {
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        wait.hide()
                        val list = castListModel<CustomerModel>()
                        customerList.clear()
                        if (list.isNotEmpty()) {
                            customerList.addAll(list)
                        } else {
                            tos.info("You have no customer!")
                        }
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun getCpByRFID(tid: String) {

        if(tid.isEmpty()){
            return
        }

        volley.getDataById(Url.ITEMS_BY_RFID, tid, object : NciApiListener {
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {

                    if (success) {
                        val model = castModel<BatchModel>()
                        if (model != null) bindDataToList(model)

                        //buttonSet()
                    } else {
                        Logger.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindDataToList(model: BatchModel) {

        itemList.find { it.prodNumber == model.prdNumber }.let { m ->
            m?.listItem?.add(model)
                ?: itemList.add(
                    BeanModel(model.prdNumber!!, mutableListOf(model))
                )
        }

        Beep.playSound(1)
        adapter.notifyDataSetChanged()
        bind.totalDetected.text = itemList.sumOf { it.listItem.size }.toString()
    }

    private fun popCustomers() {
        val tempData = mutableListOf<CustomerModel>()
        tempData.addAll(customerList)
        val cAdapter = CustomerAdapter(tempData)

        val dialog = AlertDialog.Builder(this)
        val v = PopListCpBatchBinding.inflate(LayoutInflater.from(this), bind.root, false)
        dialog.setView(v.root)
        val pop = dialog.create()

        with(v) {
            popListTitle.text = "Customers"
            popSearchTiLy.hint = "Search Customer"
            popSearchRv.adapter = cAdapter
            popSearchRv.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    selectedCustomer = customerList[position]
                    bind.customerBtn.text = selectedCustomer?.name
                    pop.dismiss()
                }
            cancelBtn.setOnClickListener {
                pop.dismiss()
            }
            popSearchInputSearch.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            popSearchInputSearch.doOnTextChanged { text, _, _, _ ->
                val list = customerList.filter { it.name?.contains(text!!, true)!! }
                tempData.clear()
                tempData.addAll(list)
                cAdapter.notifyDataSetChanged()
            }
        }
        pop.show()
    }

    private fun doScan() {
        if (UHFReader.isScanning) bind.scanBtn.hideProgress(R.string.btn_scan)
        else bind.scanBtn.showProgress {
            progressColor = Color.WHITE
            buttonTextRes = R.string.btn_stop
        }
        UHFReader.startScan()
    }

    private fun showSnackBar(outboundNumber: String){
        val sn = Snackbar.make(bind.root, "Outbound success!!\n$outboundNumber", Snackbar.LENGTH_LONG)
            .setAction("history"){
                println("action pressed!")
            }
            .setBackgroundTint(getColor(R.color.green_400))
        sn.show()
    }

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.Holder>() {
        inner class Holder(val b: BeanSoRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanSoRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = itemList[position]

            with(holder.b) {
                prodDetect.gone(true)
                prodCheck.invisible(true)

                "${position+1}".also { no.text = it }
                prodName.text = item.listItem[0].prodName
                prodQty.text = item.listItem.size.toString()
                prodNo.text = item.prodNumber
//                batchNo.text = item.listItem[0].batchNo
//                warehouseName.text = item.listItem[0].warehouse?.wrhsName
//                removeBtn.setOnClickListener {
//                    scannedList.remove(item.tid)
//                    itemList.removeIf { it.tid == item.tid }
//                    notifyDataSetChanged()
//                    //buttonSet()
//                }
            }
        }

        override fun getItemCount() = itemList.size
    }

    inner class CustomerAdapter(val list: List<CustomerModel>) : BaseAdapter() {
        override fun getCount() = list.size
        override fun getItem(position: Int) = list[position]

        override fun getItemId(position: Int) = list[position].id!!.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val customer = list[position]
            val holder: Holder
            if (convertView == null) {
                val bind = BeanPopWarehouseBinding.inflate(
                    LayoutInflater.from(parent!!.context),
                    parent,
                    false
                )
                holder = Holder(bind)
                holder.view = bind.root
                holder.view.tag = holder
            } else {
                holder = convertView.tag as Holder
            }

            with(holder.binding) {
                icon.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@OutboundActivity,
                        R.drawable.ic_person
                    )
                )
                warehouseCode.gone(true)
                warehouseName.text = customer.name
            }

            return holder.view
        }

        inner class Holder internal constructor(binding: BeanPopWarehouseBinding) {
            var view: View
            val binding: BeanPopWarehouseBinding

            init {
                view = binding.root
                this.binding = binding
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UHF Listener">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(0)
        msg.obj = epc._TID
        handler.sendMessage(msg)
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Key Event">
    private var isPressed = false
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPressed){
            isPressed = false
            if(Key.KEY_CODE_LIST.contains(keyCode)){
                doScan()
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isPressed){
            isPressed = true
            if (Key.KEY_CODE_LIST.contains(keyCode)) {
                doScan()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Lifecycle">
    override fun onPause() {
        super.onPause()
        Beep.freeSound()
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Special Model">
    data class BeanModel(
        val prodNumber: String,
        val listItem: MutableList<BatchModel> = mutableListOf<BatchModel>()
    )
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

    override fun onResume() {
        super.onResume()
        UHFReader.connectReader(this)
        UHFReader.listener = this
        UHFReader.setUpdateParam(20)
    }
}