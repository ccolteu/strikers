package com.cc.ww2blitz

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.ceil
import kotlin.math.max

/**
 * Three-layer vertical parallax. Bitmaps are decoded once and **cover-scaled**
 * (uniform scale, center crop) to the surface so 2:3 art is not stretched on
 * 9:16 phones. [update] / [draw] only mutate floats and blit.
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
  private var stage5Layer1Y = 0f
  private var stage5Layer2Y = 0f
  private var stage6Layer2Y = 0f
  var stage6SpeedModifier = 1.0f

  private val paintGround = Paint()
  private val paintStructures = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
  }
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
    ground = decodeCoverScaled(resources, R.drawable.stage1_bg_layer1_ground, width, height)
    mid = decodeCoverScaled(resources, R.drawable.stage1_bg_layer2_mid, width, height)
    high = decodeCoverScaled(resources, R.drawable.stage1_bg_layer3_high, width, height)
    yGround = 0f
    yMid = 0f
    yHigh = 0f
    stage5Layer1Y = 0f
    stage5Layer2Y = 0f
    stage6Layer2Y = 0f
    stage6SpeedModifier = 1.0f
  }

  /** [baseSpeed] is pixels this frame for layer 1. Layers 2/3 use 1.5x and 2.2x. */
  fun update(baseSpeed: Float) {
    val h = screenH.toFloat()
    if (h <= 0f) return
    yGround = wrap(yGround + baseSpeed * SPEED_GROUND, h)
    yMid = wrap(yMid + baseSpeed * SPEED_MID, h)
    yHigh = wrap(yHigh + baseSpeed * SPEED_HIGH, h)
  }

  /**
   * Stage 5: floor at [scrollSpeedY], canopy at 1.5x. Wrap by canvas height.
   */
  fun updateStage5(scrollSpeedY: Float, dt: Float) {
    val canvasHeight = screenH.toFloat()
    if (canvasHeight <= 0f) return
    val speed = scrollSpeedY
    val frame = dt
    stage5Layer1Y += speed * frame
    if (stage5Layer1Y >= canvasHeight) {
      stage5Layer1Y -= canvasHeight
    }
    stage5Layer2Y += (speed * SPEED_S5_LAYER2) * frame
    if (stage5Layer2Y >= canvasHeight) {
      stage5Layer2Y -= canvasHeight
    }
  }

  /**
   * Stage 6 ascent: [scrollSpeedY] scaled by [stage6SpeedModifier] from the
   * stage clock [elapsedTime]. Wrap uses the same three-layer blit as other maps.
   */
  fun updateStage6(scrollSpeedY: Float, dt: Float, elapsedTime: Float) {
    val t = elapsedTime
    if (t < S6_CLOUD_END) {
      stage6SpeedModifier = 1.0f
    } else if (t < S6_BURN_END) {
      val u = (t - S6_CLOUD_END) / S6_BURN_SPAN
      stage6SpeedModifier = 1.0f + u * (S6_BURN_PEAK - 1.0f)
    } else if (t < S6_ORBIT_END) {
      val u = (t - S6_BURN_END) / S6_ORBIT_SPAN
      stage6SpeedModifier = S6_BURN_PEAK + u * (S6_ORBIT_FLOOR - S6_BURN_PEAK)
    } else if (t < S6_BRAKE_END) {
      val u = (t - S6_ORBIT_END) / S6_BRAKE_SPAN
      stage6SpeedModifier = S6_ORBIT_FLOOR + u * (0f - S6_ORBIT_FLOOR)
    } else {
      stage6SpeedModifier = 0f
    }
    val speed = scrollSpeedY * stage6SpeedModifier
    update(speed * dt)
    val canvasHeight = screenH.toFloat()
    if (canvasHeight > 0f) {
      stage6Layer2Y += (scrollSpeedY * 1.5f) * dt
      if (stage6Layer2Y >= canvasHeight) {
        stage6Layer2Y -= canvasHeight
      }
    }
  }

  fun resetScroll() {
    yGround = 0f
    yMid = 0f
    yHigh = 0f
    stage5Layer1Y = 0f
    stage5Layer2Y = 0f
    stage6Layer2Y = 0f
    stage6SpeedModifier = 1.0f
  }

  fun draw(canvas: Canvas, groundOverride: Bitmap?, overlayClouds: Boolean) {
    val h = screenH.toFloat()
    if (h <= 0f) return
    val groundBmp = groundOverride ?: ground
    blit(canvas, groundBmp, yGround, h, paintGround)
    if (!overlayClouds) return
    blit(canvas, mid, yMid, h, paintMid)
    blit(canvas, high, yHigh, h, paintHigh)
  }

  fun drawStage5Floor(canvas: Canvas, floor: Bitmap?) {
    if (floor != null) {
      blitStage5(canvas, floor, stage5Layer1Y, paintGround)
    }
  }

  fun drawStage5Canopy(canvas: Canvas, canopy: Bitmap?, currentStage: Int) {
    if (currentStage != 5) return
    if (canopy == null || canopy.isRecycled) return
    blitStage5(canvas, canopy, stage5Layer2Y, paintStructures)
  }

  fun drawStage6Canopy(canvas: Canvas, canopy: Bitmap?, currentStage: Int) {
    if (currentStage != 6) return
    if (canopy == null || canopy.isRecycled) return
    blitStage5(canvas, canopy, stage6Layer2Y, paintStructures)
  }

  fun drawStage5(canvas: Canvas, floor: Bitmap?, canopy: Bitmap?, currentStage: Int) {
    if (currentStage != 5) return
    drawStage5Floor(canvas, floor)
    drawStage5Canopy(canvas, canopy, currentStage)
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

  private fun blitStage5(canvas: Canvas, bitmap: Bitmap, y: Float, paint: Paint) {
    val canvasHeight = screenH.toFloat()
    if (canvasHeight <= 0f) return
    canvas.drawBitmap(bitmap, 0f, y, paint)
    canvas.drawBitmap(bitmap, 0f, y - canvasHeight, paint)
  }

  private fun recycle(bitmap: Bitmap?) {
    if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
  }

  private companion object {
    const val SPEED_GROUND = 1.0f
    const val SPEED_MID = 1.5f
    const val SPEED_HIGH = 2.2f
    const val SPEED_S5_LAYER2 = 1.5f
    const val S6_CLOUD_END = 15.0f
    const val S6_BURN_END = 35.0f
    const val S6_BURN_SPAN = 20.0f
    const val S6_BURN_PEAK = 3.5f
    const val S6_ORBIT_END = 45.0f
    const val S6_ORBIT_SPAN = 10.0f
    const val S6_ORBIT_FLOOR = 0.4f
    const val S6_BRAKE_END = 50.0f
    const val S6_BRAKE_SPAN = 5.0f
    const val MID_ALPHA = 140
    const val HIGH_ALPHA = 36

    fun wrap(y: Float, h: Float): Float {
      var v = y % h
      if (v < 0f) v += h
      return v
    }
  }
}

