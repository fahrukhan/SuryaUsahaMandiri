package com.rfid.sum.utils

import android.graphics.Bitmap
import com.github.sumimakito.awesomeqr.AwesomeQrRenderer
import com.github.sumimakito.awesomeqr.RenderResult
import com.github.sumimakito.awesomeqr.option.RenderOption
import com.github.sumimakito.awesomeqr.option.color.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.BarcodeView

object QR {

    fun generate(s: String, qr: Generated, size: Int = 480){
//        val bLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
//        val logo = Logo()
//        logo.bitmap = bLogo
//        logo.borderRadius = 10 // radius for logo's corners
//        logo.borderWidth = 10 // width of the border to be added around the logo
//        logo.scale = 0.3f // scale for the logo in the QR code
//        logo.clippingRect = RectF(0f, 0f, 200f, 200f) // crop the logo image before applying it to the QR code

        val color = Color()
        // for blank spaces
        color.light = android.graphics.Color.WHITE
        // for non-blank spaces
        color.dark = android.graphics.Color.BLACK
        // for the background (will be overriden by background images, if set)
        color.background = android.graphics.Color.WHITE
        color.auto = false

        val renderOption = RenderOption()
        // content to encode
        renderOption.content = s
        // size of the final QR code image
        renderOption.size = size
        // width of the empty space around the QR code
        renderOption.borderWidth = 10
        // (optional) specify an error correction level
        renderOption.ecl = ErrorCorrectionLevel.M
        // (optional) specify a scale for patterns
        renderOption.patternScale = 0.90f
        // (optional) if true, blocks will be drawn as dots instead
        renderOption.roundedPatterns = true
        // if set to true, the background will NOT be drawn on the border area
        renderOption.clearBorder = true
        // set a color palette for the QR code
        renderOption.color = color
        //renderOption.background = background // set a background, keep reading to find more about it
        //renderOption.logo = logo // set a logo, keep reading to find more about it

        AwesomeQrRenderer.renderAsync(renderOption, { result ->
            qr.onGenerated(result, null)
        }, { exception ->
            qr.onGenerated(null, exception)
        })

    }

    interface Generated {
        fun onGenerated(result: RenderResult?, e: Exception?){}
        fun onBarcodeGenerated(b: Bitmap?, e:Exception?){}
    }

    fun generateBarcode(s: String, qr: Generated){
        try {
            val bCodeEncoder = BarcodeEncoder()
            val bitmap = bCodeEncoder.encodeBitmap(s, BarcodeFormat.CODE_39, 600, 200)
            qr.onBarcodeGenerated(bitmap, null)
        }catch (e: Exception){
            qr.onBarcodeGenerated(null, e)
        }
    }
}