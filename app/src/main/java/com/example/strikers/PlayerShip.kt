package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent

/**
 * Relative-drag player with a single 7-frame bank strip.
 * Frame 1 = hard left, Frame 4 = idle, Frame 7 = hard right.
 * Source cells are cached [Rect]s; the sheet stays one GPU texture.
 */
class PlayerShip(private val resources: Resources) {

  private var sheet: Bitmap? = null
  private val srcFrames = Array(FRAME_COUNT) { Rect() }
  private val dst = Rect()
  private val paint = Paint().apply { isFilterBitmap = true }

  private var screenW = 0
  private var screenH = 0
  private var halfW = 0f
  private var halfH = 0f

  private var x = 0f
  private var y = 0f
  private var health = 3
  private var isInvulnerable = false
  private var invulnTimer = 0f
  private var isGameOverFlag = false

  private var pointerId = MotionEvent.INVALID_POINTER_ID
  private var lastTouchX = 0f
  private var lastTouchY = 0f
  private var targetVelocityX = 0f
  private var isDragging = false
  private var isMovingHorizontal = false

  private var currentFrameIndex = IDLE_FRAME
  private var targetFrameIndex = IDLE_FRAME

  fun onSizeChanged(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    screenW = width
    screenH = height
    if (sheet == null) {
      sheet = loadKeyedSheet()
      mapHardcodedSrcRects()
    }
    val cellW = srcFrames[IDLE_INDEX].width()
    val cellH = srcFrames[IDLE_INDEX].height()
    val drawW = (width * SHIP_WIDTH_FRAC).toInt().coerceAtLeast(1)
    val drawH = (drawW * (cellH.toFloat() / cellW.toFloat())).toInt().coerceAtLeast(1)
    halfW = drawW * 0.5f
    halfH = drawH * 0.5f
    x = width * 0.5f
    y = height * 0.78f
    currentFrameIndex = IDLE_FRAME
    targetFrameIndex = IDLE_FRAME
    targetVelocityX = 0f
    isDragging = false
    isMovingHorizontal = false
    clamp()
    writeDst()
  }

  fun onTouch(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        if (pointerId == MotionEvent.INVALID_POINTER_ID) {
          val index = event.actionIndex
          pointerId = event.getPointerId(index)
          lastTouchX = event.getX(index)
          lastTouchY = event.getY(index)
          isDragging = true
        }
      }
      MotionEvent.ACTION_MOVE -> {
        val index = event.findPointerIndex(pointerId)
        if (index >= 0 && isDragging) {
          val tx = event.getX(index)
          val ty = event.getY(index)
          val dx = tx - lastTouchX
          val dy = ty - lastTouchY
          x += dx
          y += dy
          targetVelocityX = dx
          if (kotlin.math.abs(dx) > MOVE_THRESHOLD) {
            isMovingHorizontal = true
          }
          lastTouchX = tx
          lastTouchY = ty
          clamp()
          writeDst()
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
        releaseSteer()
      }
      MotionEvent.ACTION_POINTER_UP -> {
        if (event.getPointerId(event.actionIndex) == pointerId) {
          releaseSteer()
        }
      }
    }
    return true
  }

  private fun releaseSteer() {
    pointerId = MotionEvent.INVALID_POINTER_ID
    isDragging = false
    isMovingHorizontal = false
    targetVelocityX = 0f
    targetFrameIndex = IDLE_FRAME
  }

  fun update(dt: Float) {
    if (!isDragging || !isMovingHorizontal) {
      targetFrameIndex = IDLE_FRAME
    } else {
      targetFrameIndex = if (targetVelocityX < 0f) 6f else 0f
    }
    val lerp = (FRAME_LERP * (dt * 60f)).coerceIn(0f, 1f)
    currentFrameIndex += (targetFrameIndex - currentFrameIndex) * lerp
    isMovingHorizontal = false
    if (isInvulnerable) {
      invulnTimer -= dt
      if (invulnTimer <= 0f) {
        invulnTimer = 0f
        isInvulnerable = false
      }
    }
    clamp()
    writeDst()
  }

  fun getHitboxX(): Float = x

  fun getHitboxY(): Float = y

  fun getHealth(): Int = health

  fun isGameOver(): Boolean = isGameOverFlag

  fun takeDamage() {
    if (isInvulnerable || isGameOverFlag) return
    health -= 1
    if (health <= 0) {
      health = 0
      isGameOverFlag = true
      return
    }
    isInvulnerable = true
    invulnTimer = INVULN_SEC
  }

  fun isFiringHeld(): Boolean = isDragging

  fun leftMuzzleX(): Float = x - halfW * MUZZLE_X_FRAC

  fun rightMuzzleX(): Float = x + halfW * MUZZLE_X_FRAC

  fun muzzleY(): Float = y - halfH * MUZZLE_Y_FRAC

  fun draw(canvas: Canvas) {
    if (isGameOverFlag) return
    if (isInvulnerable && ((invulnTimer * 20f).toInt() and 1) == 0) return
    val bmp = sheet ?: return
    val frame = kotlin.math.round(currentFrameIndex).toInt().coerceIn(0, FRAME_COUNT - 1)
    canvas.drawBitmap(bmp, srcFrames[frame], dst, paint)
  }

  fun release() {
    val bmp = sheet
    if (bmp != null && !bmp.isRecycled) bmp.recycle()
    sheet = null
  }

  private fun clamp() {
    val maxX = screenW - halfW
    val maxY = screenH - halfH
    if (maxX < halfW || maxY < halfH) return
    x = x.coerceIn(halfW, maxX)
    y = y.coerceIn(halfH, maxY)
  }

  private fun writeDst() {
    dst.set(
      (x - halfW).toInt(),
      (y - halfH).toInt(),
      (x + halfW).toInt(),
      (y + halfH).toInt(),
    )
  }

  private fun mapHardcodedSrcRects() {
    val bmp = sheet ?: return
    val sheetWidth = bmp.width.toFloat()

    val cellWidthFloat = sheetWidth / FRAME_COUNT.toFloat()
    val topCoord = 400
    val bottomCoord = 600

    for (i in 0 until FRAME_COUNT) {
      val exactLeft = i * cellWidthFloat
      val exactRight = (i + 1) * cellWidthFloat

      // This adds an 8-pixel horizontal cushion inside each cell,
      // cleanly separating the wings from neighboring fields.
      val leftInt = exactLeft.toInt() + 8
      val rightInt = exactRight.toInt() - 8

      srcFrames[i].set(leftInt, topCoord, rightInt, bottomCoord)
    }
  }

  private fun loadKeyedSheet(): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, R.drawable.player_sprite_sheet, opts)
      ?: error("Missing drawable player_sprite_sheet")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyGreen(bmp)
    return bmp
  }

  private fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    for (rowY in 0 until h) {
      bmp.getPixels(row, 0, w, 0, rowY, w, 1)
      for (i in 0 until w) {
        val c = row[i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[i] = 0
        }
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
    }
  }

  private companion object {
    const val FRAME_COUNT = 7
    const val IDLE_INDEX = 3
    const val IDLE_FRAME = 3f

    // Ensure this sits at 0.16f or 0.18f to give your wide-winged plane a crisp arcade presence
    const val SHIP_WIDTH_FRAC = 0.16f

    const val MOVE_THRESHOLD = 0.5f
    const val FRAME_LERP = 0.2f
    const val MUZZLE_X_FRAC = 0.42f
    const val MUZZLE_Y_FRAC = 0.38f
    const val INVULN_SEC = 2.0f
  }
}
