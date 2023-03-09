package com.rfid.medisafe.controller

interface IRScanning {
    fun outPutScanner(id: String?){}
    fun busyScanner(busy: Boolean){}
    fun stopScan()
    fun scanLoad()
}