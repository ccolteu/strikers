package com.cc.ww2blitz

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.ceil

/**
 * Three-layer vertical parallax background system engineered for zero runtime allocation.
 *
 * To prevent horizontal tearing on screens with aspect ratios shorter than the native
 * 1080x2400 artwork, assets are scaled horizontally to match the screen width exactly,
 * with the vertical height scaled by the identical proportion. Tiling and wrap boundaries
 * track these scaled asset heights directly rather than the screen viewport bounds,
 * preserving authored vertical loop seams.
 */
class ParallaxBackground(private val resources: Resources) {

  private var ground: Bitmap? = null
  private var mid: Bitmap? = null
  private var high: Bitmap? = null

  private var screenW = 0
  private var screenH = 0

  // Cached, proportionally-scaled asset heights to drive accurate stitching loops
  private var groundHeight = 0f
  private var midHeight = 0f
  private var highHeight = 0f

  // Stage 5 & 6 specific height caching structures
  private var stage5Layer1Height = 0f
  private var stage5Layer2Height = 0f
  private var stage6Layer2Height = 0f

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

    // Decode and apply width-locked scaling configurations to layer artwork
    ground = decodeWidthLockedScaled(resources, R.drawable.stage1_bg_layer1_ground, width)
    mid = decodeWidthLockedScaled(resources, R.drawable.stage1_bg_layer2_mid, width)
    high = decodeWidthLockedScaled(resources, R.drawable.stage1_bg_layer3_high, width)

    // Cache the precise pixel heights of the scaled assets for the wrapping pipeline
    groundHeight = ground?.height?.toFloat() ?: height.toFloat()
    midHeight = mid?.height?.toFloat() ?: height.toFloat()
    highHeight = high?.height?.toFloat() ?: height.toFloat()

    yGround = 0f
    yMid = 0f
    yHigh = 0f
    stage5Layer1Y = 0f
    stage5Layer2Y = 0f
    stage6Layer2Y = 0f
    stage6SpeedModifier = 1.0f
  }

  /** Updates your generic vertical scrolling offsets based on each layer's custom wrap boundaries. */
  fun update(baseSpeed: Float) {
    if (screenH <= 0) return
    yGround = wrap(yGround + baseSpeed * SPEED_GROUND, groundHeight)
    yMid = wrap(yMid + baseSpeed * SPEED_MID, midHeight)
    yHigh = wrap(yHigh + baseSpeed * SPEED_HIGH, highHeight)
  }

  /** Stage 5: floor at scrollSpeedY, canopy at 1.5x. Wrapped strictly by individual asset heights. */
  fun updateStage5(scrollSpeedY: Float, dt: Float) {
    if (screenH <= 0) return
    val speed = scrollSpeedY
    val frame = dt

    stage5Layer1Y += speed * frame
    if (stage5Layer1Y >= stage5Layer1Height) {
      stage5Layer1Y -= stage5Layer1Height
    }

    stage5Layer2Y += (speed * SPEED_S5_LAYER2) * frame
    if (stage5Layer2Y >= stage5Layer2Height) {
      stage5Layer2Y -= stage5Layer2Height
    }
  }

  /** Stage 6 ascent: scrollSpeedY scaled by timeline modifiers. Wrapped by individual asset heights. */
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

    if (stage6Layer2Height > 0f) {
      stage6Layer2Y += (scrollSpeedY * 1.5f) * dt
      if (stage6Layer2Y >= stage6Layer2Height) {
        stage6Layer2Y -= stage6Layer2Height
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

  /** Injects cached asset heights into the layout blitting calculations to preserve stitching boundaries. */
  fun draw(canvas: Canvas, groundOverride: Bitmap?, overlayClouds: Boolean) {
    if (screenH <= 0) return

    val groundBmp = groundOverride ?: ground
    val activeGroundH = if (groundOverride != null) groundOverride.height.toFloat() else groundHeight

    blit(canvas, groundBmp, yGround, activeGroundH, paintGround)
    if (!overlayClouds) return
    blit(canvas, mid, yMid, midHeight, paintMid)
    blit(canvas, high, yHigh, highHeight, paintHigh)
  }

  fun drawStage5Floor(canvas: Canvas, floor: Bitmap?) {
    if (floor != null) {
      stage5Layer1Height = floor.height.toFloat()
      blit(canvas, floor, stage5Layer1Y, stage5Layer1Height, paintGround)
    }
  }

  fun drawStage5Canopy(canvas: Canvas, canopy: Bitmap?, currentStage: Int) {
    if (currentStage != 5) return
    if (canopy == null || canopy.isRecycled) return
    stage5Layer2Height = canopy.height.toFloat()
    blit(canvas, canopy, stage5Layer2Y, stage5Layer2Height, paintStructures)
  }

  fun drawStage6Canopy(canvas: Canvas, canopy: Bitmap?, currentStage: Int) {
    if (currentStage != 6) return
    if (canopy == null || canopy.isRecycled) return
    stage6Layer2Height = canopy.height.toFloat()
    blit(canvas, canopy, stage6Layer2Y, stage6Layer2Height, paintStructures)
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

  /** Core tiling optimization: offsets secondary slices by asset height footprints instead of display dimensions. */
  private fun blit(canvas: Canvas, bitmap: Bitmap?, y: Float, assetH: Float, paint: Paint) {
    if (bitmap == null) return
    canvas.drawBitmap(bitmap, 0f, y, paint)
    canvas.drawBitmap(bitmap, 0f, y - assetH, paint)
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

    fun wrap(y: Float, maxBounds: Float): Float {
      var v = y % maxBounds
      if (v < 0f) v += maxBounds
      return v
    }
  }
}

/** Scales asset to span target width perfectly, preserving uncropped vertical aspect ratio and loop seams. */
internal fun decodeWidthLockedScaled(resources: Resources, id: Int, targetW: Int): Bitmap {
  val opts = BitmapFactory.Options().apply {
    inScaled = false
    inPreferredConfig = Bitmap.Config.ARGB_8888
  }
  val src = BitmapFactory.decodeResource(resources, id, opts)
    ?: error("Missing drawable $id")
  if (src.width == targetW) return src

  val widthScale = targetW.toFloat() / src.width.toFloat()
  val scaledW = targetW
  val scaledH = ceil(src.height * widthScale).toInt()

  val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
  if (scaled !== src) src.recycle()
  return scaled
}
