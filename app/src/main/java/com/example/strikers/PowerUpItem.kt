package com.example.strikers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class PowerUpSlot {
  var x = 0f
  var y = 0f
  var vx = 0f
  var vy = 0f
  var isActive = false
  var itemType = PowerUpItem.ITEM_TYPE_POWERUP
  var medalFrameTime = 0f
  var medalFrameIndex = 0
}

class PowerUpItem {
  var x = 0f
  var y = 0f
  var vx = 0f
  var vy = 0f
  var isActive = false
  var itemType = ITEM_TYPE_POWERUP
  var medalFrameTime = 0f
  var medalFrameIndex = 0

  private val pool = Array(POOL_SIZE) { PowerUpSlot() }
  private val itemDst = RectF()
  private val medalPaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
  }

  fun spawn(startX: Float, startY: Float) {
    spawn(startX, startY, ITEM_TYPE_POWERUP)
  }

  fun spawn(startX: Float, startY: Float, type: Int) {
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (!s.isActive) {
        fillSlot(s, startX, startY, type)
        if (type == ITEM_TYPE_POWERUP) {
          syncLegacy(s)
        }
        return
      }
      i++
    }
  }

  fun update(dt: Float, screenW: Int) {
    update(dt, screenW, 2500)
  }

  fun update(dt: Float, screenW: Int, screenH: Int) {
    isActive = false
    val floor = screenH + 48f
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (s.isActive) {
        s.x += s.vx * dt
        s.y += s.vy * dt
        if (s.itemType != ITEM_TYPE_MEDAL) {
          if (s.x <= 30f) {
            s.x = 30f
            s.vx = -s.vx
          }
          if (s.x >= screenW - 30f) {
            s.x = screenW - 30f
            s.vx = -s.vx
          }
        }
        if (s.itemType == ITEM_TYPE_MEDAL) {
          s.medalFrameTime += dt
          while (s.medalFrameTime >= MEDAL_FRAME_SEC) {
            s.medalFrameTime -= MEDAL_FRAME_SEC
            s.medalFrameIndex++
            if (s.medalFrameIndex >= MEDAL_FRAME_COUNT) {
              s.medalFrameIndex = 0
            }
          }
        }
        if (s.y > floor) s.isActive = false
        if (s.isActive && s.itemType == ITEM_TYPE_POWERUP) {
          syncLegacy(s)
        }
      }
      i++
    }
  }

  fun getPool(): Array<PowerUpSlot> = pool

  fun getPoolSize(): Int = POOL_SIZE

  fun deactivateAll() {
    var i = 0
    while (i < POOL_SIZE) {
      pool[i].isActive = false
      i++
    }
    isActive = false
  }

  fun draw(canvas: Canvas, powerUpBmp: Bitmap?, medalFrames: Array<Bitmap?>, powerPaint: Paint) {
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (s.isActive) {
        if (s.itemType == ITEM_TYPE_MEDAL) {
          val frame = medalFrames[s.medalFrameIndex]
          if (frame != null) {
            val hx = MEDAL_HALF
            itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
            canvas.drawBitmap(frame, null, itemDst, medalPaint)
          }
        } else if (powerUpBmp != null) {
          val hx = POWERUP_HALF
          itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
          canvas.drawBitmap(powerUpBmp, null, itemDst, powerPaint)
        }
      }
      i++
    }
  }

  private fun fillSlot(s: PowerUpSlot, startX: Float, startY: Float, type: Int) {
    s.x = startX
    s.y = startY
    s.vx = if (type == ITEM_TYPE_MEDAL) {
      0f
    } else if ((startX.toInt() and 1) == 0) {
      120f
    } else {
      -120f
    }
    s.vy = if (type == ITEM_TYPE_MEDAL) 110f else 90f
    s.itemType = type
    s.medalFrameTime = 0f
    s.medalFrameIndex = 0
    s.isActive = true
  }

  private fun syncLegacy(s: PowerUpSlot) {
    x = s.x
    y = s.y
    vx = s.vx
    vy = s.vy
    itemType = s.itemType
    medalFrameTime = s.medalFrameTime
    medalFrameIndex = s.medalFrameIndex
    isActive = true
  }

  companion object {
    const val ITEM_TYPE_POWERUP = 0
    const val ITEM_TYPE_MEDAL = 1
    const val POOL_SIZE = 16
    const val MEDAL_FRAME_COUNT = 8
    const val MEDAL_FRAME_SEC = 1f / 15f
    const val POWERUP_HALF = 32f
    const val MEDAL_HALF = 36f
  }
}
