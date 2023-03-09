package com.rfid.sum.activities.ui.search

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.iterator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.*
import com.rfid.sum.databinding.*
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.*
import com.rfid.sum.widget.ModalBottomSheet
import com.rfid.sum.widget.SignalMeter
import kotlinx.coroutines.*
import rx.Observable
import rx.subscriptions.CompositeSubscription
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class SearchActivity : BaseBinding<ActivitySearchBinding>(), UHFScanning, Pop.PopListener, Slider.OnSliderTouchListener {
    override fun getViewBinding() = ActivitySearchBinding.inflate(layoutInflater)
    override fun useReader() = true

    lateinit var pop: Pop
    private val sub = CompositeSubscription()
    private val rssiList = mutableListOf<Double>()
    private val selectedBatchList = mutableListOf<BatchModel>()
    private val productList = mutableListOf<BatchModel>()
    private val batchMap = mutableMapOf<String, List<BatchModel>>()

    private var progressMeter = 0.0f
    private var isSearching = false
    private var currRssi = 0.0f
    private var lastRssi = 0.0f
    private var selectedWarehouse: WarehouseModel? = null
    private var productSelected: BatchModel? = null
    private var selectedBatch: BatchModel? = null
    private var cpSelected: String? = null
    lateinit var batchPop: AlertDialog
    private val modalBottomSheet = ModalBottomSheet()
    private var byProductSearching = true
    private val chipList = mutableListOf<String>()

    private val handler = object : Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0 -> {
                    val r = msg.obj as Double
                    updateSearchResult(r)
                }
                1 -> {
                    val tag = msg.obj as EPCModel
                    if (selectedBatchList.any{it.tid == tag._TID}){
                        val batch = selectedBatchList.find { it.tid == tag._TID }
                        if (batch != null){
                            val suffix = batch.batchNo!!.replace(batch.prdNumber!!, "")
                            if (!chipList.contains(suffix)){
                                chipList.add(suffix)
                                val c = ItemChipsBinding.inflate(
                                    LayoutInflater.from(this@SearchActivity),
                                    bind.root,
                                    false
                                )
                                with(c.root) {
                                    text = suffix
                                    bind.chipGroup.addView(this)
                                }

                            }
//                            else{
//                                with(bind){
//                                    chipGroup.removeAllViews()
//
//                                    (0..chipGroup.childCount).forEach { i ->
//                                        //val c: Chip = chipGroup.getChildAt(i) as Chip
//                                    }
//                                }
//                            }
                        }
                        updateSearchResult(tag.RSSI())
                    }

//                    if (productSelected?.cpBatch!!.any { it.tid == tag._TID }){
//                        updateSearchResult(tag.RSSI())
//
//                        val batch = productSelected?.cpBatch!!.find { it.tid == tag._TID }
//                        if(batch != null){
//                            val suffix = batch.batchNo!!.replace(batch.prdNumber!!, "")
//                            if (!chipList.contains(suffix)){
//                                chipList.add(suffix)
//                                val c = ItemChipsBinding.inflate(
//                                    LayoutInflater.from(this@SearchActivity),
//                                    bind.root,
//                                    false
//                                )
//                                with(c.root) {
//                                    text = suffix
//                                    bind.chipGroup.addView(this)
//                                }
//
//                            }
//                        }
//
//                    }
                }
            }
        }
    }

    private fun updateSearchResult(r: Double){
        val dec = DecimalFormat("#,###.##")
        if (!rssiList.contains(r)) rssiList.add(r)
        val rsPer = rssiToHundred(rssiList, r)
        val p = rsPer / 100F
        if (p in 0.0f..1.0f){
            currRssi = p
            Beep.playSound(1)
            bind.rssiTv.text = dec.format(r)
        }
    }

    private val corScope = CoroutineScope(Job() + Dispatchers.Main)
    private fun startSearching() {
        Logger.info("JOB STARTED")
        corScope.launch {
            var i = 0F

            while (progressMeter > 0F) {
                delay(1000)
                bind.meter.setProgress(progressMeter)
                progressMeter -= 0.02f
            }

            if(progressMeter <= 0F) coroutineContext.cancel()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSearch()
        initMeter()
    }

    private fun initMeter() {
        sub.add(Observable.interval(300, TimeUnit.MILLISECONDS).subscribe(){ _ ->
            handler.post{
                animate(bind.meter)
            }
        })
    }
    private fun animate(meter: SignalMeter) {
        if (currRssi <= lastRssi){
            val minus = when {
                progressMeter > 0.50f -> 0.13f
                progressMeter > 0.40f -> 0.10f
                progressMeter > 0.30f -> 0.07f
                progressMeter > 0.20f -> 0.05f
                progressMeter > 0.06f -> 0.03f
                else -> 0.01f
            }
//            println("minus ===>>> $progressMeter ==> $minus")
            if (progressMeter > 0f) {
                progressMeter -= minus
                meter.setProgress(progressMeter)
            }
            lastRssi = currRssi
        }else{
            lastRssi = currRssi
            progressMeter = lastRssi
            meter.setProgress(progressMeter)
        }
        // bind.lineProgress.progress = (progressMeter*100).toInt()

    }

    private fun initSearch() {
        //UHFReader.connectReader(this)
        UHFReader.listener = this
        UHFReader.setUpdateParam(30)
        Beep.initSound(this)
        pop = Pop(this)
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        with(bind){
            scanBtn.setOnClickListener {

                if (productSelected == null){
                    tos.info("Please select product!")
                    return@setOnClickListener
                }

                line1.gone(!isSearching)
                line2.gone(isSearching)

                if (isSearching){
                    isSearching = false
                    scanBtn.hideProgress(R.string.btn_scan)
                    UHFReader.stop()
                }else{
                    isSearching = true
                    scanBtn.showProgress{
                        progressColorRes = R.color.white
                        buttonTextRes = R.string.btn_stop
                    }

                    lineProduct.gone(selectedBatch != null)
                    lineBatch.gone(selectedBatch == null)

                    if(selectedBatch == null) {
                        UHFReader.startScan()
                        Logger.info("Scan all")
                    }else{

                        Logger.info("Scan single")
                        UHFReader.startScanEpcTidMatchTid(selectedBatch?.tid!!)
                    }

                    startSearching()
                }

            }
            locationBtn.setOnClickListener {
                pop.warehouseListPop(root, object : Pop.PopListener {
                    override fun onWarehouseSelected(wh: WarehouseModel) {
                        selectedWarehouse = wh
                        locationBtn.text = wh.wrhsName
                        loadItemByWarehouse()
                    }
                })
            }
            productBtn.setOnClickListener {
//                val sortedList = mutableListOf<BatchModel>()
//                productList.forEach {
//                    sortedList.add(it.cpBatch[0])
//                }
                if(productList.isEmpty()){
                    tos.info("No product!")
                    return@setOnClickListener
                }
                pop.popProductList("CP Number", root, productList, this@SearchActivity, false)
            }
            batchBtn.setOnClickListener {
                if(productSelected == null){
                    tos.info("Please select product!")
                    return@setOnClickListener
                }

                val dialog = AlertDialog.Builder(this@SearchActivity)
                val v = PopSearchBatchListBinding.inflate(layoutInflater)
                v.title.text = "Batch List"
                val bAdapter = BatchAdapter()
                v.batchRv.layoutManager = LinearLayoutManager(this@SearchActivity)
                v.batchRv.adapter = bAdapter
                dialog.setView(v.root)
                batchPop = dialog.create()

                v.cancelBtn.setOnClickListener {
                    batchPop.dismiss()
                }

                batchPop.show()
            }
            settingBtn.setOnClickListener {
                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
        }
    }

    private fun loadItemByWarehouse() {
        if (selectedWarehouse == null) {
            tos.error("Select location.")
            return
        }

        wait.show()
        val id = "${selectedWarehouse?.wrhsCode}/prdnmbr"
        volley.getDataById(Url.ITEMS_BY_WH, id, object :
            NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        val list = castListModel<BatchModel>()
                        productList.clear()
                        selectedBatchList.clear()
                        bind.productBtn.text = getString(R.string.select_product_btn)
                        bind.batchBtn.text = getString(R.string.select_batch_btn)

                        if (list.isEmpty()) {
                            wait.hide()
                            tos.info("Warehouse have no items!")
                        } else {
                            wait.hide()
                            productList.addAll(list)
                        }
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun loadBatch(cp: String) {
        bind.chipGroup.removeAllViews()
        chipList.clear()
        bind.productNumb.text = cp
        "Select Batch".also { bind.batchBtn.text = it }

        if (batchMap.containsKey(cp)){
            if (batchMap[cp]?.size == productSelected?.qty){
                selectedBatchList.clear()
                selectedBatchList.add(BatchModel(
                    batchNo = "$cp.*",
                    prodName = batchMap[cp]?.get(0)?.prodName,
                    batchInQty = "0.00"
                ))
                batchMap[cp]?.let { selectedBatchList.addAll(it) }

                return
            }
        }

        wait.show("Load batches...")
        val id = "${selectedWarehouse?.wrhsCode}/prdnmbr/$cp"
        volley.getDataById(Url.ITEMS_BY_WH, id, object :  NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<BatchModel>()
                        selectedBatchList.clear()
                        bind.batchBtn.text = getString(R.string.select_batch_btn)

                        if (list.isNotEmpty()){
                            println(list)
                            selectedBatchList.add(BatchModel(
                                batchNo = "${list[0].prdNumber}.*",
                                prodName = list[0].prodName,
                                batchInQty = "0.00"
                            ))
                            selectedBatchList.addAll(list)

                            if (batchMap.containsKey(cp)){
                                batchMap.remove(cp)
                            }
                            batchMap.put(cp, list)

                        }else tos.info("No batch loaded!")
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun setDataItem() {

        if (byProductSearching) return

        with(bind){
            rfidTv.text = selectedBatch?.tid
            batchNoTv.text = selectedBatch?.batchNo
            batchInQtyTv.text = selectedBatch?.batchInQty!!.toString()
            uomCodeTv.text = selectedBatch?.uomCode
            prodCodeTv.text = selectedBatch?.prodCode
            prodNameTv.text = selectedBatch?.prodName
            "LOT ${selectedBatch?.lot}".also { lotTv.text = it }
        }
        // wait.hide()
        // startJob()
    }

    // <editor-fold defaultstate="collapsed" desc="UHF Listener">
    override fun outPutEpc(epc: EPCModel) {
        val obt = if (byProductSearching) 1 else 0
        val m = handler.obtainMessage(obt)
        m.obj = if (byProductSearching) epc else epc.RSSI()
        Logger.info(epc._TID)
        handler.sendMessage(m)
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Pop Listener">
    override fun onCpSelected(cp: String) {
        cpSelected = cp
        val c = productList.find { it.prdNumber == cp }
        productSelected = c

        "$cp (${c?.qty!!})".also { bind.productBtn.text = it }
        loadBatch(cp)
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class BatchAdapter: RecyclerView.Adapter<BatchAdapter.Holder>(){
        inner class Holder(val b: BeanPopListCpBatchBinding): RecyclerView.ViewHolder(b.root)
        override fun getItemCount() = selectedBatchList.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanPopListCpBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cp = selectedBatchList[position]

            with(holder.b){
                Ui.gone(lineTotal)
                icon.setImageDrawable(ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_bookmark_24))
                prodNumber.text = cp.batchNo
                prodName.text = cp.prodName
                totalBatch.text = cp.qty.toString()
                holder.itemView.setOnClickListener {
                    bind.batchBtn.text = cp.batchNo

                    if (position == 0){
                        byProductSearching = true
                        selectedBatch = null
                    }else{
                        byProductSearching = false
                        selectedBatch = cp
                        setDataItem()
                    }
                    batchPop.dismiss()
                }

//                getColor(R.color.highLightText).let {
//                    Format.setHighLightedText(prodName, inputEt.text.toString(), it)
//                    Format.setHighLightedText(prodNumber, inputEt.text.toString(), it)
//                }
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Physic Button">
    private var isPressed = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isSearching) return true

        if (!isPressed){
            isPressed = true
            if(UHFReader.validButton(keyCode)){
                bind.scanBtn.performClick()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPressed){
            isPressed = false
            if(UHFReader.validButton(keyCode)){
                bind.scanBtn.performClick()
            }
        }
        return super.onKeyUp(keyCode, event)
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

    private fun rssiToHundred(list: List<Double>, rssi: Double): Int {
        val sl = list.sorted()
        val range = sl.last() - sl.first()
        val percent = range / 100.0
        val rssiConvert = rssi - sl.first()
        val res = rssiConvert / percent
        return res.toInt()
    }

    override fun onPause() {
        super.onPause()
        UHFReader.dispose()
        Beep.freeSound()
        sub.clear()
        sub.unsubscribe()
    }
}