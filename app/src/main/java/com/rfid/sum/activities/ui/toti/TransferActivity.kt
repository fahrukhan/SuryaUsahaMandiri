package com.rfid.sum.activities.ui.toti

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rfid.sum.activities.BaseBinding
import com.rfid.sum.databinding.ActivityTransferBinding

class TransferActivity : BaseBinding<ActivityTransferBinding>() {
    override fun getViewBinding() = ActivityTransferBinding.inflate(layoutInflater)
    override fun useReader() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTrf()
    }

    private fun initTrf() {

    }
}