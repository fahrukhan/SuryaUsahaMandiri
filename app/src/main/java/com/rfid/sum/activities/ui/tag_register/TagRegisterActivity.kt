package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.pda.rfid.EPCModel
import com.rfid.sum.R
import com.rfid.sum.SumApplication
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.controller.UHFReader
import com.rfid.sum.controller.UHFScanning
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.room.DaoViewModel
import com.rfid.sum.data.room.DaoViewModelFactory
import com.rfid.sum.data.room.model.SumApiHistoryModel
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.data.support.TokenInitialize
import com.rfid.sum.databinding.ActivityTagRegisterBinding
import com.rfid.sum.databinding.BeanCpBatchChooserRvBinding
import com.rfid.sum.rest.ApiRequest
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.SumApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.rest.model.TokenModel
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import com.rfid.sum.utils.Ui
import org.json.JSONObject
import java.util.*

class TagRegisterActivity : BaseBinding<ActivityTagRegisterBinding>(),
    UHFScanning, Pop.PopListener, PopupMenu.OnMenuItemClickListener {
    override fun getViewBinding() = ActivityTagRegisterBinding.inflate(layoutInflater)
    override fun useReader() = false

    //private val cpBatchList = mutableListOf<GetCPBatchModel>()
    private var cpGroup: MutableMap<String, List<BatchModel>> = mutableMapOf()
    private var cpListFiltered = mutableListOf<BatchModel>()
    private var cpListSelected = mutableListOf<BatchModel>()
    private var productList = mutableListOf<BatchModel>()

    private var selectedDate: String? = null
    private lateinit var pop: Pop
    private val adapter = CpBatchChooserAdapter()
    private var isScanning = false
    private var selectedMode = MANUAL_REG
    private var paramList: MutableMap<String, SetBatchParamList> = mutableMapOf()

    //private var pickedArray = mutableListOf<Long>()
    private var timePickedToPostArray = mutableMapOf<String, String>()
    private val selectedCpBatch = mutableListOf<String>()
    private var syncBatchLoadedCount = 0;

//    private val dao: DaoViewModel by viewModels {
//        DaoViewModelFactory((application as SumApplication).repository)
//    }

    companion object {
        const val MANUAL_REG = 0
        const val AUTO_REG = 1
    }

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                selectedMode -> {
                    val epc = msg.obj as EPCModel
                    setRfidToBatch(epc, selectedMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)

        initTagReg()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun initTagReg() {
        pop = Pop(this)
//        val c = UHFReader.connectReader(this)
//        if (!c.success) {
//            tos.error(c.message)
//            return
//        }

        UHFReader.listener = this
        UHFReader.setAntennaPower(5)

        with(bind) {
            cpRv.layoutManager = LinearLayoutManager(this@TagRegisterActivity)
            cpRv.adapter = adapter

            val popMenu = PopupMenu(this@TagRegisterActivity, moreIc)
            popMenu.menuInflater.inflate(R.menu.tag_reg_menu, popMenu.menu)
            popMenu.setOnMenuItemClickListener(this@TagRegisterActivity)

            scanBtn.setOnClickListener {
                if (isScanning) {
                    UHFReader.stop()
                    selectedMode = MANUAL_REG
                } else {
                    selectedMode = AUTO_REG
                    UHFReader.startScan()
                }
                isScanning = !isScanning
                //scanBtn.showLoadingAnimation(isScanning)
                adapter.notifyDataSetChanged()
            }

            popCpBtn.setOnClickListener {
//                cpGroup = cpBatchList.groupBy { it.prdNmbr!! }
//                val sortList = mutableListOf<GetCPBatchModel>()
//                cpGroup.forEach { (_, list) ->
//                    val g = list[0]
//                    g.totalBatch = list.size
//                    sortList.add(list[0])
//                }
//                println(sortList)
                pop.popProductList("CP Number", root, productList, this@TagRegisterActivity)
            }

//            saveBtn.setOnClickListener {
//                if (!preSaving()) {
//                    return@setOnClickListener
//                }
//                loadToken(ApiRequest.SetBatchRFID)
//            }

            calendarBtn.setOnClickListener {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
/**                        .setSelection(
//                            Pair(
//                                MaterialDatePicker.thisMonthInUtcMilliseconds(),
//                                MaterialDatePicker.todayInUtcMilliseconds()
//                            )
                        ) **/
                        .build()
                datePicker.show(supportFragmentManager, "tag")
                datePicker.addOnPositiveButtonClickListener {
                    //pickedArray.clear()
                    timePickedToPostArray.clear()
                    val calFirst = Calendar.getInstance()
                    val calSecond = Calendar.getInstance()
                    calFirst.timeInMillis = it
                    calSecond.timeInMillis = it

                    val calFirstStr = Format.calendarString(calFirst, "dd/MM/yyyy")
                    timePickedToPostArray["StartDate"] = Format.calendarString(calFirst, "yyyyMMdd")
                    timePickedToPostArray["EndDate"] = Format.calendarString(calSecond, "yyyyMMdd")
                    calendarBtn.text = calFirstStr
                    selectedDate = Format.calendarString(calFirst, "yyyyMMdd")
                    println(timePickedToPostArray)

                    loadProduct()
                    //loadToken(ApiRequest.GetCPBatchTrans)
                }
            }

            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    val newLIst = cpListSelected.filter {
                        it.batchNo!!.contains(newText!!, true).or(
                            it.prodName!!.contains(newText, true)
                        )
                    }
                    cpListFiltered.clear()
                    cpListFiltered.addAll(newLIst)
                    adapter.notifyDataSetChanged()
                    return true
                }
            })

            selectAllCb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cpListFiltered.forEach { cp ->
                        selectedCpBatch.let { if (!it.contains(cp.batchNo)) it.add(cp.batchNo!!) }
                    }
                }
                else selectedCpBatch.clear()
                nextBtnSetup()
                adapter.notifyDataSetChanged()
            }

            nextBtn.setOnClickListener {
                val list = cpListSelected.filter { selectedCpBatch.contains(it.batchNo) }

                val gson = Gson().toJson(list)
                Ui.intent<TagRegistrationQR>(this@TagRegisterActivity, gson)

            }

            syncBtn.setOnClickListener {
                if(selectedDate == null) {
                    tos.info("Please select date!")
                    return@setOnClickListener
                }
                startSync()
            }
        }
    }

    private fun startSync(){
        wait.show("Synchronize...")
        val id = "?date=$selectedDate"
        println(Url.BATCH_SYNC+id)
        volley.getDataById(Url.BATCH_SYNC, id, object: NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list =  castListModel<String>()
                        if (list.isNotEmpty()){
                            syncBatchLoadedCount += list.size
                            nextSync()
                        }else tos.info("Tidak ada data.");
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token, 0, 24000, 1)
    }

    private fun nextSync(){
        wait.show("Synchronize...\n$syncBatchLoadedCount batches loaded")
        val id = "?date=$selectedDate"
        println(Url.BATCH_SYNC+id)
        volley.getDataById(Url.BATCH_SYNC, id, object: NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list =  castListModel<String>()
                        if (list.isNotEmpty()){
                            syncBatchLoadedCount += list.size
                            nextSync()
                        }else {
                            syncBatchLoadedCount = 0
                            tos.info("Sinkronisasi Selesai")
                            loadProduct()
                        }
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token, 0, 24000, 1)
    }

    private fun loadProduct() {
        if (selectedDate == null) return

        wait.show("Load product...")
        "Select Cp".also { bind.popCpBtn.text = it }
        val id = "?date=$selectedDate"
        //println(Url.PRODUCT_SYNCED+"/"+id)
        volley.getDataById(Url.PRODUCT_SYNCED, id, object: NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<BatchModel>()
                        productList.clear()
                        if (list.isNotEmpty()){
                            productList.addAll(list)
                            bind.popCpBtn.isEnabled = true
                        }else{
                            tos.info("No product loaded.")
                            bind.popCpBtn.isEnabled = false
                        }

                        cpListSelected.clear()
                        cpListFiltered.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun preSaving(): Boolean {
        if (paramList.isEmpty()) {
            tos.info("No RFID to save!")
            return false
        }
        if (timePickedToPostArray.isEmpty()) {
            tos.info("Please select the date range.")
            return false
        }

        return true
    }

//    private fun loadToken(ar: ApiRequest) {
//        tokenMgt.getToken(ar, object : TokenInitialize {
//            override fun activeToken(mToken: TokenModel?) {
//                wait.show()
//                when (ar) {
//                    ApiRequest.GetCPBatchTrans -> loadCpBatch(mToken?.token!!)
//                    ApiRequest.SetBatchRFID -> postBatchRfid(mToken?.token!!)
//                    else -> { /* no-op */
//                    }
//                }
//            }
//
//            override fun freshToken(mToken: TokenModel?) {
//                when (ar) {
//                    ApiRequest.GetCPBatchTrans -> loadCpBatch(mToken?.token!!)
//                    ApiRequest.SetBatchRFID -> postBatchRfid(mToken?.token!!)
//                    else -> { /* no-op */
//                    }
//                }
//            }
//
//            override fun refreshingToken() {
//                wait.show("Refreshing token...")
//            }
//
//            override fun failedRefresh(msg: String) {
//                wait.hide()
//                tos.error(msg)
//            }
//        })
//    }

//    private fun postBatchRfid(token: String) {
//        wait.updateMsg("Saving RFID Data...")
//
//        val paramArray = mutableListOf<SetBatchParamList>()
//        paramList.forEach { (_, v) ->
//            paramArray.add(v)
//        }
//
//        val param =
//            paramBuilder.getParam(ApiRequest.SetBatchRFID, token, parameterList = paramArray)
//        val gson = Gson().toJson(param)
//
//        volley.sumPostData(Url.GET_API, JSONObject(gson), object : SumApiListener {
//            override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
//                with(response) {
//                    if (success) {
//                        wait.hide()
//                        tos.success(msgDesc!!)
//                        dao.saveApiHistory(
//                            SumApiHistoryModel(
//                                api = ApiRequest.SetBatchRFID.toString(),
//                                method = Key.getMethod(ApiRequest.SetBatchRFID),
//                                request = paramBuilder.removedTokenParams(param)!!,
//                                response = responseToHistory()
//                            )
//                        )
//                    } else {
//                        wait.hide()
//                        if (msgCode == 104) {
//                            tos.info("Token Expired!")
//                            tokenMgt.removeToken(Key.getMethod(ApiRequest.SetBatchRFID)).also {
//                                loadToken(ApiRequest.SetBatchRFID)
//                            }
//                        } else {
//                            tos.error(errorMsg)
//                        }
//                    }
//                }
//            }
//        })
//    }

//    fun loadCpBatch(token: String) {
//        wait.updateMsg("Load CP Batch...")
//
//        val params = paramBuilder.getParam(
//            ApiRequest.GetCPBatchTrans,
//            token,
//            parameter = timePickedToPostArray
//        )
//        val gson = Gson().toJson(params)
//
//        volley.sumPostData(Url.GET_API, JSONObject(gson), object : SumApiListener {
//            @SuppressLint("NotifyDataSetChanged")
//            override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
//                with(response) {
//                    if (success) {
//                        wait.hide()
//                        val list = castListModel<GetCPBatchModel>()
//                        if (list.isNotEmpty()) {
//                            cpBatchList.addAll(list)
//                            bind.popCpBtn.isEnabled = cpBatchList.size > 0
//                            adapter.notifyDataSetChanged()
//
//                            dao.saveApiHistory(
//                                SumApiHistoryModel(
//                                    api = ApiRequest.SetBatchRFID.toString(),
//                                    method = Key.getMethod(ApiRequest.SetBatchRFID),
//                                    request = paramBuilder.removedTokenParams(params)!!,
//                                    response = responseToHistory()
//                                )
//                            )
//                        }
//                    } else {
//                        wait.hide()
//                        if (msgCode == 104) {
//                            tos.info("Token Expired!")
//                            tokenMgt.removeToken(Key.getMethod(ApiRequest.GetCPBatchTrans)).also {
//                                loadToken(ApiRequest.GetCPBatchTrans)
//                            }
//                        } else {
//                            tos.error(errorMsg)
//                        }
//                    }
//                }
//            }
//        })
//    }

    inner class CpBatchChooserAdapter() : RecyclerView.Adapter<CpBatchChooserAdapter.Holder>() {
        inner class Holder(val b: BeanCpBatchChooserRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = BeanCpBatchChooserRvBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val gcp = cpListFiltered[position]

            with(holder.b) {
                //(position+1).toString().also { noTv.text = it }
                "${position + 1}".also { no.text = it }
                batchNo.text = gcp.batchNo
                prodName.text = gcp.prodName

                batchCb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedCpBatch.contains(gcp.batchNo)) selectedCpBatch.add(gcp.batchNo!!)
                        if (bind.nextBtn.visibility != View.VISIBLE) nextBtnSetup()
                    } else {
                        selectedCpBatch.remove(gcp.batchNo)
                        if (bind.nextBtn.visibility == View.VISIBLE) nextBtnSetup()
                    }

                    Logger.info(selectedCpBatch)
                }
                batchCb.isChecked = selectedCpBatch.contains(gcp.batchNo)

                root.setOnClickListener {
                    pop.cpDetailsPop2("Batch Details", bind.root, gcp)
                }
            }
        }

        override fun getItemCount() = cpListFiltered.size
    }

    private var batchIndexSelected: Int = -1

