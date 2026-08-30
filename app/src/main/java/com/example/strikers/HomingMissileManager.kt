package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF

class HomingMissileManager(resources: Resources) {
  class Missile {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var isActive = false
  }

  private val pool = Array(POOL_SIZE) { Missile() }
  private val bitmapPaint = Paint().apply { isFilterBitmap = true }
  private val outlinePaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val shadowPaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
  }
  private val drawRect = RectF()
  private var missileBitmap: Bitmap? = loadKeyedMissile(resources)
  private var screenW = 0f
  private var screenH = 0f

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
  }

  fun getPool(): Array<Missile> = pool
  fun getPoolSize(): Int = POOL_SIZE

  fun deactivateAll() {
    var i = 0
    while (i < POOL_SIZE) {
      pool[i].isActive = false
      i++
    }
  }

  fun fireMissile(startX: Float, startY: Float, initVx: Float, initVy: Float) {
    var i = 0
    while (i < POOL_SIZE) {
      val m = pool[i]
      if (!m.isActive) {
        m.x = startX
        m.y = startY
        m.vx = initVx
        m.vy = initVy
        m.isActive = true
        return
      }
      i++
    }
  }

  fun update(dt: Float, enemyPool: Array<Enemy>, poolSize: Int, boss: BossController) {
    var i = 0
    while (i < POOL_SIZE) {
      val m = pool[i]
      if (m.isActive) {
        var targetX = 0f
        var targetY = -100f
        var foundTarget = false
        var closestDistSq = Float.MAX_VALUE

        var ei = 0
        while (ei < poolSize) {
          val enemy = enemyPool[ei]
          if (enemy.isActive && enemy.y > 0f) {
            val dx = enemy.x - m.x
            val dy = enemy.y - m.y
            val distSq = dx * dx + dy * dy
            if (distSq < closestDistSq) {
              closestDistSq = distSq
              targetX = enemy.x
              targetY = enemy.y
              foundTarget = true
            }
          }
          ei++
        }

        if (boss.isActive()) {
          val parts = boss.getComponents()
          var pi = 0
          while (pi < boss.getComponentCount()) {
            val part = parts[pi]
            if (!part.isDestroyed && part.halfW > 0f && part.y > 0f) {
              val dx = part.x - m.x
              val dy = part.y - m.y
              val distSq = dx * dx + dy * dy
              if (distSq < closestDistSq) {
                closestDistSq = distSq
                targetX = part.x
                targetY = part.y
                foundTarget = true
              }
            }
            pi++
          }
        }

        if (foundTarget) {
          val dx = targetX - m.x
          val dy = targetY - m.y
          val len = kotlin.math.sqrt(dx * dx + dy * dy)
          if (len > 0.1f) {
            val targetVx = (dx / len) * MISSILE_SPEED
            val targetVy = (dy / len) * MISSILE_SPEED
            m.vx += (targetVx - m.vx) * TURN_RATE * dt
            m.vy += (targetVy - m.vy) * TURN_RATE * dt
          }
        } else {
          m.vy += (-MISSILE_SPEED - m.vy) * TURN_RATE * dt
        }

        m.x += m.vx * dt
        m.y += m.vy * dt

        if (m.y < -30f || m.x < -30f || m.x > screenW + 30f || m.y > screenH + 30f) {
          m.isActive = false
        }
      }
      i++
    }
  }

  fun draw(canvas: Canvas) {
    val bmp = missileBitmap ?: return
    val hw = DRAW_W * 0.5f
    val hh = DRAW_H * 0.5f
    var i = 0
    while (i < POOL_SIZE) {
      val m = pool[i]
      if (m.isActive) {
        val deg = kotlin.math.atan2(m.vy, m.vx) * RAD_TO_DEG + NOSE_UP_OFFSET_DEG
        canvas.save()
        canvas.translate(m.x, m.y)
        canvas.rotate(deg)
        drawRect.set(-hw, -hh, hw, hh)
        drawRect.offset(SHADOW_PX, SHADOW_PX)
        canvas.drawBitmap(bmp, null, drawRect, shadowPaint)
        drawRect.offset(-SHADOW_PX, -SHADOW_PX)
        var oy = -OUTLINE_PX
        while (oy <= OUTLINE_PX) {
          var ox = -OUTLINE_PX
          while (ox <= OUTLINE_PX) {
            if (ox != 0f || oy != 0f) {
              drawRect.offset(ox, oy)
              canvas.drawBitmap(bmp, null, drawRect, outlinePaint)
              drawRect.offset(-ox, -oy)
            }
            ox += OUTLINE_PX
          }
          oy += OUTLINE_PX
        }
        canvas.drawBitmap(bmp, null, drawRect, bitmapPaint)
        canvas.restore()
      }
      i++
    }
  }

  fun release() {
    deactivateAll()
    val bmp = missileBitmap
    if (bmp != null && !bmp.isRecycled) bmp.recycle()
    missileBitmap = null
  }

  private fun loadKeyedMissile(resources: Resources): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, R.drawable.player_missile, opts)
      ?: error("Missing drawable player_missile")
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

  companion object {
    const val POOL_SIZE = 8
    const val MISSILE_SPEED = 650f
    const val TURN_RATE = 8.5f
    private const val DRAW_W = 48f
    private const val DRAW_H = 96f
    private const val SHADOW_PX = 2f
    private const val OUTLINE_PX = 3f
    private const val RAD_TO_DEG = 57.2957795f
    private const val NOSE_UP_OFFSET_DEG = 90f
  }
}
