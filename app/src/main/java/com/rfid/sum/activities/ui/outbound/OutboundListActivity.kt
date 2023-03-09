package com.rfid.sum.activities.ui.outbound

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.data.constant.Url
import com.rfid.sum.databinding.ActivityOutboundListBinding
import com.rfid.sum.databinding.BeanOutboundTransactionBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response

class OutboundListActivity : BaseBinding<ActivityOutboundListBinding>() {
    override fun getViewBinding() = ActivityOutboundListBinding.inflate(layoutInflater)
    override fun useReader() = false
    private val outList = mutableListOf<BeanOutboundModel>()
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initOutList()
    }

    private fun initOutList() {

        with(bind){
            outboundRv.apply {
                layoutManager = LinearLayoutManager(this@OutboundListActivity)
                adapter = this@OutboundListActivity.adapter
            }

        }
        loadOutTrx()
    }

    private fun loadOutTrx() {
        wait.show("Load Outbound Transaction..")
        volley.getData(Url.OUTBOUNDS, object : NciApiListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val list = castListModel<BeanOutboundModel>()
                        if (list.isNotEmpty()){
                            outList.addAll(list)
                            adapter.notifyDataSetChanged()
                        }
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        },session.getUser()?.token)
    }

    // <editor-fold defaultstate="collapsed" desc="Outbound Adapter">
    inner class Adapter: RecyclerView.Adapter<Adapter.Holder>(){
        inner class Holder(val b: BeanOutboundTransactionBinding): RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanOutboundTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val out = outList[position]

            with(holder){
                (position+1).toString().also { b.outNumb.text = it }
                b.outTrxNumb.text = out.receiptNumber
                b.outDate.text = out.createdAt
                b.custName.text = out.toData?.name
            }

        }

        override fun getItemCount() = outList.size
    }
    // </editor-fold>
}