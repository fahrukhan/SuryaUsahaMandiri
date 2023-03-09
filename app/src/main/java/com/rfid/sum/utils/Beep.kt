package com.rfid.sum.utils

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import com.rfid.sum.R
import java.util.HashMap

object Beep {
    private val soundMap = HashMap<Int, Int>()
    private var soundPool: SoundPool? = null
    private var volumeRatio = 0.5f
    private var am: AudioManager? = null

    fun initSound(context: Context) {
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .build()

        soundMap[1] = soundPool!!.load(context, R.raw.barcodebeep, 1)
        soundMap[2] = soundPool!!.load(context, R.raw.serror, 1)
        am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun freeSound() {
        if (soundPool != null) soundPool!!.release()
        soundPool = null
    }

    fun playSound(id: Int) {
        val audioMaxVolume: Float = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat()!!
        val audioCurrentVolume: Float = am?.getStreamVolume(AudioManager.STREAM_MUSIC)!!.toFloat()
        volumeRatio = audioCurrentVolume / audioMaxVolume
        try {
            soundPool?.play(
                soundMap[id]!!,
                volumeRatio,  // 左声道音量
                volumeRatio,  // 右声道音量
                1,  // 优先级，0为最低
                0,
                1f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}