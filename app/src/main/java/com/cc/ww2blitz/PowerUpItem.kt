package com.cc.ww2blitz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import kotlin.math.sin

class PowerUpSlot {
  var x = 0f
  var y = 0f
  var vx = 0f
  var vy = 0f
  var homeX = 0f
  var swayT = 0f
  var swayDrop = false
  var pickupPoints = 0
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
  private val medalOutlinePaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val medalShadowPaint = Paint().apply {
    isFilterBitmap = false
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
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
        if (type == ITEM_TYPE_POWERUP || type == ITEM_TYPE_BOMB) {
          syncLegacy(s)
        }
        return
      }
      i++
    }
  }

  fun spawnSway(startX: Float, startY: Float, type: Int) {
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (!s.isActive) {
        fillSlot(s, startX, startY, type)
        s.vx = 0f
        s.vy = SWAY_VY
        s.homeX = startX
        s.swayT = 0f
        s.swayDrop = true
        if (type != ITEM_TYPE_MEDAL) {
          syncLegacy(s)
        }
        return
      }
      i++
    }
  }

  fun spawnStationaryMedal(startX: Float, startY: Float, points: Int): Boolean {
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (!s.isActive) {
        fillSlot(s, startX, startY, ITEM_TYPE_MEDAL)
        s.vx = 0f
        s.vy = 0f
        s.pickupPoints = points
        return true
      }
      i++
    }
    return false
  }

  fun update(
    dt: Float,
    screenW: Int,
    screenH: Int,
    playerX: Float,
    playerY: Float,
    magnetOn: Boolean = true,
  ) {
    isActive = false
    val floor = screenH + 48f
    val edge = screenW * EDGE_STRIP_FRAC
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (s.isActive) {
        if (s.swayDrop) {
          s.swayT += dt
          s.x = s.homeX + sin(s.swayT * SWAY_RATE) * SWAY_AMP
          s.y += s.vy * dt
        } else {
          s.x += s.vx * dt
          s.y += s.vy * dt
        }
        if (s.itemType != ITEM_TYPE_MEDAL && !s.swayDrop) {
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
          if (magnetOn) {
            pullMedalTowardPlayer(s, dt, playerX, playerY, edge, screenW.toFloat())
          }
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
        if (s.isActive && (s.itemType == ITEM_TYPE_POWERUP || s.itemType == ITEM_TYPE_BOMB)) {
          syncLegacy(s)
        }
      }
      i++
    }
  }

  private fun pullMedalTowardPlayer(
    s: PowerUpSlot,
    dt: Float,
    playerX: Float,
    playerY: Float,
    edge: Float,
    screenW: Float,
  ) {
    val dx = playerX - s.x
    val dy = playerY - s.y
    val distSq = dx * dx + dy * dy
    var radius = MAGNET_RADIUS
    if (s.x < edge && playerX < edge * 1.35f) {
      radius = MAGNET_EDGE_RADIUS
    } else if (s.x > screenW - edge && playerX > screenW - edge * 1.35f) {
      radius = MAGNET_EDGE_RADIUS
    }
    if (distSq > radius * radius || distSq <= 0.0001f) return
    val dist = kotlin.math.sqrt(distSq)
    val step = MAGNET_SPEED * dt
    if (step >= dist) {
      s.x = playerX
      s.y = playerY
      s.swayDrop = false
    } else {
      val inv = step / dist
      s.x += dx * inv
      s.y += dy * inv
      s.swayDrop = false
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

  fun draw(
    canvas: Canvas,
    powerUpBmp: Bitmap?,
    bombBmp: Bitmap?,
    medalFrames: Array<Bitmap?>,
    powerPaint: Paint,
  ) {
    var i = 0
    while (i < POOL_SIZE) {
      val s = pool[i]
      if (s.isActive) {
        if (s.itemType == ITEM_TYPE_MEDAL) {
          val frame = medalFrames[s.medalFrameIndex]
          if (frame != null) {
            val hx = MEDAL_HALF
            itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
            blitOutlined(canvas, frame, medalPaint)
          }
        } else if (s.itemType == ITEM_TYPE_BOMB) {
          if (bombBmp != null) {
            val hx = POWERUP_HALF
            itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
            blitOutlined(canvas, bombBmp, powerPaint)
          }
        } else if (s.itemType == ITEM_TYPE_SHIELD) {
          if (powerUpBmp != null) {
            val hx = POWERUP_HALF
            itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
            blitOutlined(canvas, powerUpBmp, powerPaint)
          }
        } else if (powerUpBmp != null) {
          val hx = POWERUP_HALF
          itemDst.set(s.x - hx, s.y - hx, s.x + hx, s.y + hx)
          blitOutlined(canvas, powerUpBmp, powerPaint)
        }
      }
      i++
    }
  }

  private fun blitOutlined(canvas: Canvas, bmp: Bitmap, bodyPaint: Paint) {
    itemDst.offset(SHADOW_PX, SHADOW_PX)
    canvas.drawBitmap(bmp, null, itemDst, medalShadowPaint)
    itemDst.offset(-SHADOW_PX, -SHADOW_PX)
    var oy = -OUTLINE_PX
    while (oy <= OUTLINE_PX) {
      var ox = -OUTLINE_PX
      while (ox <= OUTLINE_PX) {
        if (ox != 0f || oy != 0f) {
          itemDst.offset(ox, oy)
          canvas.drawBitmap(bmp, null, itemDst, medalOutlinePaint)
          itemDst.offset(-ox, -oy)
        }
        ox += OUTLINE_PX
      }
      oy += OUTLINE_PX
    }
    canvas.drawBitmap(bmp, null, itemDst, bodyPaint)
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
    s.homeX = startX
    s.swayT = 0f
    s.swayDrop = false
    s.pickupPoints = 0
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
    const val ITEM_TYPE_BOMB = 2
    const val ITEM_TYPE_SHIELD = 3
    const val POOL_SIZE = 48
    const val MEDAL_FRAME_COUNT = 8
    const val MEDAL_FRAME_SEC = 1f / 15f
    const val POWERUP_HALF = 78f
    const val MEDAL_HALF = 36f
    const val OUTLINE_PX = 3f
    const val SHADOW_PX = 2f
    const val SWAY_VY = 72f
    const val SWAY_RATE = 3.2f
    const val SWAY_AMP = 28f
    const val MAGNET_RADIUS = 96f
    const val MAGNET_EDGE_RADIUS = 188f
    const val MAGNET_SPEED = 420f
    const val EDGE_STRIP_FRAC = 0.10f
  }
}
