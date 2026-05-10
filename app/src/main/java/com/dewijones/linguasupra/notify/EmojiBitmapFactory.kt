package com.dewijones.linguasupra.notify

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * RemoteViews ImageView/ImageButton can't render emoji text directly across all launchers,
 * so we rasterise emoji strings to a Bitmap once and reuse. Width is measured to fit the
 * actual glyph run, so single emojis come out square-ish and combos (e.g. flag + vibe)
 * come out as wider strips. Cached by string + height.
 */
object EmojiBitmapFactory {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun render(text: String, heightPx: Int = 96): Bitmap {
        val key = "$text:$heightPx"
        cache[key]?.let { return it }
        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = heightPx * 0.78f
        }
        val measured = paint.measureText(text).toInt()
        val widthPx = (measured + heightPx / 6).coerceAtLeast(heightPx)
        val bitmap = createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baselineY = heightPx / 2f - (paint.descent() + paint.ascent()) / 2f
        val startX = (widthPx - measured) / 2f
        canvas.drawText(text, startX, baselineY, paint)
        cache[key] = bitmap
        return bitmap
    }
}
