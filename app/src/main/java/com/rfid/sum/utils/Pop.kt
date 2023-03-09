package com.rfid.sum.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rfid.sum.R
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.model.BatchModel
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.data.model.WarehouseModel
import java.lang.StringBuilder

class Pop(val context: Context) {
    private val dialog = AlertDialog.Builder(context)

    fun cpBatchListPop(
        title: String,
        root: ViewGroup,
        optList: List<GetCPBatchModel>,
        popListener: PopListener,
        hideTotal: Boolean = false
    ){
        val v = LayoutInflater.from(context).inflate(R.layout.pop_list_cp_batch, root, false)
        val titleTv: TextView = v.findViewById(R.id.popList_title)
        val rv: ListView = v.findViewById(R.id.popSearch_rv)
        val inputEt: TextInputEditText = v.findViewById(R.id.popSearch_inputSearch)
        val cancelBtn: MaterialButton = v.findViewById(R.id.cancelBtn)
        //val lineTotal: LinearLayout = v.findViewById(R.id.lineTotal)

        dialog.setView(v)

        val listPop = dialog.create()
        titleTv.text = title
        val filteredList = mutableListOf<GetCPBatchModel>()
        filteredList.addAll(optList)

        class ListSearchAdapter(private val list: List<GetCPBatchModel>): BaseAdapter(){
            override fun getCount(): Int {
                return list.size
            }

            override fun getItem(p0: Int): Any {
                return list[p0]
            }

            override fun getItemId(p0: Int): Long {
                return p0.toLong()
            }

            @SuppressLint("ViewHolder", "InflateParams")
            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val vw = LayoutInflater.from(context).inflate(R.layout.bean_pop_list_cp_batch, null)
                val prodNumber: TextView = vw.findViewById(R.id.prodNumber)
                val prodName: TextView = vw.findViewById(R.id.prodName)
                val totalBatch: TextView = vw.findViewById(R.id.totalBatch)
                val lineTotal: LinearLayout = vw.findViewById(R.id.lineTotal)

                if (hideTotal) Ui.gone(lineTotal)

                val cp = list[p0]
                prodNumber.text = cp.prdNmbr
                prodName.text = cp.prodName
                totalBatch.text = cp.totalBatch.toString()

                context.getColor(R.color.highLightText).let {
                    Format.setHighLightedText(prodName, inputEt.text.toString(), it)
                    Format.setHighLightedText(prodNumber, inputEt.text.toString(), it)
                }

                return vw
            }
        }

        val adapter = ListSearchAdapter(filteredList)
        rv.adapter = adapter
        rv.setOnItemClickListener { _, _, i, _ ->
            val m = filteredList[i]
            popListener.onCpSelected(m.prdNmbr!!)
            listPop.dismiss()
        }

        cancelBtn.setOnClickListener {
            listPop.dismiss()
        }

