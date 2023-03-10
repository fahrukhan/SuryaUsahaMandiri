package com.rfid.sum.activities.ui.packing

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.*
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.databinding.ActivityPackingListCheckerBinding
import com.rfid.sum.databinding.BeanPackingListCheckerBinding
import com.rfid.sum.databinding.BeanPackingListCheckerGroupBinding
import com.rfid.sum.databinding.ItemChipsBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Pop
import kotlinx.coroutines.internal.artificialFrame
import org.json.JSONObject
import java.text.Normalizer.Form

class PackingListChecker : BaseBinding<ActivityPackingListCheckerBinding>(), NciApiListener,
    UHFScanning {
    override fun getViewBinding() = ActivityPackingListCheckerBinding.inflate(layoutInflater)
    override fun useReader() = false

    private val packingListLoaded = mutableListOf<DolsListModel>()
    private val packingListLoadedFiltered = mutableListOf<DolsListModel>()
    private val detectionList = mutableListOf<String>()
    private val scannedList = mutableListOf<String>()
    private val pAdapter = Adapter()
    lateinit var pop: Pop

    private val handler = object: Handler(Looper.myLooper()!!){
        @SuppressLint("NotifyDataSetChanged")
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0 -> {
                    val tid = msg.obj as String
                    if (!scannedList.contains(tid)){
                        println(tid)
                        scannedList.add(tid)
                        detectionList.add(tid)
                        pAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(Key.INTENT_DATA).let {
            if (it != null) initPack(it)
            else {
                tos.error("Gagal saat memproses nomor packinglist")
                finish()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initPack(it: String) {
        pop = Pop(this)
        UHFReader.listener = this

        with(bind){
            rvBatches.apply {
                layoutManager = LinearLayoutManager(this@PackingListChecker)
                adapter = pAdapter
            }

            btSave.setOnClickListener {
                data class ParamModel(
                    val transdestnmbr: String,
                    val batch_list: MutableList<String> = mutableListOf()
                )
                val paramList = mutableListOf<ParamModel>()

                packingListLoaded.forEach { dm ->
                    dm.plList.forEach { pl ->
                        pl.batchList.forEach { b ->
                            if (detectionList.contains(b.tid)){
                                paramList.find { it.transdestnmbr == dm.dolsNumber }.let { pm ->
                                    if (pm == null){
                                        val newParamModel = ParamModel(
                                            dm.dolsNumber, mutableListOf(b.batchNo!!)
                                        )
                                        paramList.add(newParamModel)
                                    }else{
                                        pm.batch_list.add(b.batchNo!!)
                                    }
                                }
                            }
                        }
                    }
                }
                if (paramList.size == 0 ){
                    tos.info("Tidak ada batch terdeteksi")
                    return@setOnClickListener
                }


                val params = mutableMapOf(
                    "data" to paramList
                )
                wait.show()
                val gson = Gson().toJson(params)
                volley.nciPostData(Url.OUTBOUNDS, JSONObject(gson), object : NciApiListener{
                    override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                        with(response){
                            wait.hide()
                            if (success){
                                val list = castListModel<String>()
                                if (list.isNotEmpty()) {
                                    showSuccessDialog(message)
                                }
                                else tos.info("Tidak ada batch terdeteksi.")
                            }else{
                                tos.error(errorMsg)
                            }
                        }
                    }
                }, session.getUser()?.token)
            }

            btScan.setOnClickListener {
//                val a = listOf( "E2003412013318000510C59A","E2003412013218000510C85F","E2003412013D18000510C77A","E2003412012C18000510C70D")
//                if (detectionList.isNotEmpty()) {
//                    detectionList.clear()
//                    return@setOnClickListener
//                }
//
//                runOnUiThread {
//                    val b = (200..2000).random()
//                    a.forEach {
//                        outPutEpc(EPCModel())
//                    }
//                }
                doScan()
            }

            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String?): Boolean {
                    val list = packingListLoaded.filter { it.dolsNumber.contains(newText!!, true) }
                    packingListLoadedFiltered.clear()
                    packingListLoadedFiltered.addAll(list)
                    pAdapter.notifyDataSetChanged()
                    return true
                }
            })

            btReset.setOnClickListener {
                if (detectionList.isEmpty()) return@setOnClickListener

                val dialog = AlertDialog.Builder(this@PackingListChecker)
                dialog.setTitle("Reset").setMessage("Hasil scanning akan di reset, Yakin?")
                    .setPositiveButton("OK"){ d, _ ->
                        detectionList.clear()
                        scannedList.clear()
                        packingListLoaded.forEach { it.plList.forEach { p -> p.detectedList.clear() } }
                        packingListLoadedFiltered.forEach { it.plList.forEach { p -> p.detectedList.clear() } }
                        pAdapter.notifyDataSetChanged()
                        d.dismiss()
                    }
                    .setNegativeButton("CANCEL"){ d, _ ->
                        d.dismiss()
                    }
                    .show()
            }
        }

        val gson = Gson().fromJson(it, List::class.java)
        val data = ResponseModel(data = gson)
        val list = data.castListModel<String>()
        if (list.isNotEmpty()) loadBatch(list)
        else {
            tos.error("Pilih nomor packinglist!")
            finish()
        }
    }

    private val packingList = mutableListOf<String>()
    private fun loadBatch(list: List<String>) {
        packingList.clear()
        packingList.addAll(list)
        val id = packingList[0].replace("/", "-")
        wait.show("Memuat batch \n${packingList[0]}")
        volley.getDataById(Url.PACKING_LIST_BY_DOL, id, this, session.getUser()?.token, 0)
    }

    private var countLoad = 1
    private fun nextLoad() {
        wait.updateMsg("Memuat batch \n${packingList[countLoad]}")
        val id = packingList[countLoad].replace("/", "-")
        volley.getDataById(Url.PACKING_LIST_BY_DOL, id, this, session.getUser()?.token, 1)
    }

    private fun doScan() {

        if (packingListLoadedFiltered.isEmpty()) {
            tos.info("No data.")
            return
        }

        if (UHFReader.isScanning) {
            bind.btScan.hideProgress(R.string.btn_scan)
        } else {
            bind.btScan.showProgress {
                progressColor = Color.WHITE
                buttonTextRes = R.string.btn_stop
            }
        }
        UHFReader.startScan()
    }

    private fun showSuccessDialog(message: String) {
        val alert = AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("Selesai"){ d, _ ->
                d.dismiss()
                finish()
            }
            .setNegativeButton("RESET"){ d, _ ->
                bind.btReset.performClick()
                d.dismiss()
            }
        alert.show()
    }

    /* ======================== CEK RESPON ADA SIMULASI ============================= */
    // <editor-fold defaultstate="collapsed" desc="Api Interface">
    @SuppressLint("NotifyDataSetChanged")
    override fun onResponse(response: Response, errorMsg: String, key: Int?) {
        with(response){
            if (success){
                when(key){
                    0 -> {
                        val list = castListModel<BatchModel>()
                        if (!packingListLoaded.any { it.dolsNumber == list[0].transDestNumber!!}){

                            val list2 = list.groupBy { it.prodCode }
                            val groupList = mutableListOf<PackingListCheckerObject>()
                            list2.forEach { (_, v) ->
                                groupList.add(
                                    PackingListCheckerObject(
                                        v[0].prodCode ?: "null",
                                        v[0].prodName ?: "null",
                                        v, mutableListOf())
                                )
                            }

                            val date = list[0].transDestDate!!.replace("00:00:00", "")
                            bind.date.text = date

                            DolsListModel(
                                list[0].transDestNumber!!,
                                list[0].transDestDate!!,
                                groupList
                            ).let {
                                packingListLoaded.add(it)
                                packingListLoadedFiltered.add(it)
                            }
                        }

                        if (packingList.size > 1) {
                            countLoad = 1
                            nextLoad()
                        }else{
                            pAdapter.notifyDataSetChanged()
                            wait.hide()
                            tos.success("Selesai")
                        }
                    }
                    1 -> {
                        val list = castListModel<BatchModel>()
                        if (!packingListLoaded.any { it.dolsNumber == list[0].transDestNumber!!}){
                            val list2 = list.groupBy { it.prodCode }
                            val groupList = mutableListOf<PackingListCheckerObject>()
                            list2.forEach { (_, v) ->
                                groupList.add(
                                    PackingListCheckerObject(
                                        v[0].prodCode ?: "null",
                                        v[0].prodName ?: "null",
                                        v, mutableListOf())
                                )
                            }

                            DolsListModel(
                                list[0].transDestNumber!!,
                                list[0].transDestDate!!,
                                groupList
                            ).let {
                                packingListLoaded.add(it)
                                packingListLoadedFiltered.add(it)
                            }
                        }

                        if (countLoad < (packingList.size-1)) {
                            countLoad++
                            nextLoad()
                        }else{
//                            val rfids = mutableListOf(
//                                "E2003412013318000510C59A","E2003412013218000510C85F","E2003412013D18000510C77A","E2003412012C18000510C70D"
//                            )
//
//                            val batchList = mutableListOf(
//                                "D23B0109.01.0202.002","D23B0109.01.0202.054","D23B0109.01.0202.055","D23B0109.01.0202.060"
//                            )
//
//                            var i = 0
//                            packingListLoadedFiltered.forEach { dlm ->
//                                dlm.plList.forEach { pl ->
//                                    pl.batchList.forEach { b ->
//                                        if (batchList.contains(b.batchNo)){
//                                            b.tid = rfids[i]
//                                            i++
//                                        }
//                                    }
//                                }
//                            }

                            pAdapter.notifyDataSetChanged()
                            wait.hide()
                        }
                    }
                    else -> {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
            else{
                wait.hide()
                tos.error(errorMsg)
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class Adapter: RecyclerView.Adapter<Adapter.Holder>(){
        inner class Holder(val v: BeanPackingListCheckerBinding): RecyclerView.ViewHolder(v.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanPackingListCheckerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val pack = packingListLoadedFiltered[position]

            with(holder){
                v.prodCode.gone(true)
                v.prodName.gone(true)

                (position+1).toString().also { v.no.text = it }
                v.botSpace.gone((position+1) != itemCount)

                v.dolNo.text = pack.dolsNumber
                v.prodQty.text = pack.plList.sumOf { it.batchList.size }.toString()
                pack.plList.forEach {
                    it.batchList.forEach { b ->
                        if (b.tid != null && !it.detectedList.contains(b.tid)){
                            if (detectionList.contains(b.tid)) it.detectedList.add(b.tid!!)
                            else it.detectedList.remove(b.tid)
                        }
                    }
                }
                v.prodDetect.text = pack.plList.sumOf { it.detectedList.size }.toString()

                val groupAdapter = PackingListGroupAdapter(pack.plList)
                v.batchRv.apply {
                    layoutManager = LinearLayoutManager(this@PackingListChecker)
                    adapter = groupAdapter
                }

                bind.tvQtyBatch.text = packingListLoaded.sumOf { it.plList.sumOf { p -> p.batchList.size } }.toString()
                bind.tvCountDetected.text = packingListLoaded.sumOf { it.plList.sumOf { p -> p.detectedList.size } }.toString()
            }

            holder.itemView.setOnClickListener {
                holder.v.batchRv.gone(holder.v.batchRv.visibility == View.VISIBLE)
            }


        }

        override fun getItemCount() = packingListLoadedFiltered.size

    }
    inner class PackingListGroupAdapter(val list: List<PackingListCheckerObject>): RecyclerView.Adapter<PackingListGroupAdapter.Holder>(){
        inner class Holder(val v: BeanPackingListCheckerGroupBinding): RecyclerView.ViewHolder(v.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanPackingListCheckerGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val bl = list[position]
            with(holder.v){
                prodName.text = bl.batchList[0].prodName

                val prodNumb = bl.batchList[0].prdNumber
                    ?: Format.getCPNumberFromBatch(bl.batchList[0].batchNo!!) ?: "null"
                prodNumber.text = prodNumb

                chipGroup.removeAllViews()
                bl.batchList.forEach { cp ->
                    val secColor = getColor(R.color.secondaryTextLight)
                    val primColor = getColor(R.color.primaryColor)
                    detectionList.contains(cp.tid).let { d ->
                        val c = ItemChipsBinding.inflate(
                            LayoutInflater.from(this@PackingListChecker),
                            root,
                            false
                        )
                        with(c.root) {
                            setTextColor(
                                ColorStateList.valueOf(
                                    if (cp.tid != null) if (d) Color.WHITE else primColor
                                    else secColor
                                )
                            )
                            chipStrokeColor = ColorStateList.valueOf( if (cp.tid != null) secColor else Color.TRANSPARENT)

                            chipBackgroundColor = ColorStateList.valueOf(
                                if (cp.tid != null) if (d) primColor else Color.WHITE
                                else getColor(R.color.tertiaryTextLight)
                            )

//                            bl.detectedList.apply {
//                                if(d) add(cp.tid!!)
//                                else remove(cp.tid)
//                            }

                            val batchShorted = cp.batchNo?.replace(prodNumb, "")
                            batchShorted.also { s -> text = s }

                            setOnClickListener {
                                pop.cpDetailsPop2(cp.batchNo!!, root, cp)
                            }
                            chipGroup.addView(this)
                        }
                    }
                }
            }
        }
        override fun getItemCount() = list.size
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

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(0)
        msg.obj = epc._TID
        handler.sendMessage(msg)
    }

//    fun outPutEpc(epc: String){
//        val msg = handler.obtainMessage(0)
//        msg.obj = epc
//        handler.sendMessage(msg)
//    }
    // </editor-fold>

    data class DolsListModel(
        val dolsNumber: String,
        val dolsDate: String,
        val plList: MutableList<PackingListCheckerObject> = mutableListOf()
    )

    data class PackingListCheckerObject(
        val prodCode: String,
        val prodName: String,
        val batchList: List<BatchModel> = mutableListOf(),
        val detectedList: MutableList<String> = mutableListOf(),
    )
}