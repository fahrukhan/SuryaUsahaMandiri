package com.rfid.sum.activities.ui.packing

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.activities.ui.packing.PackingListChecker.*
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.OutboundModel
import com.rfid.sum.databinding.ActivityOutboundHistoryBinding
import com.rfid.sum.databinding.ActivityOutboundHistoryDetailsBinding
import com.rfid.sum.databinding.BeanPackingListCheckerBinding
import com.rfid.sum.databinding.BeanPackingListCheckerGroupBinding
import com.rfid.sum.databinding.ItemChipsBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Format
import com.rfid.sum.utils.Pop

class OutboundHistoryDetails : BaseBinding<ActivityOutboundHistoryDetailsBinding>() {
    override fun getViewBinding() = ActivityOutboundHistoryDetailsBinding.inflate(layoutInflater)
    override fun useReader() = false
    private var oId: OutboundModel? = null
    private var outboundDetail: OutboundDetailModel? = null
    private val outboundItemList = mutableListOf<OutboundDetailModel>()
    private val dolLIst = mutableListOf<DolsListModel>()

    private val packingListLoaded = mutableListOf<DolsListModel>()
    private val packingListLoadedFiltered = mutableListOf<DolsListModel>()
    //private val detectionList = mutableListOf<String>()
    lateinit var pop: Pop

    private val dolAdapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(Key.INTENT_DATA).let {
            if (it == null){
                tos.error("Tidak dapat memuat data!")
                finish()
            }else{
                val gson = Gson().toJson(it)
                println(it)
                oId = Gson().fromJson(it, OutboundModel::class.java)
                initDetail()
            }
        }
    }

    private fun initDetail() {
        pop = Pop(this)
        with(bind){
            tvReceiptNumber.text = oId?.receiptNumber
            tvUserName.text = oId?.user?.name
            outboundDate.text = oId?.at
            rvDetails.apply {
                layoutManager = LinearLayoutManager(this@OutboundHistoryDetails)
                adapter = dolAdapter
            }
        }

        loadDetails()
    }

    private fun loadDetails() {
        if (oId == null) return

        wait.show()
        volley.getDataById(Url.OUTBOUNDS, oId?.id.toString(), object: NciApiListener{
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response){
                    wait.hide()
                    if (success){
                        val m = castModel<OutboundDetailModel>()
                        outboundDetail = null
                        if (m != null){
                            outboundDetail = m
                        }
                        bindDetails()
                    }else{
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindDetails() {
        val grouped = outboundDetail?.data?.groupBy { it.transDestNumber }
        println("SIZE: ${grouped?.size}")
        grouped?.forEach { (k, v) ->
            val list2 = v.groupBy { it.prodCode }
            val groupList = mutableListOf<PackingListCheckerObject>()
            list2.forEach { (_, v2) ->
                val checkedList = v2.filter { it.status!! }
                val detectionList = checkedList.map { it.batchNo!! }.toMutableList()
                groupList.add(
                    PackingListCheckerObject(
                        v2[0].prodCode ?: "null",
                        v2[0].prodName ?: "null",
                        v2, detectionList)
                )
            }

            DolsListModel(k!!, v[0].transDestDate!!, groupList).let {
                packingListLoaded.add(it)
                packingListLoadedFiltered.add(it)
            }
        }

        dolAdapter.notifyDataSetChanged()
    }

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
                v.batchRv.gone(false)

                (position+1).toString().also { v.no.text = it }
                v.botSpace.gone((position+1) != itemCount)

                v.dolNo.text = pack.dolsNumber
                v.prodQty.text = pack.plList.sumOf { it.batchList.size }.toString()

//                pack.plList.forEach {
//                    it.batchList.forEach { b ->
//                        if (b.tid != null && !it.detectedList.contains(b.tid)){
//                            if (detectionList.contains(b.tid)) it.detectedList.add(b.tid!!)
//                            else it.detectedList.remove(b.tid)
//                        }
//                    }
//                }
                v.prodDetect.text = pack.plList.sumOf { it.detectedList.size }.toString()
                pack.plList.forEach {
                    println(it.detectedList)
                }

                val groupAdapter = PackingListGroupAdapter(pack.plList)
                v.batchRv.apply {
                    layoutManager = LinearLayoutManager(this@OutboundHistoryDetails)
                    adapter = groupAdapter
                }

//                bind.tvQtyBatch.text = packingListLoaded.sumOf { it.plList.sumOf { p -> p.batchList.size } }.toString()
//                bind.tvCountDetected.text = packingListLoaded.sumOf { it.plList.sumOf { p -> p.detectedList.size } }.toString()
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
                    bl.detectedList.contains(cp.batchNo).let { d ->
                        val c = ItemChipsBinding.inflate(
                            LayoutInflater.from(this@OutboundHistoryDetails),
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

}