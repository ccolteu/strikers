package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Three-layer vertical parallax. Bitmaps are decoded and scaled once to the
 * surface size; [update] / [draw] only mutate floats and blit.
 *
 * Mid and high layers are authored on black. [PorterDuff.Mode.SCREEN] drops
 * the black and keeps the clouds. High clouds also use a fixed 40% alpha.
 */
class ParallaxBackground(private val resources: Resources) {

  private var ground: Bitmap? = null
  private var mid: Bitmap? = null
  private var high: Bitmap? = null

  private var screenW = 0
  private var screenH = 0

  private var yGround = 0f
  private var yMid = 0f
  private var yHigh = 0f

  private val paintGround = Paint()
  private val paintMid = Paint().apply {
    isFilterBitmap = false
    alpha = MID_ALPHA
    xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
  }
  private val paintHigh = Paint().apply {
    isFilterBitmap = false
    alpha = HIGH_ALPHA
    xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
  }

  fun onSizeChanged(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    if (width == screenW && height == screenH && ground != null) return
    screenW = width
    screenH = height
    recycle(ground)
    recycle(mid)
    recycle(high)
    ground = loadScaled(R.drawable.bg_layer1_ground, width, height)
    mid = loadScaled(R.drawable.bg_layer2_mid, width, height)
    high = loadScaled(R.drawable.bg_layer3_high, width, height)
    yGround = 0f
    yMid = 0f
    yHigh = 0f
  }

  /** [baseSpeed] is pixels this frame for layer 1. Layers 2/3 use 1.5x and 2.2x. */
  fun update(baseSpeed: Float) {
    val h = screenH.toFloat()
    if (h <= 0f) return
    yGround = wrap(yGround + baseSpeed * SPEED_GROUND, h)
    yMid = wrap(yMid + baseSpeed * SPEED_MID, h)
    yHigh = wrap(yHigh + baseSpeed * SPEED_HIGH, h)
  }

  fun resetScroll() {
    yGround = 0f
    yMid = 0f
    yHigh = 0f
  }

  fun draw(canvas: Canvas, groundOverride: Bitmap?) {
    val h = screenH.toFloat()
    if (h <= 0f) return
    val groundBmp = groundOverride ?: ground
    blit(canvas, groundBmp, yGround, h, paintGround)
    if (groundOverride != null) return
    blit(canvas, mid, yMid, h, paintMid)
  }

  fun release() {
    recycle(ground)
    recycle(mid)
    recycle(high)
    ground = null
    mid = null
    high = null
    screenW = 0
    screenH = 0
  }

  private fun blit(canvas: Canvas, bitmap: Bitmap?, y: Float, h: Float, paint: Paint) {
    if (bitmap == null) return
    canvas.drawBitmap(bitmap, 0f, y, paint)
    canvas.drawBitmap(bitmap, 0f, y - h, paint)
  }

  private fun loadScaled(id: Int, width: Int, height: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val src = BitmapFactory.decodeResource(resources, id, opts)
      ?: error("Missing drawable $id")
    if (src.width == width && src.height == height) return src
    val scaled = Bitmap.createScaledBitmap(src, width, height, true)
    if (scaled !== src) src.recycle()
    return scaled
  }

  private fun recycle(bitmap: Bitmap?) {
    if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
  }

  private companion object {
    const val SPEED_GROUND = 1.0f
    const val SPEED_MID = 1.5f
    const val SPEED_HIGH = 2.2f
    const val MID_ALPHA = 140
    const val HIGH_ALPHA = 36

    fun wrap(y: Float, h: Float): Float {
      var v = y % h
      if (v < 0f) v += h
      return v
    }
  }
}
