package com.rfid.sum.activities.ui.opname

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.data.model.ItemModel
import com.rfid.sum.data.model.WarehouseModel
import com.rfid.sum.databinding.*
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Pop
import com.rfid.sum.utils.Tos
import com.rfid.sum.widget.ModalBottomSheet
import org.json.JSONObject
import java.math.BigDecimal

class StockOpname : BaseBinding<ActivityStockOpnameBinding>(), Slider.OnSliderTouchListener,
    UHFScanning {
    override fun getViewBinding() = ActivityStockOpnameBinding.inflate(layoutInflater)
    override fun useReader() = true
    private var locationSelected: WarehouseModel? = null
    lateinit var pop: Pop
    private val soItemList = mutableListOf<SoItemModel>()
    private val soItemFilteredList = mutableListOf<SoItemModel>()
    private val noteList = mutableListOf<BatchNote>()
    private val adapter = Adapter()
    private val modalBottomSheet = ModalBottomSheet()
    private val scannedList = mutableListOf<String>()

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    val tid = msg.obj as String
                    if (soItemList.sumOf { it.tidList.size } == soItemList.sumOf { it.detectedList.size }) {
                        tos.success("All items detected.")
                        if (UHFReader.isScanning) doScan()
                    }
                    if (!scannedList.contains(tid)) {
                        scannedList.add(tid)
                        detectedItem(tid)
                    }
                }
                1 -> {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun detectedItem(tid: String) {
        soItemList.find { it.tidList.contains(tid) && !it.detectedList.contains(tid) }.let {
            it?.detectedList?.add(tid)
        }
        val total = soItemList.sumOf { it.detectedList.size }
        bind.totalDetected.text = total.toString()

        //adapter.notifyDataSetChanged()
        adapter.notifyDataSetChanged()
        buttonSet()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initOpname()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initOpname() {
        pop = Pop(this)
        UHFReader.listener = this
        modalBottomSheet.sliderTouchListener = this
        modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()

        with(bind) {
            soRv.layoutManager = LinearLayoutManager(this@StockOpname)
            soRv.adapter = adapter

            locationBtn.setOnClickListener {
                pop.warehouseListPop(root, object : Pop.PopListener {
                    override fun onWarehouseSelected(wh: WarehouseModel) {
                        locationSelected = wh
                        locationBtn.text = wh.wrhsName
                        scannedList.clear()
                        loadItemByWarehouse()
                    }
                })
            }
            settingBtn.setOnClickListener {
                if (UHFReader.isScanning) {
                    tos.info("Scanning in progress.")
                    return@setOnClickListener
                }

                modalBottomSheet.currentPower = UHFReader.getAntennaPower().toFloat()
                modalBottomSheet.show(supportFragmentManager, "modal")
            }
            resetBtn.setOnClickListener {
                fun clear() {
                    soItemList.forEach {
                        it.detectedList.clear()
                    }
                    scannedList.clear()
                    noteList.clear()
                    bind.totalDetected.text = "0"
                    adapter.notifyDataSetChanged()
                }

                val dialog = AlertDialog.Builder(this@StockOpname)
                    .setTitle("RESET").setMessage("Clear all detected items?")
                    .setPositiveButton("OK") { _, _ -> clear() }
                    .setNegativeButton("CANCEL") { d, _ -> d.dismiss() }
                dialog.show()
            }
            scanBtn.setOnClickListener {
                doScan()
            }

            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    val list = soItemFilteredList.filter {
                        it.prodName.contains(newText!!, true) ||
                                it.prodNo.contains(newText, true)
                    }
                    soItemList.clear()
                    soItemList.addAll(list)
                    //adapter.notifyDataSetChanged()
                    adapter.notifyDataSetChanged()
                    return true
                }
            })

            saveBtn.setOnClickListener {
                if (soItemList.sumOf { it.detectedList.size } < 1) {
                    tos.error("No item detected!")
                    return@setOnClickListener
                }
                saveBtn.isEnabled = false
                saveSOResult()
            }
        }
    }

    private fun saveSOResult() {
        wait.show()

        val detectedList = mutableListOf<String>()
        soItemList.forEach {
            detectedList.addAll(it.detectedList)
        }
        val param = mapOf(
            "wrhscode" to locationSelected?.wrhsCode,
            "tid_list" to detectedList,
            "notes" to noteList
        )

        val gson = Gson().toJson(param)
//        println(JSONObject(gson))
//        wait.hide()
//        bind.saveBtn.isEnabled = true
//        return

        volley.nciPostData(Url.STOCK_OPNAME, JSONObject(gson), object : NciApiListener {
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        wait.hide()
                        val m = castModel<SoResponseModel>()
                        if (m != null) {
                            showSuccessDialog(m)
                        } else {
                            tos.error("Error, please check web page for result!")
                        }
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                    bind.saveBtn.isEnabled = true
                }
            }
        }, session.getUser()?.token)

        val g = Gson().toJson(param)
        println(g)
    }


    private fun doScan() {
        if (locationSelected == null) {
            tos.error("Select Location.")
            return
        }

        if (soItemList.isEmpty()) {
            tos.info("No data.")
            return
        }

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

    private fun loadItemByWarehouse() {
        if (locationSelected == null) {
            tos.error("Select location.")
            return
        }

        wait.show()
        volley.getDataById(
            Url.ITEMS_BY_WH,
            locationSelected?.wrhsCode.toString(),
            object : NciApiListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                    with(response) {
                        if (success) {
                            val list = castListModel<BatchModel>()
                            if (list.isEmpty()) {
                                wait.hide()
                                tos.info("Warehouse have no items!")
                                soItemList.clear()

                                bind.totalQty.text = "0"
                                adapter.notifyDataSetChanged()
                            } else {
                                setSoData(list)
                            }
                        } else {
                            wait.hide()
                            tos.error(errorMsg)
                        }
                    }
                }
            },
            session.getUser()?.token
        )
    }

    private fun bindData(s: MutableList<SoItemModel>, im: BatchModel) {
        s.find { o -> o.prodNo == im.prdNumber }.apply {
            im.tid = im.tid
            if (this != null) {
                cpBatch.add(im)
                tidList.add(im.tid!!)
            } else {
                s.add(
                    SoItemModel(
                        im.prdNumber!!,
                        im.prodName!!,
                        mutableListOf(im),
                        mutableListOf(im.tid!!)
                    )
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSoData(list: List<BatchModel>) {
        soItemList.clear()
        soItemFilteredList.clear()

        list.forEach { im ->
            soItemList.let { s -> bindData(s, im) }
            soItemFilteredList.let { s -> bindData(s, im) }
        }

        val total = soItemList.sumOf { it.tidList.size }
        bind.totalQty.text = total.toString()

        wait.hide()
        adapter.notifyDataSetChanged()
    }

    private fun buttonSet() {
        (soItemList.size > 0).let { b ->
            with(bind) {
                if (b && resetBtn.visibility != View.VISIBLE) {
                    YoYo.with(Techniques.FadeInRight).duration(200)
                        .onStart { resetBtn.invisible(false) }
                        .playOn(resetBtn)
                }
                if (!b && resetBtn.visibility == View.VISIBLE) {
                    YoYo.with(Techniques.FadeOutRight).duration(200)
                        .onEnd { resetBtn.invisible(true) }
                        .playOn(resetBtn)
                }

                saveBtn.gone(!b)
                resetBtn.gone(!b)
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="UHF Listener">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(0)
        msg.obj = epc._TID
        handler.sendMessage(msg)
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Dialog">
    fun showDetail(m: SoItemModel) {
        val dialog = AlertDialog.Builder(this)
        val b = BeanPopSoDetailBinding.inflate(LayoutInflater.from(this))
        b.title.text = m.prodName
        b.subTitle.text = m.prodNo
        b.detailRv.layoutManager = LinearLayoutManager(this)
        b.detailRv.adapter = DetailsAdapter(m)

        dialog.setView(b.root)
        dialog.setPositiveButton("OK") { d, _ ->
            d.dismiss()
        }
        dialog.show()
    }

    private fun showNoteDialog(cp: BatchModel) {
        val dialog = AlertDialog.Builder(this)
        val b = DialogAddBatchNoteBinding.inflate(LayoutInflater.from(this))
        b.title.text = cp.batchNo
        dialog.setView(b.root)
        val pop = dialog.create()

        var haveNote = false
        var batchNote = noteList.find { it.tid == cp.tid }.also {
            haveNote = (it != null)
            if (it!=null){
                b.note.setText(it.note)
                b.qty.setText(it.batchinqty_so)
            }else{
                b.note.setText("")
                b.qty.setText(cp.batchInQty.toString())
            }
        }

        b.saveBtn.setOnClickListener {
            val note = b.note.text.toString()
            var qty = b.qty.text.toString()

            if (note.isEmpty() && qty == cp.batchInQty.toString()) {
                tos.info("Nothing saved!", Tos.SHORT_TOA, true)
            } else {


                try {
                    val newQty = String.format("%.4f", BigDecimal(qty.toDouble()))
                    qty = newQty
                }catch (ne: java.lang.NumberFormatException){
                    tos.error("error qty format!")
                    return@setOnClickListener
                }

                noteList.find { it.tid == cp.tid!! }.let {
                    if (it != null) {
                        it.batchinqty_so = qty
                        it.note = note
                    } else {
                        noteList.add(BatchNote(cp.tid!!, qty, note))
                    }
                }
            }
            println(noteList)
            adapter.notifyDataSetChanged()
            pop.dismiss()
        }
        b.deleteBtn.setOnClickListener {
            if (haveNote){
                noteList.remove(batchNote)
                adapter.notifyDataSetChanged()
                tos.success("Note removed!")
                pop.dismiss()
            }
        }
        b.cancelBtn.setOnClickListener { pop.dismiss() }

        pop.show()
    }

    private fun showSuccessDialog(m: SoResponseModel) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Stock Opname").setMessage(
            "Stock opname results successfully saved!\n" +
                    "\n${m.receiptNumber}\n${m.createdAt}"
        )
        dialog.setPositiveButton("OK") { d, _ ->
            soItemList.clear()
            soItemFilteredList.clear()
            noteList.clear()

            buttonSet()
            adapter.notifyDataSetChanged()
            d.dismiss()
        }
        dialog.show()
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val b: BeanSoRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                BeanSoRvBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = soItemList[position]

            with(holder.b) {
                batchRv.layoutManager = LinearLayoutManager(this@StockOpname)
                batchRv.adapter = DetailsAdapter(item)

                "${position + 1}".also { no.text = it }

                prodName.text = item.prodName
                prodNo.text = item.prodNo
                prodQty.text = item.tidList.size.toString()
                prodDetect.text = item.detectedList.size.toString()
                if (item.tidList.size == item.detectedList.size) {
                    prodCheck.imageTintList = ColorStateList.valueOf(getColor(R.color.green_400))
                    prodCheck.setImageDrawable(
                        ContextCompat.getDrawable(this@StockOpname, R.drawable.ic_check_square)
                    )
                } else {
                    prodCheck.imageTintList =
                        ColorStateList.valueOf(getColor(R.color.lightBackground))
                    prodCheck.setImageDrawable(
                        ContextCompat.getDrawable(this@StockOpname, R.drawable.ic_minus_square)
                    )
                }

                chipGroup.removeAllViews()
                (item.cpBatch).forEach { cp ->
                    val secColor = getColor(R.color.secondaryTextLight)
                    val primColor = getColor(R.color.primaryColor)
                    item.detectedList.contains(cp.tid).let { d ->
                        val c = ItemChipsBinding.inflate(
                            LayoutInflater.from(this@StockOpname),
                            root,
                            false
                        )
                        with(c.root) {
                            setTextColor(
                                ColorStateList.valueOf(
                                    if (d) Color.WHITE else secColor
                                )
                            )
                            chipStrokeColor = ColorStateList.valueOf(secColor)

                            chipBackgroundColor = ColorStateList.valueOf(
                                if (d) primColor else Color.WHITE
                            )

                            val batchShorted = cp.batchNo?.replace(cp.prdNumber!!, "")
                            batchShorted.also { s -> text = s }

                            noteList.any { it.tid == cp.tid!! }.let {
                                if (it) {
                                    isChipIconVisible = true
                                    chipStartPadding = 12f
                                }else{
                                    isChipIconVisible = false
                                    chipStartPadding = 0f
                                }
                            }

                            setOnClickListener {
                                showNoteDialog(cp)
                            }

                            chipGroup.addView(this)
                        }

                    }
                }
            }

            holder.itemView.setOnClickListener {
                //showDetail(item)
                //holder.b.batchRv.gone(holder.b.batchRv.visibility == View.VISIBLE)
                holder.b.lineChip.gone(holder.b.lineChip.visibility == View.VISIBLE)
            }
        }

        override fun getItemCount() = soItemList.size
    }

    inner class DetailsAdapter(val model: SoItemModel) :
        RecyclerView.Adapter<DetailsAdapter.Holder>() {
        inner class Holder(val b: BeanSoDetailRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                BeanSoDetailRvBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cp = model.cpBatch[position]

            with(holder.b) {
                "${position + 1}".also { no.text = it }
                //bottomLine.gone(((position+1) == model.cpBatch.size))
                batchNo.text = cp.batchNo
                tid.text = cp.tid

                val secColor = getColor(R.color.secondaryTextLight)
                val primColor = getColor(R.color.primaryColor)
                model.detectedList.contains(cp.tid).let { d ->
                    // prodCheck.gone(!it)
                    arrayOf(batchNo, tid, no).forEach {
                        it.setTextColor(
                            ColorStateList.valueOf(
                                if (d) primColor else secColor
                            )
                        )
                    }
                }
                prodNote.gone(!noteList.any { it.tid == cp.tid!! })

            }

            holder.itemView.setOnClickListener {
                showChips(cp)
            }
        }

        override fun getItemCount() = model.cpBatch.size
    }

    private fun showChips(cp: BatchModel) {

    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Special Model">
    data class SoItemModel(
        val prodNo: String,
        val prodName: String,
        val cpBatch: MutableList<BatchModel> = mutableListOf(),
        val tidList: MutableList<String> = mutableListOf(),
        val detectedList: MutableList<String> = mutableListOf(),
        var allDetect: Boolean = false
    )

    data class BatchNote(
        val tid: String,
        var batchinqty_so: String,
        var note: String
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

    // <editor-fold defaultstate="collapsed" desc="Physic Button">
    private var isPressed = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isPressed) {
            isPressed = true
            if (UHFReader.validButton(keyCode)) {
                doScan()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPressed) {
            isPressed = false
            if (UHFReader.validButton(keyCode)) {
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
}
