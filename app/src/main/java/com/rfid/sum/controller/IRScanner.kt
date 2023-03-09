package com.rfid.medisafe.controller

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.pda.scanner.ScanReader
import com.pda.scanner.Scanner
import com.port.Adapt
import com.rfid.sum.utils.Logger
import java.nio.charset.Charset

object IRScanner{
   lateinit var listen: IRScanning
   private val scanner: Scanner? = ScanReader.getScannerInstance()
   val toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME)
   var busy = false


   private val handler = object : Handler(Looper.myLooper()!!){
      override fun handleMessage(msg: Message) {
         when(msg.what){
            0 -> {
               val id = msg.obj as String
               Logger.info(id)
               listen.outPutScanner(id)
            }
            1 -> {
               listen.stopScan()
            }
         }
      }
   }

   fun init(context: Context) : Boolean{
      return scanner?.open(context) ?: false
   }

//   fun init(){
//      if (!scanInit()){
//         listen.message("Open power failed")
//      }
//   }

   fun dispose() {
      scanner?.close()
   }

   private fun decode() {
      if (busy) {
         listen.busyScanner(true)
         return
      }

      busy = true
      listen.scanLoad()
      object : Thread() {
         override fun run() {
            val id: ByteArray? = scanner?.decode()

            if(id==null){
               busy = false
               sendMsg(1)
               return
            }

            val idString: String?
            var utf8 = String(id,  Charset.forName("utf8"))

            if (utf8.contains("\ufffd")) {
               utf8 = String(id, Charset.forName("gbk"))
            }
            idString = """
               $utf8
               
               """.trimIndent()
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)

            sendMsg(0, idString)

            busy = false
         }
      }.start()
   }

   fun trigger(){
      decode()
   }

   fun sendMsg(key: Int, idString: String = ""){
      val msg = handler.obtainMessage(0)
      msg.obj = idString
      handler.sendMessage(msg)
   }
}