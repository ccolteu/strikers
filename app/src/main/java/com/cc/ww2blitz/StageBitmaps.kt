package com.cc.ww2blitz

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.ceil

/** Decode stage PNGs from `assets/stages/N/`. Never call on the vsync path. */
object StageBitmaps {

  fun decode(
    assets: AssetManager,
    path: String,
    keyed: Boolean = false,
    widthLock: Int = 0,
  ): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = keyed
    }
    val src = assets.open(path).use { stream ->
      BitmapFactory.decodeStream(stream, null, opts)
    } ?: error("Missing stage asset $path")
    var bmp = if (src.config == Bitmap.Config.ARGB_8888 && (!keyed || src.isMutable)) {
      src
    } else {
      src.copy(Bitmap.Config.ARGB_8888, keyed).also { src.recycle() }
    }
    if (keyed) keyGreen(bmp)
    if (widthLock > 0 && bmp.width != widthLock) {
      val scale = widthLock.toFloat() / bmp.width.toFloat()
      val scaledH = ceil(bmp.height * scale).toInt().coerceAtLeast(1)
      val scaled = Bitmap.createScaledBitmap(bmp, widthLock, scaledH, true)
      if (scaled !== bmp) bmp.recycle()
      bmp = scaled
    }
    return bmp
  }

  fun tryDecode(
    assets: AssetManager,
    path: String?,
    keyed: Boolean = false,
    widthLock: Int = 0,
  ): Bitmap? {
    if (path == null || path.isEmpty()) return null
    return try {
      decode(assets, path, keyed, widthLock)
    } catch (_: Exception) {
      null
    }
  }

  fun recycle(bmp: Bitmap?) {
    if (bmp != null && !bmp.isRecycled) bmp.recycle()
  }

  fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    var y = 0
    while (y < h) {
      bmp.getPixels(row, 0, w, 0, y, w, 1)
      var x = 0
      while (x < w) {
        val p = row[x]
        val r = (p ushr 16) and 0xFF
        val g = (p ushr 8) and 0xFF
        val b = p and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[x] = 0
        }
        x++
      }
      bmp.setPixels(row, 0, w, 0, y, w, 1)
      y++
    }
  }
}