        inputEt.doOnTextChanged { text, _, _, _ ->
            val list = optList.filter { it.batchNo?.contains(text!!, true)!! || it.prodName!!.contains(text!!, true) }
            filteredList.clear()
            filteredList.addAll(list)
            adapter.notifyDataSetChanged()
        }
        listPop.show()
    }

    fun popProductList(
        title: String,
        root: ViewGroup,
        optList: List<BatchModel>,
        popListener: PopListener,
        hideTotal: Boolean = false
    ){
        val v = LayoutInflater.from(context).inflate(R.layout.pop_list_cp_batch, root, false)
        val titleTv: TextView = v.findViewById(R.id.popList_title)
        val rv: ListView = v.findViewById(R.id.popSearch_rv)
        val inputEt: TextInputEditText = v.findViewById(R.id.popSearch_inputSearch)
        val cancelBtn: MaterialButton = v.findViewById(R.id.cancelBtn)
        //val lineTotal: LinearLayout = v.findViewById(R.id.lineTotal)

        dialog.setView(v)

        val listPop = dialog.create()
        titleTv.text = title
        val filteredList = mutableListOf<BatchModel>()
        filteredList.addAll(optList)

        class ListSearchAdapter(private val list: List<BatchModel>): BaseAdapter(){
            override fun getCount(): Int {
                return list.size
            }

            override fun getItem(p0: Int): Any {
                return list[p0]
            }

            override fun getItemId(p0: Int): Long {
                return p0.toLong()
            }

            @SuppressLint("ViewHolder", "InflateParams")
            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val vw = LayoutInflater.from(context).inflate(R.layout.bean_pop_list_cp_batch, null)
                val prodNumber: TextView = vw.findViewById(R.id.prodNumber)
                val prodName: TextView = vw.findViewById(R.id.prodName)
                val totalBatch: TextView = vw.findViewById(R.id.totalBatch)
                val lineTotal: LinearLayout = vw.findViewById(R.id.lineTotal)

                //Ui.gone(prodName)

                if (hideTotal) Ui.gone(lineTotal)

                val cp = list[p0]
                prodNumber.text = cp.prdNumber
                prodName.text = cp.prodName
                totalBatch.text = cp.qty.toString()

                context.getColor(R.color.highLightText).let {
                    Format.setHighLightedText(prodName, inputEt.text.toString(), it)
                    Format.setHighLightedText(prodNumber, inputEt.text.toString(), it)
                }

                return vw
            }
        }

        val adapter = ListSearchAdapter(filteredList)
        rv.adapter = adapter
        rv.setOnItemClickListener { _, _, i, _ ->
            val m = filteredList[i]
            popListener.onCpSelected(m.prdNumber!!)
            listPop.dismiss()
        }

        cancelBtn.setOnClickListener {
            listPop.dismiss()
        }

        inputEt.doOnTextChanged { text, _, _, _ ->
            val list = optList.filter { it.prdNumber?.contains(text!!, true)!! }
            filteredList.clear()
            filteredList.addAll(list)
            adapter.notifyDataSetChanged()
        }
        listPop.show()
    }

    fun cpDetailsPop(title: String, root: ViewGroup, gcm: GetCPBatchModel){
        val v = LayoutInflater.from(context).inflate(R.layout.pop_batch_detail, root, false)
        val titleTv: TextView = v.findViewById(R.id.title)
        val batchNoTv: TextView = v.findViewById(R.id.batchNo)
        val contentTv: TextView = v.findViewById(R.id.content)
        dialog.setView(v)
        val listPop = dialog.create()

        titleTv.text = title
        batchNoTv.text = gcm.batchNo
        val sb = StringBuilder();
        sb.append("Product Number: ").append(gcm.prdNmbr).append("\n")
        sb.append("Batch Number: ").append(gcm.batchNo).append("\n")
        sb.append("Product Code: ").append(gcm.prodCode).append("\n")
        sb.append("Product Name: ").append(gcm.prodName).append("\n")
        sb.append("Product Date: ").append(gcm.prdDate).append("\n")
        sb.append("Start Date: ").append(gcm.startDate).append("\n")
        sb.append("End Date: ").append(gcm.endDate).append("\n")
        sb.append("Result Seq: ").append(gcm.resultSeq).append("\n")
        sb.append("Batch In Qty: ").append(gcm.batchInQty).append("\n")
        sb.append("UOM Code: ").append(gcm.uomCode).append("\n")
        sb.append("Lot: ").append(gcm.lot).append("\n")
        sb.append("Batch Exp Date: ").append(gcm.batchExpDate).append("\n")
        sb.append("Warehouse Code: ").append(gcm.wrhsCode).append("\n")
        sb.append("Warehouse Name: ").append(gcm.wrhsName).append("\n")
        sb.append("transdtbatchid: ").append(gcm.transDtBatchId).append("\n")

        contentTv.text = sb.toString()
        listPop.show()
    }

    fun cpDetailsPop2(title: String, root: ViewGroup, gcm: BatchModel){
        val v = LayoutInflater.from(context).inflate(R.layout.pop_batch_detail, root, false)
        val titleTv: TextView = v.findViewById(R.id.title)
        val batchNoTv: TextView = v.findViewById(R.id.batchNo)
        val contentTv: TextView = v.findViewById(R.id.content)
        dialog.setView(v)
        val listPop = dialog.create()

        titleTv.text = title
        batchNoTv.text = gcm.batchNo
        val sb = StringBuilder();
        sb.append("Product Number: ").append(gcm.prdNumber).append("\n")
        sb.append("Batch Number: ").append(gcm.batchNo).append("\n")
        sb.append("Product Code: ").append(gcm.prodCode).append("\n")
        sb.append("Product Name: ").append(gcm.prodName).append("\n")
        sb.append("Product Date: ").append(gcm.prdDate).append("\n")
        sb.append("Start Date: ").append(gcm.startDate).append("\n")
        sb.append("End Date: ").append(gcm.endDate).append("\n")
        sb.append("Result Seq: ").append(gcm.resultSeq).append("\n")
        sb.append("Batch In Qty: ").append(if(gcm.batchInQty==null || gcm.batchInQty == "0.0000") gcm.batchInQtyReg else gcm.batchInQty).append("\n")
        sb.append("UOM Code: ").append(gcm.uomCode ?: gcm.uomCodeReg).append("\n")
        sb.append("Lot: ").append(gcm.lot).append("\n")
        sb.append("Batch Exp Date: ").append(gcm.batchExpDate).append("\n")
        sb.append("Warehouse Code: ").append(gcm.wrhsCode).append("\n")
        sb.append("Warehouse Name: ").append(gcm.wrhsName).append("\n")
        sb.append("transdtbatchid: ").append(gcm.transDtBatchId).append("\n")
        sb.append("rfid: ").append(gcm.tid ?: "unregistered").append("\n")

        contentTv.text = sb.toString()
        listPop.show()
    }

    fun warehouseListPop(root: ViewGroup, popListener: PopListener){
        val v = LayoutInflater.from(context).inflate(R.layout.pop_list_cp_batch, root, false)
        val titleTv: TextView = v.findViewById(R.id.popList_title)
        val rv: ListView = v.findViewById(R.id.popSearch_rv)
        val inputEt: TextInputEditText = v.findViewById(R.id.popSearch_inputSearch)
        val cancelBtn: MaterialButton = v.findViewById(R.id.cancelBtn)

        dialog.setView(v)
        val listPop = dialog.create()

        val warehouseList = Key.warehouseList
        titleTv.text = "Warehouse"
        val filteredList = mutableListOf<WarehouseModel>()
        filteredList.addAll(warehouseList)

        class ListSearchAdapter(private val list: List<WarehouseModel>): BaseAdapter(){
            override fun getCount(): Int {
                return list.size
            }

            override fun getItem(p0: Int): Any {
                return list[p0]
            }

            override fun getItemId(p0: Int): Long {
                return p0.toLong()
            }

            @SuppressLint("ViewHolder", "InflateParams")
            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val vw = LayoutInflater.from(context).inflate(R.layout.bean_pop_warehouse, null)
                val warehouseCode: TextView = vw.findViewById(R.id.warehouseCode)
                val warehouseName: TextView = vw.findViewById(R.id.warehouseName)

                val cp = list[p0]
                warehouseCode.text = cp.wrhsCode
                warehouseName.text = cp.wrhsName

                context.getColor(R.color.highLightText).let {
                    Format.setHighLightedText(warehouseCode, inputEt.text.toString(), it)
                    Format.setHighLightedText(warehouseName, inputEt.text.toString(), it)
                }

                return vw
            }
        }

        val adapter = ListSearchAdapter(filteredList)
        rv.adapter = adapter
        rv.setOnItemClickListener { _, _, i, _ ->
            val m = filteredList[i]
            popListener.onWarehouseSelected(m)
            listPop.dismiss()
        }

        cancelBtn.setOnClickListener {
            listPop.dismiss()
        }

        inputEt.doOnTextChanged { text, _, _, _ ->
            val list = warehouseList.filter { it.wrhsCode?.contains(text!!, true)!! || it.wrhsName!!.contains(text!!, true) }
            filteredList.clear()
            filteredList.addAll(list)
            adapter.notifyDataSetChanged()
        }
        listPop.show()
    }

    fun confirmDialog(title: String, msg: String, f: () -> Unit ){
        dialog.setTitle(title).setMessage(msg)
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
                f()
            }.setNegativeButton("CANCEL"){ d, _ -> d.dismiss()}
            .show()
    }

    interface PopListener {
        fun onCpSelected(cp: String) {}
        fun onWarehouseSelected(wh: WarehouseModel) {}
    }
}