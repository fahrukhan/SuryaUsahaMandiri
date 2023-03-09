package com.rfid.sum.controller

import com.pda.rfid.EPCModel

interface UHFScanning {
    fun outPutEpc(epc: EPCModel)
}