/** Uniform scale to cover [width]x[height], then center-crop. Does not stretch. */
internal fun decodeCoverScaled(resources: Resources, id: Int, width: Int, height: Int): Bitmap {
  val opts = BitmapFactory.Options().apply {
    inScaled = false
    inPreferredConfig = Bitmap.Config.ARGB_8888
  }
  val src = BitmapFactory.decodeResource(resources, id, opts)
    ?: error("Missing drawable $id")
  if (src.width == width && src.height == height) return src
  val scale = max(width.toFloat() / src.width, height.toFloat() / src.height)
  val scaledW = ceil(src.width * scale).toInt().coerceAtLeast(width)
  val scaledH = ceil(src.height * scale).toInt().coerceAtLeast(height)
  val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
  if (scaled !== src) src.recycle()
  if (scaled.width == width && scaled.height == height) return scaled
  val cropX = ((scaled.width - width) / 2).coerceAtLeast(0)
  val cropY = ((scaled.height - height) / 2).coerceAtLeast(0)
  val cropW = width.coerceAtMost(scaled.width - cropX)
  val cropH = height.coerceAtMost(scaled.height - cropY)
  val cropped = Bitmap.createBitmap(scaled, cropX, cropY, cropW, cropH)
  if (cropped !== scaled) scaled.recycle()
  return cropped
}
