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
  private val pinkPaint = Paint().apply {
    color = 0xFFFF4EC8.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val laserPaint = Paint().apply {
    color = 0xFFE040FB.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val cyanPaint = Paint().apply {
    color = 0xFF18F0FF.toInt()
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
  private var spiralGate = S6_SPIRAL_INTERVAL
  private var s5RingAlt = 0
  private var s5MagmaSeed = 1L
  private var s6RailAlt = 0
  private var s6LaserTimer = S6_WAVE_INTERVAL
  private var s6RingTimer = S6_RING_INTERVAL
  private var s6BurstRemaining = 0
  private var s6BurstGap = S6_BURST_GAP

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
    fireBullet(startX, startY, velX, velY, 0)
  }

  fun fireBullet(startX: Float, startY: Float, velX: Float, velY: Float, flags: Int) {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (!b.isActive) {
        b.x = startX
        b.y = startY
        b.vx = velX
        b.vy = velY
        b.flags = flags
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

  fun resetStage6Boss() {
    turretTimer = S6_RAIL_INTERVAL
    s6LaserTimer = S6_WAVE_INTERVAL
    s6RingTimer = S6_RING_INTERVAL
    spiralAngle = 0f
    spiralGate = S6_SPIRAL_INTERVAL
    s6RailAlt = 0
    s6BurstRemaining = 0
    s6BurstGap = S6_BURST_GAP
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

  fun updateStage6Boss(
    dt: Float,
    leftX: Float,
    leftY: Float,
    rightX: Float,
    rightY: Float,
    lensX: Float,
    lensY: Float,
    playerX: Float,
    playerY: Float,
    leftDestroyed: Boolean,
    rightDestroyed: Boolean,
  ) {
    val leftLive = !leftDestroyed
    val rightLive = !rightDestroyed
    if (leftLive && rightLive) {
      if (s6BurstRemaining <= 0) {
        turretTimer -= dt
        if (turretTimer <= 0f) {
          turretTimer = S6_RAIL_INTERVAL
          s6BurstRemaining = S6_BURST_COUNT
          s6BurstGap = S6_BURST_GAP
        }
      }
      if (s6BurstRemaining > 0) {
        s6BurstGap -= dt
        if (s6BurstGap <= 0f) {
          val ox = if (s6RailAlt == 0) leftX else rightX
          val oy = if (s6RailAlt == 0) leftY else rightY
          fireS6AimedTriple(ox, oy, playerX, playerY)
          s6BurstRemaining -= 1
          s6BurstGap = S6_BURST_GAP
          if (s6BurstRemaining == 0) {
            s6RailAlt = if (s6RailAlt == 0) 1 else 0
          }
        }
      }
      if (s6LaserTimer <= 0f) {
        s6LaserTimer = S6_WAVE_INTERVAL
      }
      s6LaserTimer -= dt
      if (s6LaserTimer <= 0f) {
        s6LaserTimer = S6_WAVE_INTERVAL
        fireS6CoreColumn(lensX, lensY)
      }
      return
    }
    if (leftLive || rightLive) {
      turretTimer -= dt
      if (turretTimer <= 0f) {
        turretTimer = S6_OVERCHARGE_INTERVAL
        val ox = if (leftLive) leftX else rightX
        val oy = if (leftLive) leftY else rightY
        fireBullet(ox, oy, 0f, S6_STREAM_SPEED, EnemyBullet.FLAG_CYAN)
      }
      s6RingTimer -= dt
      if (s6RingTimer <= 0f) {
        s6RingTimer = S6_RING_INTERVAL
        fireS6CoreRing(lensX, lensY)
      }
      return
    }
    spiralAngle += S6_SPIRAL_SPIN * dt
    if (spiralAngle > 6.2831855f) {
      spiralAngle -= 6.2831855f
    }
    spiralGate -= dt
    if (spiralGate <= 0f) {
      spiralGate = S6_SPIRAL_INTERVAL
      val a0 = spiralAngle
      val a1 = spiralAngle + S6_HELIX_A1
      val a2 = spiralAngle + S6_HELIX_A2
      val a3 = spiralAngle + S6_HELIX_A3
      val spd = S6_SPIRAL_SPEED
      fireBullet(lensX, lensY, cos(a0) * spd, sin(a0) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(a1) * spd, sin(a1) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(a2) * spd, sin(a2) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(a3) * spd, sin(a3) * spd, EnemyBullet.FLAG_PINK)
      val cc0 = -spiralAngle
      val cc1 = -spiralAngle + S6_HELIX_A1
      val cc2 = -spiralAngle + S6_HELIX_A2
      val cc3 = -spiralAngle + S6_HELIX_A3
      fireBullet(lensX, lensY, cos(cc0) * spd, sin(cc0) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(cc1) * spd, sin(cc1) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(cc2) * spd, sin(cc2) * spd, EnemyBullet.FLAG_PINK)
      fireBullet(lensX, lensY, cos(cc3) * spd, sin(cc3) * spd, EnemyBullet.FLAG_PINK)
    }
  }

  private fun fireS6AimedTriple(muzzleX: Float, muzzleY: Float, playerX: Float, playerY: Float) {
    val dx = playerX - muzzleX
    val dy = playerY - muzzleY
    val angle = atan2(dy, dx)
    val vx = cos(angle) * S6_BURST_SPEED
    val vy = sin(angle) * S6_BURST_SPEED
    val px = -sin(angle) * S6_PARALLEL_SEP
    val py = cos(angle) * S6_PARALLEL_SEP
    fireBullet(muzzleX - px, muzzleY - py, vx, vy, EnemyBullet.FLAG_CYAN)
    fireBullet(muzzleX, muzzleY, vx, vy, EnemyBullet.FLAG_CYAN)
    fireBullet(muzzleX + px, muzzleY + py, vx, vy, EnemyBullet.FLAG_CYAN)
  }

  private fun fireS6CoreColumn(lensX: Float, lensY: Float) {
    var i = 0
    while (i < S6_CORE_SHOTS) {
      val ox = lensX + (i.toFloat() - 1.5f) * S6_CORE_SEP
      fireBullet(ox, lensY, 0f, S6_CORE_VY, EnemyBullet.FLAG_PINK)
      i++
    }
  }

  private fun fireS6CoreRing(originX: Float, originY: Float) {
    var i = 0
    while (i < S6_RING_COUNT) {
      val ang = i * S6_RING_STEP
      val vx = cos(ang) * S6_RING_SPEED
      val vy = sin(ang) * S6_RING_SPEED
      fireBullet(originX, originY, vx, vy, EnemyBullet.FLAG_PINK)
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
        val cullX = if ((b.flags and EnemyBullet.FLAG_LASER) != 0) S6_LASER_HW else r
        val cullY = if ((b.flags and EnemyBullet.FLAG_LASER) != 0) S6_LASER_HH else r
        if (b.x + cullX < 0f || b.x - cullX > w || b.y + cullY < 0f || b.y - cullY > h) {
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
        val laser = (b.flags and EnemyBullet.FLAG_LASER) != 0
        val pink = (b.flags and EnemyBullet.FLAG_PINK) != 0
        val cyan = (b.flags and EnemyBullet.FLAG_CYAN) != 0
        if (laser) {
          drawRect.set(b.x - S6_LASER_HW, b.y - S6_LASER_HH, b.x + S6_LASER_HW, b.y + S6_LASER_HH)
          canvas.drawRect(drawRect, laserPaint)
        } else {
          val fill = if (pink) pinkPaint else if (cyan) cyanPaint else bulletPaint
          drawRect.set(b.x - hw, b.y - hh, b.x + hw, b.y + hh)
          canvas.drawOval(drawRect, fill)
          drawRect.set(b.x - hw + 5f, b.y - hh + 5f, b.x + hw - 5f, b.y + hh - 5f)
          canvas.drawOval(drawRect, innerGlowPaint)
        }
      }
      i++
    }
  }

  companion object {
    const val POOL_SIZE = 720
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
    const val S6_RAIL_INTERVAL = 1.4f
    const val S6_BURST_COUNT = 3
    const val S6_BURST_GAP = 0.08f
    const val S6_BURST_SPEED = 500f
    const val S6_OVERCHARGE_INTERVAL = 0.4f
    const val S6_STREAM_SPEED = 500f
    const val S6_WAVE_INTERVAL = 2.5f
    const val S6_PARALLEL_SEP = 10f
    const val S6_CORE_SHOTS = 4
    const val S6_CORE_SEP = 11f
    const val S6_CORE_VY = 220f
    const val S6_RING_INTERVAL = 1.8f
    const val S6_SPIRAL_INTERVAL = 0.08f
    const val S6_SPIRAL_SPIN = 9.5f
    const val S6_HELIX_A1 = 1.57f
    const val S6_HELIX_A2 = 3.14f
    const val S6_HELIX_A3 = 4.71f
    const val S6_LASER_HW = 28f
    const val S6_LASER_HH = 6f
    const val S6_RING_COUNT = 12
    const val S6_RING_STEP = 0.5235988f
    const val S6_RING_SPEED = 340f
    const val S6_SPIRAL_SPEED = 480f
  }
}
