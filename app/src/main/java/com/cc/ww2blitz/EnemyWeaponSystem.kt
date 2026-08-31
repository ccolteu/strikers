package com.cc.ww2blitz

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class EnemyWeaponSystem {

  private val pool = Array(POOL_SIZE) { EnemyBullet() }
  private val bulletPaint = Paint().apply {
    color = 0xFF00FF66.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val innerGlowPaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val drawRect = RectF()
  private var screenW = 0f
  private var screenH = 0f
  private val clearX = FloatArray(CLEAR_SLOTS)
  private val clearY = FloatArray(CLEAR_SLOTS)
  private val clearT = FloatArray(CLEAR_SLOTS)

  private var turretTimer = 0f
  private var coreVentTimer = 0f
  private var magmaTimer = 0f
  private var spiralAngle = 0f
  private var spiralGate = 0f
  private var s5RingAlt = 0
  private var s5MagmaSeed = 1L

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
  }

  fun getBulletPool(): Array<EnemyBullet> = pool

  fun getPoolSize(): Int = POOL_SIZE

  fun deactivateAll() {
    var i = 0
    while (i < POOL_SIZE) {
      pool[i].isActive = false
      pool[i].flags = 0
      i++
    }
    i = 0
    while (i < CLEAR_SLOTS) {
      clearT[i] = 0f
      i++
    }
  }

  fun convertActiveToScoreItems() {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        PowerUpManager.instance.spawnBulletCancelDrop(b.x, b.y)
        b.isActive = false
        b.flags = 0
        b.vx = 0f
        b.vy = 0f
      }
      i++
    }
  }

  fun beginDeathClear(originX: Float, originY: Float) {
    var i = 0
    while (i < CLEAR_SLOTS) {
      if (clearT[i] <= 0f) {
        clearX[i] = originX
        clearY[i] = originY
        clearT[i] = CLEAR_LIFE
        return
      }
      i++
    }
    clearX[0] = originX
    clearY[0] = originY
    clearT[0] = CLEAR_LIFE
  }

  fun fireBullet(startX: Float, startY: Float, velX: Float, velY: Float) {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (!b.isActive) {
        b.x = startX
        b.y = startY
        b.vx = velX
        b.vy = velY
        b.flags = 0
        b.isActive = true
        return
      }
      i++
    }
  }

  fun resetStage5Boss() {
    turretTimer = S5_VOLLEY_INTERVAL
    coreVentTimer = S5_RING_INTERVAL
    magmaTimer = 0f
    spiralAngle = 0f
    spiralGate = 0f
    s5RingAlt = 0
  }

  fun updateStage5Boss(
    dt: Float,
    centerX: Float,
    centerY: Float,
    bossWidth: Float,
    playerX: Float,
    playerY: Float,
    leftDestroyed: Boolean,
    rightDestroyed: Boolean,
  ) {
    val leftTurretX = centerX - 0.38f * bossWidth
    val leftTurretY = centerY
    val rightTurretX = centerX + 0.38f * bossWidth
    val rightTurretY = centerY
    val coreVentX = centerX
    val coreVentY = centerY + 0.10f * bossWidth
    val leftLive = !leftDestroyed
    val rightLive = !rightDestroyed
    if (leftLive && rightLive) {
      turretTimer -= dt
      if (turretTimer <= 0f) {
        turretTimer = S5_VOLLEY_INTERVAL
        fireS5DownSpread(leftTurretX, leftTurretY)
        fireS5DownSpread(rightTurretX, rightTurretY)
      }
      coreVentTimer -= dt
      if (coreVentTimer <= 0f) {
        coreVentTimer = S5_RING_INTERVAL
        fireS5Ring(coreVentX, coreVentY)
      }
      return
    }
    if (leftLive || rightLive) {
      turretTimer -= dt
      if (turretTimer <= 0f) {
        turretTimer = S5_PANIC_INTERVAL
        if (leftLive) {
          fireS5PlayerFan(leftTurretX, leftTurretY, playerX, playerY)
        } else {
          fireS5PlayerFan(rightTurretX, rightTurretY, playerX, playerY)
        }
      }
      magmaTimer -= dt
      if (magmaTimer <= 0f) {
        magmaTimer = S5_MAGMA_INTERVAL
        var n = 0
        while (n < 3) {
          s5MagmaSeed = s5MagmaSeed * 1664525L + 1013904223L
          val u = ((s5MagmaSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
          val ox = (u * 0.30f - 0.15f) * bossWidth
          fireBullet(coreVentX + ox, coreVentY, 0f, S5_MAGMA_VY)
          n++
        }
      }
      return
    }
    spiralAngle += S5_SPIRAL_SPIN * dt
    spiralGate -= dt
    if (spiralGate <= 0f) {
      spiralGate = S5_SPIRAL_INTERVAL
      fireS5SpiralPair(coreVentX, coreVentY, spiralAngle)
      fireS5SpiralPair(coreVentX, coreVentY, -spiralAngle)
    }
  }

  private fun fireS5DownSpread(originX: Float, originY: Float) {
    var i = 0
    while (i < 3) {
      val ox = originX + (i - 1) * S5_BARREL_SEP
      fireBullet(ox, originY, 0f, S5_VOLLEY_VY)
      i++
    }
  }

  private fun fireS5Ring(originX: Float, originY: Float) {
    val twoPi = 6.2831855f
    val step = twoPi / 8f
    val phase = if (s5RingAlt == 0) 0f else step * 0.5f
    s5RingAlt = if (s5RingAlt == 0) 1 else 0
    var i = 0
    while (i < 8) {
      val ang = phase + i * step
      fireBullet(originX, originY, cos(ang) * S5_RING_SPEED, sin(ang) * S5_RING_SPEED)
      i++
    }
  }

  private fun fireS5PlayerFan(originX: Float, originY: Float, playerX: Float, playerY: Float) {
    val targetAngle = atan2(playerY - originY, playerX - originX)
    var i = 0
    while (i < 5) {
      val ang = targetAngle + (i - 2) * S5_FAN_STEP
      fireBullet(originX, originY, cos(ang) * S5_FAN_SPEED, sin(ang) * S5_FAN_SPEED)
      i++
    }
  }

  private fun fireS5SpiralPair(originX: Float, originY: Float, ang: Float) {
    fireBullet(originX, originY, cos(ang) * S5_SPIRAL_SPEED, sin(ang) * S5_SPIRAL_SPEED)
    fireBullet(
      originX,
      originY,
      cos(ang + 3.1415927f) * S5_SPIRAL_SPEED,
      sin(ang + 3.1415927f) * S5_SPIRAL_SPEED,
    )
  }

  fun update(dt: Float) {
    val w = screenW
    val h = screenH
    val r = HALF_BULLET_WIDTH
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        b.x += b.vx * dt
        b.y += b.vy * dt
        if (b.x + r < 0f || b.x - r > w || b.y + r < 0f || b.y - r > h) {
          b.isActive = false
          b.flags = 0
        }
      }
      i++
    }
    updateDeathClears(dt)
  }

  private fun updateDeathClears(dt: Float) {
    var i = 0
    while (i < CLEAR_SLOTS) {
      if (clearT[i] > 0f) {
        clearT[i] -= dt
        val age = CLEAR_LIFE - clearT[i]
        var u = age / CLEAR_LIFE
        if (u < 0f) u = 0f
        if (u > 1f) u = 1f
        val radius = CLEAR_RADIUS * u
        val rSq = radius * radius
        val ox = clearX[i]
        val oy = clearY[i]
        var bi = 0
        while (bi < POOL_SIZE) {
          val b = pool[bi]
          if (b.isActive) {
            val dx = b.x - ox
            val dy = b.y - oy
            if ((dx * dx) + (dy * dy) <= rSq) {
              b.isActive = false
              b.flags = 0
            }
          }
          bi++
        }
        if (clearT[i] < 0f) clearT[i] = 0f
      }
      i++
    }
  }

  fun draw(canvas: Canvas) {
    val hw = HALF_BULLET_WIDTH
    val hh = HALF_BULLET_HEIGHT
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        drawRect.set(b.x - hw, b.y - hh, b.x + hw, b.y + hh)
        canvas.drawOval(drawRect, bulletPaint)
        drawRect.set(b.x - hw + 5f, b.y - hh + 5f, b.x + hw - 5f, b.y + hh - 5f)
        canvas.drawOval(drawRect, innerGlowPaint)
      }
      i++
    }
  }

  companion object {
    const val POOL_SIZE = 220
    const val HALF_BULLET_WIDTH = 18f
    const val HALF_BULLET_HEIGHT = 18f
    const val CLEAR_SLOTS = 4
    const val CLEAR_RADIUS = 150f
    const val CLEAR_LIFE = 0.15f
    const val S5_VOLLEY_INTERVAL = 1.8f
    const val S5_PANIC_INTERVAL = 0.9f
    const val S5_RING_INTERVAL = 3.0f
    const val S5_VOLLEY_VY = 400f
    const val S5_BARREL_SEP = 14f
    const val S5_RING_SPEED = 360f
    const val S5_FAN_STEP = 0.1745329f
    const val S5_FAN_SPEED = 480f
    const val S5_MAGMA_INTERVAL = 1.5f
    const val S5_MAGMA_VY = 190f
    const val S5_SPIRAL_SPIN = 6.5f
    const val S5_SPIRAL_INTERVAL = 0.15f
    const val S5_SPIRAL_SPEED = 440f
  }
}
