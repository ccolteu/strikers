package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class ParticleManager(private val resources: Resources) {

  private val pool = Array(POOL_SIZE) { ActiveExplosion() }
  private val srcRects = Array(FRAME_COUNT) { Rect() }
  private val dstRect = RectF()
  private val paint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
  }
  private val lock = Any()
  private var sheet: Bitmap? = null
  private var halfW = 0f
  private var halfH = 0f

  fun onSizeChanged(width: Int, height: Int) {
    if (sheet == null) {
      sheet = loadKeyedSheet()
      cacheSrcRects(sheet!!)
    }
    val bmp = sheet ?: return
    val cellW = (bmp.width / FRAME_COUNT).coerceAtLeast(1)
    val cellH = bmp.height.coerceAtLeast(1)
    val targetDrawW = (width * EXPLOSION_WIDTH_FRAC).toInt().coerceAtLeast(1)
    val aspect = cellH.toFloat() / cellW.toFloat()
    val targetDrawH = (targetDrawW * aspect).toInt().coerceAtLeast(1)
    halfW = targetDrawW * 0.5f
    halfH = targetDrawH * 0.5f
  }

  fun triggerExplosion(centerX: Float, centerY: Float) {
    synchronized(lock) {
      var i = 0
      while (i < POOL_SIZE) {
        val e = pool[i]
        if (!e.isActive) {
          e.x = centerX
          e.y = centerY
          e.currentFrameTime = 0f
          e.currentFrameIndex = 0
          e.isActive = true
          return
        }
        i++
      }
    }
  }

  fun update(dt: Float) {
    synchronized(lock) {
      var i = 0
      while (i < POOL_SIZE) {
        val e = pool[i]
        if (e.isActive) {
          e.currentFrameTime += dt
          while (e.currentFrameTime >= FRAME_SEC) {
            e.currentFrameTime -= FRAME_SEC
            e.currentFrameIndex++
            if (e.currentFrameIndex > LAST_FRAME) {
              e.isActive = false
              break
            }
          }
        }
        i++
      }
    }
  }

  fun draw(canvas: Canvas) {
    val bmp = sheet ?: return
    synchronized(lock) {
      val hw = halfW
      val hh = halfH
      var i = 0
      while (i < POOL_SIZE) {
        val e = pool[i]
        if (e.isActive) {
          val frame = e.currentFrameIndex
          if (frame in 0..LAST_FRAME) {
            dstRect.set(e.x - hw, e.y - hh, e.x + hw, e.y + hh)
            canvas.drawBitmap(bmp, srcRects[frame], dstRect, paint)
          }
        }
        i++
      }
    }
  }

  fun release() {
    val bmp = sheet
    if (bmp != null && !bmp.isRecycled) bmp.recycle()
    sheet = null
  }

  private fun cacheSrcRects(bitmap: Bitmap) {
    val cellWidth = bitmap.width / FRAME_COUNT
    val bitmapHeight = bitmap.height
    var i = 0
    while (i < FRAME_COUNT) {
      srcRects[i].set(i * cellWidth + 2, 0, (i + 1) * cellWidth - 2, bitmapHeight)
      i++
    }
  }

  private fun loadKeyedSheet(): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, R.drawable.explosion_sheet, opts)
      ?: error("Missing drawable explosion_sheet")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyGreen(bmp)
    return bmp
  }

  private fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    var rowY = 0
    while (rowY < h) {
      bmp.getPixels(row, 0, w, 0, rowY, w, 1)
      var i = 0
      while (i < w) {
        val c = row[i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[i] = 0
        }
        i++
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
      rowY++
    }
  }

  private companion object {
    const val POOL_SIZE = 20
    const val FRAME_COUNT = 8
    const val LAST_FRAME = 7
    const val FRAME_SEC = 0.05f
    const val EXPLOSION_WIDTH_FRAC = 0.18f * 1.5f
  }
}