//    inner class CpBatchAdapter() : RecyclerView.Adapter<CpBatchAdapter.Holder>() {
//        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
//            //val noTv: TextView = v.findViewById(R.id.no)
//            val batchNoTv: TextView = v.findViewById(R.id.batchNo)
//            val prodName: TextView = v.findViewById(R.id.prodName)
//            val rfidTv: TextView = v.findViewById(R.id.rfid)
//            val scanBtn: MaterialButton = v.findViewById(R.id.scanBtn)
//            val progress: ProgressBar = v.findViewById(R.id.progress)
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
//            val v = LayoutInflater.from(parent.context)
//                .inflate(R.layout.bean_cp_batch_rv, parent, false)
//            return Holder(v)
//        }
//
//        @SuppressLint("NotifyDataSetChanged")
//        override fun onBindViewHolder(holder: Holder, position: Int) {
//            val gcp = cpListFiltered[position]
//
//            with(holder) {
//                //(position+1).toString().also { noTv.text = it }
//                batchNoTv.text = gcp.batchNo
//                prodName.text = gcp.prodName
//
//                fun setBeanBtn() {
//                    if (selectedMode == MANUAL_REG) {
//                        Ui.show(scanBtn)
//                        Ui.gone(progress)
//                    } else {
//                        Ui.show(progress)
//                        Ui.gone(scanBtn)
//                    }
//                }
//
//                if (gcp.rfid != null) {
//                    rfidTv.text = gcp.rfid
//                    scanBtn.icon =
//                        ContextCompat.getDrawable(this@TagRegisterActivity, R.drawable.ic_clear_24)
//                    Ui.show(scanBtn)
//                    Ui.gone(progress)
//                } else {
//                    rfidTv.text = getString(R.string.minus)
//                    scanBtn.icon = ContextCompat.getDrawable(
//                        this@TagRegisterActivity,
//                        R.drawable.ic_play_arrow_24
//                    )
//                    setBeanBtn()
//                }
//
//                scanBtn.setOnClickListener {
//                    selectedMode = MANUAL_REG
//                    if (gcp.rfid == null) {
//                        batchIndexSelected = adapterPosition
//                        Reader.startSingleScan()
//                    } else {
//                        batchIndexSelected = -1
//                        gcp.rfid = null
//                        if (paramList.containsKey(gcp.batchNo)) {
//                            paramList.remove(gcp.batchNo)
//                        }
//                        notifyDataSetChanged()
//                    }
//
//
//                }
//
//                itemView.setOnClickListener {
//                    pop.cpDetailsPop("Batch Details", bind.root, gcp)
//                }
//            }
//        }
//
//        override fun getItemCount(): Int {
//            return cpListFiltered.size
//        }
//    }

    private fun nextBtnSetup(){
        (selectedCpBatch.size > 0).let { b ->
            with(bind){
                if (b && nextBtn.visibility != View.VISIBLE){
                    YoYo.with(Techniques.FadeInRight).duration(200)
                        .onStart { nextBtn.invisible(false) }
                        .playOn(nextBtn)
                }
                if (!b && nextBtn.visibility == View.VISIBLE){
                    YoYo.with(Techniques.FadeOutRight).duration(200)
                        .onEnd { nextBtn.invisible(true) }
                        .playOn(nextBtn)
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Pop Interface">

    @SuppressLint("NotifyDataSetChanged")
    override fun onCpSelected(cp: String) {
        val scp = productList.find { it.prdNumber == cp }
        "$cp (${scp!!.qty})".also { bind.popCpBtn.text = it }

        if(cpGroup.containsKey(cp) && (cpGroup[cp]?.size ?: 0) == scp.qty!!){
            loadBatches(cp)
//            if ((cpGroup[cp]?.size ?: 0) == scp.qty!!){
//                loadBatches(cp)
//            }else{
//                getBatches(cp)
//            }
        }else{
            getBatches(cp)
        }

        bind.selectAllCb.isChecked = false
        Logger.info(cp)
        adapter.notifyDataSetChanged()
    }

    private fun getBatches(cp: String) {
        wait.show("Load batches...")
        volley.getDataById(Url.PRODUCT_SYNCED, cp, object: NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<BatchModel>()
                        if (list.isNotEmpty()){
                            if (cpGroup.containsKey(cp)){
                                cpGroup.remove(cp)
                            }
                            cpGroup[cp] = list
                            adapter.notifyDataSetChanged()
                        }else{
                            tos.info("no batches loaded")
                        }

                        loadBatches(cp)
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun loadBatches(cp: String) {
        cpListFiltered.clear()
        cpListSelected.clear()
        cpListFiltered.addAll(cpGroup[cp]!!)
        cpListSelected.addAll(cpGroup[cp]!!)
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UHF Interface">
    override fun outPutEpc(epc: EPCModel) {
        val msg = handler.obtainMessage(selectedMode)
        msg.obj = epc
        handler.sendMessage(msg)
    }
    // </editor-fold>

    @SuppressLint("NotifyDataSetChanged")
    private fun setRfidToBatch(epc: EPCModel, i: Int) {
        when (i) {
            MANUAL_REG -> {
                val batchNo = cpListFiltered[batchIndexSelected].batchNo

                if (cpListFiltered.find { it.tid == epc._EPC } != null) {
                    val m = cpListFiltered.find { it.tid == epc._EPC }
                    if (m?.tid != cpListFiltered[batchIndexSelected].tid) {
                        tos.info("RFID: ${epc._EPC} \nalready selected by ${m?.batchNo}")
                    }
                    return
                }

                if (batchIndexSelected > -1) {
                    cpListFiltered[batchIndexSelected].tid = epc._EPC
                    paramList[batchNo!!] = SetBatchParamList(batchNo, epc._EPC)
                }
//                else {
//                    cpListFiltered[batchIndexSelected].tid = null
//                }

                adapter.notifyDataSetChanged()
            }
            AUTO_REG -> {
                Logger.info("AUTO REG => ${epc._EPC}")
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Lifecycle">
    override fun onPause() {
        super.onPause()
        UHFReader.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        UHFReader.dispose()
    }
    // </editor-fold>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tag_reg_menu, menu)
        val cb = menu.findItem(R.id.regMode).actionView as CheckBox
        cb.setOnCheckedChangeListener { _, b ->
            if (b) {
                tos.info("AUTO")
            } else {
                tos.info("MANUAL")
            }
        }

        return true
    }

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        when (p0?.itemId) {
            R.id.regMode -> {

            }
        }
        return true
    }

    data class SetBatchParamList(
        val BatchNo: String,
        val RFID: String
    )

}