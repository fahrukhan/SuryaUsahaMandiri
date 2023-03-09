package com.rfid.sum.activities.ui.stock

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.activities.ui.opname.StockOpname.*
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.data.model.ItemModel
import com.rfid.sum.data.model.WarehouseModel
import com.rfid.sum.databinding.ActivityStockInfoBinding
import com.rfid.sum.databinding.BeanPopStockInfoDetailBinding
import com.rfid.sum.databinding.BeanStockInfoRvBinding
import com.rfid.sum.databinding.BeanStockInfoSeriesBinding
import com.rfid.sum.databinding.PopSiByWarehouseBinding
import com.rfid.sum.rest.NciApiListener
import com.rfid.sum.rest.model.Response
import com.rfid.sum.utils.Logger
import com.rfid.sum.utils.Pop
import com.rfid.sum.utils.Wait

class StockInfo : BaseBinding<ActivityStockInfoBinding>() {
    override fun getViewBinding() = ActivityStockInfoBinding.inflate(layoutInflater)
    override fun useReader() = false
    private var locationSelected: WarehouseModel? = null
    private val itemList = mutableListOf<SoItemModel>()
    private val itemFilteredList = mutableListOf<SoItemModel>()
    private var infoMode = SiMode.WH
    private val adapter = Adapter()
    lateinit var pop: Pop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStock()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initStock() {
        pop = Pop(this)

        with(bind) {
            wait = Wait(this@StockInfo, root)
            siRv.layoutManager = LinearLayoutManager(this@StockInfo)
            siRv.adapter = adapter

            scopeTg.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        cpNumTg.id -> {
//                            tos.info("Under maintetance")
//                            return@addOnButtonCheckedListener

                            itemList.clear()
                            adapter.notifyDataSetChanged()
                            infoMode = SiMode.CP
                            locationBtn.gone(true)
                            loadItemByCp()
                        }
                        warehouseTg.id -> {
                            bind.totalQty.text = getString(R.string.zero)
                            itemList.clear()
                            adapter.notifyDataSetChanged()
                            infoMode = SiMode.WH
                            locationBtn.gone(false)
                            locationBtn.text = getString(R.string.label_select_warehouse)
                        }
                    }
                    headerSpace.gone(infoMode == SiMode.WH)
                }
            }
            scopeTg.check(warehouseTg.id)
            locationBtn.setOnClickListener {
                pop.warehouseListPop(root, object : Pop.PopListener {
                    override fun onWarehouseSelected(wh: WarehouseModel) {
                        locationSelected = wh
                        locationBtn.text = wh.wrhsName
                        loadItemByWarehouse()
                    }
                })
            }
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String?): Boolean {
                    val list = itemFilteredList.filter {
                        it.prodName.contains(newText!!, true) ||
                                it.prodNo.contains(newText, true)
                    }
                    itemList.clear()
                    itemList.addAll(list)
                    adapter.notifyDataSetChanged()
                    return true
                }
            })
        }
    }

    private fun loadItemByWarehouse() {
        if (locationSelected == null) {
            tos.error("Select location.")
            return
        }

        wait.show()
        volley.getDataById(Url.ITEMS_BY_WH, locationSelected?.wrhsCode.toString(), object :
            NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        val list = castListModel<BatchModel>()
                        if (list.isEmpty()) {
                            wait.hide()
                            tos.info("Warehouse have no items!")
                            itemList.clear()
                            bind.totalQty.text = "0"
                            adapter.notifyDataSetChanged()
                        } else {
                            wait.hide()
                            setDataByWarehouse(list)
                        }
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun loadItemByCp() {
        wait.show()
        volley.getData(Url.ITEMS, object : NciApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: Response, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        val list = castListModel<BatchModel>()
                        if (list.isEmpty()) {
                            wait.hide()
                            tos.info("Warehouse have no items!")
                            itemList.clear()
                            bind.totalQty.text = "0"
                            adapter.notifyDataSetChanged()
                        } else {
                            wait.hide()
                            setDataByWarehouse(list)
                        }
                    } else {
                        wait.hide()
                        tos.error(errorMsg)
                    }
                }
            }
        }, session.getUser()?.token)
    }

    private fun bindData(s: MutableList<SoItemModel>, im: BatchModel){
        s.find { o -> o.prodNo == im.prdNumber }.apply {
            im.tid = im.tid
            if (this != null){
                cpBatch.add(im)
                tidList.add(im.tid!!)
            }else{
                s.add(SoItemModel(
                    im.prdNumber!!,
                    im.prodName!!,
                    mutableListOf(im),
                    mutableListOf(im.tid!!)
                ))
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setDataByWarehouse(list: List<BatchModel>) {
        itemList.clear()
        itemFilteredList.clear()
        list.forEach { im ->
            bindData(itemList,im)
            bindData(itemFilteredList,im)
        }

        val total = itemList.sumOf { it.tidList.size }
        bind.totalQty.text = total.toString()

        wait.hide()
        adapter.notifyDataSetChanged()
    }

    private val warehouses = Key.warehouseList
    fun showWarehouseDialog(m: SoItemModel) {
        val whCustomList = mutableListOf<WhCustom>()

        val x = Gson().toJson(m)
        Logger.info("BWH: $x")
        m.cpBatch.forEach { n ->
            whCustomList.find { o -> o.whCode == n.wrhsCode }.let { p ->
                if (p != null) p.cpBatch.add(n)
                else whCustomList.add(WhCustom(n.wrhsName!!, n.wrhsCode!!, mutableListOf(n)))
            }
        }

        val a = Gson().toJson(whCustomList)
        Logger.info("WH: $a")

        val wAdapter = WarehouseAdapter(whCustomList)
        val dialog = AlertDialog.Builder(this)
        val b = PopSiByWarehouseBinding.inflate(LayoutInflater.from(this))
        b.title.text = m.prodName
        b.subTitle.text = m.prodNo
        b.detailRv.layoutManager = LinearLayoutManager(this)
        b.detailRv.adapter = wAdapter
        b.detailRv.setItemViewCacheSize(warehouses.size)
        dialog.setView(b.root)
        dialog.setPositiveButton("OK") { d, _ -> d.dismiss() }
        dialog.show()
    }

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val b: BeanStockInfoRvBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = itemList.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanStockInfoRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = itemList[position]

            with(holder.b) {
                "${position + 1}".also { no.text = it }

                prodName.text = item.prodName
                prodNo.text = item.prodNo
                prodQty.text = item.tidList.size.toString()
                infoIc.gone(infoMode == SiMode.WH)
                holder.itemView.setOnClickListener {
                    if (infoMode == SiMode.CP) showWarehouseDialog(item)
                }

            }
        }
    }
    inner class WarehouseAdapter(private val whCustomList: MutableList<WhCustom>) :
        RecyclerView.Adapter<WarehouseAdapter.Holder>() {
        inner class Holder(val b: BeanPopStockInfoDetailBinding) : RecyclerView.ViewHolder(b.root)
        override fun getItemCount() = whCustomList.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanPopStockInfoDetailBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val wh = whCustomList[position]
            with(holder.b) {
                whName.text = wh.whName
                qtyTv.text = wh.batchQty().toString()
                val sAdapter = SeriesAdapter(wh.cpBatch)

                seriesRv.layoutManager = LinearLayoutManager(this@StockInfo)
                seriesRv.adapter = sAdapter

                seriesRv.apply {
                    arrowBtn.setOnClickListener {
                        sAdapter.notifyDataSetChanged()
                        gone(visibility == View.VISIBLE)
                        arrowBtn.icon = ContextCompat.getDrawable(
                            this@StockInfo,
                            if (visibility == View.VISIBLE) R.drawable.ic_keyboard_arrow_down_24
                            else R.drawable.ic_keyboard_arrow_right_24
                        )
                        root.setBackgroundColor(
                            if (visibility == View.GONE) Color.WHITE
                            else ContextCompat.getColor(this@StockInfo, R.color.yellow_50)
                        )
                    }
                }
            }
        }
    }
    inner class SeriesAdapter(private val series: List<BatchModel>): RecyclerView.Adapter<SeriesAdapter.Holder>(){
        inner class Holder(val b: BeanStockInfoSeriesBinding): RecyclerView.ViewHolder(b.root)
        override fun getItemCount() = series.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanStockInfoSeriesBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cp = series[position]
            with(holder.b){
                "${position+1}".also { noTv.text = it }
                cpNo.text = cp.batchNo
            }
        }
    }
    // </editor-fold>

    enum class SiMode { CP, WH }
    data class WhCustom(
        val whName: String,
        val whCode: String,
        val cpBatch: MutableList<BatchModel> = mutableListOf(),
    ) {
        fun batchQty() = cpBatch.size
    }
}