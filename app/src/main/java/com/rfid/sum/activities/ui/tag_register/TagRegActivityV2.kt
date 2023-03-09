package com.rfid.sum.activities.ui.tag_register

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.rfid.sum.R
import com.rfid.sum.SumApplication
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Url
import com.rfid.sum.data.model.GetCPBatchModel
import com.rfid.sum.data.model.ProductModel
import com.rfid.sum.data.room.DaoViewModel
import com.rfid.sum.data.room.DaoViewModelFactory
import com.rfid.sum.data.room.model.SumApiHistoryModel
import com.rfid.sum.data.support.TokenInitialize
import com.rfid.sum.databinding.ActivityTagRegV2Binding
import com.rfid.sum.databinding.BeanProductBinding
import com.rfid.sum.databinding.DialogSelectProductBinding
import com.rfid.sum.databinding.ItemChipsBinding
import com.rfid.sum.rest.ApiRequest
import com.rfid.sum.rest.SumApiListener
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.rest.model.TokenModel
import com.rfid.sum.utils.Format
import org.json.JSONObject

class TagRegActivityV2 : BaseBinding<ActivityTagRegV2Binding>() {
    override fun getViewBinding() = ActivityTagRegV2Binding.inflate(layoutInflater)
    override fun useReader() = false

    private val productList = mutableListOf<ProductModel>()
    private val productFilteredList = mutableListOf<ProductModel>()
    private var adapter = ProductAdapter()

    private val dao: DaoViewModel by viewModels {
        DaoViewModelFactory((application as SumApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTagReg()
    }

    private fun initTagReg() {
        with(bind){
            productBtn.setOnClickListener { dialogProduct() }
        }

        loadToken(ApiRequest.GetProductMaster)
    }

    private fun loadProduct(token: String) {
        wait.updateMsg("Load Product...")

        val params = paramBuilder.getParam(
            ApiRequest.GetProductMaster,
            token
        )
        val gson = Gson().toJson(params)

        volley.sumPostData(Url.GET_API, JSONObject(gson), object : SumApiListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        wait.hide()
                        val list = castListModel<ProductModel>()
                        if (list.isNotEmpty()) {
                            productList.addAll(list)
                            productFilteredList.addAll(list)
                            println(list[0])

                            dao.saveApiHistory(
                                SumApiHistoryModel(
                                    api = ApiRequest.GetProductMaster.toString(),
                                    method = Key.getMethod(ApiRequest.GetProductMaster),
                                    request = paramBuilder.removedTokenParams(params)!!,
                                    response = responseToHistory()
                                )
                            )
                        }
                    } else {
                        wait.hide()
                        if (msgCode == 104) {
                            tos.info("Token Expired!")
                            tokenMgt.removeToken(Key.getMethod(ApiRequest.GetProductMaster)).also {
                                loadToken(ApiRequest.GetProductMaster)
                            }
                        } else {
                            tos.error(errorMsg)
                        }
                    }
                }
            }
        })
    }

    private var searchClue = ""
    private lateinit var dialog: AlertDialog.Builder
    private lateinit var pop: AlertDialog

    @SuppressLint("NotifyDataSetChanged")
    private fun dialogProduct(){
        val dsp = DialogSelectProductBinding.inflate(LayoutInflater.from(this))
        dialog = AlertDialog.Builder(this)
        dialog.setView(dsp.root)
        pop = dialog.create()

        dsp.productRv.layoutManager = LinearLayoutManager(this)
        dsp.productRv.adapter = adapter
        dsp.search.doOnTextChanged { text, _, _, _ ->
            val list = productList.filter { it.prodcode!!.contains(text!!) || it.prodname!!.contains(text) }
            productFilteredList.clear()
            productFilteredList.addAll(list)
            searchClue = dsp.search.text.toString()
            adapter.notifyDataSetChanged()
        }

        dsp.title.text = getString(R.string.product)
        dsp.cancelBtn.setOnClickListener {
            pop.dismiss()
        }
        pop.show()
    }

    private fun loadToken(ar: ApiRequest) {
        tokenMgt.getToken(ar, object : TokenInitialize {
            override fun activeToken(mToken: TokenModel?) {
                wait.show()
                loadProduct(mToken?.token!!)
            }

            override fun freshToken(mToken: TokenModel?) {
                loadProduct(mToken?.token!!)
            }

            override fun refreshingToken() {
                wait.show("Refreshing token...")
            }

            override fun failedRefresh(msg: String) {
                wait.hide()
                tos.error(msg)
            }
        })
    }



    private fun setSelectedProduct(product: ProductModel) {

        (0..10).forEach {
            val b = ItemChipsBinding.inflate(LayoutInflater.from(this), null, false)
            with(b.root){
                "Chip $it".also { s -> text = s }
            }

            bind.chipGroup.addView(b.root)
        }

        with(bind){
            prodCode.text = product.prodcode
            prodName.text = product.prodname
            uomCode.text = product.uomcode
            colorCode.text = product.colorcode
            colorName.text = product.colorname
        }
        pop.dismiss()
    }

    // <editor-fold defaultstate="collapsed" desc="Adapter">
    inner class ProductAdapter(): RecyclerView.Adapter<ProductAdapter.Holder>() {
        inner class Holder(val b: BeanProductBinding) : RecyclerView.ViewHolder(b.root)
        override fun getItemCount() = productFilteredList.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            BeanProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val product = productFilteredList[position]

            with(holder.b){
                (position+1).toString().let { no.text = it }
                prodCode.text = product.prodcode
                prodName.text = product.prodname
                getColor(R.color.highLightText).let {
                    Format.setHighLightedText(prodCode, searchClue, it)
                    Format.setHighLightedText(prodName, searchClue, it)
                }

            }

            holder.itemView.setOnClickListener {
                setSelectedProduct(product)
            }
        }
    }
    // </editor-fold>

}