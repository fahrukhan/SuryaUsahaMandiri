package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.databinding.ActivityTagRegisterHistoryBinding
import com.rfid.sum.databinding.BeanCpBatchChooserRvBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import java.util.*


class TagRegisterHistory : BaseBinding<ActivityTagRegisterHistoryBinding>(), Pop.PopListener {
    override fun getViewBinding() = ActivityTagRegisterHistoryBinding.inflate(layoutInflater)
    override fun useReader() = false
    private var selectedDate = ""
    private var selectedCp = ""
    private var productList = mutableListOf<BatchModel>()
    private val batchSelectedList = mutableListOf<BatchModel>()
    private val batchSelectedFilteredList = mutableListOf<BatchModel>()
    private val batchAdapter = BatchAdapter()
    private lateinit var pop: Pop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initHis()
    }

    private fun initHis() {
        pop = Pop(this)

        with(bind) {
            rvBatches.layoutManager = LinearLayoutManager(this@TagRegisterHistory)
            rvBatches.adapter = batchAdapter

            Calendar.getInstance().let {
                tvDate.text = Format.calendarString(it, "dd-MM-yyyy")
                selectedDate = Format.calendarString(it, "yyyy-MM-dd")
            }

            btnDate.setOnClickListener {
                val datePicker =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select date")
                        .build()
                datePicker.show(supportFragmentManager, "tag")
                datePicker.addOnPositiveButtonClickListener {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it
                    val calFirstStr = Format.calendarString(cal, "dd-MM-yyyy")
                    tvDate.text = calFirstStr
                    selectedDate = Format.calendarString(cal, "yyyy-MM-dd")
                    loadRegisteredProduct()
                }
            }
            spCpNumber.setOnClickListener {
                pop.popProductList("CP Number", root, productList, this@TagRegisterHistory)
            }
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onQueryTextChange(newText: String?): Boolean {
                    val newLIst = batchSelectedList.filter {
                        it.batchNo!!.contains(newText!!, true).or(
                            it.prodName!!.contains(newText, true)
                        )
                    }
                    batchSelectedFilteredList.clear()
                    batchSelectedFilteredList.addAll(newLIst)
                    batchAdapter.notifyDataSetChanged()
                    return true
                }
            })
        }
        loadRegisteredProduct()
    }

    private fun loadRegisteredProduct() {
        if (selectedDate.isEmpty()){
            return
        }
        wait.show()
        val id = "prdnmbr?registered_at=$selectedDate"
        volley.getDataById(Url.ITEMS, id, object : NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<BatchModel>()
                        productList.clear()
                        if (list.isNotEmpty()){
                            productList.addAll(list)
                            bind.spCpNumber.isEnabled = true
                        }else{
                            tos.info("No product loaded.")
                            bind.spCpNumber.isEnabled = false
                        }
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun loadRegisteredProductByCp(cp: String) {
        if (selectedDate.isEmpty() || cp.isEmpty()){
            return
        }

        wait.show()
        val id = "prdnmbr/${cp}?registered_at=$selectedDate"
        volley.getDataById(Url.ITEMS, id, object : NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        Logger.info(data.toString())
                        val list = castListModel<BatchModel>()
                        batchSelectedList.clear()
                        batchSelectedFilteredList.clear()

                        if (list.isNotEmpty()){
                            batchSelectedList.addAll(list)
                            batchSelectedFilteredList.addAll(list)
                            bind.spCpNumber.isEnabled = true
                        }else{
                            tos.info("No batch loaded.")
                            bind.spCpNumber.isEnabled = false
                        }

                        bind.tvTotalBatch.text = batchSelectedFilteredList.size.toString()
                        batchAdapter.notifyDataSetChanged()
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    override fun onCpSelected(cp: String) {
        selectedCp = cp
        bind.spCpNumber.text = selectedCp
        loadRegisteredProductByCp(selectedCp)
    }

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class BatchAdapter() : RecyclerView.Adapter<BatchAdapter.Holder>() {
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
            val gcp = batchSelectedFilteredList[position]

            with(holder.b) {
                //(position+1).toString().also { noTv.text = it }
                "${position + 1}".also { no.text = it }

                val params: LinearLayout.LayoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 10, 0, 0)
                batchNo.layoutParams = params
                batchNo.text = gcp.batchNo

                val params2: LinearLayout.LayoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params2.setMargins(0, 0, 0, 10)
                prodName.layoutParams = params2
                prodName.text = gcp.prodName
                prodName.setTextColor(ColorStateList.valueOf(getColor(R.color.secondaryTextLight)))

                batchCb.gone(true)
//                batchCb.setOnCheckedChangeListener { _, isChecked ->
//                    if (isChecked) {
//                        if (!selectedCpBatch.contains(gcp.batchNo)) selectedCpBatch.add(gcp.batchNo!!)
//                        if (bind.nextBtn.visibility != View.VISIBLE) nextBtnSetup()
//                    } else {
//                        selectedCpBatch.remove(gcp.batchNo)
//                        if (bind.nextBtn.visibility == View.VISIBLE) nextBtnSetup()
//                    }
//
//                    Logger.info(selectedCpBatch)
//                }
//                batchCb.isChecked = selectedCpBatch.contains(gcp.batchNo)

                root.setOnClickListener {
                    pop.cpDetailsPop2("Batch Details", bind.root, gcp)
                }
            }
        }

        override fun getItemCount() = batchSelectedFilteredList.size
    }
    // </editor-fold>
}

fun main() {
    val a = "D22L0692.01.0101.003|B11A40487D0033|1616 NEW 40033 DOUBLING|0033|1616 NEW|30.0000|M\n"
    val b = a.trim()
    println(b)
}