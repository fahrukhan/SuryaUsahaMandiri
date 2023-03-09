package com.rfid.sum.activities.ui.packing

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.OutboundModel
import com.rfid.sum.databinding.ActivityOutboundHistoryBinding
import com.rfid.sum.databinding.BeanCpBatchChooserRvBinding
import com.rfid.sum.databinding.BeanOutboundHistoryBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Ui
import java.util.*

class OutboundHistory : BaseBinding<ActivityOutboundHistoryBinding>() {
    override fun getViewBinding() = ActivityOutboundHistoryBinding.inflate(layoutInflater)
    override fun useReader() = false

    private var selectedDate = ""
    private val historyList = mutableListOf<OutboundModel>()
    private val hAdapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initHis()
    }

    private fun initHis() {

        with(bind) {

            rvOutbound.apply {
                layoutManager = LinearLayoutManager(this@OutboundHistory)
                adapter = hAdapter
            }

            datePicker.setOnClickListener {
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
                    loadOutboundHistory()
                }
            }

            val date = Calendar.getInstance()
            selectedDate = Format.calendarString(date, "yyyy-MM-dd")
            tvDate.text = Format.calendarString(date, "dd-MM-yyyy")
            loadOutboundHistory()
        }
    }

    private fun loadOutboundHistory() {
        wait.show()
        val url = "${Url.OUTBOUNDS}?date=$selectedDate"
        volley.getData(url, object : NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<OutboundModel>()
                        if (list.isNotEmpty()){
                            historyList.addAll(list)
                            hAdapter.notifyDataSetChanged()
                        }
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        },session.getUser()?.token)
    }


    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class Adapter: RecyclerView.Adapter<Adapter.Holder>(){
        inner class Holder(val v: BeanOutboundHistoryBinding): RecyclerView.ViewHolder(v.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanOutboundHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val his = historyList[position]
            with(holder.v){
                (position+1).toString().also { no.text = it }

                dolNo.text = his.receiptNumber
                dolDate.text = his.at
            }

            holder.itemView.setOnClickListener {
                val gson = Gson().toJson(his)
                Ui.intent<OutboundHistoryDetails>(this@OutboundHistory, gson)
            }
            bind.tvTotalPl.text = itemCount.toString()
        }

        override fun getItemCount() = historyList.size
    }
    // </editor-fold>
}
