package com.castivio.feature.settings

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * A page of Castivio's site as a QR code, [pixels] on a side, for `QrPlate` to draw as the activation and licence screens
 * draw theirs.
 *
 * The address and nothing else -- no device, no account: a QR code on a television is photographed and shared, and
 * `SettingsQrTest` reads each one back. Medium correction, which keeps the modules large enough to be read from a sofa,
 * and one module of margin, which the white plate widens.
 */
internal fun siteQrBitmap(url: String, pixels: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        url,
        BarcodeFormat.QR_CODE,
        pixels,
        pixels,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val width = matrix.width
    val height = matrix.height
    val buffer = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) buffer[row + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
        setPixels(buffer, 0, width, 0, 0, width, height)
    }
}
