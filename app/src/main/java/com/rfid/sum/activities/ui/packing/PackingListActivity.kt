package com.rfid.sum.activities.ui.packing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.PackingListModel
import com.rfid.sum.databinding.ActivityPackingListBinding
import com.rfid.sum.databinding.BeanCpBatchChooserRvBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Ui
import java.util.*

class PackingListActivity : BaseBinding<ActivityPackingListBinding>() {
    override fun getViewBinding() = ActivityPackingListBinding.inflate(layoutInflater)
    override fun useReader() = false
    private var selectedDate: String? = null
    private val plAdapter = PackingAdapter()
    private val packingListSynced = mutableListOf<PackingListModel>()
    private val packingListSyncedFiltered = mutableListOf<PackingListModel>()
    private val selectedDoList = mutableListOf<String>()
    private var syncBatchLoadedCount = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPl()
    }

    private fun initPl() {

        with(bind){
            packingListSyncedRv.apply {
                layoutManager = LinearLayoutManager(this@PackingListActivity)
                adapter = plAdapter
            }
            calendarBtn.setOnClickListener {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .build()
                datePicker.show(supportFragmentManager, "tag")
                datePicker.addOnPositiveButtonClickListener {
                    val calFirst = Calendar.getInstance()
                    calFirst.timeInMillis = it
                    val calFirstStr = Format.calendarString(calFirst, "dd-MM-yyyy")
                    calendarBtn.text = calFirstStr
                    selectedDate = Format.calendarString(calFirst, "yyyyMMdd")
                    loadPackingList()
                }
            }
            nextBtn.setOnClickListener {
                if (selectedDoList.isEmpty()) return@setOnClickListener
                val gson = Gson().toJson(selectedDoList)
                Ui.intent<PackingListChecker>(this@PackingListActivity, gson!!)
                //tos.info(gson)
            }
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?) = true
                @SuppressLint("NotifyDataSetChanged")
                override fun onQueryTextChange(newText: String?): Boolean {
                    val list = packingListSynced.filter { it.transdestnmbr!!.contains(newText!!, true) }
                    packingListSyncedFiltered.clear()
                    packingListSyncedFiltered.addAll(list)
                    plAdapter.notifyDataSetChanged()
                    return true
                }
            })
            syncBtn.setOnClickListener {
                if(selectedDate == null) {
                    tos.info("Please select date!")
                    return@setOnClickListener
                }
                startSync()
            }

            btnHistory.setOnClickListener {
                Ui.intent<OutboundHistory>(this@PackingListActivity)
            }
        }
    }

    private fun startSync(){
        wait.show("Synchronize...")
        val id = "sync?date=$selectedDate"
        println(Url.PACKING_LIST+id)
        volley.getDataById(Url.PACKING_LIST, id, object: NciApiListener{
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
        val id = "sync?date=$selectedDate"
        println(Url.PACKING_LIST+id)
        volley.getDataById(Url.PACKING_LIST, id, object: NciApiListener{
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
                            loadPackingList()
                        }
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token, 0, 24000, 1)
    }

    private fun loadPackingList() {
        if (selectedDate == null) tos.info("Please select date!").also { return }

        wait.show("Load packing list...")
        val id = "transdestnmbr?date=$selectedDate"
        volley.getDataById(Url.PACKING_LIST, id, object: NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<PackingListModel>()
                        packingListSynced.clear()

                        if (list.isNotEmpty()) {
                            packingListSynced.addAll(list)
                            packingListSyncedFiltered.addAll(list)
                        }
                        else tos.info("No packing list loaded!")
                        plAdapter.notifyDataSetChanged()
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun nextBtnSetup(){
        (selectedDoList.size > 0).let { b ->
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

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    //packing list synced
    inner class PackingAdapter() : RecyclerView.Adapter<PackingAdapter.Holder>() {
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
            val pl = packingListSyncedFiltered[position]

            with(holder.b) {
                //(position+1).toString().also { noTv.text = it }
                "${position + 1}".also { no.text = it }
                "${pl.transdestnmbr} (${pl.qty})".also { batchNo.text = it }
                prodName.text = pl.prodname
                //prodName.gone(true)

                batchCb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedDoList.contains(pl.transdestnmbr)) selectedDoList.add(pl.transdestnmbr!!)
                        if (bind.nextBtn.visibility != View.VISIBLE) nextBtnSetup()
                    } else {
                        selectedDoList.remove(pl.transdestnmbr)
                        if (bind.nextBtn.visibility == View.VISIBLE) nextBtnSetup()
                    }

                    Logger.info(selectedDoList)
                }
                batchCb.isChecked = selectedDoList.contains(pl.transdestnmbr)
                root.setOnClickListener {
                    //pop.cpDetailsPop2("Batch Details", bind.root, gcp)
                }
            }
        }

        override fun getItemCount() = packingListSyncedFiltered.size
    }
    // </editor-fold>

}