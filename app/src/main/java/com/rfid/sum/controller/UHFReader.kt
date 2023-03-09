package com.rfid.sum.controller

import android.content.Context
import android.util.Log
import com.pda.rfid.EPCModel
import com.pda.rfid.IAsynchronousMessage
import com.pda.rfid.uhf.UHFReader
import com.port.Adapt
import com.rfid.sum.data.constant.Key
import com.rfid.sum.utils.Logger

object UHFReader: IAsynchronousMessage {
    private val reader = UHFReader.getUHFInstance()
    lateinit var listener: UHFScanning
    private var nowAntennaNo = 1
    private var uhfState = false
    var isScanning = false
    private var duplicateEnable = false
    var maxPower = 30 // Maximum transmitting power of the reader
    var minPower = 0 // Minimum transmitting power of the reader
    private var upDataTime = 20

    fun connectReader(context: Context): ConnectReaderModel {
        Adapt.init(context)
        Adapt.enablePauseInBackGround(context)


        return if (!uhfInit()){
            ConnectReaderModel(false, "Low battery, please charge!")
        }else{
            try {
                uhfGetReaderProperty()
                Thread.sleep(50)
                stop()
                Thread.sleep(50)
                uhfSetTagUpdateParam()
                ConnectReaderModel(true, "OK")
            }catch (e: java.lang.Exception){
                println(e.message)
                ConnectReaderModel(false, e.message!!)
            }
        }
    }

    fun stop(){
        isScanning = false
        UHFReader._Config.Stop()
    }

    fun dispose(){
        if (uhfState){
            uhfState = false
            UHFReader._Config.CloseConnect()
        }
    }

    fun startScan(){
        if (!isScanning){
            isScanning = true
            UHFReader._Tag6C.GetEPC_TID(nowAntennaNo, 1)
        }else{
            stop()
        }
    }

    fun startSingleScan(){
        if (isScanning){
            isScanning = false
        }
        UHFReader._Tag6C.GetEPC_TID(nowAntennaNo, 0)
    }

    fun startScanEpcTidMatchTid(tid: String){
        if (isScanning){
            isScanning = false
        }
        UHFReader._Tag6C.GetEPC_TID_MatchTID(nowAntennaNo, 1, tid)
    }

    fun setAntennaPower(i: Int){
        var power = i
        if (i > 30) power = 30
        if (i < 1) power = 1

        UHFReader._Config.SetANTPowerParam(nowAntennaNo, i)
    }

    fun getAntennaPower(): Int{
        return UHFReader._Config.GetANTPowerParam()
    }

    private fun uhfInit(): Boolean {
        val log: IAsynchronousMessage = this
        var rt = false
        try {
            if (!uhfState) {
                val ret = UHFReader.getUHFInstance().OpenConnect(log)
                if (ret) {
                    rt = true
                    uhfState = true
                }
                Thread.sleep(500)
            } else {
                rt = true
            }
        } catch (ex: Exception) {
            Log.d("debug", "On the UHF electric abnormal:" + ex.message)
        }
        return rt
    }

    fun setUpdateParam(i: Int){
        UHFReader._Config.SetTagUpdateParam(i, 0)
    }

    private fun uhfGetReaderProperty() {
        //String propertyStr = CLReader.GetReaderProperty();
        val propertyStr = UHFReader._Config.GetReaderProperty()
        //Log.d("Debug", "Get Reader Property:" + propertyStr);
        val propertyArr = propertyStr.split("\\|").toTypedArray()
        val hmPower = mutableMapOf<Int, Int>()
        hmPower.apply {
            put(1, 1)
            put(2, 3)
            put(3, 7)
            put(4, 15)
        }

        if (propertyArr.size > 3) {
            try {
                minPower = propertyArr[0].toInt()
                maxPower = propertyArr[1].toInt()
                val powerIndex = propertyArr[2].toInt()
                nowAntennaNo = hmPower[powerIndex]!!
            } catch (ex: java.lang.Exception) {
                Log.d("Debug", "Get Reader Property failure and conversion failed!")
            }
        } else {
            Log.d("Debug", "Get Reader Property failure")
        }
    }

    fun validButton(keyCode: Int): Boolean{
        Logger.info("Key Pressed: $keyCode")
        return (Adapt.DEVICE_TYPE_HY820 == Adapt.getDeviceType() && (Key.KEY_CODE_LIST.contains(keyCode)))
    }

    /**
     * Set tag upload parameters
     */
    private fun uhfSetTagUpdateParam() {
        // First query whether the current Settings are consistent, if not before setting
        // String searchRT = CLReader.GetTagUpdateParam();
        val searchRT = UHFReader._Config.GetTagUpdateParam()
        val arrRT = searchRT.split("\\|").toTypedArray()
        if (arrRT.size >= 2) {
            val nowUpDataTime = arrRT[0].toInt()
            Log.d("Debug", "Check the label to upload time:$nowUpDataTime")
            if (upDataTime != nowUpDataTime) {
                //CLReader.SetTagUpdateParam("1," + _UpDataTime); // Set the tag repeat upload time to 20ms
                UHFReader._Config.SetTagUpdateParam(upDataTime, 0) //RSSIFilter
                Log.d("Debug", "Sets the label upload time...")
            }
        } else {
            Log.d("Debug", "Query tags while uploading failure...")
        }
    }

    private val scannedList = mutableListOf<EPCModel>()
    override fun OutPutEPC(p0: EPCModel?) {
        if (p0 != null){
            if (duplicateEnable){
                listener.outPutEpc(p0)
            }else{
                if (!scannedList.any { it._EPC == p0._EPC }){
                    listener.outPutEpc(p0)
                }
            }
        }
    }

    data class ConnectReaderModel(
        val success: Boolean,
        val message: String
    )
}