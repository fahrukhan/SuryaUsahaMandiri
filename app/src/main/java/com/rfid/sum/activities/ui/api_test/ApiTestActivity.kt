package com.rfid.sum.activities.ui.api_test

import android.R.attr.*
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.rfid.sum.R
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.databinding.ActivityApiTestBinding


class ApiTestActivity : BaseBinding<ActivityApiTestBinding>() {
    override fun getViewBinding() = ActivityApiTestBinding.inflate(layoutInflater)
    override fun useReader() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)


        listOf(PRODUCT_MASTER, WAREHOUSE_MASTER, CP_BATCH, PACKING_LIST).forEachIndexed { i, s ->
            val button = MaterialButton(ContextThemeWrapper(this, R.style.OutlineButton))
            button.id = i
            button.text = s

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            button.layoutParams = params
            bind.groupButton.addView(button)
        }

        bind.groupButton.addOnButtonCheckedListener{  btn, id, checked ->
            if (checked){
                tos.info(id.toString())
            }
        }
    }

    companion object {
        const val PRODUCT_MASTER = "Product Master"
        const val WAREHOUSE_MASTER = "Warehouse Master"
        const val CP_BATCH = "CP Batch"
        const val PACKING_LIST = "Packing List"
    }
}
